<template>
  <div class="m-app">
    <!-- 顶栏 -->
    <header class="m-topbar">
      <div class="m-brand">
        <span class="m-logo">2A</span>
        <span class="m-title">AgriOS</span>
      </div>
      <div class="m-topbar-right">
        <span v-if="!online" class="m-offline-badge">📡 {{ t('m.offline') }}</span>
        <span v-if="pendingSync > 0" class="m-sync-badge" :title="t('m.syncPending', { n: pendingSync })">
          ⏳ {{ pendingSync }}
        </span>
        <el-dropdown trigger="click" @command="onLangChange">
          <span class="m-lang">{{ currentLangLabel }} ▼</span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item v-for="l in SUPPORT_LOCALES" :key="l.code" :command="l.code">
                {{ l.label }}
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <!-- 内容区 (路由出口) -->
    <main class="m-main">
      <router-view />
    </main>

    <!-- 底部 nav -->
    <nav class="m-bottom-nav">
      <router-link to="/m/" class="m-nav-item" exact-active-class="active">
        <span class="m-nav-icon">🏠</span>
        <span class="m-nav-label">{{ t('m.home') }}</span>
      </router-link>
      <router-link to="/m/activity/new" class="m-nav-item" active-class="active">
        <span class="m-nav-icon">📝</span>
        <span class="m-nav-label">{{ t('m.activity') }}</span>
      </router-link>
      <router-link to="/m/harvest/new" class="m-nav-item" active-class="active">
        <span class="m-nav-icon">🌾</span>
        <span class="m-nav-label">{{ t('m.harvest') }}</span>
      </router-link>
      <router-link to="/m/tasks" class="m-nav-item" active-class="active">
        <span class="m-nav-icon">✓</span>
        <span class="m-nav-label">{{ t('m.tasks') }}</span>
        <span v-if="pendingTasks > 0" class="m-nav-badge">{{ pendingTasks }}</span>
      </router-link>
      <a href="#" class="m-nav-item" @click.prevent="onLogout">
        <span class="m-nav-icon">👤</span>
        <span class="m-nav-label">{{ t('m.me') }}</span>
      </a>
    </nav>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { SUPPORT_LOCALES, persistLocale } from '@/i18n'
import { countPending, flush as flushQueue } from '@/utils/offlineQueue'

const { t, locale } = useI18n()
const router = useRouter()
const auth = useAuthStore()

// 当前语言显示
const currentLangLabel = computed(() => {
  const opt = SUPPORT_LOCALES.find(l => l.code === locale.value)
  return opt?.flag || locale.value.toUpperCase()
})

function onLangChange(code) {
  locale.value = code
  persistLocale(code)
}

// 网络状态监听 + 离线队列联动
const online = ref(navigator.onLine)
const pendingSync = ref(0)
let pendingPoll = null

async function refreshPending() {
  try { pendingSync.value = await countPending() } catch { /* IndexedDB not ready */ }
}

async function onOnline()  {
  online.value = true
  await refreshPending()
  if (pendingSync.value > 0) {
    // Auto-flush in background, no spinner - user just sees count go down
    const res = await flushQueue().catch(e => { console.error('flush:', e); return null })
    if (res?.ok > 0) {
      ElMessage.success(t('m.syncedN', { n: res.ok }))
    }
    await refreshPending()
  }
}
function onOffline() { online.value = false }

onMounted(() => {
  window.addEventListener('online',  onOnline)
  window.addEventListener('offline', onOffline)
  refreshPending()
  // poll every 5s to catch enqueues from child pages (no built-in cross-component event)
  pendingPoll = setInterval(refreshPending, 5000)
})
onUnmounted(() => {
  window.removeEventListener('online',  onOnline)
  window.removeEventListener('offline', onOffline)
  if (pendingPoll) clearInterval(pendingPoll)
})

// 待办任务徽章 (待 20.6 接入 action_item)
const pendingTasks = ref(0)

async function onLogout() {
  await ElMessageBox.confirm(t('m.logoutConfirm'), t('common.tip'), { type: 'warning' })
    .catch(() => Promise.reject('cancel'))
  auth.clear()
  router.push('/login')
}
</script>

<style scoped>
.m-app {
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
  padding-top: env(safe-area-inset-top);
  padding-bottom: calc(64px + env(safe-area-inset-bottom));
}

/* 顶栏 */
.m-topbar {
  position: sticky;
  top: 0;
  z-index: 100;
  height: 52px;
  background: linear-gradient(135deg, #0f3a26 0%, #1f7a35 55%, #2BA84A 100%);
  color: #fff;
  padding: 0 14px;
  display: flex; align-items: center; justify-content: space-between;
  box-shadow: 0 2px 8px rgba(15, 58, 38, 0.18);
}
.m-brand { display: inline-flex; align-items: center; gap: 8px; }
.m-logo {
  display: inline-flex; align-items: center; justify-content: center;
  width: 32px; height: 32px;
  background: rgba(255,255,255,0.18);
  border-radius: 8px;
  font-weight: 800; font-size: 13px;
}
.m-title { font-weight: 700; font-size: 16px; letter-spacing: 0.5px; }
.m-topbar-right { display: inline-flex; align-items: center; gap: 8px; }
.m-offline-badge {
  background: rgba(255,255,255,0.22);
  padding: 3px 8px; border-radius: 12px; font-size: 11px;
}
.m-sync-badge {
  background: #fbbf24;
  color: #78350f;
  font-weight: 600;
  padding: 3px 8px; border-radius: 12px; font-size: 11px;
}
.m-lang {
  background: rgba(255,255,255,0.18);
  padding: 5px 10px; border-radius: 14px; font-size: 12px;
  cursor: pointer;
}

/* 主区 */
.m-main {
  flex: 1;
  padding: 14px;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

/* 底部 nav */
.m-bottom-nav {
  position: fixed;
  bottom: 0; left: 0; right: 0;
  height: 64px;
  padding-bottom: env(safe-area-inset-bottom);
  background: #fff;
  border-top: 1px solid #e4e7ed;
  box-shadow: 0 -2px 12px rgba(0,0,0,0.06);
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  z-index: 100;
}
.m-nav-item {
  position: relative;
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  gap: 2px;
  text-decoration: none;
  color: #909399;
  font-size: 11px;
  transition: color 0.15s;
}
.m-nav-item.active { color: #1f7a35; }
.m-nav-item.active .m-nav-icon { transform: scale(1.1); }
.m-nav-icon { font-size: 22px; transition: transform 0.15s; }
.m-nav-label { font-size: 10px; }
.m-nav-badge {
  position: absolute;
  top: 6px; right: 50%;
  margin-right: -22px;
  min-width: 16px; height: 16px;
  padding: 0 4px;
  background: #f56c6c;
  color: #fff; font-size: 10px; font-weight: 700;
  border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
}
</style>
