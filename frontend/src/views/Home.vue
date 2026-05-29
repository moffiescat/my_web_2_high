<template>
  <div class="page-container">
    <div class="seckill-banner">
      <div class="banner-left">
        <h2>限时秒杀</h2>
        <p>每日精选好物，低至1折起</p>
      </div>
      <div class="banner-right">FLASH<br>SALE</div>
    </div>

    <n-tabs v-model:value="activeTab" type="line" animated>
      <n-tab-pane name="seckill" tab="秒杀活动" />
      <n-tab-pane name="all" tab="全部商品" />
    </n-tabs>

    <div v-if="activeTab === 'seckill'" class="goods-grid">
      <div v-for="item in seckillList" :key="item.id" class="goods-card" @click="goDetail(item.goodsId)">
        <div class="goods-thumb" :class="'g' + (item.goodsId % 6 + 1)">
          <span class="seckill-tag">秒杀</span>
          {{ item.goodsName?.charAt(0) }}
        </div>
        <div class="goods-info">
          <h3>{{ item.goodsName }}</h3>
          <div class="price-row">
            <span class="current-price">¥{{ item.seckillPrice }}</span>
            <span class="origin-price">¥{{ item.goodsPrice }}</span>
          </div>
          <div class="countdown-box" :class="{ running: item.status === 1, ending: item.status === 1 }">
            <span class="countdown-label">{{ item.status === 1 ? '距结束' : '距开始' }}</span>
            <template v-if="item.status !== 2">
              <span class="time-block">{{ countdown(item).h }}</span>
              <span class="time-sep">:</span>
              <span class="time-block">{{ countdown(item).m }}</span>
              <span class="time-sep">:</span>
              <span class="time-block">{{ countdown(item).s }}</span>
            </template>
            <span v-else style="color:#999;">已结束</span>
          </div>
          <div v-if="item.status === 1" class="seckill-progress">
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: Math.max(10, 100 - (item.stockCount / 50 * 100)) + '%' }" />
            </div>
            <div class="progress-text">
              <span>已抢 {{ Math.max(10, 100 - item.stockCount * 2) }}%</span>
              <span style="color: var(--color-primary);">仅剩 {{ item.stockCount }} 件</span>
            </div>
          </div>
          <div style="margin-top: 12px;">
            <n-button v-if="item.status === 1" type="error" @click.stop="goDetail(item.goodsId)" class="btn-seckill-shake" block>
              立即秒杀
            </n-button>
            <span v-else-if="item.status === 0" class="status-tag upcoming">即将开始</span>
            <span v-else class="status-tag ended">已结束</span>
          </div>
        </div>
      </div>
    </div>

    <div v-if="activeTab === 'all'" class="goods-grid">
      <div v-for="(item, idx) in goodsList" :key="item.id" class="goods-card" @click="goDetail(item.id)">
        <div class="goods-thumb" :class="'g' + (idx % 6 + 1)">
          {{ item.goodsName?.charAt(0) }}
        </div>
        <div class="goods-info">
          <h3>{{ item.goodsName }}</h3>
          <p class="subtitle">{{ item.goodsTitle }}</p>
          <div class="price-row">
            <span class="current-price">¥{{ item.goodsPrice }}</span>
          </div>
          <div v-if="item.seckillPrice" style="margin-top: 8px;">
            <n-tag size="small" type="error" :bordered="false">秒杀 ¥{{ item.seckillPrice }}</n-tag>
          </div>
        </div>
      </div>
      <n-empty v-if="goodsList.length === 0" description="暂无商品" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { getGoodsList, getSeckillGoods } from '../api/goods'

const router = useRouter()
const activeTab = ref('seckill')
const goodsList = ref([])
const seckillList = ref([])
const now = ref(Date.now())
let timer = null

onMounted(async () => {
  timer = setInterval(() => now.value = Date.now(), 1000)
  const [r1, r2] = await Promise.all([
    getGoodsList().catch(() => ({ data: [] })),
    getSeckillGoods().catch(() => ({ data: [] }))
  ])
  goodsList.value = r1.data || []
  seckillList.value = r2.data || []
})

onUnmounted(() => clearInterval(timer))

function goDetail(id) {
  router.push(`/detail/${id}`)
}

function countdown(item) {
  const target = item.status === 0
    ? new Date(item.startTime).getTime()
    : new Date(item.endTime).getTime()
  const diff = Math.max(0, target - now.value)
  return {
    h: String(Math.floor(diff / 3600000)).padStart(2, '0'),
    m: String(Math.floor((diff % 3600000) / 60000)).padStart(2, '0'),
    s: String(Math.floor((diff % 60000) / 1000)).padStart(2, '0')
  }
}
</script>

<style scoped>
.n-tabs {
  margin-bottom: 20px;
}
</style>
