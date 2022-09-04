package com.practise.client;

import com.practise.common.entity.RpcRequest;
import com.practise.common.entity.RpcResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;

/**
 * @author HzeLng
 * @version 1.0
 * @description RpcProxy
 * @date 2022/3/4 16:54
 */
@Component
public class RpcProxy {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcProxy.class);

    @Autowired
    private ServiceDiscovery serviceDiscovery;

    private String serverAddress;

    public RpcProxy() {
        LOGGER.info("client:RpcProxy`s constructor");
    }

    /**
     * 创建客户端stub 应该是这个名字
     * 创建客户端的代理对象，它帮客户端调用远程请求，然后获得结果并返回给客户端
     *
     * @param targetInterfaceClass 参数指明接口类型
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T create(Class<?> targetInterfaceClass){
        return (T) Proxy.newProxyInstance(
                targetInterfaceClass.getClassLoader(),
                new Class<?>[]{targetInterfaceClass},
                new InvocationHandler() {
                    /**
                     *
                     * @param proxy
                     * @param method 这里的method应该就是外面那个 HelloService 代理对象发起了.hello()这个方法
                     * @param args
                     * @return
                     * @throws Throwable
                     */
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                        LOGGER.info("cline-invoke: the proxyObject is {}",proxy.getClass());

                        // 创建并初始化RPC请求
                        RpcRequest rpcRequest = new RpcRequest();
                        // 请求ID必须有，因为同一个客户端和同一个服务端之间可能有多次RPC请求与相应，需要表示每次请求
                        rpcRequest.setRequestId(UUID.randomUUID().toString());
                        // 声明被调用的方法所属的类，这样服务端才知道是要调用哪一个接口
                        rpcRequest.setClassName(method.getDeclaringClass().getName());
                        // 声明被调用的是哪一个方法，这样服务端才知道是接口中的哪一个方法
                        rpcRequest.setMethodName(method.getName());
                        // 声明被调用的方法需要的参数类型，服务端那边要“代加工”帮忙执行，那就还得知道参数类型是什么
                        rpcRequest.setParameterTypes(method.getParameterTypes());
                        // 声明被调用的方法需要的参数，服务端那边要“代加工”帮忙执行，那就还得知道参数值具体是什么
                        rpcRequest.setParameters(args);

                        if(serviceDiscovery != null){
                            // 拿到zk服务器上存着的 提供此项服务的地址
                            // 所以client必须知道自己要调的这些服务 哪些主机有在提供
                            // 或者说 可以根据要调用的方法，映射到zk上对应的node
                            // 然后再到这个node上拿数据，这些数据就是提供服务的主机所在地址
                            serverAddress = serviceDiscovery.discover();
                            // 拿到服务主机地址了，准备连接并且发送请求
                            String[] array = serverAddress.split(":");
                            String host = array[0];
                            int port = Integer.valueOf(array[1]);
                            // netty客户端准备 connect to the 服务器
                            // 但是直接在这里进行netty客户端那一大串代码，不好看
                            // 所以封装一个RPC客户端，专门用来发送请求的，并且接收响应
                            RpcClient rpcClient = new RpcClient(host,port);
                            RpcResponse rpcResponse = rpcClient.send(rpcRequest);
                            LOGGER.info("client: rpcRequest.getRequestId {}",rpcRequest.getRequestId());
                            LOGGER.info("client: rpcRequest.getClassName {}",rpcRequest.getClassName());
                            LOGGER.info("client: rpcRequest.getClass {}",rpcRequest.getClass());
                            LOGGER.info("client: rpcRequest.getParameterTypes {}",rpcRequest.getParameterTypes());
                            LOGGER.info("client: rpcRequest.getParameters {}",rpcRequest.getParameters());
                            LOGGER.info("client: rpcRequest.getMethodName {}",rpcRequest.getMethodName());
                            return rpcResponse.getResult();
                        }
                        else{
                            System.out.println("serviceDiscovery is null");
                            return null;
                        }


                    }
                }
        );
    }

}
