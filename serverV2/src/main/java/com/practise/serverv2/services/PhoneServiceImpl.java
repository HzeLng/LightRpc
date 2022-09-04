package com.practise.serverv2.services;

import com.practise.common.services.PhoneService;
import com.practise.serverv2.annotations.NettyRpcService;

/**
 * @author HzeLng
 * @version 1.0
 * @description PhoneServiceImpl
 * @date 2022/3/7 15:24
 */
@NettyRpcService(value = PhoneService.class, version = "1.0")
public class PhoneServiceImpl implements PhoneService {

    @Override
    public String PhoneNumber(String phoneNum) {
        return "Your phoneNum is " + phoneNum;
    }
}
