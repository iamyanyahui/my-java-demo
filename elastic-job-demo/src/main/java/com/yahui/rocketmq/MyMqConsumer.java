package com.yahui.rocketmq;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * @author yanyahui <yanyahui@kuaishou.com>
 * Created on 2021-06-20
 */
public class MyMqConsumer {
    private static final String TOPIC = "test-topic-1";
    private static final String CONSUMER_GROUP_1 = "test-topic-consumer-group-1";
    private static final String CONSUMER_GROUP_2 = "test-topic-consumer-group-2";
    private static final String NAMESRV_ADDR = "livestream-yanyahui-01.dev.kwaidc.com:9876";
    private static final ExecutorService EXECUTORS =
            Executors.newFixedThreadPool(10, new ThreadFactoryBuilder().setNameFormat("my-consumer-%d").build());


    public static void main(String[] args) throws MQClientException, InterruptedException {
        DefaultMQPushConsumer consumer1 = buildConsumer(CONSUMER_GROUP_1, ConsumeConcurrentlyStatus.CONSUME_SUCCESS);
        DefaultMQPushConsumer consumer2 = buildConsumer(CONSUMER_GROUP_1, ConsumeConcurrentlyStatus.RECONSUME_LATER);

        DefaultMQPushConsumer consumer3 = buildConsumer(CONSUMER_GROUP_2, ConsumeConcurrentlyStatus.CONSUME_SUCCESS);

        EXECUTORS.submit(() -> {
            try {
                consumer1.start();
            } catch (MQClientException e) {
                e.printStackTrace();
            }
        });
        EXECUTORS.submit(() -> {
            try {
                consumer2.start();
            } catch (MQClientException e) {
                e.printStackTrace();
            }
        });
        EXECUTORS.submit(() -> {
            try {
                consumer3.start();
            } catch (MQClientException e) {
                e.printStackTrace();
            }
        });
        TimeUnit.DAYS.sleep(1);
    }

    private static DefaultMQPushConsumer buildConsumer(String group, ConsumeConcurrentlyStatus status) throws MQClientException {
        //????????????????????????MQ??????????????????
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(group);
        //??????rocketMQ???namesrv???????????????????????????
        consumer.setNamesrvAddr(NAMESRV_ADDR);
        //????????????????????????????????????
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);//???????????????????????????*?????????????????????
        consumer.subscribe(TOPIC, "*");
        //???????????????
        consumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
            for (MessageExt msg : msgs) {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    String topic = msg.getTopic();
                    String msgbody = new String(msg.getBody(), "UTF-8");
                    String tag = msg.getTags();
                    System.out.println("topic:" + topic + ", msgbody:" + msgbody + ", tag:" + tag + ", group:" + consumer.getConsumerGroup() + ", "
                            + "instanceName:" + consumer.getInstanceName());
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    //MQ???????????????????????????1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h
                    return status;
                }
            }
            //??????????????????
            return status;
        });
        return consumer;
    }
}
