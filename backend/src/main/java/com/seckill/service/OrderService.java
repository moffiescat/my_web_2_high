package com.seckill.service;

import com.seckill.vo.OrderVo;

import java.util.List;

public interface OrderService {

    OrderVo getDetail(Long orderId);

    List<OrderVo> listByUser(Long userId);

    void cancel(Long userId, Long orderId);
}
