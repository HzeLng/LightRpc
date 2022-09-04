package com.practise.serverV2TEST;

import com.practise.common.services.HelloServiceV2;
import com.practise.serverv2.RpcServer;
import com.practise.serverv2.ServerV2Application;
import com.practise.serverv2.services.HelloServiceV2Impl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author HzeLng
 * @version 1.0
 * @description ServerTest
 * @date 2022/3/6 15:51
 */
@RunWith(SpringRunner.class)
// 要加这个classes，不然 no beans of RpcProxy 应该是容器没开启，没有识别到
@SpringBootTest(classes = ServerV2Application.class)
public class ServerTest {

    private static final Logger logger = LoggerFactory.getLogger(ServerTest.class);
    @Test
    public void ServerV2Test(){
        String serverAddress = "127.0.0.1:8000";
        String registryAddress = "127.0.0.1:2181";
        RpcServer rpcServer = new RpcServer(serverAddress,registryAddress);
        HelloServiceV2 helloServiceV2 = new HelloServiceV2Impl();

        try{
            rpcServer.start();
            Thread.sleep(3000);
        }
        catch (Exception e){
            logger.error("rpcServer start error",e.toString());
        }

    }

}
