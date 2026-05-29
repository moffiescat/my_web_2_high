package com.seckill.service;

import com.seckill.entity.Notification;

import java.util.List;

public interface NotificationService {
    void create(Long userId, String title, String content, String type);
    List<Notification> listByUser(Long userId);
    int markAllRead(Long userId);
}
