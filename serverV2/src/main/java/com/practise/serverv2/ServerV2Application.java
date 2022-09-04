package com.practise.serverv2;

import com.practise.common.services.HelloServiceV2;
import com.practise.serverv2.services.HelloServiceV2Impl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author HzeLng
 * @version 1.0
 * @description ServerV2Application
 * @date 2022/3/6 15:52
 */
@SpringBootApplication
public class ServerV2Application {
    private static final Logger logger = LoggerFactory.getLogger(ServerV2Application.class);
    public static void main(String[] args) {
        SpringApplication.run(ServerV2Application.class,args);

    }
}
