<template>
  <el-container class="layout">
    <!-- 左侧栏 -->
    <el-aside :width="collapse ? '56px' : '220px'" class="aside">
      <div class="brand" :class="{ 'brand-collapsed': collapse }">
        <!--
          Brand strategy: compound mark "2Africa.AI AgriOS" — house mark
          (2Africa.AI) + product mark (AgriOS) shown together. See
          TRADEMARK.md for the legal framing (descriptive product name
          + dominant house mark + agrios.org disclaimer).
        -->
        <img v-if="collapse" src="/logo-mark.svg" class="logo-mark" alt="2A" />
        <img v-else src="/logo.svg" class="logo-full" alt="2Africa.AI AgriOS" />
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

        <el-sub-menu v-if="canMaster" index="master">
          <template #title>
            <el-icon><GoodsIcon /></el-icon>
            <span>{{ t('menu.masterData') }}</span>
          </template>
          <el-menu-item v-if="can('master:crop:list')" index="/master/crops">{{ t('menu.crops') }}</el-menu-item>
          <el-menu-item v-if="can('master:variety:list')" index="/master/varieties">{{ t('menu.varieties') }}</el-menu-item>
          <el-menu-item v-if="can('master:packaging-spec:list')" index="/master/packaging-specs">{{ t('menu.packagingSpecs') }}</el-menu-item>
          <el-menu-item v-if="can('master:warehouse:list')" index="/master/warehouses">{{ t('menu.warehouses') }}</el-menu-item>
          <el-menu-item v-if="can('master:input-item:list')" index="/master/input-items">{{ t('menu.inputItems') }}</el-menu-item>
        </el-sub-menu>

        <el-sub-menu v-if="canProduction" index="production">
          <template #title>
            <el-icon><ProductionIcon /></el-icon>
            <span>{{ t('menu.production') }}</span>
          </template>
          <el-menu-item v-if="can('production:plot:list')" index="/production/plots">{{ t('menu.plots') }}</el-menu-item>
          <el-menu-item v-if="can('production:planting-plan:list')" index="/production/planting-plans">{{ t('menu.plantingPlans') }}</el-menu-item>
          <el-menu-item v-if="can('production:activity:list')" index="/production/activities">{{ t('menu.activities') }}</el-menu-item>
          <el-menu-item v-if="can('production:harvest:list')" index="/production/harvests">{{ t('menu.harvests') }}</el-menu-item>
          <el-menu-item v-if="can('production:batch:list')" index="/production/batches">{{ t('menu.batches') }}</el-menu-item>
        </el-sub-menu>

        <el-sub-menu v-if="canWarehouseOps" index="warehouse-ops">
          <template #title>
            <el-icon><GoodsIcon /></el-icon>
            <span>{{ t('menu.warehouseOps') }}</span>
          </template>
          <el-menu-item v-if="can('warehouse:inbound:list')" index="/warehouse/inbound">{{ t('menu.inbound') }}</el-menu-item>
          <el-menu-item v-if="can('warehouse:outbound:list')" index="/warehouse/outbound">{{ t('menu.outbound') }}</el-menu-item>
          <el-menu-item v-if="can('warehouse:stocktake:list')" index="/warehouse/stocktake">{{ t('menu.stocktake') }}</el-menu-item>
          <el-menu-item v-if="can('warehouse:transfer:list')" index="/warehouse/transfer">{{ t('menu.transfer') }}</el-menu-item>
          <el-menu-item v-if="can('warehouse:scrap:list')" index="/warehouse/scrap">{{ t('menu.scrap') }}</el-menu-item>
          <el-menu-item v-if="can('master:input-stock:list')" index="/master/input-stock">{{ t('menu.inputStock') }}</el-menu-item>
          <el-menu-item v-if="can('master:stock-log:list')" index="/master/stock-log">{{ t('menu.stockLog') }}</el-menu-item>
          <el-menu-item v-if="can('warehouse:report:view')" index="/warehouse/reports">{{ t('menu.warehouseReports') }}</el-menu-item>
        </el-sub-menu>

        <el-sub-menu v-if="canQc" index="qc">
          <template #title>
            <el-icon><QcIcon /></el-icon>
            <span>{{ t('menu.qc') }}</span>
          </template>
          <el-menu-item v-if="can('qc:inspection:list')" index="/qc/inspections">{{ t('menu.qcInspections') }}</el-menu-item>
          <el-menu-item v-if="can('qc:trace:view')" index="/qc/trace">{{ t('menu.qcTrace') }}</el-menu-item>
          <el-menu-item v-if="can('qc:complaint:list')" index="/qc/complaints">{{ t('menu.complaints') }}</el-menu-item>
          <el-menu-item v-if="can('qc:recall:list')" index="/qc/recalls">{{ t('menu.recalls') }}</el-menu-item>
          <el-menu-item v-if="can('qc:gap-report:view')" index="/qc/gap-reports">{{ t('menu.gapReports') }}</el-menu-item>
        </el-sub-menu>

        <el-sub-menu v-if="canPackhouse" index="packhouse">
          <template #title>
            <el-icon><BoxIcon /></el-icon>
            <span>{{ t('menu.packhouse') }}</span>
          </template>
          <el-menu-item v-if="can('packhouse:packing:list')" index="/packhouse/packings">{{ t('menu.packings') }}</el-menu-item>
          <el-menu-item v-if="can('packhouse:inventory:list')" index="/packhouse/inventory">{{ t('menu.inventory') }}</el-menu-item>
        </el-sub-menu>

        <el-sub-menu v-if="canSales" index="sales">
          <template #title>
            <el-icon><SalesIcon /></el-icon>
            <span>{{ t('menu.sales') }}</span>
          </template>
          <el-menu-item v-if="can('sales:customer:list')" index="/sales/customers">{{ t('menu.customers') }}</el-menu-item>
          <el-menu-item v-if="can('sales:order:list')" index="/sales/orders">{{ t('menu.orders') }}</el-menu-item>
        </el-sub-menu>

        <el-sub-menu v-if="canOperations" index="operations">
          <template #title>
            <el-icon><OpsIcon /></el-icon>
            <span>{{ t('menu.operations') }}</span>
          </template>
          <el-menu-item index="/operations/action-board">{{ t('menu.actionBoard') }}</el-menu-item>
        </el-sub-menu>

        <!--
          Sprint 48b: CS module restructured into 5 sub-menus
          (对话/投诉/分析/团队/设置). Visible to every STAFF user.
        -->
        <el-sub-menu index="cs">
          <template #title>
            <el-icon><ServiceIcon /></el-icon>
            <span>{{ t('menu.customerService') }}</span>
          </template>
          <el-menu-item index="/service">{{ t('menu.csConversations') }}</el-menu-item>
          <el-menu-item index="/service/complaints">{{ t('menu.csComplaints') }}</el-menu-item>
          <el-menu-item index="/service/analytics">{{ t('menu.csAnalytics') }}</el-menu-item>
          <el-menu-item index="/service/team">{{ t('menu.csTeam') }}</el-menu-item>
          <el-menu-item index="/service/settings">{{ t('menu.csSettings') }}</el-menu-item>
        </el-sub-menu>

        <el-sub-menu v-if="canFinance" index="finance">
          <template #title>
            <el-icon><FinanceIcon /></el-icon>
            <span>{{ t('menu.finance') }}</span>
          </template>
          <el-menu-item v-if="can('finance:report:view')" index="/finance/reports">{{ t('menu.reports') }}</el-menu-item>
          <el-menu-item v-if="can('finance:ar:view')" index="/finance/ar">{{ t('menu.ar') }}</el-menu-item>
          <el-menu-item v-if="can('finance:cash-flow:view')" index="/finance/cash-flow">{{ t('menu.cashFlow') }}</el-menu-item>
          <el-menu-item v-if="can('finance:monthly:view')" index="/finance/monthly">{{ t('menu.monthly') }}</el-menu-item>
        </el-sub-menu>

        <el-sub-menu v-if="canProcurement" index="procurement">
          <template #title>
            <el-icon><ProcurementIcon /></el-icon>
            <span>{{ t('menu.procurement') }}</span>
          </template>
          <el-menu-item v-if="can('procurement:supplier:list')" index="/procurement/suppliers">{{ t('menu.suppliers') }}</el-menu-item>
          <el-menu-item v-if="can('procurement:po:list')" index="/procurement/orders">{{ t('menu.purchaseOrders') }}</el-menu-item>
          <el-menu-item v-if="can('procurement:ap:view')" index="/procurement/ap">{{ t('menu.ap') }}</el-menu-item>
        </el-sub-menu>

        <el-menu-item index="/demo/files">
          <el-icon><FolderIcon /></el-icon>
          <template #title>{{ t('menu.fileDemo') }}</template>
        </el-menu-item>

        <el-sub-menu v-if="canSeeSystem" index="system">
          <template #title>
            <el-icon><SettingIcon /></el-icon>
            <span>{{ t('menu.system') }}</span>
          </template>
          <el-menu-item v-if="can('system:user:list')" index="/system/users">{{ t('menu.sysUsers') }}</el-menu-item>
          <el-menu-item v-if="can('system:role:list')" index="/system/roles">{{ t('menu.sysRoles') }}</el-menu-item>
        </el-sub-menu>
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
  CircleCheck as QcIcon,
  ChatRound as ServiceIcon,
  Setting as SettingIcon,
  Expand as ExpandIcon,
  Fold as FoldIcon,
  ArrowDown,
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { logout } from '@/api/auth'
import { SUPPORT_LOCALES, persistLocale } from '@/i18n'
import { hasPerm, hasAnyPerm } from '@/utils/perms'

