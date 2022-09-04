package com.practise.client;

import com.practise.common.services.HelloService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author HzeLng
 * @version 1.0
 * @description clientApplication
 * @date 2022/3/4 18:16
 */
@SpringBootApplication
public class clientApplication {
    @Autowired
    static RpcProxy rpcProxy;
    public static void main(String[] args) {
        SpringApplication.run(clientApplication.class,args);

        HelloService helloService = rpcProxy.create(HelloService.class);
        System.out.println("!");
        if(helloService == null){
            System.out.println("wrong !");
        }
        String result = helloService.hello("world!!!");
        System.out.println("成功啦！！！" + result);
    }
}
