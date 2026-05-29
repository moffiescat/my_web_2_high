<template>
  <div class="page-container">
    <n-card title="购物车" :bordered="true" style="border-radius:12px;">
      <template #header-extra>
        <n-tag size="small" round :bordered="false">{{ cartItems.length }} 件商品</n-tag>
      </template>
      <div v-if="cartItems.length > 0">
        <n-data-table
          :columns="cartColumns"
          :data="cartItems"
          :bordered="false"
          size="small"
          :loading="loading"
        />
        <div style="text-align:right;padding:20px 0;">
          <span style="font-size:16px;margin-right:16px;">
            合计: <span style="color:#e4393c;font-size:24px;font-weight:700;font-family:'DIN','Helvetica Neue',sans-serif;">¥{{ total.toFixed(2) }}</span>
          </span>
          <n-button type="error" size="large" @click="handleCheckout">立即结算</n-button>
        </div>
      </div>
      <n-empty v-else description="购物车是空的，快去逛逛吧～">
        <template #extra>
          <n-button type="error" @click="$router.push('/')">去逛逛</n-button>
        </template>
      </n-empty>
    </n-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, h } from 'vue'
import { NButton, NInputNumber } from 'naive-ui'
import { getCartList, updateCartQuantity, removeFromCart } from '../api/cart'

const cartItems = ref([])
const loading = ref(false)

const total = computed(() => {
  return cartItems.value.reduce((sum, item) => {
    const price = item.seckillPrice || item.goodsPrice
    return sum + (price || 0) * item.quantity
  }, 0)
})

const cartColumns = [
  {
    title: '商品', key: 'goodsName', minWidth: 300,
    render(row) {
      return h('div', { style: { display: 'flex', alignItems: 'center', gap: '12px' } }, [
        h('div', { class: `cart-thumb g${(row.goodsId % 6) + 1}` }, row.goodsName?.charAt(0)),
        h('div', {}, [
          h('p', { style: { margin: 0, fontWeight: '600' } }, row.goodsName),
          h('p', { style: { margin: '4px 0 0', color: '#999', fontSize: '12px' } },
            row.seckillPrice ? `秒杀价 ¥${row.seckillPrice}` : `原价 ¥${row.goodsPrice}`),
        ])
      ])
    }
  },
  {
    title: '单价', key: 'price', width: 120,
    render(row) {
      const price = row.seckillPrice || row.goodsPrice
      return h('span', { style: { color: '#e4393c', fontWeight: '600' } }, `¥${price}`)
    }
  },
  {
    title: '数量', key: 'quantity', width: 120,
    render(row, index) {
      return h(NInputNumber, {
        value: row.quantity,
        min: 1, max: 10, size: 'small',
        onUpdateValue: (v) => { onQuantityChange(row, index, v) }
      })
    }
  },
  {
    title: '小计', key: 'subtotal', width: 120,
    render(row) {
      const price = row.seckillPrice || row.goodsPrice
      const subtotal = ((price || 0) * row.quantity).toFixed(2)
      return h('span', {
        style: { color: '#e4393c', fontWeight: '700', fontFamily: "'DIN','Helvetica Neue',sans-serif" }
      }, `¥${subtotal}`)
    }
  },
  {
    title: '操作', key: 'actions', width: 80,
    render(row) {
      return h(NButton, {
        type: 'error', text: true, size: 'small',
        onClick: () => onRemove(row)
      }, { default: () => '删除' })
    }
  },
]

onMounted(() => loadCart())

async function loadCart() {
  loading.value = true
  const res = await getCartList().catch(() => ({ data: null }))
  cartItems.value = res.data || []
  loading.value = false
}

async function onQuantityChange(row, index, v) {
  cartItems.value[index].quantity = v
  await updateCartQuantity(row.id, v).catch(() => {})
}

async function onRemove(row) {
  await removeFromCart(row.id).catch(() => {})
  cartItems.value = cartItems.value.filter(item => item.id !== row.id)
  window.$message.success('已删除')
}

function handleCheckout() {
  window.$message.info('结算功能开发中...')
}
</script>

<style scoped>
.cart-thumb {
  width: 64px; height: 64px; border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  font-size: 24px; color: #fff; flex-shrink: 0;
}
</style>
