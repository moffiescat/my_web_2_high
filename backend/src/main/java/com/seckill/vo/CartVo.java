package com.seckill.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CartVo {
    private Long id;
    private Long userId;
    private Long goodsId;
    private String goodsName;
    private String goodsImg;
    private BigDecimal goodsPrice;
    private BigDecimal seckillPrice;
    private Integer quantity;
    private LocalDateTime createTime;
}
