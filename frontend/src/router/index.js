import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../store/user'

const routes = [
  { path: '/', name: 'Home', component: () => import('../views/Home.vue') },
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue') },
  { path: '/register', name: 'Register', component: () => import('../views/Register.vue') },
  { path: '/detail/:id', name: 'Detail', component: () => import('../views/Detail.vue') },
  { path: '/order/list', name: 'OrderList', component: () => import('../views/OrderList.vue') },
  { path: '/profile', name: 'Profile', component: () => import('../views/Profile.vue') },
  { path: '/messages', name: 'Messages', component: () => import('../views/Messages.vue') },
  { path: '/cart', name: 'Cart', component: () => import('../views/Cart.vue') },
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

const publicRoutes = ['/login', '/register']

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  if (!userStore.isLoggedIn() && !publicRoutes.includes(to.path)) {
    next('/login')
  } else if (userStore.isLoggedIn() && publicRoutes.includes(to.path)) {
    next('/')
  } else {
    next()
  }
})

export default router
