<template>
  <div class="page">
    <!-- 过滤器 -->
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item :label="t('order.code')">
          <el-input
            v-model="query.code"
            placeholder="SO-yyyyMMdd"
            clearable
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item :label="t('order.customer')">
          <el-select
            v-model="query.customerId"
            :placeholder="t('common.all')"
            clearable
            filterable
            style="width: 220px"
          >
            <el-option
              v-for="c in customerOptions"
              :key="c.id"
              :label="`${c.name} (${c.code})`"
              :value="c.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('common.status')">
          <el-select
            v-model="query.status"
            :placeholder="t('common.all')"
            clearable
            style="width: 150px"
          >
            <el-option
              v-for="s in STATUS_OPTIONS"
              :key="s.value"
              :label="s.label"
              :value="s.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('order.orderDate')">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="→"
            :start-placeholder="t('date.pickStart')"
            :end-placeholder="t('date.pickEnd')"
            value-format="YYYY-MM-DD"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="SearchIcon" @click="reload(1)">{{ t('common.search') }}</el-button>
          <el-button :icon="RefreshIcon" @click="onReset">{{ t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <div class="toolbar">
        <el-button type="primary" :icon="PlusIcon" @click="onCreate">{{ t('order.new') }}</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border stripe row-key="id">
        <el-table-column :label="t('order.code')" min-width="160">
          <template #default="{ row }">
            <router-link :to="`/sales/orders/${row.id}`" class="link-code">
              <code class="order-code">{{ row.code }}</code>
            </router-link>
          </template>
        </el-table-column>
        <el-table-column prop="orderDate" :label="t('order.orderDate')" width="110" align="center" />
        <el-table-column :label="t('order.customer')" min-width="200">
          <template #default="{ row }">
            <div>{{ row.customerName }}</div>
            <code class="dim small">{{ row.customerCode }}</code>
          </template>
        </el-table-column>
        <el-table-column prop="deliveryDate" :label="t('order.deliveryDate')" width="110" align="center" />
        <el-table-column :label="t('order.itemCount')" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.itemCount }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('order.totalAmount')" min-width="140" align="right">
          <template #default="{ row }">
            <strong>{{ formatMoney(row.totalAmount) }}</strong>
            <span class="dim small"> {{ row.currency }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.status')" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" effect="dark" size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('paymentStatus.label')" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="paymentTag(row.paymentStatus)" size="small">
              {{ paymentLabel(row.paymentStatus) }}
            </el-tag>
            <div v-if="row.dueDate" class="dim small" style="margin-top: 2px">
              <span :class="{ overdue: isOverdue(row) }">
                {{ t('paymentStatus.dueDate') }}: {{ row.dueDate }}
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.actions')" width="230" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="goDetail(row)">
              {{ t('common.view') }}
            </el-button>
            <el-button
              v-if="isEditable(row.status)"
              link type="primary" size="small" @click="onEdit(row)">
              {{ t('common.edit') }}
            </el-button>
            <el-button
              v-if="row.status === 'pending'"
              link type="success" size="small" @click="onConfirm(row)">
              {{ t('order.confirm') }}
            </el-button>
            <el-button
              v-if="canCancel(row.status)"
              link type="warning" size="small" @click="onCancel(row)">
              {{ t('order.cancel') }}
            </el-button>
            <el-button
              v-if="canDelete(row.status)"
              link type="danger" size="small" @click="onDelete(row)">
              {{ t('common.delete') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pager"
        background
        layout="total, sizes, prev, pager, next, jumper"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        :page-size="pageSize"
        :current-page="page"
        @size-change="onSizeChange"
        @current-change="onPageChange"
      />
    </el-card>

    <!-- 新建/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? t('order.editTitle') : t('order.createTitle')"
      width="900"
      :close-on-click-modal="false"
      destroy-on-close
      @closed="onDialogClosed"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <div class="form-grid">
          <el-form-item :label="t('order.customer')" prop="customerId">
            <el-select v-model="form.customerId" filterable :placeholder="t('order.pickCustomer')" style="width: 100%">
              <el-option
                v-for="c in customerOptions"
                :key="c.id"
                :label="`${c.name} (${c.code})`"
                :value="c.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('order.currency')" prop="currency">
            <el-select v-model="form.currency" style="width: 100%">
              <el-option label="KES" value="KES" />
              <el-option label="USD" value="USD" />
              <el-option label="EUR" value="EUR" />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('order.orderDate')" prop="orderDate">
            <el-date-picker v-model="form.orderDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
          <el-form-item :label="t('order.deliveryDate')" prop="deliveryDate">
            <el-date-picker v-model="form.deliveryDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
        </div>

        <!-- 账期 + 应付日 预览 (根据所选客户的 creditDays 自动计算) -->
        <el-form-item v-if="form.customerId" :label="t('paymentStatus.dueDate')">
          <div class="terms-preview">
            <el-tag type="info" size="small">
              {{ t('customer.creditDays') }}: {{ selectedCustomerCreditDays }} {{ t('customer.daysSuffix') }}
              <span v-if="selectedCustomerTermsLabel"> · {{ selectedCustomerTermsLabel }}</span>
            </el-tag>
            <span class="arrow"> → </span>
            <strong class="due-date">{{ dueDatePreview || '-' }}</strong>
            <span class="dim small" style="margin-left: 8px">
              ({{ t('order.orderDate') }} + {{ selectedCustomerCreditDays }} {{ t('customer.daysSuffix') }})
            </span>
          </div>
        </el-form-item>

        <el-form-item :label="t('order.shipTo')" prop="shipTo">
          <el-input v-model="form.shipTo" :placeholder="t('order.shipToPlaceholder')" maxlength="255" />
        </el-form-item>

        <el-form-item :label="t('common.remark')">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>

        <!-- Items -->
        <div class="items-block">
          <div class="items-head">
            <span class="items-title">{{ t('order.items') }}</span>
            <el-button :icon="PlusIcon" link type="primary" @click="addItem">
              {{ t('order.addLine') }}
            </el-button>
          </div>

          <el-table :data="form.items" border size="small" :empty-text="t('order.emptyItems')">
            <el-table-column label="#" type="index" width="40" align="center" />
            <el-table-column :label="t('order.sku')" min-width="280">
              <template #default="{ row }">
                <el-select
                  v-model="row.skuId"
                  filterable
                  :placeholder="t('order.pickSku')"
                  style="width: 100%"
                >
                  <el-option
                    v-for="s in skuOptions"
                    :key="s.id"
                    :label="`${s.code} · ${s.name}`"
                    :value="s.id"
                  />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column :label="t('order.qty')" width="130">
              <template #default="{ row }">
                <el-input-number
                  v-model="row.qty"
                  :min="0.001" :precision="3" :step="1" :controls="false"
                  style="width: 100%"
                />
              </template>
            </el-table-column>
            <el-table-column :label="t('order.unitPrice') + ` (${form.currency})`" width="140">
              <template #default="{ row }">
                <el-input-number
                  v-model="row.unitPrice"
                  :min="0" :precision="2" :step="1" :controls="false"
                  style="width: 100%"
                />
              </template>
            </el-table-column>
            <el-table-column :label="t('order.amount')" width="120" align="right">
              <template #default="{ row }">
                <strong>{{ formatMoney(lineAmount(row)) }}</strong>
              </template>
            </el-table-column>
            <el-table-column :label="t('common.remark')" min-width="160">
              <template #default="{ row }">
                <el-input v-model="row.remark" maxlength="255" size="small" />
              </template>
            </el-table-column>
            <el-table-column width="50" align="center">
              <template #default="{ $index }">
                <el-button
                  link type="danger" :icon="DeleteIcon" size="small"
                  :disabled="form.items.length <= 1"
                  @click="removeItem($index)"
                />
              </template>
            </el-table-column>
          </el-table>

          <div class="grand-total">
            {{ t('order.grandTotal') }}:
            <strong>{{ formatMoney(grandTotal) }}</strong>
            <span class="dim"> {{ form.currency }}</span>
          </div>
        </div>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="onSubmit">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search as SearchIcon,
  Refresh as RefreshIcon,
  Plus as PlusIcon,
  Delete as DeleteIcon,
} from '@element-plus/icons-vue'
import {
  listOrders, createOrder, updateOrder,
  confirmOrder, cancelOrder, deleteOrder, getOrderDetail,
} from '@/api/salesOrder'
import { listCustomers } from '@/api/customer'
import { listSkus } from '@/api/sku'

const { t } = useI18n()
const router = useRouter()

const STATUS_OPTIONS = computed(() => [
  { value: 'pending',   label: t('orderStatus.pending'),   tag: 'info'    },
  { value: 'confirmed', label: t('orderStatus.confirmed'), tag: 'primary' },
  { value: 'locked',    label: t('orderStatus.locked'),    tag: 'warning' },
  { value: 'shipping',  label: t('orderStatus.shipping'),  tag: 'warning' },
  { value: 'shipped',   label: t('orderStatus.shipped'),   tag: 'success' },
  { value: 'delivered', label: t('orderStatus.delivered'), tag: 'success' },
  { value: 'completed', label: t('orderStatus.completed'), tag: 'success' },
  { value: 'cancelled', label: t('orderStatus.cancelled'), tag: 'info'    },
  { value: 'returned',  label: t('orderStatus.returned'),  tag: 'danger'  },
])
const STATUS_MAP = computed(() => Object.fromEntries(STATUS_OPTIONS.value.map(s => [s.value, s])))
function statusLabel(v) { return STATUS_MAP.value[v]?.label || v }
function statusTag(v)   { return STATUS_MAP.value[v]?.tag || 'info' }

function isEditable(s) { return s === 'pending' || s === 'confirmed' }
function canCancel(s)  { return s === 'pending' || s === 'confirmed' }
function canDelete(s)  { return s === 'pending' || s === 'cancelled' }

const PAYMENT_TAG = { unpaid: 'info', partial: 'warning', paid: 'success' }
function paymentLabel(v) { return t(`paymentStatus.${v || 'unpaid'}`, v || 'unpaid') }
function paymentTag(v)   { return PAYMENT_TAG[v] || 'info' }
function isOverdue(row) {
  if (!row.dueDate || row.paymentStatus === 'paid') return false
  return row.dueDate < new Date().toISOString().slice(0, 10)
}

function formatMoney(v) {
  if (v == null) return '0.00'
  return Number(v).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ----- list + filters -----
const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const query = reactive({ code: '', customerId: null, status: '', dateFrom: null, dateTo: null })
const dateRange = ref(null)
watch(dateRange, (v) => {
  if (Array.isArray(v) && v.length === 2) { query.dateFrom = v[0]; query.dateTo = v[1] }
  else { query.dateFrom = null; query.dateTo = null }
})

async function reload(toPage) {
  if (toPage) page.value = toPage
  loading.value = true
  try {
    const data = await listOrders({ ...query, page: page.value, size: pageSize.value })
    list.value = data.list
    total.value = data.total
  } finally { loading.value = false }
}
function onReset() {
  query.code = ''; query.customerId = null; query.status = ''
  dateRange.value = null
  reload(1)
}
function onPageChange(p) { page.value = p; reload() }
function onSizeChange(s) { pageSize.value = s; reload(1) }
function goDetail(row) { router.push(`/sales/orders/${row.id}`) }

// ----- customers / skus lookup -----
const customerOptions = ref([])
const skuOptions = ref([])
async function loadCustomers() {
  const data = await listCustomers({ status: 'active', page: 1, size: 500 })
  customerOptions.value = data.list
}
async function loadSkus() {
  const data = await listSkus({ status: 1, page: 1, size: 500 })
  skuOptions.value = data.list
}

onMounted(async () => {
  await Promise.all([loadCustomers(), loadSkus()])
  reload(1)
})

// ----- create / edit dialog -----
const dialogVisible = ref(false)
const editingId = ref(null)
const saving = ref(false)
const formRef = ref(null)

const todayIso = () => new Date().toISOString().slice(0, 10)
const emptyForm = () => ({
  customerId: null,
  orderDate: todayIso(),
  deliveryDate: todayIso(),
  shipTo: '',
  currency: 'KES',
  remark: '',
  items: [{ skuId: null, qty: null, unitPrice: null, remark: '' }],
})
const form = reactive(emptyForm())

const rules = computed(() => ({
  customerId:    [{ required: true, message: t('valid.required', { field: t('order.customer') }), trigger: 'change' }],
  orderDate:     [{ required: true, message: t('valid.required', { field: t('order.orderDate') }), trigger: 'change' }],
  deliveryDate:  [{ required: true, message: t('valid.required', { field: t('order.deliveryDate') }), trigger: 'change' }],
  shipTo:        [{ required: true, message: t('valid.required', { field: t('order.shipTo') }), trigger: 'blur' }],
  currency:      [{ required: true, message: t('valid.required', { field: t('order.currency') }), trigger: 'change' }],
}))

function onCreate() {
  editingId.value = null
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}
async function onEdit(row) {
  // 详情接口拿完整 items
  const detail = await getOrderDetail(row.id)
  editingId.value = row.id
  Object.assign(form, {
    customerId: detail.order.customerId,
    orderDate: detail.order.orderDate,
    deliveryDate: detail.order.deliveryDate,
    shipTo: detail.order.shipTo,
    currency: detail.order.currency,
    remark: detail.order.remark || '',
    items: detail.items.length
      ? detail.items.map(it => ({
          skuId: it.skuId, qty: Number(it.qty),
          unitPrice: Number(it.unitPrice), remark: it.remark || '',
        }))
      : [{ skuId: null, qty: null, unitPrice: null, remark: '' }],
  })
  dialogVisible.value = true
}
function onDialogClosed() {
  editingId.value = null
  Object.assign(form, emptyForm())
  formRef.value?.clearValidate()
}
function addItem()    { form.items.push({ skuId: null, qty: null, unitPrice: null, remark: '' }) }
function removeItem(i) { if (form.items.length > 1) form.items.splice(i, 1) }
function lineAmount(row) {
  const q = Number(row.qty) || 0
  const p = Number(row.unitPrice) || 0
  return q * p
}
const grandTotal = computed(() => form.items.reduce((s, r) => s + lineAmount(r), 0))

// ----- 应付日预览 (根据所选客户的 creditDays 计算) -----
const selectedCustomer = computed(() => {
  if (!form.customerId) return null
  return customerOptions.value.find(c => c.id === form.customerId) || null
})
const selectedCustomerCreditDays = computed(() => {
  return Number(selectedCustomer.value?.creditDays ?? 0)
})
const selectedCustomerTermsLabel = computed(() => {
  return selectedCustomer.value?.paymentTerms || ''
})
const dueDatePreview = computed(() => {
  if (!form.orderDate) return ''
  const d = new Date(form.orderDate)
  if (Number.isNaN(d.getTime())) return ''
  d.setDate(d.getDate() + selectedCustomerCreditDays.value)
  return d.toISOString().slice(0, 10)
})

async function onSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  const validItems = form.items.filter(it => it.skuId && Number(it.qty) > 0)
  if (validItems.length === 0) {
    ElMessage.warning(t('order.atLeastOneItem'))
    return
  }
  saving.value = true
  try {
    const payload = { ...form, items: validItems }
    if (editingId.value) {
      await updateOrder(editingId.value, payload)
      ElMessage.success(t('common.updateSuccess'))
    } else {
      await createOrder(payload)
      ElMessage.success(t('common.createSuccess'))
    }
    dialogVisible.value = false
    reload()
  } catch {} finally { saving.value = false }
}

// ----- state actions -----
async function onConfirm(row) {
  await ElMessageBox.confirm(
    t('order.confirmConfirm', { code: row.code }),
    t('common.tip'),
    { type: 'warning' }
  ).catch(() => Promise.reject('cancel'))
  try {
    await confirmOrder(row.id)
    ElMessage.success(t('order.statusChanged', { label: t('orderStatus.confirmed') }))
    reload()
  } catch (e) { if (e === 'cancel') return }
}
async function onCancel(row) {
  await ElMessageBox.confirm(
    t('order.confirmCancel', { code: row.code }),
    t('common.tip'),
    { type: 'warning' }
  ).catch(() => Promise.reject('cancel'))
  try {
    await cancelOrder(row.id)
    ElMessage.success(t('order.statusChanged', { label: t('orderStatus.cancelled') }))
    reload()
  } catch (e) { if (e === 'cancel') return }
}
async function onDelete(row) {
  await ElMessageBox.confirm(
    t('order.confirmDelete', { code: row.code }),
    t('common.tip'),
    { type: 'warning' }
  ).catch(() => Promise.reject('cancel'))
  try {
    await deleteOrder(row.id)
    ElMessage.success(t('common.deleteSuccess'))
    reload()
  } catch (e) { if (e === 'cancel') return }
}
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.filter-card :deep(.el-card__body),
.table-card :deep(.el-card__body) { padding: 16px; }
.toolbar { margin-bottom: 12px; display: flex; justify-content: flex-end; }
.pager { margin-top: 14px; justify-content: flex-end; }

.order-code {
  font-family: 'Consolas', monospace;
  background: #f5f7fa;
  padding: 2px 8px;
  border-radius: 4px;
  color: #1f7a35;
  font-size: 12px;
  font-weight: 600;
}
.link-code { text-decoration: none; }
.link-code .order-code:hover { background: #ecf9ef; }

.dim { color: #909399; }
.small { font-size: 11px; }
.overdue { color: #f56c6c; font-weight: 600; }

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
}

.terms-preview {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 4px 10px; background: #f7faff;
  border: 1px dashed #b3d8ff; border-radius: 4px;
  font-size: 13px;
}
.terms-preview .arrow { color: #909399; margin: 0 4px; }
.terms-preview .due-date {
  color: #1f7a35; font-weight: 700; font-family: 'Consolas', monospace;
}

.items-block { margin-top: 8px; }
.items-head {
  display: flex; align-items: center; justify-content: space-between;
  background: #f5f7fa; padding: 8px 12px; border-radius: 4px 4px 0 0;
}
.items-title { font-weight: 600; font-size: 13px; color: #1f2329; }

.grand-total {
  margin-top: 10px;
  text-align: right;
  font-size: 14px;
  padding: 8px 12px;
  background: #ecf9ef;
  border-radius: 4px;
  color: #1f7a35;
}
</style>
