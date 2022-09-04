package com.practise.serverv2.services;

import com.practise.common.entity.Person;
import com.practise.common.services.HelloServiceV2;
import com.practise.serverv2.annotations.NettyRpcService;

/**
 * @author HzeLng
 * @version 1.0
 * @description HelloServiceV2Impl
 * @date 2022/3/5 22:05
 */
@NettyRpcService(value = HelloServiceV2.class,version = "1.0")
public class HelloServiceV2Impl implements HelloServiceV2 {

    @Override
    public String helloV2(String name) {
        return "helloV2 " + name;
    }

    @Override
    public Person helloV2(Person person) {
        person.setName("helloV2" + person.getName());
        return person;
    }
}
