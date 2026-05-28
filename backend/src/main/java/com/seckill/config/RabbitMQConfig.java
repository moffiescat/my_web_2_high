package com.seckill.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_SECKILL = "seckill.topic";
    public static final String QUEUE_ORDER = "seckill.order.queue";
    public static final String QUEUE_NOTIFY = "seckill.notify.queue";
    public static final String RK_ORDER = "seckill.order";
    public static final String RK_NOTIFY = "seckill.notify";

    @Bean
    public TopicExchange seckillExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE_SECKILL).durable(true).build();
    }

    @Bean
    public Queue orderQueue() {
        return QueueBuilder.durable(QUEUE_ORDER).build();
    }

    @Bean
    public Queue notifyQueue() {
        return QueueBuilder.durable(QUEUE_NOTIFY).build();
    }

    @Bean
    public Binding orderBinding() {
        return BindingBuilder.bind(orderQueue()).to(seckillExchange()).with(RK_ORDER);
    }

    @Bean
    public Binding notifyBinding() {
        return BindingBuilder.bind(notifyQueue()).to(seckillExchange()).with(RK_NOTIFY);
    }
}
