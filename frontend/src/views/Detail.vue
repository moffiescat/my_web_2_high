<template>
  <div class="page-container">
    <div class="mb-4">
      <n-button text @click="$router.push('/')">← 返回首页</n-button>
    </div>
    <div v-if="goods" class="detail-layout">
      <div class="detail-thumb" :class="'g' + (goods.id % 6 + 1)">
        {{ goods.goodsName?.charAt(0) }}
      </div>
      <div class="detail-info">
        <h2>{{ goods.goodsName }}</h2>
        <p class="detail-subtitle">{{ goods.goodsTitle }}</p>

        <!-- 秒杀价格 -->
        <div v-if="goods.seckillPrice" class="seckill-price-box">
          <div class="label">秒杀价</div>
          <div>
            <span class="price">¥{{ goods.seckillPrice }}</span>
            <span class="origin">原价 ¥{{ goods.goodsPrice }}</span>
          </div>
          <div style="display:flex;align-items:center;gap:16px;margin-top:12px;">
            <Countdown :start-time="goods.startTime" :end-time="goods.endTime" />
          </div>
          <div class="mt-4">
            <n-tag v-if="goods.stockCount > 10" type="success">库存充足 ({{ goods.stockCount }}件)</n-tag>
            <n-tag v-else-if="goods.stockCount > 0" type="warning">库存紧张 (仅剩{{ goods.stockCount }}件)</n-tag>
            <n-tag v-else type="error">已售罄</n-tag>
          </div>
          <div class="mt-4">
            <n-button
              v-if="seckillStatus === 'ready'"
              type="error"
              size="large"
              class="btn-seckill-shake"
              style="width: 220px; height: 50px; font-size: 18px;"
              @click="handleSeckill"
              :loading="seckilling"
            >
              立即秒杀
            </n-button>
            <n-alert v-else-if="seckillStatus === 'queuing'" type="warning" :closable="false">
              <template #header>
                <span style="display:block;text-align:center;">排队中，正在为您抢购...</span>
              </template>
            </n-alert>
            <n-result v-else-if="seckillStatus === 'success'" status="success" title="恭喜！秒杀成功！" description="请前往我的订单查看" />
            <n-result v-else-if="seckillStatus === 'soldout'" status="error" title="很遗憾，商品已售罄" description="下次早点来哦" />
          </div>
        </div>

        <!-- 普通商品价格 -->
        <div v-else>
          <div class="seckill-price-box">
            <div class="label">商品价格</div>
            <span class="price">¥{{ goods.goodsPrice }}</span>
          </div>
        </div>

        <p class="detail-desc">{{ goods.goodsDetail }}</p>
      </div>
    </div>
    <n-empty v-else description="商品不存在" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getGoodsDetail } from '../api/goods'
import { getSeckillPath, executeSeckill, getSeckillResult } from '../api/seckill'
import Countdown from '../components/Countdown.vue'

const route = useRoute()
const goods = ref(null)
const seckilling = ref(false)
const seckillStatus = ref('ready')

onMounted(async () => {
  const res = await getGoodsDetail(route.params.id).catch(() => ({ data: null }))
  goods.value = res.data
})

async function handleSeckill() {
  seckilling.value = true
  try {
    const pathRes = await getSeckillPath(goods.value.id)
    await executeSeckill(pathRes.data, goods.value.id)
    seckillStatus.value = 'queuing'
    pollResult()
  } catch {
    seckilling.value = false
  }
}

function pollResult() {
  let interval = 1000
  const maxInterval = 10000
  let timer = null

  async function poll() {
    try {
      const res = await getSeckillResult(goods.value.id)
      const orderId = res.data
      if (orderId > 0) {
        clearTimeout(timer)
        seckillStatus.value = 'success'
        seckilling.value = false
        window.$message.success(`秒杀成功！订单号: ${orderId}`)
        return
      } else if (orderId === -1) {
        clearTimeout(timer)
        seckillStatus.value = 'soldout'
        seckilling.value = false
        return
      }
    } catch {
      clearTimeout(timer)
      seckillStatus.value = 'ready'
      seckilling.value = false
      return
    }
    // 指数退避: 1s → 2s → 4s → ... → 10s
    interval = Math.min(interval * 2, maxInterval)
    timer = setTimeout(poll, interval)
  }

  timer = setTimeout(poll, interval)
}
</script>
