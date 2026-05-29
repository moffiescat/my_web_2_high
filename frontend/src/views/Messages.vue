<template>
  <div class="page-container">
    <n-card title="消息中心" :bordered="true" style="border-radius:12px;">
      <n-empty description="暂无消息">
        <template #icon>
          <n-icon size="64"><NotificationsOutline /></n-icon>
        </template>
      </n-empty>
      <n-timeline style="margin-top:20px;padding:0 40px;">
        <n-timeline-item time="2026-05-29 10:00" type="info">
          <n-card :bordered="true" hoverable style="border-radius:8px;">
            <h4 style="margin:0 0 4px;">秒杀活动通知</h4>
            <p style="margin:0;color:#999;font-size:13px;">iPhone 15 Pro Max 秒杀活动已开始，限量 20 件！</p>
          </n-card>
        </n-timeline-item>
        <n-timeline-item time="2026-05-29 12:00" type="success">
          <n-card :bordered="true" hoverable style="border-radius:8px;">
            <h4 style="margin:0 0 4px;">秒杀活动通知</h4>
            <p style="margin:0;color:#999;font-size:13px;">MacBook Pro 14 秒杀即将开始，12:00 准时开抢！</p>
          </n-card>
        </n-timeline-item>
        <n-timeline-item time="2026-05-29 14:00" type="warning">
          <n-card :bordered="true" hoverable style="border-radius:8px;">
            <h4 style="margin:0 0 4px;">秒杀活动通知</h4>
            <p style="margin:0;color:#999;font-size:13px;">AirPods Pro 2 秒杀活动即将开始！</p>
          </n-card>
        </n-timeline-item>
      </n-timeline>
    </n-card>
  </div>
</template>

<script setup>
import { NIcon } from 'naive-ui'
import { NotificationsOutline } from '@vicons/ionicons5'
</script>
