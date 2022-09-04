package com.practise.serverv2.annotations;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * @author HzeLng
 * @version 1.0
 * @description TestAnnotation
 * @date 2022/3/8 21:12
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
// @Component
@Documented
public @interface TestAnnotation {
}
