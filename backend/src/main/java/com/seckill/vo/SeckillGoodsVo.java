package com.seckill.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SeckillGoodsVo {
    private Long id;
    private Long goodsId;
    private String goodsName;
    private String goodsImg;
    private BigDecimal goodsPrice;
    private BigDecimal seckillPrice;
    private Integer stockCount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    /** 秒杀状态: 0-未开始 1-进行中 2-已结束 */
    private Integer status;
}
