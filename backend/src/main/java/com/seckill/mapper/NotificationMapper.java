package com.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.entity.Notification;
import org.apache.ibatis.annotations.Param;

public interface NotificationMapper extends BaseMapper<Notification> {

    /** 全部标记已读 — SQL 见 mapper/NotificationMapper.xml */
    int markAllRead(@Param("userId") Long userId);
}
