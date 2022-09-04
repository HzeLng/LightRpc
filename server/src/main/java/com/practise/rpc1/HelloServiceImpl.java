package com.practise.rpc1;

import com.practise.common.entity.Person;
import com.practise.common.services.HelloService;
import com.practise.rpc1.annotations.RpcService;

/**
 * @author HzeLng
 * @version 1.0
 * @description HelloServiceImpl
 * @date 2022/3/5 10:11
 */
@RpcService(HelloService.class)
public class HelloServiceImpl implements HelloService {

    @Override
    public String hello(String name) {
        return "hello " + name;
    }

    @Override
    public String hello(Person person) {
        return "hello " + person.getName() + "! your age is " + person.getAge();
    }
}
