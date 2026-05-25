<template>
  <div class="dashboard" v-loading="loading">
    <!-- 顶部欢迎条 -->
    <div class="hero">
      <div>
        <div class="greeting">{{ greeting }}, {{ auth.nickname || auth.username }} 👋</div>
        <div class="sub">Welcome to <strong>2Africa AgriOS</strong> · AI Farm Management OS for Africa</div>
      </div>
      <el-button :icon="RefreshIcon" plain @click="load">刷新</el-button>
    </div>

    <!-- KPI 4 卡片 -->
    <div class="kpi-grid">
      <div class="kpi-card" :class="['c-blue']">
        <div class="kpi-label">进行中计划</div>
        <div class="kpi-value">{{ data.activePlanCount ?? '-' }}</div>
        <div class="kpi-foot">已排期 / 种植中</div>
      </div>
      <div class="kpi-card" :class="['c-orange']">
        <div class="kpi-label">待审核农事</div>
        <div class="kpi-value">{{ data.pendingActivityCount ?? '-' }}</div>
        <div class="kpi-foot">需 Manager 审批</div>
      </div>
      <div class="kpi-card" :class="['c-green']">
        <div class="kpi-label">今日采收 (kg)</div>
        <div class="kpi-value">{{ formatNum(data.todayHarvestKg) }}</div>
        <div class="kpi-foot">{{ todayDate }}</div>
      </div>
      <div class="kpi-card" :class="['c-purple']">
        <div class="kpi-label">待处理批次</div>
        <div class="kpi-value">{{ data.pendingBatchCount ?? '-' }}</div>
        <div class="kpi-foot">pending 状态</div>
      </div>
    </div>

    <!-- 两张图卡 -->
    <div class="charts-row">
      <el-card shadow="never" class="chart-card">
        <template #header><span>📈 近 7 天采收趋势 (kg)</span></template>
        <div class="trend-bars">
          <div
            v-for="(d, i) in data.harvest7Days || []"
            :key="i"
            class="trend-col"
            :title="`${d.date}: ${formatNum(d.qty)} kg`"
          >
            <div class="bar-wrap">
              <div
                class="bar"
                :style="{ height: barHeight(d.qty) + '%' }"
              ></div>
            </div>
            <div class="bar-value">{{ shortNum(d.qty) }}</div>
            <div class="bar-label">{{ shortDate(d.date) }}</div>
          </div>
        </div>
        <div v-if="totalWeek === 0" class="empty">本周还没有采收记录</div>
        <div v-else class="trend-foot">本周合计: <strong>{{ formatNum(totalWeek) }}</strong> kg</div>
      </el-card>

      <el-card shadow="never" class="chart-card">
        <template #header><span>🥑 按作物分布 (累计 kg)</span></template>
        <div v-if="!data.harvestByCrop || data.harvestByCrop.length === 0" class="empty">还没有采收数据</div>
        <div v-else class="crop-bars">
          <div v-for="row in data.harvestByCrop" :key="row.crop_id" class="crop-row">
            <div class="crop-name">{{ row.crop_name || '(未关联作物)' }}</div>
            <div class="crop-bar-wrap">
              <div
                class="crop-bar"
                :style="{ width: cropPercent(row.qty) + '%' }"
              ></div>
            </div>
            <div class="crop-qty">{{ formatNum(row.qty) }}</div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 两份清单 -->
    <div class="lists-row">
      <el-card shadow="never" class="list-card">
        <template #header>
          <div class="list-hdr">
            <span>⏳ 待审核农事 (Top 5)</span>
            <router-link to="/production/activities">
              <el-button link size="small" type="primary">前往审核 →</el-button>
            </router-link>
          </div>
        </template>
        <el-table v-if="data.pendingActivities?.length" :data="data.pendingActivities" :show-header="true" size="small">
          <el-table-column label="日期" prop="occur_date" width="100" />
          <el-table-column label="类型" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="actTagType(row.activity_type)">
                {{ actTypeLabel(row.activity_type) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="计划" prop="plan_code" min-width="120" />
          <el-table-column label="地块" prop="plot_name" min-width="130" />
          <el-table-column label="操作人" prop="operator_name" width="100" />
        </el-table>
        <div v-else class="empty">暂无待审核农事 🎉</div>
      </el-card>

      <el-card shadow="never" class="list-card">
        <template #header>
          <div class="list-hdr">
            <span>🌾 最近采收 (Top 5)</span>
            <router-link to="/production/harvests">
              <el-button link size="small" type="primary">查看全部 →</el-button>
            </router-link>
          </div>
        </template>
        <el-table v-if="data.recentHarvests?.length" :data="data.recentHarvests" :show-header="true" size="small">
          <el-table-column label="日期" prop="harvest_date" width="100" />
          <el-table-column label="采收单号" prop="code" width="160" />
          <el-table-column label="作物" prop="crop_name" min-width="100" />
          <el-table-column label="地块" prop="plot_name" min-width="130" />
          <el-table-column label="量(kg)" align="right" width="100">
            <template #default="{ row }">
              <strong>{{ formatNum(row.qty_kg) }}</strong>
            </template>
          </el-table-column>
          <el-table-column label="批次" prop="batch_code" min-width="180">
            <template #default="{ row }">
              <code class="batch-code">{{ row.batch_code }}</code>
            </template>
          </el-table-column>
        </el-table>
        <div v-else class="empty">暂无采收记录</div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Refresh as RefreshIcon } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { getDashboardSummary } from '@/api/dashboard'

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

// ============================================================
// 工具
// ============================================================
const todayDate = new Date().toISOString().slice(0, 10)

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6)  return '🌙 夜深了'
  if (h < 12) return '☀️ 早上好'
  if (h < 14) return '🍱 中午好'
  if (h < 18) return '☕ 下午好'
  return '🌇 晚上好'
})

