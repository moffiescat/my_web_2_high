<template>
  <div class="order-list">
    <el-header class="header">
      <el-button @click="$router.push('/')">返回首页</el-button>
      <h1>我的订单</h1>
      <div></div>
    </el-header>

    <el-main>
      <el-table :data="orders" style="width:100%" v-loading="loading">
        <el-table-column prop="id" label="订单号" width="180" />
        <el-table-column prop="goodsName" label="商品" />
        <el-table-column prop="goodsPrice" label="金额" width="120">
          <template #default="{ row }">¥{{ row.goodsPrice }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 0" type="warning">待支付</el-tag>
            <el-tag v-else-if="row.status === 1" type="success">已支付</el-tag>
            <el-tag v-else-if="row.status === 2" type="info">已取消</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" type="danger" size="small" @click="handleCancel(row.id)">
              取消
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && orders.length === 0" description="暂无订单" />
    </el-main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getOrderList, cancelOrder } from '../api/order'
import { ElMessage } from 'element-plus'

const orders = ref([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  const res = await getOrderList().catch(() => ({ data: [] }))
  orders.value = res.data || []
  loading.value = false
})

async function handleCancel(orderId) {
  try {
    await cancelOrder(orderId)
    ElMessage.success('订单已取消')
    const res = await getOrderList()
    orders.value = res.data || []
  } catch { /* error handled by interceptor */ }
}
</script>

<style scoped>
.header { display: flex; justify-content: space-between; align-items: center; height: 64px; }
.el-main { max-width: 1000px; margin: 0 auto; padding: 20px; }
</style>
