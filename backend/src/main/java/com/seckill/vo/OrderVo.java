package com.seckill.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderVo {
    private Long id;
    private Long goodsId;
    private String goodsName;
    private BigDecimal goodsPrice;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime payTime;
}
