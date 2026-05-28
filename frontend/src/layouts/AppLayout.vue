<template>
  <el-container class="layout">
    <!-- 左侧栏 -->
    <el-aside :width="collapse ? '56px' : '220px'" class="aside">
      <div class="brand" :class="{ 'brand-collapsed': collapse }">
        <img v-if="collapse" src="/logo-mark.svg" class="logo-mark" alt="2A" />
        <img v-else src="/logo.svg" class="logo-full" alt="2Africa AgriOS" />
      </div>

      <el-menu
        :default-active="activeMenu"
        :collapse="collapse"
        background-color="#0f3a26"
        text-color="#b9d1c1"
        active-text-color="#fff"
        router
        class="menu"
      >
        <el-menu-item index="/">
          <el-icon><HomeIcon /></el-icon>
          <template #title>{{ t('menu.home') }}</template>
        </el-menu-item>

        <el-sub-menu index="master">
          <template #title>
            <el-icon><GoodsIcon /></el-icon>
            <span>{{ t('menu.masterData') }}</span>
          </template>
          <el-menu-item index="/master/crops">{{ t('menu.crops') }}</el-menu-item>
          <el-menu-item index="/master/varieties">{{ t('menu.varieties') }}</el-menu-item>
          <el-menu-item index="/master/packaging-specs">{{ t('menu.packagingSpecs') }}</el-menu-item>
          <el-menu-item index="/master/warehouses">{{ t('menu.warehouses') }}</el-menu-item>
          <el-menu-item index="/master/input-items">{{ t('menu.inputItems') }}</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="production">
          <template #title>
            <el-icon><ProductionIcon /></el-icon>
            <span>{{ t('menu.production') }}</span>
          </template>
          <el-menu-item index="/production/plots">{{ t('menu.plots') }}</el-menu-item>
          <el-menu-item index="/production/planting-plans">{{ t('menu.plantingPlans') }}</el-menu-item>
          <el-menu-item index="/production/activities">{{ t('menu.activities') }}</el-menu-item>
          <el-menu-item index="/production/harvests">{{ t('menu.harvests') }}</el-menu-item>
          <el-menu-item index="/production/batches">{{ t('menu.batches') }}</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="warehouse-ops">
          <template #title>
            <el-icon><GoodsIcon /></el-icon>
            <span>{{ t('menu.warehouseOps') }}</span>
          </template>
          <el-menu-item index="/warehouse/inbound">{{ t('menu.inbound') }}</el-menu-item>
          <el-menu-item index="/warehouse/outbound">{{ t('menu.outbound') }}</el-menu-item>
          <el-menu-item index="/warehouse/stocktake">{{ t('menu.stocktake') }}</el-menu-item>
          <el-menu-item index="/warehouse/transfer">{{ t('menu.transfer') }}</el-menu-item>
          <el-menu-item index="/warehouse/scrap">{{ t('menu.scrap') }}</el-menu-item>
          <el-menu-item index="/master/input-stock">{{ t('menu.inputStock') }}</el-menu-item>
          <el-menu-item index="/master/stock-log">{{ t('menu.stockLog') }}</el-menu-item>
          <el-menu-item index="/warehouse/reports">{{ t('menu.warehouseReports') }}</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="packhouse">
          <template #title>
            <el-icon><BoxIcon /></el-icon>
            <span>{{ t('menu.packhouse') }}</span>
          </template>
          <el-menu-item index="/packhouse/packings">{{ t('menu.packings') }}</el-menu-item>
          <el-menu-item index="/packhouse/inventory">{{ t('menu.inventory') }}</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="sales">
          <template #title>
            <el-icon><SalesIcon /></el-icon>
            <span>{{ t('menu.sales') }}</span>
          </template>
          <el-menu-item index="/sales/customers">{{ t('menu.customers') }}</el-menu-item>
          <el-menu-item index="/sales/orders">{{ t('menu.orders') }}</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="operations">
          <template #title>
            <el-icon><OpsIcon /></el-icon>
            <span>{{ t('menu.operations') }}</span>
          </template>
          <el-menu-item index="/operations/action-board">{{ t('menu.actionBoard') }}</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="finance">
          <template #title>
            <el-icon><FinanceIcon /></el-icon>
            <span>{{ t('menu.finance') }}</span>
          </template>
          <el-menu-item index="/finance/reports">{{ t('menu.reports') }}</el-menu-item>
          <el-menu-item index="/finance/ar">{{ t('menu.ar') }}</el-menu-item>
          <el-menu-item index="/finance/cash-flow">{{ t('menu.cashFlow') }}</el-menu-item>
          <el-menu-item index="/finance/monthly">{{ t('menu.monthly') }}</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="procurement">
          <template #title>
            <el-icon><ProcurementIcon /></el-icon>
            <span>{{ t('menu.procurement') }}</span>
          </template>
          <el-menu-item index="/procurement/suppliers">{{ t('menu.suppliers') }}</el-menu-item>
          <el-menu-item index="/procurement/orders">{{ t('menu.purchaseOrders') }}</el-menu-item>
          <el-menu-item index="/procurement/ap">{{ t('menu.ap') }}</el-menu-item>
        </el-sub-menu>

        <el-menu-item index="/demo/files">
          <el-icon><FolderIcon /></el-icon>
          <template #title>{{ t('menu.fileDemo') }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 顶栏 -->
      <el-header class="topbar">
        <el-button text :icon="collapse ? ExpandIcon : FoldIcon" @click="collapse = !collapse" />
        <div class="topbar-right">
          <!-- 语言切换 -->
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
            {{ auth.nickname || auth.username }}
            <el-tag size="small" type="success" effect="dark" round style="margin-left: 8px">
              {{ auth.roles[0] || 'USER' }}
            </el-tag>
          </span>
          <el-button link type="primary" @click="onLogout">{{ t('common.logout') }}</el-button>
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
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  HomeFilled as HomeIcon,
  Goods as GoodsIcon,
  Folder as FolderIcon,
  Sunny as ProductionIcon,
  Box as BoxIcon,
  Wallet as SalesIcon,
  Bell as OpsIcon,
  Money as FinanceIcon,
  ShoppingCart as ProcurementIcon,
  Expand as ExpandIcon,
  Fold as FoldIcon,
  ArrowDown,
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { logout } from '@/api/auth'
import { SUPPORT_LOCALES, persistLocale } from '@/i18n'

