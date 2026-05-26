<template>
  <el-dialog
    v-model="visible"
    :title="dialogTitle"
    width="640px"
    :close-on-click-modal="false"
    destroy-on-close
    @closed="onClosed"
  >
    <div v-loading="loading" class="pnl-body">
      <div v-if="data" class="pnl-grid">
        <!-- 左侧:成本明细 -->
        <div class="pnl-side">
          <div class="pnl-header">
            <el-icon><Coin /></el-icon>
            <span>{{ t('pnl.costBreakdown') }}</span>
          </div>
          <div class="pnl-row">
            <span class="pnl-label">{{ t('activity.laborCost') }}</span>
            <span class="pnl-value">{{ fmt(data.laborCost) }}</span>
          </div>
          <div class="pnl-row">
            <span class="pnl-label">{{ t('activity.waterCost') }}</span>
            <span class="pnl-value">{{ fmt(data.waterCost) }}</span>
          </div>
          <div class="pnl-row">
            <span class="pnl-label">{{ t('activity.electricityCost') }}</span>
            <span class="pnl-value">{{ fmt(data.electricityCost) }}</span>
          </div>
          <div class="pnl-row">
            <span class="pnl-label">{{ t('activity.fertilizerCost') }}</span>
            <span class="pnl-value">{{ fmt(data.fertilizerCost) }}</span>
          </div>
          <div class="pnl-row">
            <span class="pnl-label">{{ t('activity.otherCost') }}</span>
            <span class="pnl-value">{{ fmt(data.otherCost) }}</span>
          </div>
          <div class="pnl-row">
            <span class="pnl-label">{{ t('pnl.inputCost') }}</span>
            <span class="pnl-value">{{ fmt(data.inputCost) }}</span>
          </div>
          <el-divider style="margin: 8px 0" />
          <div class="pnl-row total">
            <span class="pnl-label">{{ t('pnl.totalCost') }}</span>
            <span class="pnl-value">{{ fmt(data.totalCost) }} {{ data.currency }}</span>
          </div>
        </div>

        <!-- 右侧:收入 + 毛利 -->
        <div class="pnl-side">
          <div class="pnl-header">
            <el-icon><TrendCharts /></el-icon>
            <span>{{ t('pnl.revenue') }} & {{ t('pnl.grossProfit') }}</span>
          </div>
          <div class="pnl-row big">
            <span class="pnl-label">{{ t('pnl.revenue') }}</span>
            <strong class="pnl-value revenue">{{ fmt(data.totalRevenue) }} {{ data.currency }}</strong>
          </div>
          <div class="pnl-row big">
            <span class="pnl-label">{{ t('pnl.totalCost') }}</span>
            <strong class="pnl-value cost">- {{ fmt(data.totalCost) }} {{ data.currency }}</strong>
          </div>
          <el-divider style="margin: 8px 0" />
          <div class="pnl-row big total">
            <span class="pnl-label">{{ t('pnl.grossProfit') }}</span>
            <strong :class="['pnl-value', 'profit', data.grossProfit >= 0 ? 'pos' : 'neg']">
              {{ fmt(data.grossProfit) }} {{ data.currency }}
            </strong>
          </div>
          <div class="margin-block">
            <div class="margin-label">{{ t('pnl.grossMargin') }}</div>
            <div v-if="data.grossMarginPct != null" class="margin-value"
                 :class="data.grossMarginPct >= 0 ? 'pos' : 'neg'">
              {{ data.grossMarginPct }}%
            </div>
            <div v-else class="margin-na">{{ t('pnl.grossMarginNa') }}</div>
          </div>
          <div class="meta">
            {{ t('pnl.activityCount') }}: <strong>{{ data.activityCount }}</strong>
          </div>
        </div>
      </div>

      <div v-else class="pnl-empty">{{ t('pnl.noData') }}</div>

      <div v-if="kind === 'batch'" class="prorated-note">
        ℹ️ {{ t('pnl.proratedNote') }}
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Coin, TrendCharts } from '@element-plus/icons-vue'
import { getPlanPnL, getBatchPnL } from '@/api/finance'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  // 'plan' or 'batch'
  kind: { type: String, required: true },
  refId: { type: [Number, String], default: null },
  refCode: { type: String, default: '' },
})
const emit = defineEmits(['update:modelValue'])

const { t } = useI18n()
const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const loading = ref(false)
const data = ref(null)

const dialogTitle = computed(() => {
  if (props.kind === 'batch') return t('pnl.batchTitle', { code: props.refCode || props.refId })
  return t('pnl.planTitle', { code: props.refCode || props.refId })
})

function fmt(v) {
  if (v == null) return '0.00'
  return Number(v).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

async function loadData() {
  if (!props.refId) return
  loading.value = true
  try {
    data.value = props.kind === 'batch'
      ? await getBatchPnL(props.refId)
      : await getPlanPnL(props.refId)
  } catch (e) {
    ElMessage.error('Failed to load P&L')
    data.value = null
  } finally {
    loading.value = false
  }
}

function onClosed() {
  data.value = null
}

watch(visible, (v) => {
  if (v && props.refId) loadData()
})
</script>

<style scoped>
.pnl-body { min-height: 320px; }

.pnl-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.pnl-side {
  background: #f5f7fa;
  border-radius: 6px;
  padding: 14px;
}

.pnl-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  font-size: 13px;
  color: #1f2329;
  margin-bottom: 10px;
}

.pnl-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  font-size: 13px;
  padding: 3px 0;
  color: #606266;
}
.pnl-row.big { font-size: 14px; padding: 6px 0; }
.pnl-row.total { font-weight: 600; color: #1f2329; }

.pnl-label { flex: 1; }
.pnl-value { font-family: 'Consolas', monospace; }

.pnl-value.revenue { color: #2BA84A; }
.pnl-value.cost    { color: #909399; }
.pnl-value.profit.pos { color: #2BA84A; font-size: 16px; }
.pnl-value.profit.neg { color: #f56c6c; font-size: 16px; }

.margin-block {
  margin-top: 10px;
  padding: 10px 12px;
  background: #fff;
  border-radius: 4px;
  text-align: center;
}
.margin-label { font-size: 11px; color: #909399; }
.margin-value {
  font-size: 22px;
  font-weight: 700;
  font-family: 'Consolas', monospace;
}
.margin-value.pos { color: #2BA84A; }
.margin-value.neg { color: #f56c6c; }
.margin-na { color: #c0c4cc; font-size: 13px; margin-top: 4px; }

.meta {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
  text-align: right;
}

.pnl-empty {
  text-align: center;
  color: #c0c4cc;
  font-size: 14px;
  padding: 60px 0;
}

.prorated-note {
  margin-top: 12px;
  font-size: 12px;
  color: #909399;
  text-align: center;
  background: #ecf9ef;
  padding: 8px;
  border-radius: 4px;
}
</style>
