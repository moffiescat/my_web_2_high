package com.seckill.utils;

import cn.hutool.core.util.IdUtil;
import org.springframework.stereotype.Component;

@Component
public class SnowflakeUtil {

    public long nextId() {
        return IdUtil.getSnowflakeNextId();
    }
}
