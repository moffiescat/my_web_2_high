package com.seckill.service;

import com.seckill.utils.PageResult;
import com.seckill.vo.OrderVo;

public interface OrderService {

    OrderVo getDetail(Long userId, Long orderId);

    PageResult<OrderVo> listByUser(Long userId, int page, int size);

    void cancel(Long userId, Long orderId);
}
