package com.practise.common.utils;

/**
 * @author HzeLng
 * @version 1.0
 * @description Serializer
 * @date 2022/3/5 20:56
 */
public abstract class Serializer {
    public abstract <T> byte[] serialize(T obj);

    public abstract <T> Object deserialize(byte[] bytes, Class<T> clazz);
}
