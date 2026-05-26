<template>
  <div class="page" v-loading="loading">
    <div class="page-head">
      <el-button :icon="ArrowLeft" text @click="goBack">{{ t('po.backToList') }}</el-button>
      <div class="head-actions" v-if="data?.order">
        <el-button v-if="data.order.status === 'draft'" type="success" :icon="CheckIcon" @click="onConfirm">
          {{ t('po.confirm') }}
        </el-button>
        <el-button v-if="data.order.status === 'confirmed' || data.order.status === 'partial_received'"
                   type="primary" :icon="VanIcon" @click="onReceive">
          {{ t('po.receive') }}
        </el-button>
        <el-button v-if="data.order.paymentStatus !== 'paid' && !['cancelled', 'draft'].includes(data.order.status)"
                   type="warning" plain @click="openPaymentDialog">
          💰 {{ t('vpay.record') }}
        </el-button>
        <el-button v-if="data.order.status === 'draft' || data.order.status === 'confirmed'"
                   type="warning" :icon="CloseIcon" @click="onCancel">
          {{ t('po.cancel') }}
        </el-button>
      </div>
    </div>

    <!-- 头信息 -->
    <el-card shadow="never" class="info-card" v-if="data?.order">
      <template #header>
        <div class="card-head">
          <span class="card-title">
            <el-icon><Tickets /></el-icon>
            <code class="po-code">{{ data.order.code }}</code>
          </span>
          <el-tag :type="statusTag(data.order.status)" effect="dark" size="small">
            {{ statusLabel(data.order.status) }}
          </el-tag>
        </div>
      </template>

      <el-descriptions :column="3" border size="small">
        <el-descriptions-item :label="t('po.supplier')">
          <el-tag size="small" type="primary">{{ data.order.supplierName }}</el-tag>
          <span class="dim small" style="margin-left: 4px">{{ data.order.supplierCode }}</span>
        </el-descriptions-item>
        <el-descriptions-item :label="t('po.orderDate')">{{ data.order.orderDate }}</el-descriptions-item>
        <el-descriptions-item :label="t('po.expectedDate')">{{ data.order.expectedDate || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="t('po.currency')">
          <el-tag size="small">{{ data.order.currency }}</el-tag>
          <span v-if="data.order.fxRate && Number(data.order.fxRate) !== 1" class="dim small" style="margin-left: 6px">
            × {{ data.order.fxRate }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item :label="t('po.totalAmount')" :span="2">
          <strong style="font-size: 16px; color: #fa8c16">
            {{ fmtMoney(data.order.totalAmount) }} {{ data.order.currency }}
          </strong>
        </el-descriptions-item>
        <el-descriptions-item :label="t('paymentStatus.label')">
          <el-tag :type="paymentTag(data.order.paymentStatus)" effect="dark" size="small">
            {{ paymentLabel(data.order.paymentStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="t('vpay.paidAmount') + ' / ' + t('vpay.outstanding')">
          <strong style="color: #2BA84A">{{ fmtMoney(data.order.paidAmount) }}</strong>
          <span class="dim"> / </span>
          <strong style="color: #fa8c16">{{ fmtMoney(outstanding) }}</strong>
          <span class="dim"> {{ data.order.currency }}</span>
        </el-descriptions-item>
        <el-descriptions-item :label="t('paymentStatus.dueDate')">
          <span :class="{ overdue: isOverdue }">{{ data.order.dueDate || '-' }}</span>
          <el-tag v-if="isOverdue" type="danger" size="small" effect="dark" style="margin-left: 4px">
            {{ t('paymentStatus.overdue') }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="t('common.remark')" :span="3">
          {{ data.order.remark || '-' }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 明细 -->
    <el-card shadow="never" class="info-card">
      <template #header>
        <div class="card-head">
          <span class="card-title">
            <el-icon><List /></el-icon>
            {{ t('po.items') }} ({{ data?.items?.length || 0 }})
          </span>
        </div>
      </template>

      <el-table :data="data?.items || []" border stripe size="small" :empty-text="t('po.emptyItems')">
        <el-table-column type="index" label="#" width="50" align="center" />
        <el-table-column :label="t('po.inputType')" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="inputTypeTag(row.inputType)">{{ inputTypeLabel(row.inputType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" :label="t('po.itemDesc')" min-width="240" />
        <el-table-column :label="t('po.quantity')" width="120" align="right">
          <template #default="{ row }">
            <strong>{{ fmtNumber(row.quantity) }}</strong>
            <span class="dim small"> {{ row.unit }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('po.unitPrice')" width="120" align="right">
          <template #default="{ row }">{{ fmtMoney(row.unitPrice) }}</template>
        </el-table-column>
        <el-table-column :label="t('po.amount')" width="140" align="right">
          <template #default="{ row }">
            <strong>{{ fmtMoney(row.amount) }}</strong>
          </template>
        </el-table-column>
        <el-table-column :label="t('po.receivedQty')" width="120" align="right">
          <template #default="{ row }">
            <span :class="receivedClass(row)">{{ fmtNumber(row.receivedQty || 0) }}</span>
            <span class="dim small"> / {{ fmtNumber(row.quantity) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" :label="t('common.remark')" min-width="160">
          <template #default="{ row }">
            <span v-if="row.remark">{{ row.remark }}</span>
            <span v-else class="dim">-</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="grand-total">
        {{ t('po.grandTotal') }}:
        <strong>{{ fmtMoney(data?.order?.totalAmount) }}</strong>
        <span class="dim"> {{ data?.order?.currency }}</span>
      </div>
    </el-card>

    <!-- 付款记录 -->
    <el-card shadow="never" class="info-card">
      <template #header>
        <div class="card-head">
          <span class="card-title">
            <el-icon><Money /></el-icon>
            {{ t('vpay.history') }} ({{ payments.length }})
          </span>
          <el-tag v-if="totalPaid > 0" type="success" size="small">
            {{ t('vpay.paidAmount') }}: {{ fmtMoney(totalPaid) }} KES
          </el-tag>
        </div>
      </template>

      <el-table :data="payments" border stripe size="small" :empty-text="t('vpay.emptyHistory')">
        <el-table-column :label="t('vpay.paymentCode')" width="180">
          <template #default="{ row }">
            <code class="vpay-code">{{ row.code }}</code>
          </template>
        </el-table-column>
        <el-table-column prop="paymentDate" :label="t('payment.paymentDate')" width="110" align="center" />
        <el-table-column :label="t('vpay.amount')" width="130" align="right">
          <template #default="{ row }">
            <strong>{{ fmtMoney(row.amount) }}</strong>
            <span class="dim small"> {{ row.currency }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('vpay.amountKes')" width="130" align="right">
          <template #default="{ row }">
            <strong style="color: #2BA84A">{{ fmtMoney(row.amountKes) }}</strong>
          </template>
        </el-table-column>
        <el-table-column :label="t('payment.method')" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="methodTag(row.method)">{{ methodLabel(row.method) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('payment.referenceNo')" min-width="140">
          <template #default="{ row }">
            <code v-if="row.referenceNo" class="dim small">{{ row.referenceNo }}</code>
            <span v-else class="dim">-</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.status')" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="vpayStatusTag(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" :label="t('common.remark')" min-width="140">
          <template #default="{ row }">
            <span v-if="row.remark">{{ row.remark }}</span>
            <span v-else class="dim">-</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.actions')" width="100" fixed="right" align="center">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'cleared'"
              link type="danger" size="small" @click="onReverse(row)"
            >
              {{ t('vpay.reverse') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 付款弹窗 -->
    <VendorPaymentDialog
      v-model="payDialogVisible"
      :po-info="paymentTargetInfo"
      @saved="onPaymentSaved"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  Check as CheckIcon,
  Close as CloseIcon,
  Van as VanIcon,
  Tickets,
  List,
  Money,
} from '@element-plus/icons-vue'
import {
  getPurchaseOrder,
  confirmPurchaseOrder,
  receivePurchaseOrder,
  cancelPurchaseOrder,
} from '@/api/purchaseOrder'
import { listVendorPayments, reverseVendorPayment } from '@/api/vendorPayment'
import VendorPaymentDialog from '@/components/VendorPaymentDialog.vue'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const poId = computed(() => Number(route.params.id))
const loading = ref(false)
const data = ref(null)
const payments = ref([])

// ----- enums + helpers -----
const INPUT_TAG = {
  labor: 'primary', fertilizer: 'success', seed: 'success', pesticide: 'warning',
  water: 'info', electricity: 'info', equipment: '', service: 'info', other: ''
}
function inputTypeTag(v) { return INPUT_TAG[v] || '' }
function inputTypeLabel(v) { return t(`po.input${v ? v.charAt(0).toUpperCase() + v.slice(1) : ''}`, v) }

const STATUS_TAG = {
  draft: 'info', confirmed: 'primary',
  partial_received: 'warning', received: 'success', cancelled: 'info',
}
function statusTag(v) { return STATUS_TAG[v] || 'info' }
function statusLabel(v) { return t(`poStatus.${v}`, v) }

const PAYMENT_TAG = { unpaid: 'info', partial: 'warning', paid: 'success' }
function paymentTag(v) { return PAYMENT_TAG[v] || 'info' }
function paymentLabel(v) { return t(`paymentStatus.${v}`, v) }

const METHOD_TAG = {
  cash: 'success', bank: 'primary', cheque: 'info',
  loop_online: 'warning', loop_pos: 'warning'
}
function methodTag(v) { return METHOD_TAG[v] || '' }
function methodLabel(v) {
  const map = {
    cash: t('payment.methodCash'), bank: t('payment.methodBank'), cheque: t('payment.methodCheque'),
    loop_online: t('payment.methodLoopOnline'), loop_pos: t('payment.methodLoopPos'),
  }
  return map[v] || v
}

const VPAY_STATUS_TAG = { pending: 'info', cleared: 'success', reversed: 'danger' }
function vpayStatusTag(v) { return VPAY_STATUS_TAG[v] || 'info' }

function fmtMoney(v) {
  if (v == null) return '0.00'
  return Number(v).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function fmtNumber(v) {
  if (v == null) return '0'
  return Number(v).toLocaleString(undefined, { maximumFractionDigits: 3 })
}
function receivedClass(item) {
  if (!item) return ''
  const recv = Number(item.receivedQty || 0)
  const qty = Number(item.quantity || 0)
  if (qty <= 0) return 'dim'
  if (recv >= qty) return 'received-full'
  if (recv > 0) return 'received-partial'
  return 'dim'
}

const outstanding = computed(() => {
  if (!data.value?.order) return 0
  const t = Number(data.value.order.totalAmount) || 0
  const p = Number(data.value.order.paidAmount) || 0
  return Math.max(0, t - p)
})
const isOverdue = computed(() => {
  if (!data.value?.order || data.value.order.paymentStatus === 'paid') return false
  const due = data.value.order.dueDate
  return due && due < new Date().toISOString().slice(0, 10)
})
const totalPaid = computed(() =>
  payments.value
    .filter(p => p.status === 'cleared')
    .reduce((s, p) => s + (Number(p.amountKes) || 0), 0)
)

// ----- load -----
async function load() {
  loading.value = true
  try {
    data.value = await getPurchaseOrder(poId.value)
    await loadPayments()
  } catch (e) {
    ElMessage.error('Failed to load PO')
  } finally {
    loading.value = false
  }
}
async function loadPayments() {
  try {
    const res = await listVendorPayments({ poId: poId.value, page: 1, size: 100 })
    payments.value = res.list || []
  } catch {
    payments.value = []
  }
}
onMounted(load)
watch(poId, (v) => { if (v) load() })

function goBack() { router.push('/procurement/orders') }

// ----- 状态推进 -----
async function onConfirm() {
  await ElMessageBox.confirm(t('po.confirmConfirm', { code: data.value.order.code }), t('common.tip'), { type: 'warning' })
    .catch(() => Promise.reject('cancel'))
  try {
    await confirmPurchaseOrder(poId.value)
    ElMessage.success(t('po.confirmedMsg'))
    await load()
  } catch (e) { if (e === 'cancel') return }
}
async function onReceive() {
  await ElMessageBox.confirm(t('po.confirmReceive', { code: data.value.order.code }), t('common.tip'), { type: 'warning' })
    .catch(() => Promise.reject('cancel'))
  try {
    await receivePurchaseOrder(poId.value)
    ElMessage.success(t('po.receivedMsg'))
    await load()
  } catch (e) { if (e === 'cancel') return }
}
async function onCancel() {
  await ElMessageBox.confirm(t('po.confirmCancel', { code: data.value.order.code }), t('common.tip'), { type: 'warning' })
    .catch(() => Promise.reject('cancel'))
  try {
    await cancelPurchaseOrder(poId.value)
    ElMessage.success(t('po.cancelledMsg'))
    await load()
  } catch (e) { if (e === 'cancel') return }
}

// ----- 付款 -----
const payDialogVisible = ref(false)
const paymentTargetInfo = ref(null)
function openPaymentDialog() {
  const o = data.value.order
  paymentTargetInfo.value = {
    id: o.id, code: o.code, supplierName: o.supplierName,
    totalAmount: o.totalAmount, paidAmount: o.paidAmount, currency: o.currency,
  }
  payDialogVisible.value = true
}
async function onPaymentSaved() {
  await load()
}

async function onReverse(row) {
  await ElMessageBox.confirm(
    t('vpay.confirmReverse', { code: row.code }),
    t('common.tip'), { type: 'warning' }
  ).catch(() => Promise.reject('cancel'))
  try {
    await reverseVendorPayment(row.id)
    ElMessage.success(t('vpay.reversedMsg'))
    await load()
  } catch (e) { if (e === 'cancel') return }
}
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.page-head { display: flex; align-items: center; justify-content: space-between; }
.info-card :deep(.el-card__body)   { padding: 16px; }
.info-card :deep(.el-card__header) { padding: 12px 16px; }

.card-head { display: flex; align-items: center; justify-content: space-between; }
.card-title {
  display: inline-flex; align-items: center; gap: 6px;
  font-weight: 600; font-size: 14px; color: #1f2329;
}

.po-code {
  font-family: 'Consolas', monospace;
  background: #fff5e6;
  padding: 2px 8px;
  border-radius: 4px;
  color: #fa8c16;
  font-size: 12px;
  font-weight: 600;
}
.vpay-code {
  font-family: 'Consolas', monospace;
  background: #ecf9ef;
  padding: 2px 8px;
  border-radius: 4px;
  color: #1f7a35;
  font-size: 12px;
  font-weight: 600;
}

.received-full    { color: #1f7a35; font-weight: 600; }
.received-partial { color: #e6a23c; font-weight: 600; }
.overdue { color: #f56c6c; font-weight: 700; }

.dim { color: #909399; }
.small { font-size: 11px; }

.grand-total {
  margin-top: 12px;
  text-align: right;
  font-size: 14px;
  padding: 10px 14px;
  background: #fff5e6;
  border-radius: 4px;
  color: #fa8c16;
}
</style>
