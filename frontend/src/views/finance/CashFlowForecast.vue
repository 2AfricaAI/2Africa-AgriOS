<template>
  <div class="page" v-loading="loading">
    <!-- 顶部 hero -->
    <div class="hero">
      <div>
        <div class="hero-title">
          <el-icon><TrendCharts /></el-icon>
          {{ t('cashFlow.title') }}
        </div>
        <div class="hero-subtitle">{{ t('cashFlow.subtitle') }}</div>
      </div>
      <div class="hero-actions">
        <el-input-number
          v-model="openingBalance"
          :min="0" :step="10000" :precision="2"
          :controls="false"
          style="width: 160px"
          :placeholder="t('cashFlow.opening')"
        />
        <span class="cur-tag">KES</span>
        <el-button type="primary" :icon="RefreshIcon" @click="load">
          {{ t('common.refresh') }}
        </el-button>
      </div>
    </div>

    <!-- 资金缺口预警 -->
    <el-alert
      v-if="forecast?.gapAlert"
      :title="t('cashFlow.gapAlertTitle', {
        week: (forecast.minBalanceWeek || 0) + 1,
        date: forecast.minBalanceDate || '',
        amount: fmt(forecast.minBalance),
      })"
      type="error"
      effect="dark"
      :closable="false"
      show-icon
      style="margin-top: 8px"
    />

    <!-- KPI 卡片 -->
    <div class="kpis" v-if="forecast">
      <div class="kpi">
        <div class="kpi-label">{{ t('cashFlow.opening') }}</div>
        <div class="kpi-value">{{ fmt(forecast.openingBalance) }}</div>
        <div class="kpi-unit">KES</div>
      </div>
      <div class="kpi positive">
        <div class="kpi-label">{{ t('cashFlow.totalIn13w') }}</div>
        <div class="kpi-value">+{{ fmt(forecast.totalInflow13w) }}</div>
        <div class="kpi-unit">KES</div>
      </div>
      <div class="kpi negative">
        <div class="kpi-label">{{ t('cashFlow.totalOut13w') }}</div>
        <div class="kpi-value">-{{ fmt(forecast.totalOutflow13w) }}</div>
        <div class="kpi-unit">KES</div>
      </div>
      <div class="kpi" :class="netFlowClass">
        <div class="kpi-label">{{ t('cashFlow.netFlow13w') }}</div>
        <div class="kpi-value">{{ signed(forecast.netFlow13w) }}</div>
        <div class="kpi-unit">KES</div>
      </div>
      <div class="kpi" :class="endingClass">
        <div class="kpi-label">{{ t('cashFlow.endingBalance') }}</div>
        <div class="kpi-value">{{ fmt(forecast.endingBalance) }}</div>
        <div class="kpi-unit">KES</div>
      </div>
    </div>

    <!-- 图表 -->
    <el-card shadow="never" class="chart-card" v-if="forecast">
      <template #header>
        <div class="card-head">
          <span class="card-title">
            <el-icon><DataLine /></el-icon>
            {{ t('cashFlow.chartTitle') }}
          </span>
          <span class="dim small">{{ t('cashFlow.horizon', { date: forecast.horizonEnd }) }}</span>
        </div>
      </template>
      <v-chart :option="chartOption" class="cash-chart" autoresize />
    </el-card>

    <!-- 13 周明细 -->
    <el-card shadow="never" class="weeks-card" v-if="forecast">
      <template #header>
        <div class="card-head">
          <span class="card-title">
            <el-icon><Calendar /></el-icon>
            {{ t('cashFlow.weeksTitle') }}
          </span>
        </div>
      </template>

      <el-table :data="forecast.weeks" border stripe size="small" row-key="weekIndex" :empty-text="t('common.empty')">
        <el-table-column type="expand" width="36">
          <template #default="{ row }">
            <div class="items-wrap">
              <div v-if="row.inflowItems?.length || row.outflowItems?.length" class="items-grid">
                <div class="items-col">
                  <div class="items-col-title positive">📥 {{ t('cashFlow.inflow') }} ({{ row.inflowItems.length }})</div>
                  <el-table :data="row.inflowItems" size="small" border>
                    <el-table-column :label="t('cashFlow.date')" width="100">
                      <template #default="{ row: r }">{{ r.date }}</template>
                    </el-table-column>
                    <el-table-column :label="t('cashFlow.itemType')" width="100">
                      <template #default="{ row: r }">
                        <el-tag size="small" :type="itemTypeTag(r.type)">{{ t(`cashFlow.type_${r.type}`) }}</el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column :label="t('cashFlow.party')" min-width="140">
                      <template #default="{ row: r }">
                        <strong>{{ r.name }}</strong>
                        <code v-if="r.orderCode" class="dim small">{{ r.orderCode }}</code>
                      </template>
                    </el-table-column>
                    <el-table-column :label="t('cashFlow.amount')" width="120" align="right">
                      <template #default="{ row: r }"><strong class="positive">+{{ fmt(r.amount) }}</strong></template>
                    </el-table-column>
                  </el-table>
                </div>
                <div class="items-col">
                  <div class="items-col-title negative">📤 {{ t('cashFlow.outflow') }} ({{ row.outflowItems.length }})</div>
                  <el-table :data="row.outflowItems" size="small" border>
                    <el-table-column :label="t('cashFlow.date')" width="100">
                      <template #default="{ row: r }">{{ r.date }}</template>
                    </el-table-column>
                    <el-table-column :label="t('cashFlow.itemType')" width="100">
                      <template #default="{ row: r }">
                        <el-tag size="small" :type="itemTypeTag(r.type)">{{ t(`cashFlow.type_${r.type}`) }}</el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column :label="t('cashFlow.party')" min-width="140">
                      <template #default="{ row: r }">
                        <strong>{{ r.name }}</strong>
                        <code v-if="r.orderCode" class="dim small">{{ r.orderCode }}</code>
                      </template>
                    </el-table-column>
                    <el-table-column :label="t('cashFlow.amount')" width="120" align="right">
                      <template #default="{ row: r }"><strong class="negative">-{{ fmt(r.amount) }}</strong></template>
                    </el-table-column>
                  </el-table>
                </div>
              </div>
              <div v-else class="empty-week">{{ t('cashFlow.emptyWeek') }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="weekIndex" label="#" width="60" align="center">
          <template #default="{ row }">
            <strong>W{{ row.weekIndex + 1 }}</strong>
          </template>
        </el-table-column>
        <el-table-column :label="t('cashFlow.period')" min-width="180">
          <template #default="{ row }">
            <span>{{ row.weekStart }} → {{ row.weekEnd }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('cashFlow.inflow')" min-width="120" align="right">
          <template #default="{ row }">
            <strong v-if="Number(row.inflow) > 0" class="positive">+{{ fmt(row.inflow) }}</strong>
            <span v-else class="dim">0.00</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('cashFlow.outflow')" min-width="120" align="right">
          <template #default="{ row }">
            <strong v-if="Number(row.outflow) > 0" class="negative">-{{ fmt(row.outflow) }}</strong>
            <span v-else class="dim">0.00</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('cashFlow.netFlow')" min-width="120" align="right">
          <template #default="{ row }">
            <strong :class="netRowClass(row.netFlow)">{{ signed(row.netFlow) }}</strong>
          </template>
        </el-table-column>
        <el-table-column :label="t('cashFlow.cumBalance')" min-width="140" align="right">
          <template #default="{ row }">
            <strong :class="balanceClass(row.cumulativeBalance)">{{ fmt(row.cumulativeBalance) }}</strong>
            <el-tag v-if="Number(row.cumulativeBalance) < 0" type="danger" size="small" effect="dark" style="margin-left: 4px">⚠️</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Refresh as RefreshIcon, TrendCharts, DataLine, Calendar } from '@element-plus/icons-vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart } from 'echarts/charts'
import {
  GridComponent, TooltipComponent, TitleComponent, LegendComponent, MarkLineComponent,
} from 'echarts/components'
import VChart from 'vue-echarts'
import { getCashFlowForecast } from '@/api/finance'

use([CanvasRenderer, LineChart, BarChart, GridComponent, TooltipComponent, TitleComponent, LegendComponent, MarkLineComponent])

const { t } = useI18n()
const loading = ref(false)
const forecast = ref(null)

// localStorage 缓存上次输入的 opening balance
const STORAGE_KEY = 'cashflow.openingBalance'
const openingBalance = ref(Number(localStorage.getItem(STORAGE_KEY)) || 100000)

watch(openingBalance, (v) => {
  localStorage.setItem(STORAGE_KEY, String(v ?? 0))
})

async function load() {
  loading.value = true
  try {
    forecast.value = await getCashFlowForecast(openingBalance.value || 0)
  } catch {
    ElMessage.error('Failed to load cash flow forecast')
  } finally {
    loading.value = false
  }
}
onMounted(load)

// ----- helpers -----
function fmt(v) {
  if (v == null) return '0.00'
  return Number(v).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function signed(v) {
  if (v == null) return '0.00'
  const n = Number(v)
  return (n >= 0 ? '+' : '') + n.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function netRowClass(v) {
  const n = Number(v) || 0
  if (n > 0) return 'positive'
  if (n < 0) return 'negative'
  return 'dim'
}
function balanceClass(v) {
  const n = Number(v) || 0
  if (n < 0) return 'red strong'
  if (n < 100000) return 'amber'
  return 'positive'
}
const netFlowClass = computed(() => netRowClass(forecast.value?.netFlow13w))
const endingClass = computed(() => balanceClass(forecast.value?.endingBalance))

const ITEM_TYPE_TAG = { order_due: 'info', promise: 'success', po_due: 'warning' }
function itemTypeTag(t) { return ITEM_TYPE_TAG[t] || '' }

// ----- ECharts 配置 -----
const chartOption = computed(() => {
  const weeks = forecast.value?.weeks || []
  const labels = weeks.map(w => `W${w.weekIndex + 1}`)
  const inflow = weeks.map(w => Number(w.inflow) || 0)
  const outflow = weeks.map(w => -(Number(w.outflow) || 0)) // 负值显示在 0 下方
  const cumulative = weeks.map(w => Number(w.cumulativeBalance) || 0)

  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' },
      formatter: (params) => {
        const wIdx = params[0]?.dataIndex
        const w = weeks[wIdx]
        if (!w) return ''
        const fmtN = n => Number(n).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })
        return `<strong>${w.label}</strong><br/>` +
               `<span style="color:#2BA84A">📥 Inflow:</span> +${fmtN(w.inflow)} KES<br/>` +
               `<span style="color:#f56c6c">📤 Outflow:</span> -${fmtN(w.outflow)} KES<br/>` +
               `<span style="color:#909399">Net:</span> ${Number(w.netFlow) >= 0 ? '+' : ''}${fmtN(w.netFlow)} KES<br/>` +
               `<strong style="color:${Number(w.cumulativeBalance) < 0 ? '#f56c6c' : '#1f7a35'}">📊 Balance:</strong> ${fmtN(w.cumulativeBalance)} KES`
      },
    },
    legend: {
      data: [t('cashFlow.inflow'), t('cashFlow.outflow'), t('cashFlow.cumBalance')],
      top: 5,
    },
    grid: { left: 60, right: 60, top: 50, bottom: 30 },
    xAxis: { type: 'category', data: labels },
    yAxis: [
      {
        type: 'value', name: t('cashFlow.weekly'),
        axisLabel: { formatter: v => (v / 1000) + 'K' },
      },
      {
        type: 'value', name: t('cashFlow.cumBalance'),
        position: 'right',
        axisLabel: { formatter: v => (v / 1000) + 'K' },
      },
    ],
    series: [
      {
        name: t('cashFlow.inflow'), type: 'bar', stack: 'flow', data: inflow,
        itemStyle: { color: '#2BA84A' },
      },
      {
        name: t('cashFlow.outflow'), type: 'bar', stack: 'flow', data: outflow,
        itemStyle: { color: '#f56c6c' },
      },
      {
        name: t('cashFlow.cumBalance'), type: 'line', yAxisIndex: 1, data: cumulative,
        smooth: true, symbol: 'circle', symbolSize: 7,
        lineStyle: { color: '#1677ff', width: 3 },
        itemStyle: { color: '#1677ff' },
        markLine: {
          silent: true,
          symbol: 'none',
          lineStyle: { type: 'dashed', color: '#f56c6c' },
          data: [{ yAxis: 0, label: { formatter: 'Zero', position: 'insideEndTop' } }],
        },
      },
    ],
  }
})
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.hero {
  background:
    radial-gradient(circle at 90% 20%, rgba(255, 255, 255, 0.15), transparent 40%),
    linear-gradient(135deg, #1a3a6e 0%, #1677ff 55%, #4ba1ff 100%);
  color: #fff; padding: 18px 24px; border-radius: 10px;
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
  box-shadow: 0 6px 20px rgba(22, 119, 255, 0.22);
}
.hero-title { font-size: 20px; font-weight: 600; display: inline-flex; align-items: center; gap: 8px; }
.hero-subtitle { font-size: 13px; opacity: .85; margin-top: 4px; }
.hero-actions {
  display: inline-flex; align-items: center; gap: 8px;
  background: rgba(255, 255, 255, .15);
  padding: 6px 10px; border-radius: 6px;
}
.hero-actions .cur-tag { color: rgba(255,255,255,.85); font-weight: 600; font-size: 12px; }
.hero :deep(.el-button) { background: rgba(255,255,255,.18); border-color: rgba(255,255,255,.3); color: #fff; }
.hero :deep(.el-input-number .el-input__inner) { background: #fff; }

.kpis { display: grid; grid-template-columns: repeat(5, 1fr); gap: 12px; }
.kpi {
  background: #fff; padding: 12px 16px; border-radius: 8px;
  border-left: 3px solid #909399;
  box-shadow: 0 1px 3px rgba(0,0,0,.04);
}
.kpi-label { font-size: 11px; color: #909399; text-transform: uppercase; letter-spacing: 0.5px; }
.kpi-value { font-size: 18px; font-weight: 700; margin: 4px 0; color: #1f2329; }
.kpi-unit { font-size: 11px; color: #909399; }
.kpi.positive { border-left-color: #2BA84A; }
.kpi.positive .kpi-value { color: #2BA84A; }
.kpi.negative { border-left-color: #f56c6c; }
.kpi.negative .kpi-value { color: #f56c6c; }
.kpi.red { border-left-color: #f56c6c; }
.kpi.red .kpi-value { color: #f56c6c; }
.kpi.amber { border-left-color: #e6a23c; }
.kpi.amber .kpi-value { color: #e6a23c; }
.kpi.dim .kpi-value { color: #909399; }

.chart-card :deep(.el-card__body) { padding: 12px; }
.weeks-card :deep(.el-card__body) { padding: 12px; }
.cash-chart { height: 320px; width: 100%; }
.card-head { display: flex; align-items: center; justify-content: space-between; }
.card-title { display: inline-flex; align-items: center; gap: 6px; font-weight: 600; }

.items-wrap { padding: 12px 18px; background: #f7f9fc; }
.items-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.items-col-title { font-weight: 600; font-size: 13px; padding: 4px 0; }
.items-col-title.positive { color: #1f7a35; }
.items-col-title.negative { color: #c45656; }
.empty-week { color: #909399; font-style: italic; text-align: center; padding: 20px; }

.positive { color: #1f7a35; }
.negative { color: #c45656; }
.red { color: #f56c6c; }
.amber { color: #e6a23c; }
.strong { font-weight: 700; }
.dim { color: #909399; font-family: 'Consolas', monospace; font-size: 12px; }
.small { font-size: 11px; }
</style>
