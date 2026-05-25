<template>
  <el-container class="layout">
    <!-- 左侧栏 -->
    <el-aside :width="collapse ? '56px' : '200px'" class="aside">
      <div class="brand">
        <span class="dot"></span>
        <span v-if="!collapse" class="brand-text">2Africa AgriOS</span>
      </div>

      <el-menu
        :default-active="activeMenu"
        :collapse="collapse"
        background-color="#001529"
        text-color="#c5cad1"
        active-text-color="#fff"
        router
        class="menu"
      >
        <el-menu-item index="/">
          <el-icon><HomeIcon /></el-icon>
          <template #title>首页</template>
        </el-menu-item>

        <el-sub-menu index="master">
          <template #title>
            <el-icon><GoodsIcon /></el-icon>
            <span>主数据</span>
          </template>
          <el-menu-item index="/master/crops">作物</el-menu-item>
          <el-menu-item index="/master/varieties">品种</el-menu-item>
          <el-menu-item index="/master/packaging-specs">包装规格</el-menu-item>
          <el-menu-item index="/master/warehouses">仓库库位</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="production">
          <template #title>
            <el-icon><ProductionIcon /></el-icon>
            <span>生产</span>
          </template>
          <el-menu-item index="/production/planting-plans">种植计划</el-menu-item>
          <el-menu-item index="/production/activities">农事记录</el-menu-item>
        </el-sub-menu>

        <el-menu-item index="/demo/files">
          <el-icon><FolderIcon /></el-icon>
          <template #title>文件上传演示</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 顶栏 -->
      <el-header class="topbar">
        <el-button text :icon="collapse ? ExpandIcon : FoldIcon" @click="collapse = !collapse" />
        <div class="topbar-right">
          <span class="welcome">
            {{ auth.nickname || auth.username }}
            <el-tag size="small" type="success" effect="dark" round style="margin-left: 8px">
              {{ auth.roles[0] || 'USER' }}
            </el-tag>
          </span>
          <el-button link type="primary" @click="onLogout">退出登录</el-button>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  HomeFilled as HomeIcon,
  Goods as GoodsIcon,
  Folder as FolderIcon,
  Sunny as ProductionIcon,
  Expand as ExpandIcon,
  Fold as FoldIcon,
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { logout } from '@/api/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const collapse = ref(false)
const activeMenu = computed(() => route.path)

async function onLogout() {
  await ElMessageBox.confirm('确认要退出登录吗?', '提示', { type: 'warning' }).catch(() => null)
  try { await logout() } catch {}
  auth.clear()
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<style scoped>
.layout {
  height: 100vh;
}

.aside {
  background: #001529;
  transition: width .2s;
  overflow: hidden;
}

.brand {
  height: 48px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 14px;
  color: #fff;
  font-weight: 600;
  font-size: 14px;
  letter-spacing: 0.2px;
  border-bottom: 1px solid #1f3a5f;
  white-space: nowrap;
}

.brand .dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #52c41a;
  flex-shrink: 0;
  box-shadow: 0 0 6px rgba(82, 196, 26, 0.6);
}

.menu {
  border-right: none;
}

.menu:not(.el-menu--collapse) {
  width: 200px;
}

.topbar {
  height: 48px !important;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 2px rgba(0, 21, 41, .04);
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.welcome {
  font-size: 13px;
  color: #1f2329;
}

.main {
  background: #f0f2f5;
  overflow: auto;
}
</style>
