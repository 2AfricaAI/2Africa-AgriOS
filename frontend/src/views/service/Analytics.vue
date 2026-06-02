<template>
  <!--
    Sprint 49: CS Analytics dashboard (filling the Sprint 48b stub).
    MVP scope: 4 KPI cards + line chart (conversations over time) +
    donut chart (channel mix). Date-range selector (7d / 30d / 90d).
    Future sprints add per-agent SLA, FRT/TTR percentiles, CSAT.
  -->
  <div class="analytics" v-loading="loading">
    <!-- Hero / range picker -->
    <header class="hero">
      <div class="hero-text">
        <h2 class="hero-title">{{ t('cs.analyticsTitle') }}</h2>
        <p class="hero-sub">{{ t('cs.analyticsSub') }}</p>
      </div>
      <div class="hero-actions">
        <el-radio-group v-model="windowDays" size="default" @change="load">
          <el-radio-button :value="7">{{ t('cs.analyticsRange7') }}</el-radio-button>
          <el-radio-button :value="30">{{ t('cs.analyticsRange30') }}</el-radio-button>
          <el-radio-button :value="90">{{ t('cs.analyticsRange90') }}</el-radio-button>
        </el-radio-group>
        <el-button text :icon="RefreshIcon" @click="load">{{ t('common.refresh') }}</el-button>
      </div>
    </header>

    <!-- KPI cards -->
    <section class="kpi-grid">
      <div class="kpi-card kpi-total">
        <div class="kpi-label">{{ t('cs.kpiTotal') }}</div>
        <div class="kpi-value">{{ formatN(overview.totalConversations) }}</div>
        <div class="kpi-foot">{{ t('cs.kpiWindow', { d: overview.windowDays || windowDays }) }}</div>
      </div>
      <div class="kpi-card kpi-open">
        <div class="kpi-label">{{ t('cs.kpiOpen') }}</div>
        <div class="kpi-value">{{ formatN(overview.openConversations) }}</div>
        <div class="kpi-foot">{{ t('cs.kpiSnapshot') }}</div>
      </div>
      <div class="kpi-card kpi-pending">
        <div class="kpi-label">{{ t('cs.kpiPending') }}</div>
        <div class="kpi-value">{{ formatN(overview.pendingConversations) }}</div>
        <div class="kpi-foot">{{ t('cs.kpiSnapshot') }}</div>
      </div>
      <div class="kpi-card kpi-resolved">
        <div class="kpi-label">{{ t('cs.kpiResolved') }}</div>
        <div class="kpi-value">{{ formatN(overview.resolvedConversations) }}</div>
        <div class="kpi-foot">{{ t('cs.kpiWindow', { d: overview.windowDays || windowDays }) }}</div>
      </div>
    </section>

    <!-- Charts row -->
    <section class="charts-grid">
      <div class="chart-card">
        <div class="chart-head">
          <h3 class="chart-title">{{ t('cs.chartDailyTitle') }}</h3>
          <span class="chart-sub">{{ t('cs.chartDailySub') }}</span>
        </div>
        <v-chart class="chart" :option="dailyOption" autoresize />
      </div>
      <div class="chart-card">
        <div class="chart-head">
          <h3 class="chart-title">{{ t('cs.chartChannelTitle') }}</h3>
          <span class="chart-sub">{{ t('cs.chartChannelSub') }}</span>
        </div>
        <v-chart class="chart" :option="channelOption" autoresize />
      </div>
    </section>

    <!-- Footer note -->
    <p class="footnote">{{ t('cs.analyticsFootnote') }}</p>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Refresh as RefreshIcon } from '@element-plus/icons-vue'

import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart } from 'echarts/charts'
import {
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent,
} from 'echarts/components'
import VChart from 'vue-echarts'

import { getAnalyticsOverview } from '@/api/service'

use([
  CanvasRenderer,
  LineChart,
  PieChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent,
])

const { t } = useI18n()

const windowDays = ref(30)
const overview = ref({
  windowDays: 30,
  totalConversations: 0,
  openConversations: 0,
  pendingConversations: 0,
  resolvedConversations: 0,
  byChannel: [],
  byStatus: [],
  dailyConversations: [],
})
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const data = await getAnalyticsOverview(windowDays.value)
    overview.value = data || overview.value
  } catch (err) {
    ElMessage.error(err?.message || t('cs.analyticsLoadFailed'))
  } finally {
    loading.value = false
  }
}

