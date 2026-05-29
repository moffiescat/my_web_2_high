package com.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seckill.constant.AppConstants;
import com.seckill.entity.Cart;
import com.seckill.entity.Goods;
import com.seckill.entity.SeckillGoods;
import com.seckill.mapper.CartMapper;
import com.seckill.mapper.GoodsMapper;
import com.seckill.mapper.SeckillGoodsMapper;
import com.seckill.service.CartService;
import com.seckill.vo.CartVo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    private final CartMapper cartMapper;
    private final GoodsMapper goodsMapper;
    private final SeckillGoodsMapper seckillGoodsMapper;

    public CartServiceImpl(CartMapper cartMapper, GoodsMapper goodsMapper, SeckillGoodsMapper seckillGoodsMapper) {
        this.cartMapper = cartMapper;
        this.goodsMapper = goodsMapper;
        this.seckillGoodsMapper = seckillGoodsMapper;
    }

    @Override
    public void add(Long userId, Long goodsId) {
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods == null) {
            throw new RuntimeException(AppConstants.MSG_SECKILL_GOODS_NOT_FOUND);
        }
        // 检查是否已在购物车
        Cart exist = cartMapper.selectOne(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId).eq(Cart::getGoodsId, goodsId));
        if (exist != null) {
            exist.setQuantity(exist.getQuantity() + 1);
            cartMapper.updateById(exist);
            return;
        }
        Cart cart = new Cart();
        cart.setUserId(userId);
        cart.setGoodsId(goodsId);
        cart.setGoodsName(goods.getGoodsName());
        cart.setGoodsImg(goods.getGoodsImg());
        cart.setGoodsPrice(goods.getGoodsPrice());
        cart.setQuantity(1);
        cartMapper.insert(cart);
    }

    @Override
    public List<CartVo> list(Long userId) {
        List<Cart> carts = cartMapper.selectList(
                new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, userId).orderByDesc(Cart::getCreateTime));
        if (carts.isEmpty()) return new ArrayList<>();

        // 批量查秒杀价
        List<Long> goodsIds = carts.stream().map(Cart::getGoodsId).collect(Collectors.toList());
        Map<Long, SeckillGoods> sgMap = seckillGoodsMapper.selectList(
                new LambdaQueryWrapper<SeckillGoods>().in(SeckillGoods::getGoodsId, goodsIds))
                .stream().collect(Collectors.toMap(SeckillGoods::getGoodsId, g -> g, (a, b) -> a));

        return carts.stream().map(c -> {
            CartVo vo = new CartVo();
            vo.setId(c.getId());
            vo.setUserId(c.getUserId());
            vo.setGoodsId(c.getGoodsId());
            vo.setGoodsName(c.getGoodsName());
            vo.setGoodsImg(c.getGoodsImg());
            vo.setGoodsPrice(c.getGoodsPrice());
            vo.setQuantity(c.getQuantity());
            vo.setCreateTime(c.getCreateTime());
            SeckillGoods sg = sgMap.get(c.getGoodsId());
            if (sg != null) vo.setSeckillPrice(sg.getSeckillPrice());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public void updateQuantity(Long userId, Long cartId, Integer quantity) {
        Cart cart = cartMapper.selectById(cartId);
        if (cart == null || !cart.getUserId().equals(userId)) return;
        if (quantity <= 0) {
            cartMapper.deleteById(cartId);
            return;
        }
        cart.setQuantity(quantity);
        cartMapper.updateById(cart);
    }

    @Override
    public void remove(Long userId, Long cartId) {
        Cart cart = cartMapper.selectById(cartId);
        if (cart == null || !cart.getUserId().equals(userId)) return;
        cartMapper.deleteById(cartId);
    }

    @Override
    public void clear(Long userId) {
        cartMapper.delete(new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, userId));
    }
}
