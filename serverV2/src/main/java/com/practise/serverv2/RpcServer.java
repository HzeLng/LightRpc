package com.practise.serverv2;

import com.practise.serverv2.annotations.NettyRpcService;
import com.practise.serverv2.annotations.TestAnnotation;
import com.practise.serverv2.core.NettyServer;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author HzeLng
 * @version 1.0
 * @description RpcServer
 * @date 2022/3/5 21:59
 */
@Component
public class RpcServer extends NettyServer implements ApplicationContextAware, InitializingBean, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(RpcServer.class);

    public RpcServer(){
        super();
        logger.info("RpcServer`s constructor");
    }

    /**
     * 如果加了component注解，就不能使用这里的构造函数
     * 报错：
     *      RpcServer required a bean of type 'java.lang.String' that could not be found.
     * @param serverAddress
     * @param registryAddress
     */
    public RpcServer(String serverAddress, String registryAddress) {
        super(serverAddress, registryAddress);
}

    /**
     * 设置应用上下文环境肯定比 propertiesSet早
     *
     * 根据Spring中 bean生命周期的理解
     * 在整个bean的生命周期过程中，分为几个大阶段：
     *          1. 先对象实例化
     *          2. 对象属性赋值 除了基本的属性赋值，还有类内那些 @Autowired和@Value注解修饰的，以及 @Resource注解修饰的）
     *                  在bean初始化之前，会调用postProcessorBeforeInitialization 也就是在bean初始化之前再插手一下
     *                  在这里会检查当前bean是否实现了EnvironmentAware，ApplicationContextAware等接口
     *                  如果实现了这些接口，就会依次调用接口内的方法将Aware前缀对应的对象注入到bean实例中
     *                  比如这里的ApplicationContextAware，接口中的方法就是setApplicationContext(ApplicationContext ctx)
     *                  那就是将ApplicationContext这个对象注入到这个bean实例
     *                  就比如下面这样
     *
     *                  所以这个方法是在整个bean对象初始化之前做的
     *                  自然比afterPropertiesSet来的早（虽然PropertiesSet是第二步，而这个xxxAware接口是在第二步之后，但是afterPropertiesSet是在InitializingBean中做的）
     *          3. bean的初始化（也就是前面两步还是正常的Java对象实例化，到这里才开始算bean）
     *
     * @param ctx
     * @throws BeansException
     */
    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        logger.info("RpcServer-setApplicationContext: get all serviceBean");
        /**
         * key是服务实现类的名字 如 [helloServiceV2Impl, phoneServiceImpl]
         * value是对应的被注解修饰的Bean对象
         */
        Map<String, Object> serviceBeanMap = ctx.getBeansWithAnnotation(NettyRpcService.class);
        if (MapUtils.isNotEmpty(serviceBeanMap)) {
            for (Object serviceBean : serviceBeanMap.values()) {
                NettyRpcService nettyRpcService = serviceBean.getClass().getAnnotation(NettyRpcService.class);
                // 拿到此注解类，实现的接口名，以接口名为key值，serviceBean也就是服务实现类为value值存入哈希表
                String interfaceName = nettyRpcService.value().getName();
                String version = nettyRpcService.version();
                super.addService(interfaceName, version, serviceBean);
            }
            // see the key is [helloServiceV2Impl, phoneServiceImpl]
            logger.info("see the key is {}",serviceBeanMap.keySet());
        }

        logger.info("RpcServer-setApplicationContext: test for test annotation and test service");
        Map<String, Object> testServiceBeanMap = ctx.getBeansWithAnnotation(TestAnnotation.class);
        if (MapUtils.isNotEmpty(testServiceBeanMap)) {
            for(String annotationName:testServiceBeanMap.keySet()){
                System.out.println(annotationName);
            }
        }
        else{
            // 如果自定义注解不加 @Component 就显示 null
            logger.info("RpcServer-setApplicationContext: null ");
        }
    }

    /**
     * 按照顺序，该方法在setApplicationContext之后，
     * 按照逻辑顺序，也就是得到本服务端上哪些被自定义注解修饰了的bean，下一步那就需要启动服务器进行监听，等待客户端的请求了
     *
     *
     * 为什么这些接口名——bean 保存在本地，为什么要保存在本地，当远端请求来的时候，能够根据接口名，得到对应实现bean
     * 虽然保存的是实现bean，但是其实真正使用时，是根据这个bean得到它的类，再根据反射去调用Method.invoke()
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        logger.info("RpcServer-afterPropertiesSet:  super.start()");
        super.start();
    }

    @Override
    public void destroy() throws Exception {
        logger.info("RpcServer-destroy:  super.stop();");
        super.stop();
    }
}
