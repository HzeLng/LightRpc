package com.practise.rpc1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

/**
 * @author HzeLng
 * @version 1.0
 * @description ServerApplication
 * @date 2022/3/4 16:27
 */
@SpringBootApplication
public class ServerApplication {
    public static void main(String[] args) {
        System.out.println("main");
        System.out.println("==== APP  STARTED ====");
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        String name = runtime.getName();
        System.out.println(name);
        System.out.println("Process ID: " + name.substring(0, name.indexOf("@")));
        System.out.println(Thread.currentThread().getName());
        SpringApplication.run(ServerApplication.class,args);
    }
}
