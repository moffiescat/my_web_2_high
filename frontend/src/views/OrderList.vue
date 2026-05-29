<template>
  <div class="page-container">
    <div class="order-header-bar">
      <h2>我的订单</h2>
    </div>
    <div style="background:#fff;border-radius:12px;padding:24px;box-shadow:0 2px 12px rgba(0,0,0,0.06);">
      <n-data-table
        :columns="columns"
        :data="orders"
        :loading="loading"
        :bordered="false"
        striped
        size="small"
      />
      <n-empty v-if="!loading && orders.length === 0" description="暂无订单，快去抢购吧～" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, h } from 'vue'
import { NButton } from 'naive-ui'
import { getOrderList, cancelOrder } from '../api/order'

const orders = ref([])
const loading = ref(false)

const columns = [
  { title: '订单号', key: 'id', width: 190 },
  {
    title: '商品', key: 'goodsName', minWidth: 200,
    render(row) {
      if (row.goodsName) return row.goodsName
      return h('span', { style: { color: '#ccc' } }, `商品ID: ${row.goodsId}`)
    }
  },
  {
    title: '金额', key: 'goodsPrice', width: 120,
    render(row) {
      return h('span', {
        style: { color: '#e4393c', fontWeight: '600', fontFamily: "'DIN','Helvetica Neue',sans-serif" }
      }, `¥${row.goodsPrice}`)
    }
  },
  {
    title: '状态', key: 'status', width: 110,
    render(row) {
      const map = [
        { cls: 'pending', text: '待支付' },
        { cls: 'paid', text: '已支付' },
        { cls: 'cancelled', text: '已取消' },
      ]
      const s = map[row.status] || map[2]
      return h('span', { class: `status-tag ${s.cls}` }, s.text)
    }
  },
  { title: '创建时间', key: 'createTime', width: 180 },
  {
    title: '操作', key: 'actions', width: 100,
    render(row) {
      if (row.status === 0) {
        return h(NButton, {
          type: 'error', size: 'small', text: true,
          onClick: () => handleCancel(row.id)
        }, { default: () => '取消' })
      }
      return null
    }
  },
]

onMounted(async () => {
  loading.value = true
  const res = await getOrderList().catch(() => ({ data: [] }))
  orders.value = res.data || []
  loading.value = false
})

async function handleCancel(orderId) {
  try {
    await cancelOrder(orderId)
    window.$message.success('订单已取消')
    const res = await getOrderList()
    orders.value = res.data || []
  } catch { /* handled by interceptor */ }
}
</script>
