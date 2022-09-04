package com.practise.clientV2;

import com.practise.clientV2.connect.ConnectionManager;
import com.practise.clientV2.discovery.ServiceDiscovery;
import com.practise.clientV2.proxy.ObjectProxy;
import com.practise.clientV2.proxy.RpcService;
import com.practise.common.anotations.RpcAutowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author HzeLng
 * @version 1.0
 * @description RpcClient
 * @date 2022/3/5 22:30
 *
 * RPC 客户端
 */
public class RpcClient implements ApplicationContextAware, DisposableBean {
    private static final Logger logger = LoggerFactory.getLogger(RpcClient.class);

    private ServiceDiscovery serviceDiscovery;
    /**
     * 创建线程池，复用 主要用来异步获取返回结果
     */
    private static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(16, 16,
            600L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(1000));

    /**
     * 基础版：
     *      服务启动时，容器加载，ServiceDiscovery被注入成bean（这里具体涉及到Spring 容器相关的，还没学，可能不准确）
     *      所以在容器加载后，便会初始化这个bean 然后就会取zk服务器上，根据指定路径去拿到对应的结点数据
     *      结点数据 就是提供该服务的host
     *      所以一开始就拿到host信息
     * V2版：
     *      也是一开始就指定zk服务器的地址
     *      然后连接上zk服务器，从zk服务器上拉取host信息
     *      并通过ConnectionManager，进行连接并统一管理建立好的连接
     * @param address
     */
    public RpcClient(String address) {
        logger.info("RpcClient-constructor");
        this.serviceDiscovery = new ServiceDiscovery(address);
    }

    @SuppressWarnings("unchecked")
    public static <T, P> T createService(Class<T> interfaceClass, String version) {
        logger.info("RpcClient-createService: createService ");
        return (T) Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass},
                new ObjectProxy<T, P>(interfaceClass, version)
        );
    }

    public static <T, P> RpcService createAsyncService(Class<T> interfaceClass, String version) {
        return new ObjectProxy<T, P>(interfaceClass, version);
    }

    /**
     * 提交的是什么任务?
     * 在RpcFuture中调用，用来提交 获取返回结果的 任务
     * @param task
     */
    public static void submit(Runnable task) {
        threadPoolExecutor.submit(task);
    }

    public void stop() {
        logger.info("RpcClient-stop");
        threadPoolExecutor.shutdown();
        serviceDiscovery.stop();
        ConnectionManager.getInstance().stop();
    }

    @Override
    public void destroy() throws Exception {
        this.stop();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            Object bean = applicationContext.getBean(beanName);
            Field[] fields = bean.getClass().getDeclaredFields();
            try {
                for (Field field : fields) {
                    RpcAutowired rpcAutowired = field.getAnnotation(RpcAutowired.class);
                    if (rpcAutowired != null) {
                        String version = rpcAutowired.version();
                        field.setAccessible(true);
                        field.set(bean, createService(field.getType(), version));
                    }
                }
            } catch (IllegalAccessException e) {
                logger.error(e.toString());
            }
        }
    }
}
