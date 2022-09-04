package com.practise.rpc1.config;

/**
 * @author HzeLng
 * @version 1.0
 * @description RpcServer
 * @date 2022/3/4 15:17
 */

import com.practise.common.entity.RpcRequest;
import com.practise.common.entity.RpcResponse;
import com.practise.common.utils.RpcDecoder;
import com.practise.common.utils.RpcEncoder;
import com.practise.rpc1.annotations.RpcService;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.HashMap;
import java.util.Map;

/**
 * 使用Netty实现一个可支持NIO的RPC服务器，需要使用ServiceRegistry注册服务地址
 *
 * 这两个实现接口的作用是什么？
 */
@Component
public class RpcServer implements ApplicationContextAware, InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcServer.class);

    private  String serverAddress;
    @Autowired
    private ServiceRegistry serviceRegistry;

    /**
     * 存放接口名与服务对象之间的映射关系
     * 可以直接根据接口名直接找到对应的实现服务类
     */
    private Map<String, Object> handlerMap = new HashMap<>();

    public RpcServer() {
        LOGGER.info("server: RpcServer`s constructor ");
        serverAddress = "127.0.0.1:8000";
        LOGGER.info("server: the serverAddress is {}", serverAddress);
    }

    //    public RpcServer(String serverAddress, ServiceRegistry serviceRegistry) {
//        this.serverAddress = serverAddress;
//        this.serviceRegistry = serviceRegistry;
//    }

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("afterPropertiesSet");
        System.out.println("==== APP  STARTED ====");
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        String name = runtime.getName();
        System.out.println(name);
        System.out.println("Process ID: " + name.substring(0, name.indexOf("@")));
        System.out.println(Thread.currentThread().getName());

        // 初始化netty
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try{
            ServerBootstrap bootstrap = new ServerBootstrap()
                    .group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline()
                                    .addLast(new RpcDecoder(RpcRequest.class))
                                    .addLast(new RpcEncoder(RpcResponse.class))
                                    .addLast(new RpcHandler(handlerMap));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    // 长连接
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            String[] array = serverAddress.split(":");
            String host = array[0];
            int port = Integer.parseInt(array[1]);
            // 监听端口
            ChannelFuture future = bootstrap.bind(host, port).sync();
            LOGGER.debug("server started on port {}", port);

            if (serviceRegistry != null) {
                // 注册服务地址
                serviceRegistry.registry(serverAddress);
            }
            future.channel().closeFuture().sync();

        }
        finally {
            // 主动关闭netty
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

        // 获取所有带有 RpcService 注解的 Spring Bean
        Map<String, Object> serviceBeanMap = applicationContext.getBeansWithAnnotation(RpcService.class);
        if (MapUtils.isNotEmpty(serviceBeanMap)) {
            for (Object serviceBean : serviceBeanMap.values()) {
                String interfaceName = serviceBean.getClass().getAnnotation(RpcService.class).value().getName();
                LOGGER.info("setApplicationContext, interfaceName {}", interfaceName);
                LOGGER.info("setApplicationContext, serviceBean {}", serviceBean);
                handlerMap.put(interfaceName, serviceBean);
            }
        }
    }
}
