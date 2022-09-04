package com.practise.clientV2.connect;

import com.practise.clientV2.handler.RpcClientHandler;
import com.practise.clientV2.handler.RpcClientInitializer;
import com.practise.clientV2.route.RpcLoadBalance;
import com.practise.clientV2.route.RpcLoadBalanceRandom;
import com.practise.common.protocol.RpcProtocol;
import com.practise.common.protocol.RpcServiceInfo;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author HzeLng
 * @version 1.0
 * @description ConnectionManager
 * @date 2022/3/5 22:37
 */
public class ConnectionManager {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

    private EventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);
    /**
     * 线程池
     * 复用线程池
     * 在多个地方都用到：
     *                  1. 连接新的服务器 主要功能，连接提供服务的服务端
     */
    private static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(4, 8,
            600L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(1000));

    /**
     * key值是服务功能
     * value值是客户端与服务端连接建立成功后的
     */
    private Map<RpcProtocol, RpcClientHandler> connectedServerNodes = new ConcurrentHashMap<>();
    /**
     * 本地记录的 服务请求信息 因为RpcProtocol 所以就是结点的数据信息
     * 另一方面写时复制（所以在写更新的时候，这时候，如果客户端使用ConnectionManager通过这里的rpcProtocolSet的元素RpcProtocol
     * 选择handler进行连接时，读到的应该是最后一次更新的数据，不包括当前正在更新的数据）
     * 为什么不用并发安全的包呢？可能阻塞？再去了解了解并发安全的包，读写时的情况
     */
    private CopyOnWriteArraySet<RpcProtocol> rpcProtocolSet = new CopyOnWriteArraySet<>();
    private ReentrantLock lock = new ReentrantLock();
    private Condition connected = lock.newCondition();
    private long waitTimeout = 5000;
    private RpcLoadBalance loadBalance = new RpcLoadBalanceRandom();
    private volatile boolean isRunning = true;

    private ConnectionManager() {
    }

    /**
     * 单例模式 一个RpcClient只能有一个 ConnectionManager
     * 注意是静态内部类构造
     */
    private static class SingletonHolder {
        private static final ConnectionManager instance = new ConnectionManager();
    }


    public static ConnectionManager getInstance() {
        return SingletonHolder.instance;
    }

    /**
     * 根据ServiceDiscovery拉取得到的服务器host信息serviceList
     * 注意获取的所有data，也就是可能多个服务主机都提供服务，那同样的服务都建立连接，是不是太浪费了，想一想优化
     * 更新可连接的服务器（或者说建立连接请求）
     * @param serviceList
     */
    public void updateConnectedServer(List<RpcProtocol> serviceList) {
        logger.info("ConnectionManager-updateConnectedServer:");
        // Now using 2 collections to manage the service info and TCP connections because making the connection is async
        // Once service info is updated on ZK, will trigger this function
        // Actually client should only care about the service it is using
        if (serviceList != null && serviceList.size() > 0) {
            // Update local server nodes cache
            // hashset filter the duplicated RpcProtocol
            HashSet<RpcProtocol> serviceSet = new HashSet<>(serviceList.size());
            for (int i = 0; i < serviceList.size(); ++i) {
                RpcProtocol rpcProtocol = serviceList.get(i);
                serviceSet.add(rpcProtocol);
            }

            // Add new server info
            for (final RpcProtocol rpcProtocol : serviceSet) {
                // 如果当前本地的记录里没有，代表是new server info
                if (!rpcProtocolSet.contains(rpcProtocol)) {
                    connectServerNode(rpcProtocol);
                }
            }

            // Close and remove invalid server nodes
            for (RpcProtocol rpcProtocol : rpcProtocolSet) {
                if (!serviceSet.contains(rpcProtocol)) {
                    logger.info("Remove invalid service: " + rpcProtocol.toJson());
                    removeAndCloseHandler(rpcProtocol);
                }
            }
        } else {
            // No available service
            logger.error("No available service!");
            for (RpcProtocol rpcProtocol : rpcProtocolSet) {
                removeAndCloseHandler(rpcProtocol);
            }
        }
    }


    public void updateConnectedServer(RpcProtocol rpcProtocol, PathChildrenCacheEvent.Type type) {
        if (rpcProtocol == null) {
            return;
        }
        if (type == PathChildrenCacheEvent.Type.CHILD_ADDED && !rpcProtocolSet.contains(rpcProtocol)) {
            connectServerNode(rpcProtocol);
        } else if (type == PathChildrenCacheEvent.Type.CHILD_UPDATED) {
            //TODO We may don't need to reconnect remote server if the server'IP and server'port are not changed
            removeAndCloseHandler(rpcProtocol);
            connectServerNode(rpcProtocol);
        } else if (type == PathChildrenCacheEvent.Type.CHILD_REMOVED) {
            removeAndCloseHandler(rpcProtocol);
        } else {
            throw new IllegalArgumentException("Unknow type:" + type);
        }
    }

    /**
     * 根据指定RpcProtocol 发起连接请求
     * 并保存通道，因为要弄成长连接
     * @param rpcProtocol
     */
    private void connectServerNode(RpcProtocol rpcProtocol) {
        if (rpcProtocol.getServiceInfoList() == null || rpcProtocol.getServiceInfoList().isEmpty()) {
            logger.info("No service on node, host: {}, port: {}", rpcProtocol.getHost(), rpcProtocol.getPort());
            return;
        }
        // 本地的写入记录
        rpcProtocolSet.add(rpcProtocol);
        logger.info("New service node, host: {}, port: {}", rpcProtocol.getHost(), rpcProtocol.getPort());
        for (RpcServiceInfo serviceProtocol : rpcProtocol.getServiceInfoList()) {
            logger.info("New service info, name: {}, version: {}", serviceProtocol.getServiceName(), serviceProtocol.getVersion());
        }
        final InetSocketAddress remotePeer = new InetSocketAddress(rpcProtocol.getHost(), rpcProtocol.getPort());
        threadPoolExecutor.submit(new Runnable() {
            @Override
            public void run() {
                logger.info("ConnectionManager-connectServerNode：线程池提交 连接服务端的任务");
                Bootstrap b = new Bootstrap();
                b.group(eventLoopGroup)
                        .channel(NioSocketChannel.class)
                        .handler(new RpcClientInitializer());

                ChannelFuture channelFuture = b.connect(remotePeer);
                // 监听器，监听连接建立成功事件
                channelFuture.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(final ChannelFuture channelFuture) throws Exception {
                        if (channelFuture.isSuccess()) {
                            logger.info("Successfully connect to remote server, remote peer = " + remotePeer);
                            RpcClientHandler handler = channelFuture.channel().pipeline().get(RpcClientHandler.class);
                            // 连接保存在本地，复用。但是为什么是拿Handler呢
                            // handler又在哪里使用呢？
                            connectedServerNodes.put(rpcProtocol, handler);
                            // 在这个通道里告诉这个连接建立的通道是服务于哪个rpcprotocol
                            handler.setRpcProtocol(rpcProtocol);
                            signalAvailableHandler();
                        } else {
                            logger.error("Can not connect to remote server, remote peer = " + remotePeer);
                        }
                    }
                });
            }
        });
    }

    /**
     * 唤醒处于等待队列的线程
     * 也就是说 如果当前线程在chooseHandler的时候，发现size<=0，没有可用的连接时
     * 就会等待，直到新的连接建立成功
     * 当进入到这个函数，表明已经有连接建立成功了
     * 那就条件队列唤醒当前线程
     * 并让其重新回到就绪队列等待CPU调度
     */
    private void signalAvailableHandler() {
        lock.lock();
        try {
            connected.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 还没建立连接，所以利用AQS的条件变量，等待
     * 进入条件队列
     * @return
     * @throws InterruptedException
     */
    private boolean waitingForHandler() throws InterruptedException {
        lock.lock();
        try {
            logger.warn("Waiting for available service");
            return connected.await(this.waitTimeout, TimeUnit.MILLISECONDS);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 选择handler
     * @param serviceKey
     * @return
     * @throws Exception
     */
    public RpcClientHandler chooseHandler(String serviceKey) throws Exception {
        int size = connectedServerNodes.values().size();
        // 如果size为0，表示当前客户端还没有和 能够提供serviceKey对应功能的任何一个服务器建立连接
        while (isRunning && size <= 0) {
            try {
                // 那就等待
                waitingForHandler();
                // 再一次判断，因为可能被中断唤醒，size还是<=0
                size = connectedServerNodes.values().size();
            } catch (InterruptedException e) {
                logger.error("Waiting for available service is interrupted!", e);
            }
        }
        // 先负载均衡，得到对应的rpcProtocol
        // rpcProtocol内包含远端服务器host信息
        RpcProtocol rpcProtocol = loadBalance.route(serviceKey, connectedServerNodes);
        // 再根据 rpcProtocol 获取对应的handler
        RpcClientHandler handler = connectedServerNodes.get(rpcProtocol);
        if (handler != null) {
            return handler;
        } else {
            throw new Exception("Can not get available connection");
        }
    }

    /**
     * 移除已经不提供服务的host
     * 什么情况下会不提供服务呢？
     *          应该是zk检测到服务端下线了还是怎么的，得再看看服务端那边怎么写的
     *          反正结果呈现就是 从zk拉取下来的最新serviceInfo 和当前本地存储的serviceInfo进行比对
     * @param rpcProtocol
     */
    private void removeAndCloseHandler(RpcProtocol rpcProtocol) {
        RpcClientHandler handler = connectedServerNodes.get(rpcProtocol);
        if (handler != null) {
            handler.close();
        }
        connectedServerNodes.remove(rpcProtocol);
        rpcProtocolSet.remove(rpcProtocol);
    }

    /**
     * 仅仅remove而没有handler.close
     * 在handler的 channelInactive 方法中调用的
     * 什么时候会触发handler的channelInactive：
     *      1. 客户端发送关闭帧
     *      2. 客户端结束进程
     *      3. 服务端主动调用channel.close()
     * 在这里应该是第三种情况，所以不用在客户端选择handler.close()
     * @param rpcProtocol
     */
    public void removeHandler(RpcProtocol rpcProtocol) {
        rpcProtocolSet.remove(rpcProtocol);
        connectedServerNodes.remove(rpcProtocol);
        logger.info("Remove one connection, host: {}, port: {}", rpcProtocol.getHost(), rpcProtocol.getPort());
    }

    public void stop() {
        isRunning = false;
        for (RpcProtocol rpcProtocol : rpcProtocolSet) {
            removeAndCloseHandler(rpcProtocol);
        }
        signalAvailableHandler();
        threadPoolExecutor.shutdown();
        eventLoopGroup.shutdownGracefully();
    }
}