package com.practise.clientV2.proxy;

import com.practise.clientV2.handler.RpcFuture;

/**
 * @author HzeLng
 * @version 1.0
 * @description RpcService
 * @date 2022/3/6 10:24
 */
public interface RpcService<T, P, FN extends SerializableFunction<T>> {
    RpcFuture call(String funcName, Object... args) throws Exception;

    /**
     * lambda method reference
     */
    RpcFuture call(FN fn, Object... args) throws Exception;

}