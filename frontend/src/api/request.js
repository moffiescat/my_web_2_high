import axios from 'axios'
import { useUserStore } from '../store/user'
import router from '../router'

const service = axios.create({
  baseURL: '/api',
  timeout: 10000
})

service.interceptors.request.use(config => {
  const userStore = useUserStore()
  if (userStore.token) {
    config.headers.Authorization = `Bearer ${userStore.token}`
  }
  return config
})

service.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      window.$message?.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message))
    }
    return res
  },
  error => {
    if (error.response?.status === 401) {
      const userStore = useUserStore()
      userStore.logout()
      window.$message?.error('登录已过期，请重新登录')
      router.push('/login')
    } else {
      window.$message?.error(error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default service
