package com.practise.common.services;

import com.practise.common.entity.Person;

/**
 * @author HzeLng
 * @version 1.0
 * @description HelloService
 * @date 2022/3/4 18:06
 */
public interface HelloService {

    String hello(String name);

    String hello(Person person);

}