function formatNum(v) {
  if (v == null) return '0'
  return Number(v).toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 2 })
}

function shortNum(v) {
  const n = Number(v) || 0
  if (n === 0) return ''
  if (n >= 10000) return (n / 1000).toFixed(0) + 'k'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return n.toFixed(0)
}

function shortDate(d) {
  // YYYY-MM-DD → MM/DD
  return d?.slice(5).replace('-', '/') || ''
}

const maxWeekQty = computed(() => {
  const arr = data.value.harvest7Days || []
  return Math.max(...arr.map(r => Number(r.qty) || 0), 1)
})

const totalWeek = computed(() => {
  const arr = data.value.harvest7Days || []
  return arr.reduce((s, r) => s + (Number(r.qty) || 0), 0)
})

function barHeight(v) {
  const n = Number(v) || 0
  if (n === 0) return 0
  return Math.max(4, (n / maxWeekQty.value) * 100)
}

const maxCropQty = computed(() => {
  const arr = data.value.harvestByCrop || []
  return Math.max(...arr.map(r => Number(r.qty) || 0), 1)
})

function cropPercent(v) {
  const n = Number(v) || 0
  return Math.max(2, (n / maxCropQty.value) * 100)
}

// 活动类型字典
const ACT_LABEL = {
  sow: 'Sowing', fertilize: 'Fertilizing', spray: 'Spraying',
  weed: 'Weeding', water: 'Watering', prune: 'Pruning', other: 'Other'
}
const ACT_TAG = {
  sow: 'success', fertilize: 'warning', spray: 'danger',
  weed: 'info', water: 'primary', prune: 'info', other: 'info'
}
function actTypeLabel(v) { return ACT_LABEL[v] || v }
function actTagType(v)   { return ACT_TAG[v] || 'info' }
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* === Hero === */
.hero {
  background: linear-gradient(135deg, #001529 0%, #003a8c 60%, #1890ff 100%);
  color: #fff;
  padding: 18px 22px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 4px 16px rgba(0, 21, 41, 0.18);
}
.greeting { font-size: 20px; font-weight: 600; }
.sub      { font-size: 13px; opacity: .85; margin-top: 4px; }
.hero :deep(.el-button) {
  background: rgba(255, 255, 255, 0.18);
  border-color: rgba(255, 255, 255, 0.3);
  color: #fff;
}

/* === KPI cards === */
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
.c-blue::before   { background: #1890ff; }
.c-orange::before { background: #fa8c16; }
.c-green::before  { background: #52c41a; }
.c-purple::before { background: #722ed1; }

.kpi-label { font-size: 12px; color: #909399; }
.kpi-value {
  font-size: 32px;
  font-weight: 700;
  color: #1f2329;
  margin: 4px 0 2px;
  font-variant-numeric: tabular-nums;
}
.kpi-foot { font-size: 11px; color: #909399; }

/* === Charts === */
.charts-row {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 14px;
}
.chart-card { min-height: 240px; }
.empty {
  color: #c0c4cc;
  font-size: 13px;
  text-align: center;
  padding: 24px 0;
}

/* 7 天柱图 */
.trend-bars {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 6px;
  height: 160px;
  padding: 0 4px;
}
.trend-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}
.bar-wrap {
  width: 100%;
  height: 110px;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}
.bar {
  width: 70%;
  background: linear-gradient(to top, #1890ff, #69c0ff);
  border-radius: 4px 4px 0 0;
  min-height: 2px;
  transition: height .3s;
}
.bar-value { font-size: 11px; color: #606266; min-height: 14px; }
.bar-label { font-size: 11px; color: #909399; }
.trend-foot {
  text-align: right;
  font-size: 12px;
  color: #606266;
  margin-top: 8px;
  padding-right: 8px;
}

/* 按作物分布水平条 */
.crop-bars {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 4px 0;
}
.crop-row {
  display: grid;
  grid-template-columns: 110px 1fr 80px;
  align-items: center;
  gap: 10px;
  font-size: 13px;
}
.crop-name {
  color: #1f2329;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.crop-bar-wrap {
  height: 14px;
  background: #f5f7fa;
  border-radius: 7px;
  overflow: hidden;
}
.crop-bar {
  height: 100%;
  background: linear-gradient(to right, #52c41a, #95de64);
  border-radius: 7px;
  transition: width .4s;
}
.crop-qty {
  text-align: right;
  font-weight: 600;
  color: #1f2329;
  font-variant-numeric: tabular-nums;
}

/* === Lists === */
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

/* === Responsive === */
@media (max-width: 1100px) {
  .kpi-grid { grid-template-columns: repeat(2, 1fr); }
  .charts-row, .lists-row { grid-template-columns: 1fr; }
}
</style>
