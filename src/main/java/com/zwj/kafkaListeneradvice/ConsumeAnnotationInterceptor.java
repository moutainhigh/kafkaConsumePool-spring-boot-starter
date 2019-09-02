package com.zwj.kafkaListeneradvice;


import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.util.StringUtils;


/**
 * @Author: zwj
 * @Date: 2019-08-27 15:52
 */
@Slf4j
public class ConsumeAnnotationInterceptor implements MethodInterceptor, Ordered {

    private Executor executor;

    private Environment environment;

    public ConsumeAnnotationInterceptor(Executor executor, Environment environment) {
        this.executor = executor;
        this.environment = environment;
    }

    @Override
    public Object invoke(MethodInvocation invocation) {

        KafkaListener kafkaListener = invocation.getMethod().getAnnotation(KafkaListener.class);
        Object[] content = invocation.getArguments();
        String defaultMetricName = environment.getProperty("spring.kafka.consumer.group-id", "defaultMetricName");
        String groupId = kafkaListener.groupId();
        handleMsg((String) content[0],
                StringUtils.isEmpty(groupId) ? defaultMetricName : environment.getProperty(groupId.substring("${".length(), groupId.length() - 1)),
                () -> {
                    try {
                        invocation.proceed();
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                });
        return null;
    }

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE;
    }

    private void handleMsg(String content, String groupId, Runnable runnable) {
        executor.execute(() -> {
            try {
                runnable.run();
            } catch (Exception e) {
                log.warn("kafka处理消息失败，message:{},groupid:{},exception:{}", content, groupId, e);
            } finally {
            }
        });
    }

}
