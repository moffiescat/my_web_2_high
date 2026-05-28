package com.seckill.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_seckill_order")
public class SeckillOrder {
    @TableId
    private Long id;
    private Long userId;
    private Long orderId;
    private Long goodsId;
}
