<template>
  <div class="page">
    <!-- Period filter -->
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" @submit.prevent>
        <el-form-item :label="t('whReport.period')">
          <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD"
            :range-separator="t('common.to')" start-placeholder="From" end-placeholder="To"
            style="width: 280px" @change="onDateChange" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="RefreshIcon" @click="reload">{{ t('common.refresh') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- KPI doc counts -->
    <el-card shadow="never" class="kpi-card">
      <div class="section-title">{{ t('whReport.docCounts') }}</div>
      <div class="kpi-grid">
        <div v-for="t_ in DOC_TYPES" :key="t_" class="kpi-block">
          <div class="kpi-label">{{ t(`whReport.doc_${t_}`) }}</div>
          <div class="kpi-rows">
            <div v-for="s in STATUSES[t_]" :key="s" class="kpi-row">
              <span class="kpi-status" :class="`s-${s}`">{{ t(`whReport.status_${s}`, s) }}</span>
              <span class="kpi-num">{{ (report.docCounts?.[t_]?.[s]) || 0 }}</span>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <div class="two-col">
      <!-- Top inbound -->
      <el-card shadow="never" class="block-card">
        <div class="section-title">{{ t('whReport.topInbound') }}</div>
        <el-table :data="report.topInbound || []" size="small" border>
          <el-table-column type="index" label="#" width="40" align="center" />
          <el-table-column prop="inputItemCode" :label="t('inputItem.code')" width="100" />
          <el-table-column prop="inputItemName" :label="t('inputItem.name')" min-width="140" show-overflow-tooltip />
          <el-table-column :label="t('whReport.qty')" width="110" align="right">
            <template #default="{ row }">
              <strong class="text-green">+{{ fmtQty(row.totalQty) }}</strong>
              <span class="unit">{{ row.unit }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="txnCount" :label="t('whReport.txns')" width="60" align="center" />
        </el-table>
      </el-card>

      <!-- Top outbound -->
      <el-card shadow="never" class="block-card">
        <div class="section-title">{{ t('whReport.topOutbound') }}</div>
        <el-table :data="report.topOutbound || []" size="small" border>
          <el-table-column type="index" label="#" width="40" align="center" />
          <el-table-column prop="inputItemCode" :label="t('inputItem.code')" width="100" />
          <el-table-column prop="inputItemName" :label="t('inputItem.name')" min-width="140" show-overflow-tooltip />
          <el-table-column :label="t('whReport.qty')" width="110" align="right">
            <template #default="{ row }">
              <strong class="text-red">-{{ fmtQty(row.totalQty) }}</strong>
              <span class="unit">{{ row.unit }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="txnCount" :label="t('whReport.txns')" width="60" align="center" />
        </el-table>
      </el-card>
    </div>

    <!-- Low stock alert -->
    <el-card shadow="never" class="block-card">
      <div class="section-title">
        {{ t('whReport.lowStock') }}
        <el-tag v-if="(report.lowStock || []).length" type="danger" size="small" effect="dark" style="margin-left: 8px">
          {{ (report.lowStock || []).length }}
        </el-tag>
      </div>
      <el-table :data="report.lowStock || []" size="small" border :empty-text="t('whReport.lowStockEmpty')">
        <el-table-column prop="inputItemCode" :label="t('inputItem.code')" width="100" />
        <el-table-column prop="inputItemName" :label="t('inputItem.name')" min-width="160" show-overflow-tooltip />
        <el-table-column prop="warehouseName" :label="t('whReport.warehouse')" min-width="140" />
        <el-table-column :label="t('whReport.available')" width="110" align="right">
          <template #default="{ row }">{{ fmtQty(row.qtyAvailable) }} <span class="unit">{{ row.unit }}</span></template>
        </el-table-column>
        <el-table-column :label="t('whReport.minStock')" width="110" align="right">
          <template #default="{ row }">{{ fmtQty(row.minStockQty) }}</template>
        </el-table-column>
        <el-table-column :label="t('whReport.shortage')" width="110" align="right">
          <template #default="{ row }">
            <strong class="text-red">{{ fmtQty(row.shortageQty) }}</strong>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Stock by warehouse -->
    <el-card shadow="never" class="block-card">
      <div class="section-title">{{ t('whReport.stockByWh') }}</div>
      <el-table :data="report.stockByWarehouse || []" size="small" border>
        <el-table-column prop="warehouseCode" :label="t('whReport.whCode')" width="120" />
        <el-table-column prop="warehouseName" :label="t('whReport.warehouse')" min-width="160" show-overflow-tooltip />
        <el-table-column :label="t('whReport.purpose')" width="140">
          <template #default="{ row }">
            <el-tag size="small">{{ row.purpose || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="itemCount" :label="t('whReport.items')" width="80" align="center" />
        <el-table-column :label="t('whReport.totalQty')" width="120" align="right">
          <template #default="{ row }"><strong>{{ fmtQty(row.totalQty) }}</strong></template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Refresh as RefreshIcon } from '@element-plus/icons-vue'
import { getWarehouseReport } from '@/api/warehouseReport'

const { t } = useI18n()

const DOC_TYPES = ['inbound', 'outbound', 'stocktake', 'transfer', 'scrap']
const STATUSES = {
  inbound:   ['draft', 'confirmed', 'cancelled'],
  outbound:  ['draft', 'picked', 'confirmed', 'cancelled'],
  stocktake: ['draft', 'counting', 'confirmed', 'cancelled'],
  transfer:  ['draft', 'confirmed', 'cancelled'],
  scrap:     ['draft', 'confirmed', 'cancelled'],
}

function fmtQty(v) {
  if (v == null) return '-'
  return Number(v).toLocaleString(undefined, { maximumFractionDigits: 3 })
}

// last 30 days default
const today = new Date()
const monthAgo = new Date(today.getTime() - 30 * 86400000)
const dateRange = ref([monthAgo.toISOString().slice(0, 10), today.toISOString().slice(0, 10)])

const report = reactive({})

async function reload() {
  const [from, to] = dateRange.value || []
  try {
    const data = await getWarehouseReport({ from, to })
    Object.assign(report, data)
  } catch (e) { console.error(e) }
}
function onDateChange() { reload() }
onMounted(reload)
</script>

<style scoped>
.page { padding: 16px; display: flex; flex-direction: column; gap: 12px; }
.filter-card, .kpi-card, .block-card { margin: 0; }
.section-title { font-size: 16px; font-weight: 600; color: #1f2937; margin-bottom: 12px; display: flex; align-items: center; }
.kpi-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 12px; }
.kpi-block { background: #f9fafb; border-radius: 6px; padding: 10px 12px; }
.kpi-label { font-size: 13px; font-weight: 600; color: #4b5563; margin-bottom: 6px; text-transform: capitalize; }
.kpi-rows { display: flex; flex-direction: column; gap: 4px; }
.kpi-row { display: flex; justify-content: space-between; font-size: 12px; }
.kpi-status { color: #6b7280; }
.kpi-status.s-confirmed { color: #16a34a; }
.kpi-status.s-cancelled { color: #909399; }
.kpi-status.s-draft     { color: #d97706; }
.kpi-status.s-picked    { color: #d97706; }
.kpi-status.s-counting  { color: #d97706; }
.kpi-num { font-weight: 600; color: #1f2937; }
.two-col { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.unit { color: #909399; font-size: 11px; margin-left: 2px; }
.text-green { color: #16a34a; }
.text-red { color: #ef4444; }
@media (max-width: 1100px) { .two-col { grid-template-columns: 1fr; } .kpi-grid { grid-template-columns: repeat(2, 1fr); } }
</style>
