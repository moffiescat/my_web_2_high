package com.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seckill.constant.AppConstants;
import com.seckill.entity.Order;
import com.seckill.enums.OrderStatus;
import com.seckill.mapper.OrderMapper;
import com.seckill.service.OrderService;
import com.seckill.vo.OrderVo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;

    public OrderServiceImpl(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    @Override
    public OrderVo getDetail(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException(AppConstants.MSG_ORDER_NOT_FOUND);
        }
        return toVo(order);
    }

    @Override
    public List<OrderVo> listByUser(Long userId) {
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, userId)
                        .orderByDesc(Order::getCreateTime)
        );
        return orders.stream().map(this::toVo).collect(Collectors.toList());
    }

    @Override
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
