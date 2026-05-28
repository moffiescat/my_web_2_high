<template>
  <div class="detail">
    <el-header class="header">
      <el-button @click="$router.push('/')">返回首页</el-button>
      <h1>商品详情</h1>
      <div></div>
    </el-header>

    <el-main v-if="goods">
      <el-row :gutter="40">
        <el-col :span="10">
          <div class="goods-img">{{ goods.goodsName?.charAt(0) }}</div>
        </el-col>
        <el-col :span="14">
          <h2>{{ goods.goodsName }}</h2>
          <p class="title">{{ goods.goodsTitle }}</p>

          <!-- 秒杀价格区 -->
          <div v-if="goods.seckillPrice" class="seckill-box">
            <p class="seckill-price">秒杀价: ¥{{ goods.seckillPrice }}</p>
            <p class="origin-price">原价: ¥{{ goods.goodsPrice }}</p>
            <p>库存: {{ goods.stockCount }} 件</p>
            <Countdown :start-time="goods.startTime" :end-time="goods.endTime" />

            <!-- 秒杀按钮 -->
            <div v-if="seckillStatus === 'ready'" class="seckill-action">
              <el-button type="danger" size="large" @click="handleSeckill" :loading="seckilling">
                立即秒杀
              </el-button>
            </div>
            <el-tag v-else-if="seckillStatus === 'queuing'" type="warning" size="large">
              排队中...
            </el-tag>
            <el-alert v-else-if="seckillStatus === 'success'" title="恭喜！秒杀成功！" type="success" :closable="false" />
            <el-alert v-else-if="seckillStatus === 'fail'" title="很遗憾，秒杀失败" type="error" :closable="false" />
            <el-alert v-else-if="seckillStatus === 'soldout'" title="商品已售罄" type="info" :closable="false" />
          </div>

          <!-- 普通商品价格 -->
          <div v-else class="normal-box">
            <p class="price">¥{{ goods.goodsPrice }}</p>
          </div>

          <p class="detail-text">{{ goods.goodsDetail }}</p>
        </el-col>
      </el-row>
    </el-main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getGoodsDetail } from '../api/goods'
import { getSeckillPath, executeSeckill, getSeckillResult } from '../api/seckill'
import { ElMessage } from 'element-plus'
import Countdown from '../components/Countdown.vue'

const route = useRoute()
const goods = ref(null)
const seckilling = ref(false)
const seckillStatus = ref('ready')  // ready | queuing | success | fail | soldout

const isSeckillActive = computed(() => {
  if (!goods.value?.startTime) return false
  const now = Date.now()
  return now >= new Date(goods.value.startTime).getTime() && now <= new Date(goods.value.endTime).getTime()
})

onMounted(async () => {
  const res = await getGoodsDetail(route.params.id).catch(() => ({ data: null }))
  goods.value = res.data
})

async function handleSeckill() {
  seckilling.value = true
  try {
    // Step 1: 获取秒杀路径
    const pathRes = await getSeckillPath(goods.value.id)
    const path = pathRes.data

    // Step 2: 执行秒杀
    await executeSeckill(path, goods.value.id)
    seckillStatus.value = 'queuing'

    // Step 3: 轮询结果
    pollResult()
  } catch {
    seckilling.value = false
  }
}

function pollResult() {
  const timer = setInterval(async () => {
    try {
      const res = await getSeckillResult(goods.value.id)
      const orderId = res.data
      if (orderId > 0) {
        clearInterval(timer)
        seckillStatus.value = 'success'
        seckilling.value = false
        ElMessage.success(`秒杀成功！订单号: ${orderId}`)
      } else if (orderId === -1) {
        clearInterval(timer)
        seckillStatus.value = 'soldout'
        seckilling.value = false
      }
    } catch {
      clearInterval(timer)
      seckillStatus.value = 'fail'
      seckilling.value = false
    }
  }, 1000)
}
</script>

<style scoped>
.header { display: flex; justify-content: space-between; align-items: center; height: 64px; }
.el-main { max-width: 1000px; margin: 0 auto; padding: 20px; }
.goods-img {
  height: 360px; background: #ecf5ff; display: flex;
  align-items: center; justify-content: center; font-size: 72px; color: #409eff;
  border-radius: 8px;
}
.title { color: #999; margin-bottom: 16px; }
.seckill-box {
  background: #fff5f5; padding: 20px; border-radius: 8px; border: 1px solid #fde2e2;
}
.seckill-price { color: #e4393c; font-size: 28px; font-weight: bold; }
.origin-price { color: #999; text-decoration: line-through; }
.seckill-action { margin-top: 16px; }
.normal-box .price { color: #e4393c; font-size: 28px; font-weight: bold; }
.detail-text { margin-top: 24px; color: #666; line-height: 1.8; }
</style>
