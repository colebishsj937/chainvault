<template>
  <el-container class="admin-layout">
    <el-aside :width="collapsed ? '64px' : '220px'" class="sidebar">
      <div class="logo">
        <span v-if="!collapsed" class="logo-text">ChainVault</span>
        <span v-else class="logo-icon">CV</span>
      </div>

      <el-menu
        :default-active="activeMenu"
        :collapse="collapsed"
        :collapse-transition="false"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>数据总览</span>
        </el-menu-item>
        <el-menu-item index="/merchant">
          <el-icon><OfficeBuilding /></el-icon>
          <span>商户管理</span>
        </el-menu-item>
        <el-menu-item index="/merchant-api-docs">
          <el-icon><Reading /></el-icon>
          <span>商户接口文档</span>
        </el-menu-item>
        <el-menu-item index="/address">
          <el-icon><MapLocation /></el-icon>
          <span>充值地址</span>
        </el-menu-item>
        <el-menu-item index="/deposit">
          <el-icon><Download /></el-icon>
          <span>充值记录</span>
        </el-menu-item>
        <el-menu-item index="/withdraw">
          <el-icon><Upload /></el-icon>
          <span>提币记录</span>
        </el-menu-item>
        <el-menu-item index="/wallet">
          <el-icon><Wallet /></el-icon>
          <span>热钱包</span>
        </el-menu-item>
        <el-sub-menu index="/sweep">
          <template #title>
            <el-icon><RefreshRight /></el-icon>
            <span>归集</span>
          </template>
          <el-menu-item index="/sweep">归集历史</el-menu-item>
          <el-menu-item index="/sweep/settings">归集配置</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="/webhook">
          <template #title>
            <el-icon><Bell /></el-icon>
            <span>Webhook</span>
          </template>
          <el-menu-item index="/webhook">配置管理</el-menu-item>
          <el-menu-item index="/webhook/log">回调日志</el-menu-item>
        </el-sub-menu>
        <el-menu-item index="/risk">
          <el-icon><Warning /></el-icon>
          <span>风控规则</span>
        </el-menu-item>
        <el-menu-item index="/report">
          <el-icon><Document /></el-icon>
          <span>报表导出</span>
        </el-menu-item>
        <el-menu-item index="/chain-nodes">
          <el-icon><Setting /></el-icon>
          <span>链节点</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="topbar">
        <div class="topbar-left">
          <el-button link :icon="Fold" @click="collapsed = !collapsed" />
        </div>
        <div class="topbar-right">
          <el-badge :value="realtime.pendingCount" :hidden="!realtime.pendingCount">
            <el-icon :size="20"><Bell /></el-icon>
          </el-badge>
          <span class="merchant-info">{{ auth.user?.displayName || auth.user?.username }}</span>
          <el-button type="danger" size="small" @click="handleLogout">退出</el-button>
        </div>
      </el-header>

      <el-main>
        <router-view v-slot="{ Component, route: childRoute }">
          <component :is="Component" v-if="Component" :key="childRoute.fullPath" />
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  DataAnalysis, OfficeBuilding, MapLocation, Download, Upload,
  Wallet, Bell, Warning, Document, Fold, Setting, Reading, RefreshRight,
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useRealtimeStore } from '@/stores/realtime'
import { logout as apiLogout } from '@/api/modules/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const realtime = useRealtimeStore()

const collapsed = ref(false)

const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/webhook')) {
    return path
  }
  if (path.startsWith('/sweep')) {
    return path
  }
  return '/' + path.split('/')[1]
})

async function handleLogout() {
  try {
    await apiLogout()
  } catch {
    // 令牌失效时仍清理本地会话
  }
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.admin-layout {
  height: 100vh;
}
.sidebar {
  background-color: #304156;
  overflow-y: auto;
  transition: width 0.3s;
}
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: bold;
}
.logo-text {
  font-size: 20px;
}
.logo-icon {
  font-size: 18px;
}
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e6e6e6;
}
.topbar-left {
  display: flex;
  align-items: center;
}
.topbar-right {
  display: flex;
  align-items: center;
  gap: 16px;
}
.merchant-info {
  font-size: 13px;
  color: #666;
}
.admin-layout :deep(.el-main) {
  min-height: 0;
  overflow-y: auto;
}
</style>
