<template>
  <div class="dashboard" v-loading="loading">
    <!-- 顶部欢迎条 -->
    <div class="hero">
      <div>
        <div class="greeting">{{ greeting }}, {{ auth.nickname || auth.username }} 👋</div>
        <div class="sub" v-html="subtitleHtml"></div>
      </div>
      <el-button :icon="RefreshIcon" plain @click="load">{{ t('common.refresh') }}</el-button>
    </div>

    <!-- KPI 4 卡片 -->
    <div class="kpi-grid">
      <div class="kpi-card c-blue">
        <div class="kpi-label">{{ t('home.kpiActivePlans') }}</div>
        <div class="kpi-value">{{ data.activePlanCount ?? '-' }}</div>
        <div class="kpi-foot">{{ t('home.kpiActivePlansFoot') }}</div>
      </div>
      <div class="kpi-card c-orange">
        <div class="kpi-label">{{ t('home.kpiPendingActivities') }}</div>
        <div class="kpi-value">{{ data.pendingActivityCount ?? '-' }}</div>
        <div class="kpi-foot">{{ t('home.kpiPendingActivitiesFoot') }}</div>
      </div>
      <div class="kpi-card c-green">
        <div class="kpi-label">{{ t('home.kpiTodayHarvest') }}</div>
        <div class="kpi-value">{{ formatNum(data.todayHarvestKg) }}</div>
        <div class="kpi-foot">{{ todayDate }}</div>
      </div>
      <div class="kpi-card c-purple">
        <div class="kpi-label">{{ t('home.kpiPendingBatches') }}</div>
        <div class="kpi-value">{{ data.pendingBatchCount ?? '-' }}</div>
        <div class="kpi-foot">{{ t('home.kpiPendingBatchesFoot') }}</div>
      </div>
    </div>

    <!-- ECharts 图表区 -->
    <div class="charts-row">
      <el-card shadow="never" class="chart-card">
        <template #header>
          <div class="chart-hdr">
            <span>{{ t('home.chartTrendTitle') }}</span>
            <span class="dim">{{ t('home.chartTrendWeekTotal', { total: formatNum(totalWeek) }) }}</span>
          </div>
        </template>
        <v-chart
          v-if="trendOption"
          :option="trendOption"
          autoresize
          style="height: 260px"
        />
      </el-card>

      <el-card shadow="never" class="chart-card">
        <template #header><span>{{ t('home.chartByCropTitle') }}</span></template>
        <v-chart
          v-if="cropOption"
          :option="cropOption"
          autoresize
          :style="{ height: cropChartHeight + 'px' }"
        />
        <div v-else class="empty">{{ t('home.chartNoHarvestYet') }}</div>
      </el-card>
    </div>

    <!-- 两份清单 -->
    <div class="lists-row">
      <el-card shadow="never" class="list-card">
        <template #header>
          <div class="list-hdr">
            <span>{{ t('home.pendingActivitiesTop') }}</span>
            <router-link to="/production/activities">
              <el-button link size="small" type="primary">{{ t('home.goReview') }}</el-button>
            </router-link>
          </div>
        </template>
        <el-table v-if="data.pendingActivities?.length" :data="data.pendingActivities" :show-header="true" size="small">
          <el-table-column :label="t('home.colDate')" prop="occur_date" width="100" />
          <el-table-column :label="t('home.colType')" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="actTagType(row.activity_type)">
                {{ actTypeLabel(row.activity_type) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="t('home.colPlan')" prop="plan_code" min-width="120" />
          <el-table-column :label="t('home.colPlot')" prop="plot_name" min-width="130" />
          <el-table-column :label="t('home.colOperator')" prop="operator_name" width="100" />
        </el-table>
        <div v-else class="empty">{{ t('home.emptyPendingActivities') }}</div>
      </el-card>

      <el-card shadow="never" class="list-card">
        <template #header>
          <div class="list-hdr">
            <span>{{ t('home.recentHarvestsTop') }}</span>
            <router-link to="/production/harvests">
              <el-button link size="small" type="primary">{{ t('home.seeAll') }}</el-button>
            </router-link>
          </div>
        </template>
        <el-table v-if="data.recentHarvests?.length" :data="data.recentHarvests" :show-header="true" size="small">
          <el-table-column :label="t('home.colDate')" prop="harvest_date" width="100" />
          <el-table-column :label="t('home.colCode')" prop="code" width="160" />
          <el-table-column :label="t('home.colCrop')" prop="crop_name" min-width="100" />
          <el-table-column :label="t('home.colPlot')" prop="plot_name" min-width="130" />
          <el-table-column :label="t('home.colQty')" align="right" width="100">
            <template #default="{ row }">
              <strong>{{ formatNum(row.qty_kg) }}</strong>
            </template>
          </el-table-column>
          <el-table-column :label="t('home.colBatch')" prop="batch_code" min-width="180">
            <template #default="{ row }">
              <code class="batch-code">{{ row.batch_code }}</code>
            </template>
          </el-table-column>
        </el-table>
        <div v-else class="empty">{{ t('home.emptyRecentHarvests') }}</div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Refresh as RefreshIcon } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { getDashboardSummary } from '@/api/dashboard'

import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart } from 'echarts/charts'
import {
  GridComponent,
  TooltipComponent,
  TitleComponent,
  DataZoomComponent,
} from 'echarts/components'
import VChart from 'vue-echarts'

