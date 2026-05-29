<template>
  <n-config-provider :theme-overrides="themeOverrides">
    <template v-if="isAuthPage">
      <router-view />
    </template>
    <template v-else>
      <header class="top-nav">
        <div class="brand" @click="$router.push('/')">
          <div class="brand-logo">秒</div>
          <div class="brand-name"><span>Seckill</span> Mall</div>
        </div>
        <div class="nav-links">
          <template v-if="userStore.isLoggedIn()">
            <n-button text @click="handleLogout">退出登录</n-button>
          </template>
          <template v-else>
            <n-button text @click="$router.push('/login')">登录</n-button>
            <n-button type="primary" size="small" @click="$router.push('/register')">免费注册</n-button>
          </template>
        </div>
      </header>

      <div class="main-layout">
        <aside class="sidebar" :class="{ 'is-collapsed': collapsed }">
          <n-menu
            :value="activeMenu"
            :options="menuOptions"
            :collapsed="collapsed"
            :collapsed-width="64"
            :collapsed-icon-size="22"
            @update:value="handleMenuNav"
          />
          <div class="sidebar-toggle" @click="collapsed = !collapsed">
            <n-icon size="20">
              <ChevronBack v-if="!collapsed" />
              <ChevronForward v-else />
            </n-icon>
          </div>
        </aside>
        <main class="main-content">
          <router-view />
        </main>
      </div>
    </template>
  </n-config-provider>
</template>

<script setup>
import { computed, ref, h } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from './store/user'
import { NIcon, useMessage } from 'naive-ui'
import {
  HomeOutline, PersonOutline, NotificationsOutline,
  CartOutline, DocumentTextOutline, ChevronBack, ChevronForward
} from '@vicons/ionicons5'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const collapsed = ref(false)

window.$message = useMessage()

const isAuthPage = computed(() => ['Login', 'Register'].includes(route.name))

const activeMenu = computed(() => {
  if (route.path.startsWith('/order')) return '/order/list'
  if (route.path.startsWith('/detail')) return '/'
  return route.path
})

function renderIcon(icon) {
  return () => h(NIcon, null, { default: () => h(icon) })
}

const menuOptions = [
  { label: '首页', key: '/', icon: renderIcon(HomeOutline) },
  { label: '个人中心', key: '/profile', icon: renderIcon(PersonOutline) },
  { label: '消息', key: '/messages', icon: renderIcon(NotificationsOutline) },
  { label: '购物车', key: '/cart', icon: renderIcon(CartOutline) },
  { label: '我的订单', key: '/order/list', icon: renderIcon(DocumentTextOutline) },
]

function handleMenuNav(key) {
  router.push(key)
}

function handleLogout() {
  userStore.logout()
  router.push('/')
}

const themeOverrides = {
  common: {
    primaryColor: '#e4393c',
    primaryColorHover: '#c1272d',
    primaryColorPressed: '#a01f24',
    primaryColorSuppl: '#e4393c',
  },
}
</script>

<style scoped>
.main-layout {
  display: flex;
  min-height: calc(100vh - 64px);
}
.sidebar {
  position: sticky;
  top: 64px;
  height: calc(100vh - 64px);
  background: #fff;
  border-right: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  width: 200px;
  transition: width 0.3s;
}
.sidebar.is-collapsed {
  width: 64px;
}
.sidebar > :deep(.n-menu) {
  flex: 1;
}
.sidebar-toggle {
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #999;
  border-top: 1px solid var(--color-border);
  transition: color 0.2s;
}
.sidebar-toggle:hover {
  color: var(--color-primary);
}
.main-content {
  flex: 1;
  overflow-x: hidden;
}
</style>
