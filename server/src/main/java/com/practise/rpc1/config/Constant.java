package com.practise.rpc1.config;

/**
 * @author HzeLng
 * @version 1.0
 * @description Constant
 * @date 2022/3/4 15:25
 */
public interface Constant {

    int ZK_SESSION_TIMEOUT = 5000;

    String ZK_REGISTRY_PATH = "/rpcregistry";
    String ZK_DATA_PATH = ZK_REGISTRY_PATH + "/data";

}
