package com.seckill.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_order")
public class Order {
    @TableId
    private Long id;
    private Long userId;
    private Long goodsId;
    private String goodsName;
    private BigDecimal goodsPrice;
    /** 0:待支付 1:已支付 2:已取消 */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime payTime;
}
