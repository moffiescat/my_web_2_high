package com.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.seckill.constant.AppConstants;
import com.seckill.entity.Order;
import com.seckill.entity.SeckillGoods;
import com.seckill.enums.OrderStatus;
import com.seckill.mapper.OrderMapper;
import com.seckill.mapper.SeckillGoodsMapper;
import com.seckill.service.OrderService;
import com.seckill.utils.PageResult;
import com.seckill.utils.RedisKey;
import com.seckill.vo.OrderVo;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final SeckillGoodsMapper seckillGoodsMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    public OrderServiceImpl(OrderMapper orderMapper,
                            SeckillGoodsMapper seckillGoodsMapper,
                            RedisTemplate<String, Object> redisTemplate) {
        this.orderMapper = orderMapper;
        this.seckillGoodsMapper = seckillGoodsMapper;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public OrderVo getDetail(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new RuntimeException(AppConstants.MSG_ORDER_NOT_FOUND);
        }
        return toVo(order);
    }

    @Override
    public PageResult<OrderVo> listByUser(Long userId, int page, int size) {
        List<Order> all = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, userId)
                        .orderByDesc(Order::getCreateTime)
        );
        List<OrderVo> vos = all.stream().map(this::toVo).collect(Collectors.toList());
        int total = vos.size();
        int from = (page - 1) * size;
        int to = Math.min(from + size, total);
        List<OrderVo> records = from < total ? vos.subList(from, to) : List.of();
        return PageResult.of(total, page, size, records);
    }

    @Override
    @Transactional
    public void cancel(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new RuntimeException(AppConstants.MSG_ORDER_NOT_FOUND);
        }
        if (order.getStatus() != OrderStatus.PENDING.getCode()) {
            throw new RuntimeException(AppConstants.MSG_ORDER_STATUS_DENIED);
        }
        order.setStatus(OrderStatus.CANCELLED.getCode());
        orderMapper.updateById(order);

        // 恢复 MySQL 库存
        seckillGoodsMapper.update(null,
                new LambdaUpdateWrapper<SeckillGoods>()
                        .eq(SeckillGoods::getGoodsId, order.getGoodsId())
                        .setSql("stock_count = stock_count + 1")
        );

        // 恢复 Redis 库存
        String stockKey = RedisKey.seckillStock(order.getGoodsId());
        redisTemplate.opsForValue().increment(stockKey, 1);
    }

    private OrderVo toVo(Order order) {
        OrderVo vo = new OrderVo();
        vo.setId(order.getId());
        vo.setGoodsId(order.getGoodsId());
        vo.setGoodsName(order.getGoodsName());
        vo.setGoodsPrice(order.getGoodsPrice());
        vo.setStatus(order.getStatus());
        vo.setCreateTime(order.getCreateTime());
        vo.setPayTime(order.getPayTime());
        return vo;
    }
}
