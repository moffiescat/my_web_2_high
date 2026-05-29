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

const startTs = new Date(props.startTime).getTime()
const endTs = new Date(props.endTime).getTime()

const now = ref(Date.now())
let timer = null

onMounted(() => { timer = setInterval(() => now.value = Date.now(), 1000) })
onUnmounted(() => { clearInterval(timer) })

function pad(n) { return String(n).padStart(2, '0') }

const countdown = computed(() => {
  let status, diff
  if (now.value < startTs) {
    status = 0
    diff = Math.max(0, startTs - now.value)
  } else if (now.value > endTs) {
    status = 2
    diff = 0
  } else {
    status = 1
    diff = Math.max(0, endTs - now.value)
  }
  const h = pad(Math.floor(diff / 3600000))
  const m = pad(Math.floor((diff % 3600000) / 60000))
  const s = pad(Math.floor((diff % 60000) / 1000))
  return { status, h, m, s }
})

const status = computed(() => countdown.value.status)
const h = computed(() => countdown.value.h)
const m = computed(() => countdown.value.m)
const s = computed(() => countdown.value.s)
</script>

<style scoped>
.ended-text { color: #999; font-size: 14px; }
</style>
