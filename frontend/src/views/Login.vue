<template>
  <div class="auth-page">
    <div class="auth-left">
      <div class="brand-area">
        <div class="brand-icon">秒</div>
        <h1>Seckill Mall</h1>
        <p>高并发秒杀系统<br>极致性能，限时抢购</p>
      </div>
    </div>
    <div class="auth-right">
      <div class="auth-card">
        <h2>欢迎回来</h2>
        <p class="auth-subtitle">登录您的账号参与秒杀</p>
        <n-form :model="form" :rules="rules" ref="formRef" size="large">
          <n-form-item path="phone">
            <n-input v-model:value="form.phone" placeholder="手机号" />
          </n-form-item>
          <n-form-item path="password">
            <n-input v-model:value="form.password" type="password" placeholder="密码" show-password-on="click" />
          </n-form-item>
          <n-form-item>
            <n-button type="error" size="large" @click="handleLogin" :loading="loading" block>
              登 录
            </n-button>
          </n-form-item>
        </n-form>
        <p style="text-align:center;color:#999;font-size:14px;">
          还没有账号？<router-link to="/register">立即注册</router-link>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '../api/user'
import { useUserStore } from '../store/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)
const form = reactive({ phone: '', password: '' })
const rules = {
  phone: [{ required: true, message: '请输入手机号' }],
  password: [{ required: true, message: '请输入密码' }]
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const res = await login(form.phone, form.password)
    userStore.setToken(res.data)
    window.$message.success('登录成功')
    router.push('/')
  } finally {
    loading.value = false
  }
}
</script>