use([
  CanvasRenderer,
  LineChart,
  BarChart,
  GridComponent,
  TooltipComponent,
  TitleComponent,
  DataZoomComponent,
])

const { t } = useI18n()
const auth = useAuthStore()
const data = ref({})
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    data.value = await getDashboardSummary()
  } catch {} finally {
    loading.value = false
  }
}

onMounted(load)

const todayDate = new Date().toISOString().slice(0, 10)

const subtitleHtml = computed(() =>
  t('home.subtitle', { brand: `<strong>${t('brand')}</strong>` })
)

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6)  return t('home.greetingNight')
  if (h < 12) return t('home.greetingMorning')
  if (h < 14) return t('home.greetingNoon')
  if (h < 18) return t('home.greetingAfternoon')
  return t('home.greetingEvening')
})

function formatNum(v) {
  if (v == null) return '0'
  return Number(v).toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 2 })
}

const totalWeek = computed(() => {
  const arr = data.value.harvest7Days || []
  return arr.reduce((s, r) => s + (Number(r.qty) || 0), 0)
})

const trendOption = computed(() => {
  const arr = data.value.harvest7Days || []
  if (arr.length === 0) return null

  return {
    grid: { top: 14, right: 16, bottom: 24, left: 44 },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(0, 21, 41, 0.92)',
      borderColor: 'transparent',
      textStyle: { color: '#fff', fontSize: 12 },
      formatter: (params) => {
        const p = params[0]
        return `<div style="font-weight:600;margin-bottom:4px">${p.axisValue}</div>
                <span style="color:#69c0ff">●</span> ${t('home.colQty')}
                <strong style="margin-left:8px">${formatNum(p.data)}</strong> kg`
      },
    },
    xAxis: {
      type: 'category',
      data: arr.map(r => (r.date || '').slice(5).replace('-', '/')),
      axisLine: { lineStyle: { color: '#dcdfe6' } },
      axisLabel: { color: '#909399', fontSize: 11 },
      axisTick: { show: false },
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#909399', fontSize: 11 },
      splitLine: { lineStyle: { color: '#f0f2f5', type: 'dashed' } },
    },
    series: [{
      name: t('home.colQty'),
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { color: '#1890ff', width: 2.5 },
      itemStyle: { color: '#1890ff', borderColor: '#fff', borderWidth: 2 },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(24, 144, 255, 0.35)' },
            { offset: 1, color: 'rgba(24, 144, 255, 0.02)' },
          ],
        },
      },
      data: arr.map(r => Number(r.qty) || 0),
    }],
  }
})

const cropChartHeight = computed(() => {
  const n = (data.value.harvestByCrop || []).length
  return Math.max(180, n * 36 + 40)
})

