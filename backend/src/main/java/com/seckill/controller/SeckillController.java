package com.seckill.controller;

import com.seckill.dto.SeckillDto;
import com.seckill.service.SeckillService;
import com.seckill.utils.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/seckill")
public class SeckillController {

    private final SeckillService seckillService;

    public SeckillController(SeckillService seckillService) {
        this.seckillService = seckillService;
    }

    @GetMapping("/path")
    public Result<String> getPath(@RequestParam Long goodsId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String path = seckillService.getSeckillPath(userId, goodsId);
        return Result.ok(path);
    }

    @PostMapping("/{path}/execute")
    public Result<Long> execute(@PathVariable String path,
                                @RequestBody SeckillDto dto,
                                HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Long result = seckillService.doSeckill(userId, dto.getGoodsId(), path);
        return Result.ok(result);
    }

    @GetMapping("/result/{goodsId}")
    public Result<Long> result(@PathVariable Long goodsId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Long orderId = seckillService.getSeckillResult(userId, goodsId);
        return Result.ok(orderId);
    }
}
