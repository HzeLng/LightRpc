package com.practise.clientV2.proxy;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;

/**
 * @author HzeLng
 * @version 1.0
 * @description SerializableFunction
 * @date 2022/3/6 10:25
 */
public interface SerializableFunction<T> extends Serializable {
    default String getName() throws Exception {
        Method write = this.getClass().getDeclaredMethod("writeReplace");
        write.setAccessible(true);
        SerializedLambda serializedLambda = (SerializedLambda) write.invoke(this);
        return serializedLambda.getImplMethodName();
    }
}