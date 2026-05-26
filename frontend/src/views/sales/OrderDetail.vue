<template>
  <div class="page" v-loading="loading">
    <div class="page-head">
      <el-button :icon="ArrowLeft" text @click="goBack">{{ t('order.backToList') }}</el-button>
      <div class="head-actions" v-if="data?.order">
        <el-button
          v-if="data.order.status === 'pending'"
          type="success" :icon="CheckIcon" @click="onConfirm">
          {{ t('order.confirm') }}
        </el-button>
        <el-button
          v-if="data.order.status === 'confirmed'"
          type="primary" :icon="PickIcon" @click="onPick">
          {{ t('fulfillment.pick') }}
        </el-button>
        <el-button
          v-if="data.order.paymentStatus !== 'paid' && !['cancelled','returned'].includes(data.order.status)"
          type="success" plain @click="openPaymentDialog">
          💰 {{ t('paymentStatus.recordPayment') }}
        </el-button>
        <el-button
          v-if="canCancel(data.order.status)"
          type="warning" :icon="CloseIcon" @click="onCancel">
          {{ t('order.cancel') }}
        </el-button>
      </div>
    </div>

    <el-card shadow="never" class="info-card" v-if="data?.order">
      <template #header>
        <div class="card-head">
          <span class="card-title">
            <el-icon><Tickets /></el-icon>
            <code class="order-code">{{ data.order.code }}</code>
          </span>
          <el-tag :type="statusTag(data.order.status)" effect="dark" size="small">
            {{ statusLabel(data.order.status) }}
          </el-tag>
        </div>
      </template>

      <el-descriptions :column="3" border size="small">
        <el-descriptions-item :label="t('order.customer')">
          <el-tag size="small" type="primary">{{ data.order.customerName }}</el-tag>
          <span class="dim small" style="margin-left: 4px">{{ data.order.customerCode }}</span>
        </el-descriptions-item>
        <el-descriptions-item :label="t('order.orderDate')">{{ data.order.orderDate }}</el-descriptions-item>
        <el-descriptions-item :label="t('order.deliveryDate')">{{ data.order.deliveryDate }}</el-descriptions-item>
        <el-descriptions-item :label="t('order.shipTo')" :span="2">{{ data.order.shipTo }}</el-descriptions-item>
        <el-descriptions-item :label="t('order.currency')">
          <el-tag size="small">{{ data.order.currency }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="t('order.totalAmount')" :span="3">
          <strong style="font-size: 16px; color: #1f7a35">
            {{ formatMoney(data.order.totalAmount) }} {{ data.order.currency }}
          </strong>
        </el-descriptions-item>
        <el-descriptions-item :label="t('paymentStatus.label')">
          <el-tag :type="paymentTag(data.order.paymentStatus)" effect="dark" size="small">
            {{ paymentLabel(data.order.paymentStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="t('paymentStatus.paidAmount') + ' / ' + t('paymentStatus.outstanding')">
          <strong style="color: #2BA84A">{{ formatMoney(data.order.paidAmount) }}</strong>
          <span class="dim"> / </span>
          <strong style="color: #e6a23c">{{ formatMoney(outstanding) }}</strong>
          <span class="dim"> {{ data.order.currency }}</span>
        </el-descriptions-item>
        <el-descriptions-item :label="t('paymentStatus.dueDate')">
          <span :class="{ overdue: isOverdue }">
            {{ data.order.dueDate || '-' }}
          </span>
          <el-tag v-if="isOverdue" type="danger" size="small" effect="dark" style="margin-left: 4px">
            {{ t('paymentStatus.overdue') }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="t('common.remark')" :span="3">
          {{ data.order.remark || '-' }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never" class="info-card">
      <template #header>
        <div class="card-head">
          <span class="card-title">
            <el-icon><List /></el-icon>
            {{ t('order.items') }} ({{ data?.items?.length || 0 }})
          </span>
        </div>
      </template>

      <el-table :data="data?.items || []" border stripe size="small" :empty-text="t('order.emptyItems')">
        <el-table-column label="#" type="index" width="50" align="center" />
        <el-table-column :label="t('order.sku')" min-width="240">
          <template #default="{ row }">
            <div>
              <el-tag size="small" type="primary">{{ row.grade }}</el-tag>
              <span style="margin-left: 6px">{{ row.skuName }}</span>
            </div>
            <code class="dim small">{{ row.skuCode }}</code>
          </template>
        </el-table-column>
        <el-table-column :label="t('order.qty')" width="100" align="right">
          <template #default="{ row }">
            <strong>{{ formatQty(row.qty) }}</strong>
          </template>
        </el-table-column>
        <el-table-column :label="t('order.unitPrice')" width="140" align="right">
          <template #default="{ row }">{{ formatMoney(row.unitPrice) }}</template>
        </el-table-column>
        <el-table-column :label="t('order.amount')" width="140" align="right">
          <template #default="{ row }">
            <strong>{{ formatMoney(row.amount) }}</strong>
          </template>
        </el-table-column>
        <el-table-column :label="t('order.qtyShipped')" width="100" align="right">
          <template #default="{ row }">
            <span :class="{ dim: Number(row.qtyShipped) === 0 }">{{ formatQty(row.qtyShipped) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.remark')" min-width="180">
          <template #default="{ row }">
            <span :class="{ dim: !row.remark }">{{ row.remark || '-' }}</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="grand-total">
        {{ t('order.grandTotal') }}:
        <strong>{{ formatMoney(data?.order?.totalAmount) }}</strong>
        <span class="dim"> {{ data?.order?.currency }}</span>
      </div>
    </el-card>

    <!-- Fulfillments section (Sprint 9.3+) -->
    <el-card shadow="never" class="info-card">
      <template #header>
        <div class="card-head">
          <span class="card-title">
            <el-icon><Van /></el-icon>
            {{ t('fulfillment.fulfillmentsTitle') }} ({{ fulfillments.length }})
          </span>
        </div>
      </template>

      <el-table
        :data="fulfillments"
        border stripe size="small"
        :empty-text="t('fulfillment.pickEmpty')"
      >
        <el-table-column :label="t('fulfillment.code')" min-width="160">
          <template #default="{ row }">
            <code class="ship-code">{{ row.code }}</code>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.status')" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="fulfillmentStatusTag(row.status)" effect="dark" size="small">
              {{ fulfillmentStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('fulfillment.itemCount')" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.itemCount }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('fulfillment.picker')" prop="pickerName" width="120">
          <template #default="{ row }">
            <span :class="{ dim: !row.pickerName }">{{ row.pickerName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" :label="t('common.createdAt')" width="160" />
        <el-table-column prop="shipAt" :label="t('fulfillment.shipAt')" width="160">
          <template #default="{ row }">
            <span :class="{ dim: !row.shipAt }">{{ row.shipAt || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.actions')" width="280" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'ready'"
              link type="primary" size="small" @click="openShipDialog(row)">
              {{ t('fulfillment.ship') }}
            </el-button>
            <el-button
              v-if="row.status === 'shipped'"
              link type="success" size="small" @click="onMarkDelivered(row)">
              {{ t('fulfillment.deliver') }}
            </el-button>
            <el-button
              v-if="['picking', 'ready'].includes(row.status)"
              link type="danger" size="small" @click="onCancelFulfillment(row)">
              {{ t('fulfillment.cancelPicking') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Ship dialog -->
    <el-dialog
      v-model="shipDialogVisible"
      :title="shipDialogTitle"
      width="520"
      :close-on-click-modal="false"
      destroy-on-close
      @closed="resetShipForm"
    >
      <el-form ref="shipFormRef" :model="shipForm" label-width="110px">
        <el-form-item :label="t('fulfillment.shipMethod')">
          <el-select v-model="shipForm.shipMethod" clearable style="width: 100%">
            <el-option :label="t('fulfillment.shipMethodSelf')"       value="self" />
            <el-option :label="t('fulfillment.shipMethodLogistics')"  value="logistics" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('fulfillment.trackNo')">
          <el-input v-model="shipForm.trackNo" :placeholder="t('fulfillment.shipPlaceholderTrack')" maxlength="64" />
        </el-form-item>
        <el-form-item :label="t('fulfillment.driverName')">
          <el-input v-model="shipForm.driverName" :placeholder="t('fulfillment.shipPlaceholderDriver')" maxlength="64" />
        </el-form-item>
        <el-form-item :label="t('fulfillment.driverPhone')">
          <el-input v-model="shipForm.driverPhone" placeholder="+254 7XX XXX XXX" maxlength="20" />
        </el-form-item>
        <el-form-item :label="t('fulfillment.vehicleNo')">
          <el-input v-model="shipForm.vehicleNo" :placeholder="t('fulfillment.shipPlaceholderVehicle')" maxlength="32" />
        </el-form-item>
        <el-form-item :label="t('common.remark')">
          <el-input v-model="shipForm.remark" type="textarea" :rows="2" maxlength="255" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shipDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="shipping" @click="onSubmitShip">
          {{ t('fulfillment.shipSubmit') }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 录回款 dialog -->
    <PaymentDialog
      v-model="paymentDialogVisible"
      :order-info="paymentTargetInfo"
      @saved="onPaymentSaved"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  Check as CheckIcon,
  Close as CloseIcon,
  Pointer as PickIcon,
  Tickets,
  List,
  Van,
} from '@element-plus/icons-vue'
import { getOrderDetail, confirmOrder, cancelOrder } from '@/api/salesOrder'
import {
  listFulfillmentsByOrder, pickOrder, cancelFulfillment,
  shipFulfillment, deliverFulfillment,
} from '@/api/fulfillment'
import PaymentDialog from '@/components/PaymentDialog.vue'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const orderId = computed(() => Number(route.params.id))
const loading = ref(false)
const data = ref(null)

const STATUS_MAP = computed(() => ({
  pending:   { label: t('orderStatus.pending'),   tag: 'info'    },
  confirmed: { label: t('orderStatus.confirmed'), tag: 'primary' },
  locked:    { label: t('orderStatus.locked'),    tag: 'warning' },
  shipping:  { label: t('orderStatus.shipping'),  tag: 'warning' },
  shipped:   { label: t('orderStatus.shipped'),   tag: 'success' },
  delivered: { label: t('orderStatus.delivered'), tag: 'success' },
  completed: { label: t('orderStatus.completed'), tag: 'success' },
  cancelled: { label: t('orderStatus.cancelled'), tag: 'info'    },
  returned:  { label: t('orderStatus.returned'),  tag: 'danger'  },
}))
function statusLabel(v) { return STATUS_MAP.value[v]?.label || v }
function statusTag(v)   { return STATUS_MAP.value[v]?.tag || 'info' }

function canCancel(s) { return s === 'pending' || s === 'confirmed' }

function formatMoney(v) {
  if (v == null) return '0.00'
  return Number(v).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function formatQty(v) {
  if (v == null) return '0'
  return Number(v).toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 3 })
}

// ---- fulfillment state ----
const fulfillments = ref([])
const FULFILLMENT_TAG = {
  pending:   'info',
  picking:   'warning',
  ready:     'primary',
  shipped:   'success',
  delivered: 'success',
  cancelled: 'info',
}
function fulfillmentStatusLabel(v) { return t(`fulfillmentStatus.${v}`, v) }
function fulfillmentStatusTag(v) { return FULFILLMENT_TAG[v] || 'info' }

async function loadFulfillments() {
  try {
    fulfillments.value = await listFulfillmentsByOrder(orderId.value)
  } catch (e) {
    fulfillments.value = []
  }
}

async function load() {
  loading.value = true
  try {
    data.value = await getOrderDetail(orderId.value)
    await loadFulfillments()
  } catch (e) { ElMessage.error('Failed to load order') }
  finally { loading.value = false }
}
onMounted(load)
watch(orderId, (v) => { if (v) load() })

function goBack() { router.push('/sales/orders') }

async function onConfirm() {
  await ElMessageBox.confirm(
    t('order.confirmConfirm', { code: data.value.order.code }),
    t('common.tip'),
    { type: 'warning' }
  ).catch(() => null)
  try {
    await confirmOrder(orderId.value)
    ElMessage.success(t('order.statusChanged', { label: t('orderStatus.confirmed') }))
    await load()
  } catch {}
}
async function onCancel() {
  await ElMessageBox.confirm(
    t('order.confirmCancel', { code: data.value.order.code }),
    t('common.tip'),
    { type: 'warning' }
  ).catch(() => null)
  try {
    await cancelOrder(orderId.value)
    ElMessage.success(t('order.statusChanged', { label: t('orderStatus.cancelled') }))
    await load()
  } catch {}
}

// ---- Picking ----
async function onPick() {
  await ElMessageBox.confirm(
    t('fulfillment.pickConfirm', { code: data.value.order.code }),
    t('common.tip'),
    { type: 'warning' }
  ).catch(() => Promise.reject('cancel'))
  try {
    await pickOrder(orderId.value)
    ElMessage.success(t('fulfillment.pickSuccess'))
    await load()
  } catch (e) { if (e === 'cancel') return }
}

async function onCancelFulfillment(row) {
  await ElMessageBox.confirm(
    t('fulfillment.cancelConfirm', { code: row.code }),
    t('common.tip'),
    { type: 'warning' }
  ).catch(() => Promise.reject('cancel'))
  try {
    await cancelFulfillment(row.id)
    ElMessage.success(t('fulfillment.cancelSuccess'))
    await load()
  } catch (e) { if (e === 'cancel') return }
}

// ---- Ship dialog ----
const shipDialogVisible = ref(false)
const shipping = ref(false)
const shippingTarget = ref(null)
const shipFormRef = ref(null)
const shipForm = reactive({
  shipMethod: 'self', trackNo: '', driverName: '', driverPhone: '', vehicleNo: '', remark: '',
})
const shipDialogTitle = computed(() =>
  t('fulfillment.shipTitle', { code: shippingTarget.value?.code || '' })
)
function openShipDialog(row) {
  shippingTarget.value = row
  shipForm.shipMethod  = 'self'
  shipForm.trackNo     = ''
  shipForm.driverName  = ''
  shipForm.driverPhone = ''
  shipForm.vehicleNo   = ''
  shipForm.remark      = ''
  shipDialogVisible.value = true
}
function resetShipForm() {
  shippingTarget.value = null
  shipFormRef.value?.clearValidate()
}
async function onSubmitShip() {
  if (!shippingTarget.value) return
  shipping.value = true
  try {
    await shipFulfillment(shippingTarget.value.id, { ...shipForm })
    ElMessage.success(t('fulfillment.shipSuccess'))
    shipDialogVisible.value = false
    await load()
  } catch {} finally { shipping.value = false }
}

// ---- Mark delivered ----
// ---- Payment ----
const PAYMENT_TAG = { unpaid: 'info', partial: 'warning', paid: 'success' }
function paymentLabel(v) { return t(`paymentStatus.${v}`, v) }
function paymentTag(v)   { return PAYMENT_TAG[v] || 'info' }

const outstanding = computed(() => {
  if (!data.value?.order) return 0
  const total = Number(data.value.order.totalAmount) || 0
  const paid = Number(data.value.order.paidAmount) || 0
  return Math.max(0, total - paid)
})
const isOverdue = computed(() => {
  if (!data.value?.order || data.value.order.paymentStatus === 'paid') return false
  const due = data.value.order.dueDate
  return due && due < new Date().toISOString().slice(0, 10)
})

const paymentDialogVisible = ref(false)
const paymentTargetInfo = ref(null)
function openPaymentDialog() {
  const o = data.value.order
  paymentTargetInfo.value = {
    id: o.id,
    code: o.code,
    customerName: o.customerName,
    totalAmount: o.totalAmount,
    paidAmount: o.paidAmount,
    currency: o.currency,
  }
  paymentDialogVisible.value = true
}
async function onPaymentSaved() {
  await load()
}

async function onMarkDelivered(row) {
  await ElMessageBox.confirm(
    t('fulfillment.deliverConfirm', { code: row.code }),
    t('common.tip'),
    { type: 'warning' }
  ).catch(() => Promise.reject('cancel'))
  try {
    await deliverFulfillment(row.id)
    ElMessage.success(t('fulfillment.deliverSuccess'))
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

.order-code {
  font-family: 'Consolas', monospace;
  background: #f5f7fa;
  padding: 2px 8px;
  border-radius: 4px;
  color: #1f7a35;
  font-size: 12px;
  font-weight: 600;
}

.ship-code {
  font-family: 'Consolas', monospace;
  background: #f5f7fa;
  padding: 2px 8px;
  border-radius: 4px;
  color: #1677ff;
  font-size: 12px;
  font-weight: 600;
}

.overdue { color: #f56c6c; font-weight: 700; }

.dim { color: #909399; }
.small { font-size: 11px; }

.grand-total {
  margin-top: 12px;
  text-align: right;
  font-size: 14px;
  padding: 10px 14px;
  background: #ecf9ef;
  border-radius: 4px;
  color: #1f7a35;
}
</style>
