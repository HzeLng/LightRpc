package com.practise.common.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * @author HzeLng
 * @version 1.0
 * @description RpcRequest
 * @date 2022/3/4 15:58
 */
@Getter
@Setter
public class RpcRequest {
    private static final long serialVersionUID = -2524587347775862771L;

    private String requestId;
    private String className;
    private String methodName;
    private Class<?>[] parameterTypes;
    private Object[] parameters;
    private String version;

}
