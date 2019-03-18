package com.alibaba.dubbo.performance.demo.agent;

import com.alibaba.dubbo.performance.demo.agent.dubbo.RpcClient;
import com.alibaba.dubbo.performance.demo.agent.registry.Endpoint;
import com.alibaba.dubbo.performance.demo.agent.registry.EtcdRegistry;
import com.alibaba.dubbo.performance.demo.agent.registry.IRegistry;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Future;

@RestController
public class HelloController {

    private Logger logger = LoggerFactory.getLogger(HelloController.class);

    private IRegistry registry = new EtcdRegistry(System.getProperty("etcd.url"));

    private RpcClient rpcClient = new RpcClient(registry);
    private Random random = new Random();
    private List<Endpoint> endpoints = null;
    private Object lock = new Object();
    /**
     * 使用okHttp
     */
    private OkHttpClient httpClient = new OkHttpClient();


    @RequestMapping(value = "")
    public Object invoke(@RequestParam("interface") String interfaceName,
                         @RequestParam("method") String method,
                         @RequestParam("parameterTypesString") String parameterTypesString,
                         @RequestParam("parameter") String parameter) throws Exception {
        String type = System.getProperty("type");   // 获取type参数
        if ("consumer".equals(type)) {
            long start = System.currentTimeMillis();
            Integer c = consumer(interfaceName, method, parameterTypesString, parameter);
            logger.info("CA:{}", System.currentTimeMillis() - start);
            return c;

        } else if ("provider".equals(type)) {
            long start = System.currentTimeMillis();
            byte[] bytes = provider(interfaceName, method, parameterTypesString, parameter);
            logger.info("PA:{}", System.currentTimeMillis() - start);
            return bytes;
        } else {
            return "Environment variable type is needed to set to provider or consumer.";
        }
    }

    public byte[] provider(String interfaceName, String method, String parameterTypesString, String parameter) throws Exception {

        Object result = rpcClient.invoke(interfaceName, method, parameterTypesString, parameter);
        return (byte[]) result;
    }

    public Integer consumer(String interfaceName, String method, String parameterTypesString, String parameter) throws Exception {

        if (null == endpoints) {
            synchronized (lock) {
                if (null == endpoints) {
                    endpoints = registry.find("com.alibaba.dubbo.performance.demo.provider.IHelloService");
                }
            }
        }


        /**
         * 修改负载: 1. 使用netty 线程池代替http,
         *
         */
        // 简单的负载均衡，随机取一个
        Endpoint endpoint = endpoints.get(random.nextInt(endpoints.size()));

        /**
         * 不能缓存结果 && 修改协议
         */
        String url = "http://" + endpoint.getHost() + ":" + endpoint.getPort();

        RequestBody requestBody = new FormBody.Builder()
                .add("interface", interfaceName)
                .add("method", method)
                .add("parameterTypesString", parameterTypesString)
                .add("parameter", parameter)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        /**
         * okHttp支持异步http
         */
        /*try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            byte[] bytes = response.body().bytes();
            String s = new String(bytes);
            return Integer.valueOf(s);
        }*/


        final Integer[] res = new Integer[1];
        httpClient.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                byte[] bytes = response.body().bytes();
                String s = new String(bytes);
                res[0] = Integer.valueOf(s);
                //函数回调的值不能返回 ->
                logger.info("res = {}", res[0]);
            }
        });
        return res[0];
    }
}
