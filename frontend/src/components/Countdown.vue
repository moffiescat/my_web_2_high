<template>
  <div class="countdown-box" :class="{ running: status === 1, ending: status === 1 && h === '00' && +m < 5 }">
    <span class="countdown-label">
      {{ status === 0 ? '距离开始' : status === 1 ? '距结束' : '' }}
    </span>
    <template v-if="status !== 2">
      <span class="time-block">{{ h }}</span>
      <span class="time-sep">:</span>
      <span class="time-block">{{ m }}</span>
      <span class="time-sep">:</span>
      <span class="time-block">{{ s }}</span>
    </template>
    <span v-else class="ended-text">已结束</span>
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

function pad(n) { return String(n).padStart(2, '0') }

const h = computed(() => {
  const target = status.value === 0 ? new Date(props.startTime).getTime() : new Date(props.endTime).getTime()
  const diff = Math.max(0, target - now.value)
  return pad(Math.floor(diff / 3600000))
})

const m = computed(() => {
  const target = status.value === 0 ? new Date(props.startTime).getTime() : new Date(props.endTime).getTime()
  const diff = Math.max(0, target - now.value)
  return pad(Math.floor((diff % 3600000) / 60000))
})

const s = computed(() => {
  const target = status.value === 0 ? new Date(props.startTime).getTime() : new Date(props.endTime).getTime()
  const diff = Math.max(0, target - now.value)
  return pad(Math.floor((diff % 60000) / 1000))
})
</script>

<style scoped>
.ended-text { color: #999; font-size: 14px; }
</style>
