<template>
  <div class="page" v-loading="loading">
    <div class="hero">
      <div>
        <div class="hero-title">
          <el-icon><Calendar /></el-icon>
          {{ t('monthly.title') }}
        </div>
        <div class="hero-subtitle">{{ t('monthly.subtitle') }}</div>
      </div>
      <el-button type="primary" :icon="RefreshIcon" @click="load">{{ t('common.refresh') }}</el-button>
    </div>

    <el-card shadow="never" class="table-card">
      <el-table :data="rows" border stripe size="small" :empty-text="t('pnl.emptyList')">
        <el-table-column :label="t('monthly.month')" width="120" align="center">
          <template #default="{ row }"><code class="month-code">{{ row.month }}</code></template>
        </el-table-column>
        <el-table-column :label="t('monthly.revenue')" min-width="140" align="right">
          <template #default="{ row }">
            <strong class="revenue">{{ fmt(row.revenue) }}</strong>
          </template>
        </el-table-column>
        <el-table-column :label="t('monthly.cost')" min-width="140" align="right">
          <template #default="{ row }">
            <span class="cost">{{ fmt(row.cost) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('monthly.grossProfit')" min-width="140" align="right">
          <template #default="{ row }">
            <strong :class="['profit', Number(row.gross_profit) >= 0 ? 'pos' : 'neg']">
              {{ fmt(row.gross_profit) }}
            </strong>
          </template>
        </el-table-column>
        <el-table-column :label="t('monthly.margin')" width="100" align="center">
          <template #default="{ row }">
            <strong v-if="Number(row.revenue) > 0"
                    :class="['margin', marginPct(row) >= 0 ? 'pos' : 'neg']">
              {{ marginPct(row) }}%
            </strong>
            <span v-else class="dim">-</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('monthly.orderCount')" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.order_count }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('monthly.customerCount')" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ row.customer_count }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('monthly.paymentReceived')" min-width="150" align="right">
          <template #default="{ row }">
            <span class="received">{{ fmt(row.payment_received) }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Calendar, Refresh as RefreshIcon } from '@element-plus/icons-vue'
import { getMonthlySummary } from '@/api/finance'

const { t } = useI18n()
const loading = ref(false)
const rows = ref([])

function fmt(v) {
  if (v == null) return '0.00'
  return Number(v).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function marginPct(row) {
  const rev = Number(row.revenue) || 0
  const gp = Number(row.gross_profit) || 0
  if (rev <= 0) return 0
  return Math.round((gp / rev) * 10000) / 100
}

async function load() {
  loading.value = true
  try { rows.value = await getMonthlySummary() }
  catch {} finally { loading.value = false }
}
onMounted(load)
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.hero {
  background:
    radial-gradient(circle at 90% 20%, rgba(255, 255, 255, 0.15), transparent 40%),
    linear-gradient(135deg, #0f3a26 0%, #1f7a35 55%, #2BA84A 100%);
  color: #fff; padding: 20px 24px; border-radius: 10px;
  display: flex; align-items: center; justify-content: space-between;
  box-shadow: 0 6px 20px rgba(15, 58, 38, 0.22);
}
.hero-title { font-size: 20px; font-weight: 600; display: inline-flex; align-items: center; gap: 8px; }
.hero-subtitle { font-size: 13px; opacity: .85; margin-top: 4px; }
.hero :deep(.el-button) { background: rgba(255,255,255,.18); border-color: rgba(255,255,255,.3); color: #fff; }

.month-code {
  font-family: 'Consolas', monospace;
  background: #f5f7fa; padding: 2px 8px; border-radius: 4px;
  color: #1f7a35; font-size: 12px; font-weight: 600;
}
.revenue { color: #2BA84A; font-family: 'Consolas', monospace; }
.cost    { color: #909399; font-family: 'Consolas', monospace; }
.received { color: #1677ff; font-family: 'Consolas', monospace; }
.profit.pos { color: #2BA84A; font-family: 'Consolas', monospace; }
.profit.neg { color: #f56c6c; font-family: 'Consolas', monospace; }
.margin.pos { color: #2BA84A; font-size: 14px; }
.margin.neg { color: #f56c6c; font-size: 14px; }
.dim { color: #c0c4cc; }
</style>
