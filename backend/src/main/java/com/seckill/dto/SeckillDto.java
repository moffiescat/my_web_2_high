package com.seckill.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SeckillDto {
    @NotNull(message = "商品ID不能为空")
    private Long goodsId;
}
