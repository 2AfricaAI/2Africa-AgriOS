<template>
  <el-container class="portal">
    <el-header class="topbar">
      <div class="brand-wrap">
        <img src="/logo.svg" class="logo-full" alt="2Africa AgriOS" />
        <span class="portal-tag">{{ t('portal.tag') }}</span>
      </div>
      <div class="topbar-right">
        <!-- Lang switcher -->
        <el-dropdown trigger="click" @command="onLocaleChange">
          <el-button text class="lang-btn">
            <span class="lang-flag">{{ currentLocaleFlag }}</span>
            <span class="lang-label">{{ currentLocaleLabel }}</span>
            <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item
                v-for="loc in SUPPORT_LOCALES"
                :key="loc.code"
                :command="loc.code"
                :disabled="loc.code === locale"
              >
                <span class="lang-flag-item">{{ loc.flag }}</span>
                {{ loc.label }}
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <span class="welcome">
          {{ t('portal.hello', { name: auth.nickname || auth.username }) }}
        </span>
        <el-button link type="primary" @click="onLogout">{{ t('common.logout') }}</el-button>
      </div>
    </el-header>

    <el-container>
      <!-- Slim left nav with just 3 entries -->
      <el-aside width="180px" class="portal-aside">
        <el-menu :default-active="activeMenu" router class="menu">
          <el-menu-item index="/portal/orders">
            <el-icon><Tickets /></el-icon>
            <template #title>{{ t('portal.myOrders') }}</template>
          </el-menu-item>
          <el-menu-item index="/portal/statements" disabled>
            <el-icon><Document /></el-icon>
            <template #title>
              {{ t('portal.myStatements') }}
              <el-tag size="small" type="info" effect="plain" style="margin-left: 8px">{{ t('portal.soon') }}</el-tag>
            </template>
          </el-menu-item>
          <el-menu-item index="/portal/payments" disabled>
            <el-icon><Money /></el-icon>
            <template #title>
              {{ t('portal.myPayments') }}
              <el-tag size="small" type="info" effect="plain" style="margin-left: 8px">{{ t('portal.soon') }}</el-tag>
            </template>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Tickets, Document, Money, ArrowDown,
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { logout } from '@/api/auth'
import { SUPPORT_LOCALES, persistLocale } from '@/i18n'

const { t, locale } = useI18n()
const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const activeMenu = computed(() => route.path)

const currentLocaleLabel = computed(
  () => SUPPORT_LOCALES.find(l => l.code === locale.value)?.label || locale.value,
)
const currentLocaleFlag = computed(
  () => SUPPORT_LOCALES.find(l => l.code === locale.value)?.flag || '',
)
function onLocaleChange(code) {
  if (code === locale.value) return
  locale.value = code
  persistLocale(code)
  document.title = t('brand')
  ElMessage.success(t('common.languageSwitched'))
}

async function onLogout() {
  await ElMessageBox.confirm(t('common.confirmLogout'), t('common.tip'), { type: 'warning' })
    .catch(() => null)
  try { await logout() } catch {}
  auth.clear()
  ElMessage.success(t('common.logoutSuccess'))
  router.push('/login')
}
</script>

<style scoped>
.portal { height: 100vh; background: #f8fafc; }

.topbar {
  height: 56px !important;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 2px rgba(0, 21, 41, .04);
  padding: 0 20px;
}
.brand-wrap { display: flex; align-items: center; gap: 12px; }
.logo-full { height: 32px; }
.portal-tag {
  font-size: 12px;
  font-weight: 600;
  color: #1f7a35;
  background: #ecfdf5;
  padding: 3px 10px;
  border-radius: 12px;
}

.topbar-right { display: flex; align-items: center; gap: 14px; }
.welcome { font-size: 13px; color: #475569; }
.lang-btn { font-size: 12px; cursor: pointer; }
.lang-flag, .lang-flag-item { margin-right: 6px; }

.portal-aside {
  background: #fff;
  border-right: 1px solid #ebeef5;
  padding-top: 12px;
}
.menu { border-right: none; }

.main {
  padding: 20px;
  overflow-y: auto;
}
</style>
