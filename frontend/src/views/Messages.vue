<template>
  <div class="page-container">
    <n-card title="消息中心" :bordered="true" style="border-radius:12px;">
      <template #header-extra>
        <n-button v-if="notifications.length > 0" text size="small" @click="handleMarkAllRead">全部已读</n-button>
      </template>
      <n-empty v-if="!loading && notifications.length === 0" description="暂无消息">
        <template #icon>
          <n-icon size="64"><NotificationsOutline /></n-icon>
        </template>
      </n-empty>
      <n-spin :show="loading">
        <n-timeline v-if="notifications.length > 0" style="margin-top:20px;padding:0 40px;">
          <n-timeline-item
            v-for="n in notifications"
            :key="n.id"
            :time="n.createTime"
            :type="timelineType(n)"
          >
            <n-card :bordered="true" hoverable style="border-radius:8px;" :class="{ 'unread': n.isRead === 0 }">
              <h4 style="margin:0 0 4px;">{{ n.title }}</h4>
              <p style="margin:0;color:#999;font-size:13px;">{{ n.content }}</p>
            </n-card>
          </n-timeline-item>
        </n-timeline>
      </n-spin>
    </n-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { NIcon } from 'naive-ui'
import { NotificationsOutline } from '@vicons/ionicons5'
import { getNotifications, markAllRead } from '../api/notification'

const notifications = ref([])
const loading = ref(false)

const typeMap = { seckill: 'success', order: 'info', system: 'default' }
function timelineType(n) { return typeMap[n.type] || 'info' }

onMounted(() => loadNotifications())

async function loadNotifications() {
  loading.value = true
  const res = await getNotifications().catch(() => ({ data: null }))
  notifications.value = res.data || []
  loading.value = false
}

async function handleMarkAllRead() {
  await markAllRead().catch(() => {})
  notifications.value.forEach(n => n.isRead = 1)
  window.$message.success('已全部标为已读')
}
</script>

<style scoped>
.unread :deep(.n-card) {
  border-left: 3px solid #e4393c;
}
</style>
