package com.seckill.controller;

import com.seckill.service.GoodsService;
import com.seckill.utils.Result;
import com.seckill.vo.GoodsVo;
import com.seckill.vo.SeckillGoodsVo;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/goods")
public class GoodsController {

    private final GoodsService goodsService;

    public GoodsController(GoodsService goodsService) {
        this.goodsService = goodsService;
    }

    @GetMapping("/list")
    public Result<List<GoodsVo>> list() {
        return Result.ok(goodsService.listGoods());
    }

    @GetMapping("/detail/{id}")
    public Result<GoodsVo> detail(@PathVariable Long id) {
        return Result.ok(goodsService.getDetail(id));
    }

    @GetMapping("/seckill")
    public Result<List<SeckillGoodsVo>> seckillGoods() {
        return Result.ok(goodsService.listSeckillGoods());
    }
}
