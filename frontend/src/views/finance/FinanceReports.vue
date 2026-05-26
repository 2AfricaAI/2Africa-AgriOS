<template>
  <div class="page" v-loading="loading">
    <!-- Hero -->
    <div class="hero">
      <div>
        <div class="hero-title">
          <el-icon><TrendCharts /></el-icon>
          {{ t('pnl.reportsTitle') }}
        </div>
        <div class="hero-subtitle">{{ t('pnl.reportsSubtitle') }}</div>
      </div>
      <el-button type="primary" :icon="RefreshIcon" @click="loadAll">{{ t('common.refresh') }}</el-button>
    </div>

    <el-card shadow="never" class="board-card">
      <el-tabs v-model="activeTab">
        <el-tab-pane name="plan">
          <template #label>
            <span class="tab-label">{{ t('pnl.tabPlan') }}
              <el-badge v-if="plans.length" :value="plans.length" type="primary" />
            </span>
          </template>
          <PnLTable :rows="plans" @view="(r) => openDetail(r, 'plan')" />
        </el-tab-pane>

        <el-tab-pane name="plot">
          <template #label>
            <span class="tab-label">{{ t('pnl.tabPlot') }}
              <el-badge v-if="plots.length" :value="plots.length" type="primary" />
            </span>
          </template>
          <PnLTable :rows="plots" @view="(r) => openDetail(r, 'plot')" />
        </el-tab-pane>

        <el-tab-pane name="sku">
          <template #label>
            <span class="tab-label">{{ t('pnl.tabSku') }}
              <el-badge v-if="skus.length" :value="skus.length" type="primary" />
            </span>
          </template>
          <PnLTable :rows="skus" @view="(r) => openDetail(r, 'sku')" />
        </el-tab-pane>

        <el-tab-pane name="customer">
          <template #label>
            <span class="tab-label">{{ t('pnl.tabCustomer') }}
              <el-badge v-if="customers.length" :value="customers.length" type="primary" />
            </span>
          </template>
          <PnLTable :rows="customers" @view="(r) => openDetail(r, 'customer')" />
        </el-tab-pane>

        <el-tab-pane name="channel">
          <template #label>
            <span class="tab-label">{{ t('pnl.tabChannel') }}
              <el-badge v-if="channels.length" :value="channels.length" type="primary" />
            </span>
          </template>
          <PnLTable :rows="channels" @view="(r) => openDetail(r, 'channel')" />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- Detail dialog (reuses PnLDialog for plan/batch; we just need to plug refType) -->
    <PnLDialog
      v-model="detailVisible"
      :kind="detailKind"
      :ref-id="detailRefId"
      :ref-code="detailRefCode"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, h } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElTable, ElTableColumn, ElButton, ElTag } from 'element-plus'
import { TrendCharts, Refresh as RefreshIcon } from '@element-plus/icons-vue'
import { listPlanPnL, listPlotPnL, listSkuPnL, listCustomerPnL, listChannelPnL } from '@/api/finance'
import PnLDialog from '@/components/PnLDialog.vue'

const { t } = useI18n()

const loading = ref(false)
const activeTab = ref('plan')

const plans = ref([])
const plots = ref([])
const skus = ref([])
const customers = ref([])
const channels = ref([])

const detailVisible = ref(false)
const detailKind = ref('plan')
const detailRefId = ref(null)
const detailRefCode = ref('')

function openDetail(row, kind) {
  // For plot/sku, PnLDialog doesn't have a dedicated kind — we only built plan + batch.
  // For MVP, fallback to plan/batch for those that match, otherwise show toast.
  // For Sprint 13, just open the dialog with the supported kind.
  if (kind === 'plan') {
    detailKind.value = 'plan'
    detailRefId.value = row.refId
    detailRefCode.value = row.refCode
    detailVisible.value = true
  } else {
    // For plot/sku we already have the row data inline. Show summary toast or open a simpler view.
    ElMessage.info(`${row.refType.toUpperCase()} ${row.refCode}: cost=${fmt(row.totalCost)} ${row.currency}, revenue=${fmt(row.totalRevenue)}, margin=${row.grossMarginPct ?? '-'}%`)
  }
}

