package com.seckill.mq;

import com.rabbitmq.client.Channel;
import com.seckill.config.RabbitMQConfig;
import com.seckill.service.impl.SeckillServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class SeckillConsumer {

    private static final Logger log = LoggerFactory.getLogger(SeckillConsumer.class);

    private final SeckillServiceImpl seckillService;

    public SeckillConsumer(SeckillServiceImpl seckillService) {
        this.seckillService = seckillService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_ORDER)
    public void handleOrder(Map<String, Object> msg, Channel channel,
                            @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        try {
            Long userId = Long.valueOf(msg.get("userId").toString());
            Long goodsId = Long.valueOf(msg.get("goodsId").toString());
            log.info("收到秒杀下单消息: userId={}, goodsId={}", userId, goodsId);
            seckillService.createOrder(userId, goodsId);
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("秒杀下单失败: {}", e.getMessage());
            channel.basicNack(tag, false, true);
        }
    }
}