const { t, locale } = useI18n()
const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const collapse = ref(false)
const activeMenu = computed(() => route.path)

// Sprint 35: each menu group is visible iff the user has at least one child perm.
// SUPER_ADMIN bypasses (see utils/perms.js). Individual <el-menu-item> entries
// also gate themselves via v-if=can('module:resource:list').
const can = hasPerm
const canMaster = computed(() => hasAnyPerm([
  'master:crop:list','master:variety:list','master:packaging-spec:list',
  'master:warehouse:list','master:input-item:list',
]))
const canProduction = computed(() => hasAnyPerm([
  'production:plot:list','production:planting-plan:list','production:activity:list',
  'production:harvest:list','production:batch:list',
]))
const canWarehouseOps = computed(() => hasAnyPerm([
  'warehouse:inbound:list','warehouse:outbound:list','warehouse:stocktake:list',
  'warehouse:transfer:list','warehouse:scrap:list','warehouse:report:view',
  'master:input-stock:list','master:stock-log:list',
]))
const canQc = computed(() => hasAnyPerm([
  'qc:inspection:list','qc:trace:view','qc:complaint:list','qc:recall:list','qc:gap-report:view',
]))
const canPackhouse = computed(() => hasAnyPerm([
  'packhouse:packing:list','packhouse:inventory:list',
]))
const canSales = computed(() => hasAnyPerm([
  'sales:customer:list','sales:order:list',
]))
const canOperations = computed(() => hasPerm('operations:action-board:list'))
const canFinance = computed(() => hasAnyPerm([
  'finance:report:view','finance:ar:view','finance:cash-flow:view','finance:monthly:view',
]))
const canProcurement = computed(() => hasAnyPerm([
  'procurement:supplier:list','procurement:po:list','procurement:ap:view',
]))
const canSeeSystem = computed(() => hasAnyPerm([
  'system:user:list','system:role:list',
]))

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
