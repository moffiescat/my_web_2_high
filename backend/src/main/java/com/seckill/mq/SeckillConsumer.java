package com.seckill.mq;

import com.rabbitmq.client.Channel;
import com.seckill.config.RabbitMQConfig;
import com.seckill.constant.AppConstants;
import com.seckill.service.NotificationService;
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
    private final NotificationService notificationService;

    public SeckillConsumer(SeckillServiceImpl seckillService, NotificationService notificationService) {
        this.seckillService = seckillService;
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_ORDER)
    public void handleOrder(Map<String, Object> msg, Channel channel,
                            @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        try {
            Long userId = Long.valueOf(msg.get(AppConstants.MQ_MSG_KEY_USER_ID).toString());
            Long goodsId = Long.valueOf(msg.get(AppConstants.MQ_MSG_KEY_GOODS_ID).toString());
            log.info("收到秒杀下单消息: userId={}, goodsId={}", userId, goodsId);
            seckillService.createOrder(userId, goodsId);
            // 发送通知
            notificationService.create(userId, "秒杀成功", "恭喜！商品 " + goodsId + " 秒杀订单已生成，请前往我的订单查看。", "seckill");
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("秒杀下单失败: {}", e.getMessage());
            channel.basicNack(tag, false, true);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NOTIFY)
    public void handleNotify(Map<String, Object> msg, Channel channel,
                             @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        try {
            Long userId = Long.valueOf(msg.get("userId").toString());
            String title = (String) msg.get("title");
            String content = (String) msg.get("content");
            String type = (String) msg.getOrDefault("type", "system");
            notificationService.create(userId, title, content, type);
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("通知创建失败: {}", e.getMessage());
            channel.basicNack(tag, false, true);
        }
    }
}
