<template>
  <div class="home">
    <el-header class="header">
      <h1>秒杀商城</h1>
      <div>
        <template v-if="userStore.isLoggedIn()">
          <el-button @click="$router.push('/order/list')">我的订单</el-button>
          <el-button @click="handleLogout">退出登录</el-button>
        </template>
        <template v-else>
          <el-button type="primary" @click="$router.push('/login')">登录</el-button>
          <el-button @click="$router.push('/register')">注册</el-button>
        </template>
      </div>
    </el-header>

    <el-main>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="秒杀活动" name="seckill" />
        <el-tab-pane label="全部商品" name="all" />
      </el-tabs>

      <!-- 秒杀商品列表 -->
      <div v-if="activeTab === 'seckill'" class="goods-grid">
        <div v-for="item in seckillList" :key="item.id" class="goods-card" @click="goDetail(item.goodsId)">
          <div class="goods-img">{{ item.goodsName?.charAt(0) }}</div>
          <div class="goods-info">
            <h3>{{ item.goodsName }}</h3>
            <p class="price">
              <span class="seckill-price">¥{{ item.seckillPrice }}</span>
              <span class="origin-price">¥{{ item.goodsPrice }}</span>
            </p>
            <p class="stock">剩余 {{ item.stockCount }} 件</p>
            <Countdown :end-time="item.endTime" :start-time="item.startTime" />
            <el-button v-if="item.status === 1" type="danger" @click.stop="goDetail(item.goodsId)">立即秒杀</el-button>
            <el-tag v-else-if="item.status === 0">即将开始</el-tag>
            <el-tag v-else type="info">已结束</el-tag>
          </div>
        </div>
      </div>

      <!-- 全部商品列表 -->
      <div v-if="activeTab === 'all'" class="goods-grid">
        <div v-for="item in goodsList" :key="item.id" class="goods-card" @click="goDetail(item.id)">
          <div class="goods-img">{{ item.goodsName?.charAt(0) }}</div>
          <div class="goods-info">
            <h3>{{ item.goodsName }}</h3>
            <p>{{ item.goodsTitle }}</p>
            <p class="price">¥{{ item.goodsPrice }}</p>
          </div>
        </div>
      </div>
    </el-main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getGoodsList, getSeckillGoods } from '../api/goods'
import { useUserStore } from '../store/user'
import Countdown from '../components/Countdown.vue'

const router = useRouter()
const userStore = useUserStore()
const activeTab = ref('seckill')
const goodsList = ref([])
const seckillList = ref([])

onMounted(async () => {
  const [r1, r2] = await Promise.all([
    getGoodsList().catch(() => ({ data: [] })),
    getSeckillGoods().catch(() => ({ data: [] }))
  ])
  goodsList.value = r1.data || []
  seckillList.value = r2.data || []
})

function goDetail(id) {
  router.push(`/detail/${id}`)
}

function handleLogout() {
  userStore.logout()
  location.reload()
}
</script>

<style scoped>
.header {
  display: flex; justify-content: space-between; align-items: center;
  background: #fff; border-bottom: 1px solid #eee; padding: 0 24px; height: 64px;
}
.header h1 { color: #e4393c; margin: 0; }
.el-main { max-width: 1200px; margin: 0 auto; padding: 20px; }
.goods-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 20px;
}
.goods-card {
  border: 1px solid #eee; border-radius: 8px; overflow: hidden;
  cursor: pointer; transition: box-shadow .3s;
}
.goods-card:hover { box-shadow: 0 4px 16px rgba(0,0,0,.1); }
.goods-img {
  height: 180px; background: #ecf5ff; display: flex;
  align-items: center; justify-content: center; font-size: 48px; color: #409eff;
}
.goods-info { padding: 16px; }
.goods-info h3 { margin: 0 0 8px; }
.price { color: #e4393c; font-size: 20px; font-weight: bold; }
.seckill-price { color: #e4393c; }
.origin-price { color: #999; font-size: 14px; text-decoration: line-through; margin-left: 8px; }
.stock { color: #999; font-size: 13px; margin: 4px 0; }
</style>
