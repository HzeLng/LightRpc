package com.practise.serverv2.annotations;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author HzeLng
 * @version 1.0
 * @description NettyRpcService
 * @date 2022/3/5 22:05
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface NettyRpcService {
    /**
     * 在某个服务实现类上使用该注解时
     * 该值必须被赋值，且表示的是此服务实现类实现的接口的类
     * @return
     */
    Class<?> value();

    String version() default "";
}