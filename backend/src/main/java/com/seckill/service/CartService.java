package com.seckill.service;

import com.seckill.entity.Cart;
import com.seckill.vo.CartVo;

import java.util.List;

public interface CartService {
    void add(Long userId, Long goodsId);
    List<CartVo> list(Long userId);
    void updateQuantity(Long userId, Long cartId, Integer quantity);
    void remove(Long userId, Long cartId);
    void clear(Long userId);
}