const { t, locale } = useI18n()
const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const collapse = ref(false)
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
  await ElMessageBox.confirm(t('common.confirmLogout'), t('common.tip'), { type: 'warning' }).catch(() => null)
  try { await logout() } catch {}
  auth.clear()
  ElMessage.success(t('common.logoutSuccess'))
  router.push('/login')
}
</script>

<style scoped>
.layout { height: 100vh; }

.aside {
  background: linear-gradient(180deg, #0f3a26 0%, #0a2b1c 100%);
  transition: width .2s;
  overflow: hidden;
}

.brand {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 14px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.04);
  white-space: nowrap;
}

.brand-collapsed { padding: 0 4px; }

.logo-full {
  width: 100%;
  max-width: 180px;
  height: 36px;
  object-fit: contain;
  filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.3));
  /* 把白底替换成透明 - logo 自身设计就靠色块,直接显示 */
  background: #fff;
  border-radius: 4px;
  padding: 4px 6px;
}

.logo-mark {
  width: 40px;
  height: 40px;
  object-fit: contain;
}

.menu { border-right: none; }
.menu:not(.el-menu--collapse) { width: 220px; }

/* 菜单 hover / active 适配深绿背景 */
:deep(.el-menu-item:hover),
:deep(.el-sub-menu__title:hover) {
  background-color: rgba(255, 255, 255, 0.06) !important;
}
:deep(.el-menu-item.is-active) {
  background-color: var(--brand-green) !important;
  color: #fff !important;
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

.lang-btn { font-size: 12px; color: #fff; cursor: pointer; }
</style>