function formatN(n) {
  if (n === null || n === undefined) return '—'
  return Number(n).toLocaleString('en-GB')
}

// ECharts options -- recompute when data changes via computed()
const BRAND_GREEN = '#0f3a26'
const BRAND_GREEN_LIGHT = '#27774d'
const PALETTE = ['#0f3a26', '#1677ff', '#27774d', '#b35a00', '#c45a4d', '#8a6e2f', '#7a3e8f']

const dailyOption = computed(() => {
  const series = overview.value.dailyConversations || []
  return {
    grid: { left: 36, right: 16, top: 24, bottom: 28 },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' },
    },
    xAxis: {
      type: 'category',
      data: series.map(p => p.date.slice(5)),    // MM-DD only
      axisLine: { lineStyle: { color: '#cbd5d0' } },
      axisLabel: { color: '#5b6b62', fontSize: 11 },
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLine: { show: false },
      axisLabel: { color: '#8a9690', fontSize: 11 },
      splitLine: { lineStyle: { color: '#eef2f0' } },
    },
    series: [{
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { color: BRAND_GREEN, width: 2 },
      itemStyle: { color: BRAND_GREEN },
      areaStyle: {
        color: {
          type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(15, 58, 38, 0.20)' },
            { offset: 1, color: 'rgba(15, 58, 38, 0.00)' },
          ],
        },
      },
      data: series.map(p => p.count),
    }],
  }
})

const channelOption = computed(() => {
  const slices = (overview.value.byChannel || []).map((s, i) => ({
    name: s.label || s.key,
    value: s.value,
    itemStyle: { color: PALETTE[i % PALETTE.length] },
  }))
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: {
      orient: 'horizontal',
      bottom: 0,
      textStyle: { color: '#5b6b62', fontSize: 11 },
    },
    series: [{
      type: 'pie',
      radius: ['52%', '78%'],
      avoidLabelOverlap: true,
      label: {
        show: true,
        formatter: '{b}\n{d}%',
        fontSize: 11,
        color: '#1c2e25',
      },
      labelLine: { show: true, length: 8, length2: 8 },
      data: slices.length ? slices : [{ name: t('cs.analyticsNoData'), value: 1, itemStyle: { color: '#e6ece9' }, label: { show: false } }],
    }],
  }
})

onMounted(load)
</script>

<style scoped>
.analytics {
  padding: 0 4px 24px;
}
.hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 18px;
  gap: 12px;
  flex-wrap: wrap;
}
.hero-title {
  margin: 0;
  font-size: 18px;
  color: #0f3a26;
}
.hero-sub {
  margin: 4px 0 0;
  color: #5b6b62;
  font-size: 13px;
}
.hero-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

/* ------ KPI cards ------ */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.kpi-card {
  background: #fff;
  border: 1px solid #e6ece9;
  border-radius: 10px;
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  border-top: 3px solid #0f3a26;
  transition: box-shadow 0.15s ease;
}
.kpi-card:hover {
  box-shadow: 0 6px 18px rgba(15, 58, 38, 0.08);
}
.kpi-total    { border-top-color: #0f3a26; }
.kpi-open     { border-top-color: #1677ff; }
.kpi-pending  { border-top-color: #b35a00; }
.kpi-resolved { border-top-color: #27774d; }
.kpi-label {
  font-size: 12px;
  color: #5b6b62;
  text-transform: uppercase;
  letter-spacing: 0.4px;
  font-weight: 600;
}
.kpi-value {
  font-size: 32px;
  font-weight: 700;
  color: #0f3a26;
  line-height: 1.1;
  font-variant-numeric: tabular-nums;
}
.kpi-foot {
  font-size: 11px;
  color: #8a9690;
}

/* ------ Charts ------ */
.charts-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 12px;
}
.chart-card {
  background: #fff;
  border: 1px solid #e6ece9;
  border-radius: 10px;
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-height: 320px;
}
.chart-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  flex-wrap: wrap;
}
.chart-title {
  margin: 0;
  font-size: 14px;
  color: #0f3a26;
  font-weight: 600;
}
.chart-sub {
  color: #8a9690;
  font-size: 11px;
}
.chart {
  flex: 1;
  height: 280px;
}

/* ------ Footer ------ */
.footnote {
  margin-top: 12px;
  color: #8a9690;
  font-size: 11px;
  font-style: italic;
}

@media (max-width: 900px) {
  .charts-grid {
    grid-template-columns: 1fr;
  }
}
</style>
