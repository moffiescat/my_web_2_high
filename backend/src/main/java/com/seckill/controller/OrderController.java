package com.seckill.controller;

import com.seckill.service.OrderService;
import com.seckill.utils.Result;
import com.seckill.vo.OrderVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/list")
    public Result<List<OrderVo>> list(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(orderService.listByUser(userId));
    }

    @GetMapping("/detail/{orderId}")
    public Result<OrderVo> detail(@PathVariable Long orderId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(orderService.getDetail(userId, orderId));
    }

    @PostMapping("/cancel/{orderId}")
    public Result<?> cancel(@PathVariable Long orderId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        orderService.cancel(userId, orderId);
        return Result.ok();
    }
}
