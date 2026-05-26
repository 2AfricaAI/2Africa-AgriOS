<template>
  <div class="page" v-loading="loading">
    <div class="hero">
      <div>
        <div class="hero-title">
          <el-icon><Wallet /></el-icon>
          {{ t('ap.title') }}
        </div>
        <div class="hero-subtitle">{{ t('ap.subtitle') }}</div>
      </div>
      <el-button type="primary" :icon="RefreshIcon" @click="load">{{ t('common.refresh') }}</el-button>
    </div>

    <el-card shadow="never" class="table-card">
      <el-table :data="rows" border stripe size="small" :empty-text="t('ap.empty')" row-key="supplier_id">
        <el-table-column :label="t('ap.supplier')" min-width="220">
          <template #default="{ row }">
            <strong>{{ row.supplier_name }}</strong>
            <code class="dim small"> {{ row.supplier_code }}</code>
          </template>
        </el-table-column>
        <el-table-column :label="t('ap.supplierType')" width="130" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ row.supplier_type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('ap.totalBilled')" min-width="130" align="right">
          <template #default="{ row }">{{ fmt(row.total_billed) }}</template>
        </el-table-column>
        <el-table-column :label="t('ap.totalPaid')" min-width="130" align="right">
          <template #default="{ row }">
            <span class="dim">{{ fmt(row.total_paid) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('ap.apOutstanding')" min-width="140" align="right">
          <template #default="{ row }">
            <strong class="outstanding">{{ fmt(row.ap_outstanding) }}</strong>
          </template>
        </el-table-column>
        <el-table-column :label="t('ar.aging07')" min-width="100" align="right">
          <template #default="{ row }">{{ fmt(row.aging_0_7) }}</template>
        </el-table-column>
        <el-table-column :label="t('ar.aging814')" min-width="100" align="right">
          <template #default="{ row }">
            <span :class="{ amber: Number(row.aging_8_14) > 0 }">{{ fmt(row.aging_8_14) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('ar.aging1530')" min-width="100" align="right">
          <template #default="{ row }">
            <span :class="{ orange: Number(row.aging_15_30) > 0 }">{{ fmt(row.aging_15_30) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('ar.aging30plus')" min-width="120" align="right">
          <template #default="{ row }">
            <strong :class="{ red: Number(row.aging_30_plus) > 0 }">{{ fmt(row.aging_30_plus) }}</strong>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.actions')" width="150" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="success" size="small" @click="onShowPos(row)">
              💸 {{ t('ap.viewPos') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 供应商 PO 列表弹窗 -->
    <el-dialog
      v-model="poListVisible"
      :title="t('ap.posTitle', { supplier: selectedSupplier?.supplier_name || '' })"
      width="900px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-table
        :data="supplierPos"
        v-loading="posLoading"
        border stripe size="small"
        :empty-text="t('ap.posEmpty')"
      >
        <el-table-column :label="t('po.code')" width="170">
          <template #default="{ row }"><code class="po-code">{{ row.code }}</code></template>
        </el-table-column>
        <el-table-column prop="orderDate" :label="t('po.orderDate')" width="110" align="center" />
        <el-table-column :label="t('po.totalAmount')" width="130" align="right">
          <template #default="{ row }">
            {{ fmt(row.totalAmount) }} <span class="dim small">{{ row.currency }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('vpay.paidAmount')" width="120" align="right">
          <template #default="{ row }">
            <span class="dim">{{ fmt(row.paidAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('vpay.outstanding')" width="130" align="right">
          <template #default="{ row }">
            <strong style="color: #fa8c16">{{ fmt(outstandingOf(row)) }}</strong>
          </template>
        </el-table-column>
        <el-table-column :label="t('paymentStatus.dueDate')" width="115" align="center">
          <template #default="{ row }">
            <span :class="{ overdue: isOverdue(row) }">{{ row.dueDate || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('paymentStatus.label')" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="paymentTag(row.paymentStatus)" size="small">{{ paymentLabel(row.paymentStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.actions')" width="130" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.paymentStatus !== 'paid' && row.status !== 'cancelled'"
              link type="primary" size="small"
              @click="onOpenPayment(row)"
            >
              💰 {{ t('vpay.record') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 付款弹窗 -->
    <VendorPaymentDialog
      v-model="payDialogVisible"
      :po-info="payTarget"
      @saved="onPaymentSaved"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Wallet, Refresh as RefreshIcon } from '@element-plus/icons-vue'
import { getApAging } from '@/api/vendorPayment'
import { listPurchaseOrders } from '@/api/purchaseOrder'
import VendorPaymentDialog from '@/components/VendorPaymentDialog.vue'

const { t } = useI18n()
const loading = ref(false)
const rows = ref([])

function fmt(v) {
  if (v == null) return '0.00'
  return Number(v).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function outstandingOf(row) {
  if (!row) return 0
  return Math.max(0, Number(row.totalAmount || 0) - Number(row.paidAmount || 0))
}
function isOverdue(row) {
  if (!row || row.paymentStatus === 'paid' || !row.dueDate) return false
  return row.dueDate < new Date().toISOString().slice(0, 10)
}
const PAYMENT_TAG = { unpaid: 'info', partial: 'warning', paid: 'success' }
function paymentTag(v) { return PAYMENT_TAG[v] || 'info' }
function paymentLabel(v) { return t(`paymentStatus.${v}`, v) }

async function load() {
  loading.value = true
  try { rows.value = await getApAging() }
  catch {} finally { loading.value = false }
}
onMounted(load)

// ----- 供应商 PO 列表弹窗 -----
const poListVisible = ref(false)
const selectedSupplier = ref(null)
const supplierPos = ref([])
const posLoading = ref(false)

async function onShowPos(row) {
  selectedSupplier.value = row
  poListVisible.value = true
  posLoading.value = true
  try {
    const data = await listPurchaseOrders({
      supplierId: row.supplier_id,
      page: 1,
      size: 100,
    })
    // 只保留未付清的
    supplierPos.value = (data.list || []).filter(o => o.paymentStatus !== 'paid' && o.status !== 'cancelled')
  } catch {
    supplierPos.value = []
  } finally {
    posLoading.value = false
  }
}

// ----- 付款 -----
const payDialogVisible = ref(false)
const payTarget = ref(null)

function onOpenPayment(po) {
  payTarget.value = {
    id: po.id,
    code: po.code,
    supplierName: po.supplierName,
    totalAmount: po.totalAmount,
    paidAmount: po.paidAmount,
    currency: po.currency,
  }
  payDialogVisible.value = true
}

async function onPaymentSaved() {
  // 重拉当前供应商的 PO + AP 台账
  if (selectedSupplier.value) {
    await onShowPos(selectedSupplier.value)
  }
  await load()
}
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.hero {
  background:
    radial-gradient(circle at 90% 20%, rgba(255, 255, 255, 0.15), transparent 40%),
    linear-gradient(135deg, #5a2a06 0%, #b85c1f 55%, #fa8c16 100%);
  color: #fff; padding: 20px 24px; border-radius: 10px;
  display: flex; align-items: center; justify-content: space-between;
  box-shadow: 0 6px 20px rgba(184, 92, 31, 0.22);
}
.hero-title { font-size: 20px; font-weight: 600; display: inline-flex; align-items: center; gap: 8px; }
.hero-subtitle { font-size: 13px; opacity: .85; margin-top: 4px; }
.hero :deep(.el-button) { background: rgba(255,255,255,.18); border-color: rgba(255,255,255,.3); color: #fff; }

.dim { color: #909399; font-family: 'Consolas', monospace; }
.small { font-size: 11px; }
.outstanding { color: #fa8c16; }
.amber  { color: #e6a23c; font-weight: 600; }
.orange { color: #fa8c16; font-weight: 600; }
.red    { color: #f56c6c; font-weight: 700; }
.overdue { color: #f56c6c; font-weight: 700; }

.po-code {
  font-family: 'Consolas', monospace;
  background: #f5f7fa;
  padding: 2px 8px;
  border-radius: 4px;
  color: #fa8c16; font-size: 12px; font-weight: 600;
}
</style>
