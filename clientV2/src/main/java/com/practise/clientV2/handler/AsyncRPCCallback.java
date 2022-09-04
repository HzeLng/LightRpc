package com.practise.clientV2.handler;

/**
 * @author HzeLng
 * @version 1.0
 * @description AsyncRPCCallback
 * @date 2022/3/5 22:43
 */
public interface AsyncRPCCallback {

    void success(Object result);

    void fail(Exception e);

}
