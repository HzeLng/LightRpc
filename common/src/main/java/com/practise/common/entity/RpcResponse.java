package com.practise.common.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * @author HzeLng
 * @version 1.0
 * @description RpcResponse
 * @date 2022/3/4 15:59
 */
@Setter
@Getter
public class RpcResponse {

    private String requestId;
    private Throwable error;
    private Object result;


}
