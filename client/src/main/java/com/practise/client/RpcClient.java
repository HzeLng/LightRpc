package com.practise.client;

import com.practise.common.Serializer.protostuff.ProtostuffSerializer;
import com.practise.common.entity.RpcRequest;
import com.practise.common.entity.RpcResponse;
import com.practise.common.utils.RpcDecoder;
import com.practise.common.utils.RpcEncoder;
import com.practise.common.utils.Serializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author HzeLng
 * @version 1.0
 * @description RpcClient
 * @date 2022/3/4 17:23
 */

/**
 * 为什么继承这个类就能实现客户端的功能呢？还得再去多了解netty
 */
public class RpcClient extends SimpleChannelInboundHandler<RpcResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcClient.class);

    private String host;
    private int port;

    private RpcResponse response;

    /**
     * 锁对象
     * 在RpcClient发送请求，等待响应的这段期间
     * 应该让当前线程处于等待
     * 这里又牵扯到netty另一个知识点
     * 下面那个channelRead0方法应该是另起一个线程？
     * 不然这个锁 不合逻辑啊
     */
    private final Object obj = new Object();

    public RpcClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * 接收到数据（就是响应）后的处理
     * @param channelHandlerContext
     * @param rpcResponse
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcResponse rpcResponse) throws Exception {
        // 但是既然不是同一个线程
        // 虽然是实例变量
        // 但是另一个线程可以访问吗？这应该就是涉及到netty的特殊机制了，应该是之前B站看到过的
        this.response = rpcResponse;
        // 因为已经接受到请求了
        // 可以唤醒前面 等待的 线程了
        LOGGER.info("channelRead0 {}",rpcResponse.getResult());
        synchronized (obj){
            LOGGER.info("client: get the response, wake up the thread");
            obj.notifyAll();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("client caught exception", cause);
        ctx.close();
    }

    public RpcResponse send(RpcRequest rpcRequest) throws InterruptedException, IllegalAccessException, InstantiationException {
        // 序列化器
        Serializer serializer = null;
        // 真正的netty客户端那一大串代码
        EventLoopGroup group = new NioEventLoopGroup();
        try{
            // 服务端对应的是ServerBootStrap，启动器
            Bootstrap bootstrap = new Bootstrap();
            // 启动器绑定刚刚的工作组，并且指定channel的类型
            bootstrap.group(group).channel(NioSocketChannel.class)
                    // SocketChannel是顶层接口
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline()
                                    .addLast(new RpcEncoder(RpcRequest.class))
                                    .addLast(new RpcDecoder(RpcResponse.class))
                                    // 照道理应该和服务端一样addLast(new xxxHandler())
                                    // 但是这里为了图方便，直接就合二为一了
                                    // 干脆就复用这个类实例
                                    .addLast(RpcClient.this);
                        }
                    })
                    .option(ChannelOption.SO_KEEPALIVE, true);
            // 启动器初始化定义完了
            // 可以开始请求连接到服务器了
            ChannelFuture future = bootstrap.connect(host, port).sync();
            // 同步等待连接建立成功
            // 建立成功则发送请求权
            // 通过future这个管家，拿到channel，然后发送数据
            // LOGGER.info("the rpcRequest is {}",rpcRequest.getParameters());
            future.channel().writeAndFlush(rpcRequest);
            // 发送完rpc请求后就是 等待响应了
            // 一般的思路是自旋等待或者利用锁，阻塞等待
            // 这里应该不适合自旋等待，所以利用锁机制
            synchronized (obj){
                LOGGER.info("client: waiting the response...");
                // 既然这里选择用了阻塞等待，并且释放锁（object.wait()释放锁，await也释放）
                // 然后再通过channelRead0那里去唤醒
                // 说明两个方法不是同一个线程，也就是说channelRead0会再起一个线程
                obj.wait();
            }
            if(response != null){
                // 同步等待通道关闭
                future.channel().closeFuture().sync();
            }
            // 等到通道关闭后再返回
            LOGGER.info("before close the channel {}",response.getResult());
            return response;
        }finally {
            // 后续关闭资源
            group.shutdownGracefully();
        }
    }
}
