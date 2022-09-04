package com.practise.clientv2;

import com.practise.clientV2.RpcClient;
import com.practise.common.services.HelloServiceV2;

/**
 * @author HzeLng
 * @version 1.0
 * @description HelloServiceV2HeartBeat
 * @date 2022/3/6 20:21
 */
public class HelloServiceV2HeartBeat {
    public static void main(String[] args) {
        RpcClient rpcClient = new RpcClient("127.0.0.1:2181");
        HelloServiceV2 syncClient = rpcClient.createService(HelloServiceV2.class, "1.0");
        String result = syncClient.helloV2("heart beat" + Thread.currentThread());
        System.out.println(result);
    }
}
