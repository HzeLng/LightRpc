package com.practise.clientv2;

import com.practise.clientV2.RpcClient;
import com.practise.common.services.HelloServiceV2;
import com.practise.common.services.PhoneService;

/**
 * @author HzeLng
 * @version 1.0
 * @description HelloServiceV2Test
 * @date 2022/3/6 16:43
 */
public class HelloServiceV2Test {
    public static void main(String[] args) throws InterruptedException {
        final RpcClient rpcClient = new RpcClient("127.0.0.1:2181");

        int threadNum = 15;
        final int requestNum = 25000;
        Thread[] threads = new Thread[threadNum];

        long startTime = System.currentTimeMillis();
        //benchmark for sync call
        for (int i = 0; i < threadNum; ++i) {
            threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    long beginTime = System.nanoTime();
                    for (int i = 0; i < requestNum; i++) {
                        try {
                            System.out.println("Test Test");
                            System.out.println(Thread.currentThread().getName());
                            final HelloServiceV2 syncClient = rpcClient.createService(HelloServiceV2.class, "1.0");
                            String result = syncClient.helloV2(Integer.toString(i));
                            if (!result.equals("helloV2 " + i)) {
                                System.out.println("error = " + result);
                            } else {
                                System.out.println("result = " + result);
                            }

                            final PhoneService phoneServiceProxy = rpcClient.createService(PhoneService.class,"1.0");
                            String phoneResult = phoneServiceProxy.PhoneNumber(Integer.toString(i));
                            if (!phoneResult.equals("Your phoneNum is " + i)) {
                                System.out.println("error = " + phoneResult);
                            } else {
                                System.out.println("result = " + phoneResult);
                            }

                            try {
                                Thread.sleep(0 * 1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        } catch (Exception ex) {
                            System.out.println(ex.toString());
                        }
                    }
                    long endTime = System.nanoTime();
                    long costTime = (endTime - beginTime)/1000;
                    System.out.println(costTime);
                }
            });
            threads[i].start();
        }
        for (int i = 0; i < threads.length; i++) {
            threads[i].join();
        }
        System.out.println(" join join join join join join join join join join join");
        long timeCost = (System.currentTimeMillis() - startTime);
        String msg = String.format("Sync call total-time-cost:%sms, req/s=%s", timeCost, ((double) (requestNum * threadNum)) / timeCost * 1000);
        System.out.println(msg);

        // 不主动关闭
        rpcClient.stop();
    }
}
