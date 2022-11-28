package com.atguigu.gulimall.order.config;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/21 16:22
 */
@Configuration
public class MyRabbitConfig {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /*
     * 定制化MQ模板
     * @return
     * @author djx
     * @deprecated: Talk is cheap,show me the code
     * @date 2022/11/21 16:56
     */
    @PostConstruct
    public void initRabbitmqTemplate() {
        /**
         * 1.服务收到消息就回调confirm
         *
         */
        rabbitTemplate.setConfirmCallback(new RabbitTemplate.ConfirmCallback() {
            /**
             * @param correlationData 当前消息的唯一全局Id
             * @param ack 消息是否成功
             * @param cause 失败的原因
             */
            @Override
            public void confirm(CorrelationData correlationData, boolean ack, String cause) {
                System.out.println("confirm....CorrelationData[" + correlationData + "]==>ack[" + ack + "]" + "==>cause[" + cause + "]");
            }
        });

        //消息抵达队列确定回调
        rabbitTemplate.setReturnCallback(new RabbitTemplate.ReturnCallback() {
            /**
             * 只要消息没有投递给指定的队列就回调
             * @param message 投递失败的消息
             * @param replyCode 回复的状态码
             * @param replyText 回复的本文内容
             * @param exchange 交换机
             * @param routingKey 指定的路由键
             */
            @Override
            public void returnedMessage(Message message, int replyCode, String replyText, String exchange, String routingKey) {
                System.out.println("Fail Message[" + message + "]==>replyCode[" + replyCode + "]" + "==>replyText[" + replyText + "]" + "==>exchange[" + exchange + "]" + "==>routingKey[" + routingKey + "]");
            }
        });

    }
}