const cropOption = computed(() => {
  const arr = data.value.harvestByCrop || []
  if (arr.length === 0) return null

  const sorted = [...arr].sort((a, b) => Number(b.qty) - Number(a.qty)).reverse()
  const names = sorted.map(r => r.crop_name || t('home.unassociated'))
  const values = sorted.map(r => Number(r.qty) || 0)

  return {
    grid: { top: 10, right: 60, bottom: 8, left: 100 },
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(0, 21, 41, 0.92)',
      borderColor: 'transparent',
      textStyle: { color: '#fff', fontSize: 12 },
      formatter: (p) => `<div style="font-weight:600;margin-bottom:4px">${p.name}</div>
                         <span style="color:#95de64">●</span> ${t('home.cumulativeHarvest')}
                         <strong style="margin-left:8px">${formatNum(p.value)}</strong> kg`,
    },
    xAxis: { type: 'value', show: false },
    yAxis: {
      type: 'category',
      data: names,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#1f2329', fontSize: 13 },
    },
    series: [{
      type: 'bar',
      barWidth: 18,
      data: values,
      itemStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 1, y2: 0,
          colorStops: [
            { offset: 0, color: '#52c41a' },
            { offset: 1, color: '#95de64' },
          ],
        },
        borderRadius: [0, 4, 4, 0],
      },
      label: {
        show: true,
        position: 'right',
        formatter: (p) => formatNum(p.value),
        color: '#1f2329',
        fontSize: 12,
        fontWeight: 600,
      },
    }],
  }
})

const ACT_TAG = {
  sow: 'success', fertilize: 'warning', spray: 'danger',
  weed: 'info', water: 'primary', prune: 'info', other: 'info'
}
function actTypeLabel(v) {
  return t(`actType.${v}`, v)
}
function actTagType(v) { return ACT_TAG[v] || 'info' }
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

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
.greeting { font-size: 20px; font-weight: 600; }
.sub      { font-size: 13px; opacity: .85; margin-top: 4px; }
.hero :deep(.el-button) {
  background: rgba(255, 255, 255, 0.18);
  border-color: rgba(255, 255, 255, 0.3);
  color: #fff;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}
.kpi-card {
  background: #fff;
  border-radius: 8px;
  padding: 18px 18px 14px;
  position: relative;
  overflow: hidden;
  border: 1px solid #ebeef5;
  transition: transform .15s, box-shadow .15s;
}
.kpi-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(0, 21, 41, 0.06);
}
.kpi-card::before {
  content: '';
  position: absolute;
  top: 0; left: 0;
  width: 4px; height: 100%;
}
.c-blue::before   { background: var(--brand-blue);  }
.c-orange::before { background: var(--brand-grain); }
.c-green::before  { background: var(--brand-green); }
.c-purple::before { background: var(--brand-soil);  }

.kpi-label { font-size: 12px; color: #909399; }
.kpi-value {
  font-size: 32px;
  font-weight: 700;
  color: #1f2329;
  margin: 4px 0 2px;
  font-variant-numeric: tabular-nums;
}
.kpi-foot { font-size: 11px; color: #909399; }

.charts-row {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 14px;
}
.chart-card { min-height: 300px; }
.chart-hdr {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.dim { color: #909399; font-size: 12px; }

.empty {
  color: #c0c4cc;
  font-size: 13px;
  text-align: center;
  padding: 60px 0;
}

.lists-row {
  display: grid;
  grid-template-columns: 1fr 1.4fr;
  gap: 14px;
}
.list-card :deep(.el-card__body) { padding: 8px 14px 14px; }
.list-hdr {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.batch-code {
  font-family: 'Consolas', monospace;
  background: #f5f7fa;
  padding: 1px 6px;
  border-radius: 3px;
  color: #d63384;
  font-size: 11px;
}

@media (max-width: 1100px) {
  .kpi-grid { grid-template-columns: repeat(2, 1fr); }
  .charts-row, .lists-row { grid-template-columns: 1fr; }
}
</style>
