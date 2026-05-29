<template>
  <div class="auth-page">
    <div class="auth-left">
      <div class="brand-area">
        <div class="brand-icon">秒</div>
        <h1>Seckill Mall</h1>
        <p>注册即享秒杀资格<br>好货低价，不容错过</p>
      </div>
    </div>
    <div class="auth-right">
      <div class="auth-card">
        <h2>创建账号</h2>
        <p class="auth-subtitle">注册后即可参与秒杀活动</p>
        <n-form :model="form" :rules="rules" ref="formRef" size="large">
          <n-form-item path="nickname">
            <n-input v-model:value="form.nickname" placeholder="昵称" />
          </n-form-item>
          <n-form-item path="phone">
            <n-input v-model:value="form.phone" placeholder="手机号" />
          </n-form-item>
          <n-form-item path="password">
            <n-input v-model:value="form.password" type="password" placeholder="密码" show-password-on="click" />
          </n-form-item>
          <n-form-item path="rePassword">
            <n-input v-model:value="form.rePassword" type="password" placeholder="确认密码" show-password-on="click" />
          </n-form-item>
          <n-form-item>
            <n-button type="error" size="large" @click="handleRegister" :loading="loading" block>
              注 册
            </n-button>
          </n-form-item>
        </n-form>
        <p style="text-align:center;color:#999;font-size:14px;">
          已有账号？<router-link to="/login">立即登录</router-link>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '../api/user'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const form = reactive({ nickname: '', phone: '', password: '', rePassword: '' })

const validateRePass = (_rule, value) => {
  if (value !== form.password) return new Error('两次密码不一致')
  return true
}

const rules = {
  nickname: [{ required: true, message: '请输入昵称' }],
  phone: [{ required: true, message: '请输入手机号' }],
  password: [{ required: true, message: '请输入密码' }],
  rePassword: [
    { required: true, message: '请再次输入密码' },
    { validator: validateRePass }
  ]
}

async function handleRegister() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await register(form.nickname, form.phone, form.password)
    window.$message.success('注册成功，请登录')
    router.push('/login')
  } finally {
    loading.value = false
  }
}
</script>
