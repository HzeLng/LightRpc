package com.practise.serverv2.core;

import com.practise.common.utils.ServiceUtil;
import com.practise.common.utils.ThreadPoolUtil;
import com.practise.serverv2.registry.ServiceRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLOutput;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author HzeLng
 * @version 1.0
 * @description NettyServer
 * @date 2022/3/5 17:27
 */
public class NettyServer extends Server{

    private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);

    private Thread thread;
    /**
     * 对外开放的host:port
     */
    private String serverAddress;
    /**
     * zk服务器的地址
     * 向zk服务器注册服务
     */
    private ServiceRegistry serviceRegistry;
    private Map<String, Object> serviceMap = new HashMap<>();

    public NettyServer(){
        logger.info("NettyServer`s constructor");
        String registryAddress = "127.0.0.1:2181";
        this.serverAddress = "127.0.0.1:8200";
        this.serviceRegistry = new ServiceRegistry(registryAddress);
    }

    public NettyServer(String serverAddress, String registryAddress) {
        this.serverAddress = serverAddress;
        this.serviceRegistry = new ServiceRegistry(registryAddress);
    }



    /**
     *
     * @param interfaceName
     * @param version
     * @param serviceBean
     */
    public void addService(String interfaceName, String version, Object serviceBean) {
        logger.info("Adding service, interface: {}, version: {}, bean：{}", interfaceName, version, serviceBean);
        // serviceKey:serviceName:version
        String serviceKey = ServiceUtil.makeServiceKey(interfaceName, version);
        serviceMap.put(serviceKey, serviceBean);
    }

    /**
     *
     * @throws Exception
     */
    @Override
    public void start() throws Exception {
        System.out.println(Thread.currentThread().getName());
        logger.info("NettyServer-start: start");
        thread = new Thread(new Runnable() {
            // 其实是在这个thread里面创建线程池
            // 最后调用destroy的时候，连这个线程池也一块了
            ThreadPoolExecutor threadPoolExecutor = ThreadPoolUtil.makeServerThreadPool(
                    NettyServer.class.getSimpleName(),16,32
            );

            @Override
            public void run() {
                logger.info("NettyServer-start:  netty server start ");
                EventLoopGroup bossGroup = new NioEventLoopGroup();
                EventLoopGroup workerGroup = new NioEventLoopGroup(16);
                try {
                    ServerBootstrap bootstrap = new ServerBootstrap();
                    bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                            .childHandler(new RpcServerInitializer(serviceMap, threadPoolExecutor))
                            .option(ChannelOption.SO_BACKLOG, 128)
                            // .option(ChannelOption.SO_RCVBUF,1024)
                            .childOption(ChannelOption.SO_KEEPALIVE, true);

                    String[] array = serverAddress.split(":");
                    String host = array[0];
                    int port = Integer.parseInt(array[1]);
                    ChannelFuture future = bootstrap.bind(host, port).sync();

                    if (serviceRegistry != null) {
                        serviceRegistry.registerService(host, port, serviceMap);
                    }
                    logger.info("Server started on port {}", port);
                    future.channel().closeFuture().sync();
                } catch (Exception e) {
                    if (e instanceof InterruptedException) {
                        logger.info("Rpc server remoting server stop");
                    } else {
                        logger.error("Rpc server remoting server error", e);
                    }
                } finally {
                    try {
                        logger.info("NettyServer-start-finally:");
                        serviceRegistry.unregisterService();
                        workerGroup.shutdownGracefully();
                        bossGroup.shutdownGracefully();
                    } catch (Exception ex) {
                        logger.error(ex.getMessage(), ex);
                    }
                }
            }
        });
        thread.start();
        logger.info("NettyServer-start: thread start" );
    }

    @Override
    public void stop() throws Exception {

        // destroy server thread
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }

    }
}
