package com.practise.serverv2.core;

import com.practise.common.entity.Beat;
import com.practise.common.entity.RpcRequest;
import com.practise.common.entity.RpcResponse;
import com.practise.common.utils.ServiceUtil;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cglib.reflect.FastClass;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author HzeLng
 * @version 1.0
 * @description RpcServerHandler
 * @date 2022/3/5 21:13
 */
public class RpcServerHandler extends SimpleChannelInboundHandler<RpcRequest> {

    private static final Logger logger = LoggerFactory.getLogger(RpcServerHandler.class);

    private final Map<String, Object> handlerMap;
    private final ThreadPoolExecutor serverHandlerPool;

    public RpcServerHandler(Map<String, Object> handlerMap, final ThreadPoolExecutor threadPoolExecutor) {
        this.handlerMap = handlerMap;
        this.serverHandlerPool = threadPoolExecutor;
    }

    /**
     * 服务端接收到客户端请求后，将请求提交给线程池异步处理
     * 为什么要线程池，因为netty的工作线程组也就你一开始设定的。
     * 如果和服务端建立的连接数channel超过 工作线程组的线程数
     * 那么这些线程会复用
     * 那么在高并发情况下， 就有可能出现多个channel在等一个channel释放线程资源
     * 如果当前使用线程的这个channel处理事务很耗时，那后面的都得排队等
     * 因此将处理事务的部分提交给线程池异步处理。
     * 线程池异步处理完了呢？因为是在channelRead0里提交的， 所以可以直接使用ctx返回处理结果writeAndFlush
     * @param ctx
     * @param request
     */
    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final RpcRequest request) {
        logger.info("channelRead0 current Thread {}",Thread.currentThread().getName());
        logger.info("ctx is {} and the channel is {}",ctx,ctx.channel());
        // filter beat ping
        // 心跳检测，服务端一般不做回应，为了性能和服务端压力考虑
        if (Beat.BEAT_ID.equalsIgnoreCase(request.getRequestId())) {
            logger.info("Server read heartbeat ping");
            logger.info("the channel is {}",ctx.channel());
            return;
        }

        // 从基础版的测试可以知道
        // channelRead0已经是一个nio线程了
        // 这里再用线程池有必要吗？后续再找找原因
        serverHandlerPool.execute(new Runnable() {
            @Override
            public void run() {

                logger.info("RpcServerHandler-channelRead0: Receive request " + request.getRequestId());
                logger.info("channelRead0-run: current Thread {}",Thread.currentThread().getName());
                RpcResponse response = new RpcResponse();
                response.setRequestId(request.getRequestId());
                try {
                    Object result = handle(request);
                    response.setResult(result);
                    logger.info("request.getMethodName() {}",request.getMethodName());
                    logger.info("request.getParameters() {}",request.getParameters());
                    logger.info("response.getResult() {}",response.getResult());
                } catch (Throwable t) {
                    response.setError(t);
                    logger.error("RpcServerHandler-channelRead0: RPC Server handle request error", t);
                }
                // 这里和原文不同
                // 原文监听的是 ChannelFutureListener.CLOSE 关闭事件
                // 而这里监听的仅仅是刚刚的发送操作完成事件
                // 目的就是为了保持长连接
                // 不过服务端可能一般不会主动关闭
                // 应该是客户端那边没有立即关闭
                // 在这里ctx直接复用，不用显示传引用，即使在不同的线程
                ctx.writeAndFlush(response).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        logger.info("Send response for request " + request.getRequestId());
                    }
                });
            }
        });
    }

    /**
     * 那么具体是如何处理的呢？
     * 解析RpcRequest，得到这次请求的接口名，方法名，参数信息
     * 然后根据接口名 到 保存在本地的Map 根据接口名这个key 取得对应的实现这个接口的bean对象
     * 然后得到这个bean对象的class，根据反射，调用Method.invoke()
     * 返回结果
     * @param request
     * @return
     * @throws Throwable
     */
    private Object handle(RpcRequest request) throws Throwable {
        String className = request.getClassName();
        String version = request.getVersion();
        String serviceKey = ServiceUtil.makeServiceKey(className, version);
        Object serviceBean = handlerMap.get(serviceKey);
        logger.info("");
        if (serviceBean == null) {
            logger.error("RpcServerHandler-handle: Can not find service implement with interface name: {} and version: {}", className, version);
            return null;
        }

        Class<?> serviceClass = serviceBean.getClass();
        String methodName = request.getMethodName();
        Class<?>[] parameterTypes = request.getParameterTypes();
        Object[] parameters = request.getParameters();

        logger.debug(serviceClass.getName());
        logger.debug(methodName);
        for (int i = 0; i < parameterTypes.length; ++i) {
            logger.debug(parameterTypes[i].getName());
        }
        for (int i = 0; i < parameters.length; ++i) {
            logger.debug(parameters[i].toString());
        }

        // JDK reflect
//        Method method = serviceClass.getMethod(methodName, parameterTypes);
//        method.setAccessible(true);
//        return method.invoke(serviceBean, parameters);

        // Cglib reflect
        FastClass serviceFastClass = FastClass.create(serviceClass);
//        FastMethod serviceFastMethod = serviceFastClass.getMethod(methodName, parameterTypes);
//        return serviceFastMethod.invoke(serviceBean, parameters);
        // for higher-performance
        int methodIndex = serviceFastClass.getIndex(methodName, parameterTypes);
        return serviceFastClass.invoke(methodIndex, serviceBean, parameters);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.warn("Server caught exception: " + cause.getMessage());
        ctx.close();
    }

    /**
     * 了解一下netty如何实现心跳机制的
     * @param ctx
     * @param evt
     * @throws Exception
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            ctx.channel().close();
            logger.info("the closed channel is {}",ctx.channel());
            logger.warn("Channel idle in last {} seconds, close it", Beat.BEAT_TIMEOUT);
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
