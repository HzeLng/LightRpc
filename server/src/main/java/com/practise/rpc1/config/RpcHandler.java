package com.practise.rpc1.config;

import com.practise.common.entity.RpcRequest;
import com.practise.common.entity.RpcResponse;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cglib.reflect.FastClass;
import org.springframework.cglib.reflect.FastMethod;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Map;

/**
 * @author HzeLng
 * @version 1.0
 * @description RpcHandler
 * @date 2022/3/4 16:08
 */
public class RpcHandler extends SimpleChannelInboundHandler<RpcRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcHandler.class);

    /**
     * 前面服务端存储的 接口和服务实现对象的map映射
     * 在这里服务端接收到远程客户端的请求，根据它请求的接口
     * 找到对应的服务实现类，然后反射本地执行
     * 执行完后，将结果返回
     */
    private final Map<String, Object> handlerMap;

    public RpcHandler(Map<String, Object> handlerMap) {
        this.handlerMap = handlerMap;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcRequest rpcRequest) throws Exception {
        System.out.println("channelRead0");
        System.out.println("==== APP  STARTED ====");
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        String name = runtime.getName();
        System.out.println(name);
        System.out.println("Process ID: " + name.substring(0, name.indexOf("@")));
        // 在这里可以看到 channelRead0的线程是另起一个线程 nioEventLoopGroup-3-1
        System.out.println(Thread.currentThread().getName());

        LOGGER.info("channelRead0: rpcRequest.getRequestId {}",rpcRequest.getRequestId());
        LOGGER.info("channelRead0: rpcRequest.getClassName {}",rpcRequest.getClassName());
        LOGGER.info("channelRead0: rpcRequest.getClass {}",rpcRequest.getClass());
        LOGGER.info("channelRead0: rpcRequest.getParameterTypes {}",rpcRequest.getParameterTypes());
        LOGGER.info("channelRead0: rpcRequest.getParameters {}",rpcRequest.getParameters());
        LOGGER.info("channelRead0: rpcRequest.getMethodName {}",rpcRequest.getMethodName());
        RpcResponse response = new RpcResponse();
        response.setRequestId(rpcRequest.getRequestId());
        try {
            Object result = handle(rpcRequest);
            response.setResult(result);
        } catch (Throwable t) {
            response.setError(t);
        }
        LOGGER.info("channelRead0 response.getResult() is {}",response.getResult());
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * 在服务端找到接口对应的服务实现类
     * 反射
     * 在服务端执行，并返回结果
     * @param request
     * @return
     * @throws Throwable
     */
    private Object handle(RpcRequest request) throws Throwable {
        String className = request.getClassName();
        Object serviceBean = handlerMap.get(className);
        LOGGER.info("handle: the service bean {}", serviceBean);

        Class<?> serviceClass = serviceBean.getClass();
        String methodName = request.getMethodName();
        Class<?>[] parameterTypes = request.getParameterTypes();
        Object[] parameters = request.getParameters();
        LOGGER.info("the rpcRequest is {}", parameters);

        /*Method method = serviceClass.getMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(serviceBean, parameters);*/

        FastClass serviceFastClass = FastClass.create(serviceClass);
        FastMethod serviceFastMethod = serviceFastClass.getMethod(methodName, parameterTypes);
        return serviceFastMethod.invoke(serviceBean, parameters);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("server caught exception", cause);
        ctx.close();
    }
}
