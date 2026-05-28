<template>
  <div class="page">
    <el-card shadow="never">
      <div class="hero">
        <div>
          <h2 class="title">{{ t('portal.myOrders') }}</h2>
          <p class="dim small">{{ t('portal.myOrdersHint') }}</p>
        </div>
        <el-tag size="large" type="success" effect="plain">
          {{ t('portal.tag') }}
        </el-tag>
      </div>
    </el-card>

    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item :label="t('order.code')">
          <el-input v-model="query.code" clearable :placeholder="t('order.placeholderCode')" style="width: 180px" />
        </el-form-item>
        <el-form-item :label="t('order.status')">
          <el-select v-model="query.status" :placeholder="t('common.all')" clearable style="width: 160px">
            <el-option v-for="s in ORDER_STATUSES" :key="s" :label="t(`order.st_${s}`, s)" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('order.dateFrom')">
          <el-date-picker v-model="query.dateFrom" type="date" value-format="YYYY-MM-DD" style="width: 160px" />
        </el-form-item>
        <el-form-item :label="t('order.dateTo')">
          <el-date-picker v-model="query.dateTo" type="date" value-format="YYYY-MM-DD" style="width: 160px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="SearchIcon" @click="reload(1)">{{ t('common.search') }}</el-button>
          <el-button :icon="RefreshIcon" @click="onReset">{{ t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="code" :label="t('order.code')" width="180">
          <template #default="{ row }"><code class="code">{{ row.code }}</code></template>
        </el-table-column>
        <el-table-column prop="orderDate" :label="t('order.date')" width="130" />
        <el-table-column prop="dueDate" :label="t('order.dueDate')" width="130" />
        <el-table-column :label="t('order.status')" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small" effect="dark">
              {{ t(`order.st_${row.status}`, row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('order.paymentStatus')" width="130" align="center">
          <template #default="{ row }">
            <el-tag :type="payTag(row.paymentStatus)" size="small" effect="plain">
              {{ t(`order.pay_${row.paymentStatus}`, row.paymentStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('order.totalAmount')" width="140" align="right">
          <template #default="{ row }">
            <span class="amt">{{ formatMoney(row.totalAmount) }}</span>
            <span class="dim small" style="margin-left:4px">{{ row.currency }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('order.paidAmount')" width="140" align="right">
          <template #default="{ row }">
            <span class="amt">{{ formatMoney(row.paidAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" :label="t('common.remark')" min-width="200" show-overflow-tooltip />
      </el-table>

      <el-pagination
        class="pager"
        background
        layout="total, sizes, prev, pager, next, jumper"
        :page-sizes="[10, 20, 50]"
        :total="total"
        :page-size="pageSize"
        :current-page="page"
        @size-change="onSizeChange"
        @current-change="onPageChange"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Search as SearchIcon, Refresh as RefreshIcon } from '@element-plus/icons-vue'
import request from '@/utils/request'

const { t } = useI18n()
const ORDER_STATUSES = ['draft', 'confirmed', 'picked', 'shipped', 'cancelled']

const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const loading = ref(false)
const query = reactive({ code: '', status: '', dateFrom: '', dateTo: '' })

function statusTag(s) {
  return ({ draft: 'info', confirmed: 'primary', picked: 'warning', shipped: 'success', cancelled: 'danger' })[s] || 'info'
}
function payTag(s) {
  return ({ unpaid: 'danger', partial: 'warning', paid: 'success' })[s] || 'info'
}
function formatMoney(v) {
  if (v == null) return '-'
  return Number(v).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

async function reload(toPage) {
  if (toPage) page.value = toPage
  loading.value = true
  try {
    const data = await request.get('/v1/portal/orders', {
      params: { ...query, page: page.value, size: pageSize.value },
    })
    list.value = data.list
    total.value = data.total
  } finally { loading.value = false }
}
function onReset() { Object.assign(query, { code: '', status: '', dateFrom: '', dateTo: '' }); reload(1) }
function onPageChange(p) { page.value = p; reload() }
function onSizeChange(s) { pageSize.value = s; reload(1) }

onMounted(() => reload(1))
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.hero { display: flex; justify-content: space-between; align-items: center; }
.title { margin: 0 0 4px 0; font-size: 18px; color: #1f2937; }
.dim { color: #94a3b8; }
.small { font-size: 12px; }
.code { font-family: 'Monaco', 'Consolas', monospace; font-size: 12px; background: #f5f7fa; padding: 2px 8px; border-radius: 4px; color: #1f7a35; }
.amt { font-variant-numeric: tabular-nums; font-weight: 500; color: #1f2937; }
.pager { margin-top: 14px; justify-content: flex-end; }
:deep(.el-card__body) { padding: 16px; }
</style>
