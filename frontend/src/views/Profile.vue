<template>
  <div class="profile-page">
    <!-- 头部卡片：头像 + 问候 -->
    <div class="profile-hero">
      <div class="hero-avatar">
        <n-avatar :size="72">
          <n-icon size="36"><PersonOutline /></n-icon>
        </n-avatar>
      </div>
      <div class="hero-text">
        <h2 class="hero-greeting">你好，{{ userInfo.nickname || '用户' + userId }}</h2>
        <p class="hero-sub">欢迎回到秒杀商城</p>
      </div>
    </div>

    <!-- 信息卡片 -->
    <div class="profile-cards">
      <n-card title="基本信息" :bordered="true" style="border-radius:12px;">
        <template #header-extra>
          <n-icon size="20"><PersonOutline /></n-icon>
        </template>
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">用户ID</span>
            <span class="info-value">{{ userId }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">昵称</span>
            <span class="info-value">{{ userInfo.nickname || '未设置' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">手机号</span>
            <span class="info-value">{{ userInfo.phone || '未绑定' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">注册时间</span>
            <span class="info-value">{{ userInfo.registerTime || '--' }}</span>
          </div>
        </div>
      </n-card>

      <!-- 快捷入口 -->
      <n-card title="快捷入口" :bordered="true" style="border-radius:12px;">
        <template #header-extra>
          <n-icon size="20"><GridOutline /></n-icon>
        </template>
        <div class="quick-actions">
          <div class="quick-item" @click="$router.push('/order/list')">
            <n-icon size="22"><DocumentTextOutline /></n-icon>
            <span>我的订单</span>
          </div>
          <div class="quick-item" @click="$router.push('/cart')">
            <n-icon size="22"><CartOutline /></n-icon>
            <span>购物车</span>
          </div>
          <div class="quick-item" @click="$router.push('/messages')">
            <n-icon size="22"><NotificationsOutline /></n-icon>
            <span>消息通知</span>
          </div>
        </div>
      </n-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { NIcon } from 'naive-ui'
import { PersonOutline, GridOutline, DocumentTextOutline, CartOutline, NotificationsOutline } from '@vicons/ionicons5'
import { useUserStore } from '../store/user'

const userStore = useUserStore()
const userId = ref('--')
const userInfo = ref({
  nickname: '',
  phone: '',
  registerTime: ''
})

onMounted(() => {
  try {
    const token = userStore.token
    if (!token) return
    const payload = JSON.parse(atob(token.split('.')[1]))
    userId.value = payload.userId || '--'
  } catch { /* ignore */ }
})
</script>

<style scoped>
.profile-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px;
}

.profile-hero {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 28px 32px;
  background: linear-gradient(135deg, #e4393c 0%, #ff6b6b 100%);
  border-radius: 12px;
  color: #fff;
  margin-bottom: 20px;
}

.hero-avatar :deep(.n-avatar) {
  background: rgba(255, 255, 255, 0.25);
  color: #fff;
}

.hero-greeting {
  margin: 0 0 6px 0;
  font-size: 22px;
  font-weight: 600;
}

.hero-sub {
  margin: 0;
  font-size: 14px;
  opacity: 0.85;
}

.profile-cards {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px 40px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.info-label {
  font-size: 13px;
  color: #909399;
}

.info-value {
  font-size: 15px;
  color: #303133;
}

.quick-actions {
  display: flex;
  gap: 12px;
}

.quick-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 20px 16px;
  border-radius: 10px;
  background: #f5f7fa;
  cursor: pointer;
  transition: all 0.25s;
  color: #606266;
  font-size: 14px;
}

.quick-item:hover {
  background: #fef0f0;
  color: #e4393c;
}
</style>
