<template>
  <div class="countdown">
    <span v-if="status === 0">距离开始: {{ h }}:{{ m }}:{{ s }}</span>
    <span v-else-if="status === 1" class="running">进行中 距结束: {{ h }}:{{ m }}:{{ s }}</span>
    <span v-else class="ended">已结束</span>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  endTime: String,
  startTime: String
})

const now = ref(Date.now())
let timer = null

onMounted(() => { timer = setInterval(() => now.value = Date.now(), 1000) })
onUnmounted(() => { clearInterval(timer) })

const status = computed(() => {
  const s = new Date(props.startTime).getTime()
  const e = new Date(props.endTime).getTime()
  if (now.value < s) return 0
  if (now.value > e) return 2
  return 1
})

const h = computed(() => {
  const target = status.value === 0 ? new Date(props.startTime).getTime() : new Date(props.endTime).getTime()
  const diff = Math.max(0, target - now.value)
  return String(Math.floor(diff / 3600000)).padStart(2, '0')
})

const m = computed(() => {
  const target = status.value === 0 ? new Date(props.startTime).getTime() : new Date(props.endTime).getTime()
  const diff = Math.max(0, target - now.value)
  return String(Math.floor((diff % 3600000) / 60000)).padStart(2, '0')
})

const s = computed(() => {
  const target = status.value === 0 ? new Date(props.startTime).getTime() : new Date(props.endTime).getTime()
  const diff = Math.max(0, target - now.value)
  return String(Math.floor((diff % 60000) / 1000)).padStart(2, '0')
})
</script>

<style scoped>
.countdown { font-size: 14px; color: #333; margin: 8px 0; }
.running { color: #e4393c; font-weight: bold; }
.ended { color: #999; }
</style>
