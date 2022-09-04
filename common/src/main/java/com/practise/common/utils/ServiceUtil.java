package com.practise.common.utils;

/**
 * @author HzeLng
 * @version 1.0
 * @description ServiceUtil
 * @date 2022/3/5 20:18
 */
public class ServiceUtil {

    public static final String SERVICE_CONCAT_TOKEN = "#";

    public static String makeServiceKey(String interfaceName, String version) {
        String serviceKey = interfaceName;
        if (version != null && version.trim().length() > 0) {
            serviceKey += SERVICE_CONCAT_TOKEN.concat(version);
        }
        return serviceKey;
    }

}
