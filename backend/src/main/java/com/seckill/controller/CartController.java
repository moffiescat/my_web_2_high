package com.seckill.controller;

import com.seckill.service.CartService;
import com.seckill.utils.Result;
import com.seckill.vo.CartVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    private Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    @GetMapping("/list")
    public Result<List<CartVo>> list(HttpServletRequest request) {
        return Result.ok(cartService.list(getUserId(request)));
    }

    @PostMapping("/add")
    public Result<?> add(@RequestBody Map<String, Long> body, HttpServletRequest request) {
        cartService.add(getUserId(request), body.get("goodsId"));
        return Result.ok();
    }

    @PutMapping("/{cartId}")
    public Result<?> updateQuantity(@PathVariable Long cartId,
                                    @RequestBody Map<String, Integer> body,
                                    HttpServletRequest request) {
        cartService.updateQuantity(getUserId(request), cartId, body.get("quantity"));
        return Result.ok();
    }

    @DeleteMapping("/{cartId}")
    public Result<?> remove(@PathVariable Long cartId, HttpServletRequest request) {
        cartService.remove(getUserId(request), cartId);
        return Result.ok();
    }

    @DeleteMapping("/clear")
    public Result<?> clear(HttpServletRequest request) {
        cartService.clear(getUserId(request));
        return Result.ok();
    }
}
