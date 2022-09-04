package com.practise.Test;

import com.practise.client.RpcProxy;
import com.practise.client.clientApplication;
import com.practise.common.entity.Person;
import com.practise.common.services.HelloService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;

/**
 * @author HzeLng
 * @version 1.0
 * @description HelloServiceTest
 * @date 2022/3/4 18:03
 */
@RunWith(SpringRunner.class)
// 要加这个classes，不然 no beans of RpcProxy 应该是容器没开启，没有识别到
@SpringBootTest(classes = clientApplication.class)
public class HelloServiceTest {
    @Autowired
    RpcProxy rpcProxy;

    @Test
    public void helloTest(){
        System.out.println("!!");
        HelloService helloService = rpcProxy.create(HelloService.class);
        System.out.println("!");
        if(helloService == null){
            System.out.println("wrong !");
        }
        String result = helloService.hello("HzeLng!!! ");
        System.out.println(result);
        System.out.println("成功啦！！！" + result);
    }

    @Test
    public void HelloPerson(){
        Person person = new Person();
        person.setName("Tom");
        person.setAge(18);
        HelloService helloService = rpcProxy.create(HelloService.class);
        String result = helloService.hello(person);
        System.out.println(result);
    }


}