function fmt(v) {
  if (v == null) return '0.00'
  return Number(v).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

async function loadAll() {
  loading.value = true
  try {
    const [p1, p2, p3, p4, p5] = await Promise.all([
      listPlanPnL(), listPlotPnL(), listSkuPnL(),
      listCustomerPnL(), listChannelPnL(),
    ])
    plans.value = p1
    plots.value = p2
    skus.value = p3
    customers.value = p4
    channels.value = p5
  } catch (e) {
    // 全局 axios 已 toast
  } finally {
    loading.value = false
  }
}

onMounted(loadAll)

// ---- Inline PnLTable component (small, reused for all 3 tabs) ----
const PnLTable = {
  props: { rows: Array },
  emits: ['view'],
  setup(props, { emit }) {
    return () => h(ElTable, {
      data: props.rows,
      border: true,
      stripe: true,
      size: 'small',
      emptyText: t('pnl.emptyList'),
    }, () => [
      h(ElTableColumn, {
        label: t('pnl.code'), prop: 'refCode', minWidth: 140,
      }, { default: ({ row }) => h('code', { class: 'pnl-code' }, row.refCode) }),
      h(ElTableColumn, {
        label: t('pnl.name'), prop: 'refName', minWidth: 180,
      }),
      h(ElTableColumn, {
        label: t('pnl.dim'), prop: 'dimInfo', width: 120, align: 'center',
      }, { default: ({ row }) => row.dimInfo ? h(ElTag, { size: 'small' }, () => row.dimInfo) : h('span', { class: 'dim' }, '-') }),
      h(ElTableColumn, {
        label: t('pnl.totalCost'), minWidth: 140, align: 'right',
      }, { default: ({ row }) => h('span', { class: 'cost' }, `${fmt(row.totalCost)} ${row.currency}`) }),
      h(ElTableColumn, {
        label: t('pnl.revenue'), minWidth: 140, align: 'right',
      }, { default: ({ row }) => h('strong', { class: 'revenue' }, `${fmt(row.totalRevenue)} ${row.currency}`) }),
      h(ElTableColumn, {
        label: t('pnl.grossProfit'), minWidth: 140, align: 'right',
      }, { default: ({ row }) => h('strong', { class: ['profit', Number(row.grossProfit) >= 0 ? 'pos' : 'neg'] }, fmt(row.grossProfit)) }),
      h(ElTableColumn, {
        label: t('pnl.grossMargin'), width: 110, align: 'center',
      }, { default: ({ row }) => row.grossMarginPct != null
            ? h('strong', { class: ['margin', Number(row.grossMarginPct) >= 0 ? 'pos' : 'neg'] }, `${row.grossMarginPct}%`)
            : h('span', { class: 'dim' }, '-') }),
      h(ElTableColumn, {
        label: t('common.actions'), width: 100, align: 'center', fixed: 'right',
      }, { default: ({ row }) => h(ElButton, {
            link: true, type: 'primary', size: 'small',
            onClick: () => emit('view', row),
          }, () => t('common.view')) }),
    ])
  },
}
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }

.hero {
  background:
    radial-gradient(circle at 90% 20%, rgba(255, 255, 255, 0.15), transparent 40%),
    linear-gradient(135deg, #0f3a26 0%, #1f7a35 55%, #2BA84A 100%);
  color: #fff;
  padding: 20px 24px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 6px 20px rgba(15, 58, 38, 0.22);
}
.hero-title { font-size: 20px; font-weight: 600; display: inline-flex; align-items: center; gap: 8px; }
.hero-subtitle { font-size: 13px; opacity: .85; margin-top: 4px; }
.hero :deep(.el-button) {
  background: rgba(255, 255, 255, 0.18);
  border-color: rgba(255, 255, 255, 0.3);
  color: #fff;
}

.board-card :deep(.el-card__body) { padding: 8px 16px 16px; }
.tab-label { display: inline-flex; align-items: center; gap: 6px; }

:deep(.pnl-code) {
  font-family: 'Consolas', monospace;
  background: #f5f7fa;
  padding: 2px 8px;
  border-radius: 4px;
  color: #1f7a35;
  font-size: 12px;
  font-weight: 600;
}
:deep(.cost)    { color: #909399; font-family: 'Consolas', monospace; }
:deep(.revenue) { color: #2BA84A; font-family: 'Consolas', monospace; }
:deep(.profit.pos) { color: #2BA84A; }
:deep(.profit.neg) { color: #f56c6c; }
:deep(.margin.pos) { color: #2BA84A; font-size: 14px; }
:deep(.margin.neg) { color: #f56c6c; font-size: 14px; }
:deep(.dim) { color: #c0c4cc; }
</style>
