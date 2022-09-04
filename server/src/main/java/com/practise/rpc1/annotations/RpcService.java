package com.practise.rpc1.annotations;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author HzeLng
 * @version 1.0
 * @description RpcService
 * @date 2022/3/5 9:56
 *  * 使用RpcService注解定义在服务接口的实现类上，需要对该实现类指定远程接口，因为实现类可能会实现多个接口，一定要告诉框架哪个才是远程接口
 *  *
 *  * 通过此注解，被注解修饰的服务实现类，可以被SpringBoot容器扫描并发现到
 *  * 当服务端启动时，扫描此注解修饰的服务实现类，就可以直接知道哪些类，希望export出去让其他客户端远程调用
 *  *
 *  * 还得多了解注解类
 *
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Component // 表明可被 Spring 扫描
public @interface RpcService {

    Class<?> value();

}