package com.seckill.controller;

import com.seckill.service.OrderService;
import com.seckill.utils.PageResult;
import com.seckill.utils.Result;
import com.seckill.vo.OrderVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/list")
    public Result<PageResult<OrderVo>> list(HttpServletRequest request,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(orderService.listByUser(userId, page, size));
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
