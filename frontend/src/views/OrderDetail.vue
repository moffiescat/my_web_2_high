<template>
  <div class="page-container">
    <div class="mb-4">
      <n-button text @click="$router.push('/order/list')">← 返回订单列表</n-button>
    </div>
    <n-card v-if="order" title="订单详情" :bordered="true" style="border-radius:12px;max-width:640px;">
      <div class="info-grid">
        <div class="info-item">
          <span class="info-label">订单编号</span>
          <span class="info-value">{{ order.id }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">商品名称</span>
          <span class="info-value">{{ order.goodsName }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">订单金额</span>
          <span class="info-value price">¥{{ order.goodsPrice }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">订单状态</span>
          <n-tag :type="statusType(order.status)" size="small">{{ statusText(order.status) }}</n-tag>
        </div>
        <div class="info-item">
          <span class="info-label">创建时间</span>
          <span class="info-value">{{ order.createTime }}</span>
        </div>
        <div class="info-item" v-if="order.payTime">
          <span class="info-label">支付时间</span>
          <span class="info-value">{{ order.payTime }}</span>
        </div>
      </div>
      <template v-if="order.status === 0" #action>
        <n-button type="error" @click="handleCancel">取消订单</n-button>
      </template>
    </n-card>
    <n-empty v-else description="订单不存在" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOrderDetail, cancelOrder } from '../api/order'

const route = useRoute()
const router = useRouter()
const order = ref(null)

const statusMap = { 0: '待支付', 1: '已支付', 2: '已取消' }
const statusTypeMap = { 0: 'warning', 1: 'success', 2: 'default' }

function statusText(code) { return statusMap[code] || '未知' }
function statusType(code) { return statusTypeMap[code] || 'default' }

onMounted(async () => {
  const res = await getOrderDetail(route.params.id).catch(() => ({ data: null }))
  order.value = res.data
})

async function handleCancel() {
  try {
    await cancelOrder(order.value.id)
    window.$message.success('订单已取消')
    order.value.status = 2
  } catch { /* error handled by interceptor */ }
}
</script>

<style scoped>
.info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px 40px; }
.info-item { display: flex; flex-direction: column; gap: 6px; }
.info-label { font-size: 13px; color: #909399; }
.info-value { font-size: 15px; color: #303133; }
.price { color: #e4393c; font-weight: 600; }
</style>
