package com.atguigu.gulimall.order;

import com.atguigu.gulimall.order.entity.OrderReturnApplyEntity;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.UUID;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class GulimallOrderApplicationTests {

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /*
     * 发送队列
     * @return
     * @author djx
     * @deprecated: Talk is cheap,show me the code
     * @date 2022/11/21 16:16
     */
    @Test
    public void sendMessageTest() {

        OrderReturnApplyEntity returnApplyEntity = new OrderReturnApplyEntity();
        returnApplyEntity.setId(1L);
        returnApplyEntity.setCreateTime(new Date());
        returnApplyEntity.setDescPics("呜呜呜");

        //1。发送消息
        rabbitTemplate.convertAndSend("gulimall-hello-java", "hello.java", returnApplyEntity, new CorrelationData(UUID.randomUUID().toString()));

    }


    /**
     * 1.如何创建交换机和队列绑定关系
     * 2.发送消息
     * 3.消费消息
     */
    @Test
    public void createExchange() {

        //创建交换机
        /**
         * String name, boolean durable, boolean autoDelete, Map<String, Object> arguments
         *  1.名字
         *  2.是否持久化
         *  3.是否自动杀出
         *  4.指定参数
         */
        DirectExchange directExchange = new DirectExchange("gulimall-hello-java", true, false);
        amqpAdmin.declareExchange(directExchange);
        log.info("Exchange创建成功:{}", "gulimall-hello-java");

    }

    /*
     * 创建队列
     * @return
     * @author djx
     * @deprecated: Talk is cheap,show me the code
     * @date 2022/11/21 16:02
     */
    @Test
    public void createQueue() {
        /**
         * String name, boolean durable, boolean exclusive, boolean autoDelete, Map<String, Object> arguments
         * 1.名字
         * 2.是否持久化
         * 3.是否排查
         * 4.自动删除
         * 5.队列参数
         */
        Queue queue = new Queue("gulimall-hello-queue", true, false, false);
        amqpAdmin.declareQueue(queue);
        log.info("Queue创建成功:{}", "gulimall-hello-queue");

    }

    @Test
    public void createBinding() {
        // 讲exchange指定的交换机和destion目的地进行绑定 使用roukey
        Binding binding = new Binding("gulimall-hello-queue", Binding.DestinationType.QUEUE, "gulimall-hello-java", "hello.java", null);
        amqpAdmin.declareBinding(binding);
        log.info("Binding创建成功:{}", "gulimall-hello-queue=====gulimall-hello-java");
    }


}
