package com.practise.clientV2.proxy;

/**
 * @author HzeLng
 * @version 1.0
 * @description RpcFunction
 * @date 2022/3/6 10:25
 */
@FunctionalInterface
public interface RpcFunction<T, P> extends SerializableFunction<T> {
    Object apply(T t, P p);
}