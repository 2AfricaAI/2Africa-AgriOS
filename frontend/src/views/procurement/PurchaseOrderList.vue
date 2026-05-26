<template>
  <div class="page">
    <!-- 过滤区 -->
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="query" @keyup.enter="reload(1)">
        <el-form-item :label="t('po.code')">
          <el-input v-model="query.code" clearable :placeholder="t('crop.fuzzy')" style="width: 180px" @clear="reload(1)" />
        </el-form-item>
        <el-form-item :label="t('po.supplier')">
          <el-select v-model="query.supplierId" clearable filterable :placeholder="t('common.all')" style="width: 200px" @change="reload(1)">
            <el-option v-for="s in supplierOptions" :key="s.id" :label="`${s.name} (${s.code})`" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('common.status')">
          <el-select v-model="query.status" clearable :placeholder="t('common.all')" style="width: 160px" @change="reload(1)">
            <el-option :label="t('poStatus.draft')"            value="draft" />
            <el-option :label="t('poStatus.confirmed')"        value="confirmed" />
            <el-option :label="t('poStatus.partial_received')" value="partial_received" />
            <el-option :label="t('poStatus.received')"         value="received" />
            <el-option :label="t('poStatus.cancelled')"        value="cancelled" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('po.orderDate')">
          <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD"
                          :start-placeholder="t('common.from')" :end-placeholder="t('common.to')"
                          style="width: 260px" @change="onDateChange" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="SearchIcon" @click="reload(1)">{{ t('common.search') }}</el-button>
          <el-button :icon="RefreshIcon" @click="resetQuery">{{ t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 工具栏 + 表格 -->
    <el-card shadow="never" class="table-card">
      <div class="toolbar">
        <el-button type="primary" :icon="PlusIcon" @click="onCreate">{{ t('po.new') }}</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border stripe row-key="id" @expand-change="onExpand">
        <el-table-column type="expand" width="36">
          <template #default="{ row }">
            <div class="items-wrap" v-loading="detailLoading[row.id]">
              <el-table v-if="detailItems[row.id]" :data="detailItems[row.id]" size="small" border stripe>
                <el-table-column type="index" label="#" width="50" align="center" />
                <el-table-column :label="t('po.inputType')" width="120">
                  <template #default="{ row: r }">
                    <el-tag size="small" :type="inputTypeTag(r.inputType)">{{ inputTypeLabel(r.inputType) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="description" :label="t('po.itemDesc')" min-width="240" />
                <el-table-column :label="t('po.quantity')" width="120" align="right">
                  <template #default="{ row: r }">
                    <strong>{{ Number(r.quantity).toLocaleString(undefined, { maximumFractionDigits: 3 }) }}</strong>
                    <span class="dim small"> {{ r.unit }}</span>
                  </template>
                </el-table-column>
                <el-table-column :label="t('po.unitPrice')" width="120" align="right">
                  <template #default="{ row: r }">{{ fmtMoney(r.unitPrice) }}</template>
                </el-table-column>
                <el-table-column :label="t('po.amount')" width="140" align="right">
                  <template #default="{ row: r }">
                    <strong>{{ fmtMoney(r.amount) }}</strong>
                  </template>
                </el-table-column>
                <el-table-column :label="t('po.receivedQty')" width="120" align="right">
                  <template #default="{ row: r }">
                    <span :class="receivedClass(r)">
                      {{ Number(r.receivedQty || 0).toLocaleString(undefined, { maximumFractionDigits: 3 }) }}
                    </span>
                    <span class="dim small"> / {{ Number(r.quantity).toLocaleString(undefined, { maximumFractionDigits: 3 }) }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="remark" :label="t('common.remark')" min-width="160">
                  <template #default="{ row: r }">
                    <span v-if="r.remark">{{ r.remark }}</span>
                    <span v-else class="dim">-</span>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="id" label="ID" width="60" align="center">
          <template #default="{ row }"><span class="cid">#{{ row.id }}</span></template>
        </el-table-column>
        <el-table-column :label="t('po.code')" width="180">
          <template #default="{ row }">
            <router-link :to="`/procurement/orders/${row.id}`" class="link-code">
              <code class="po-code">{{ row.code }}</code>
            </router-link>
          </template>
        </el-table-column>
        <el-table-column :label="t('po.supplier')" min-width="200">
          <template #default="{ row }">
            <div>{{ row.supplierName }}</div>
            <code class="dim small">{{ row.supplierCode }}</code>
          </template>
        </el-table-column>
        <el-table-column prop="orderDate" :label="t('po.orderDate')" width="110" align="center" />
        <el-table-column prop="expectedDate" :label="t('po.expectedDate')" width="110" align="center">
          <template #default="{ row }">
            <span v-if="row.expectedDate">{{ row.expectedDate }}</span>
            <span v-else class="dim">-</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('po.totalAmount')" width="140" align="right">
          <template #default="{ row }">
            <strong>{{ fmtMoney(row.totalAmount) }}</strong>
            <span class="dim small"> {{ row.currency }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.status')" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small" effect="dark">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('paymentStatus.label')" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="paymentTag(row.paymentStatus)" size="small">{{ paymentLabel(row.paymentStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('paymentStatus.dueDate')" width="115" align="center">
          <template #default="{ row }">
            <span :class="{ overdue: isOverdue(row) }">{{ row.dueDate || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.actions')" width="320" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="info" size="small" @click="goDetail(row)">
              {{ t('po.viewDetail') }}
            </el-button>
            <el-button link type="primary" size="small" @click="onEdit(row)" :disabled="!isEditable(row.status)">
              {{ t('common.edit') }}
            </el-button>
            <el-button v-if="row.status === 'draft'" link type="success" size="small" @click="onConfirm(row)">
              {{ t('po.confirm') }}
            </el-button>
            <el-button v-if="row.status === 'confirmed' || row.status === 'partial_received'" link type="success" size="small" @click="onReceive(row)">
              {{ t('po.receive') }}
            </el-button>
            <el-button
              v-if="row.paymentStatus !== 'paid' && row.status !== 'cancelled' && row.status !== 'draft'"
              link type="warning" size="small" @click="onOpenPayment(row)"
            >
              💰 {{ t('vpay.record') }}
            </el-button>
            <el-button v-if="row.status === 'draft' || row.status === 'confirmed'" link type="warning" size="small" @click="onCancel(row)">
              {{ t('po.cancel') }}
            </el-button>
            <el-button v-if="row.status === 'draft' || row.status === 'cancelled'" link type="danger" size="small" @click="onDelete(row)">
              {{ t('common.delete') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pager"
        background
        layout="total, prev, pager, next, sizes"
        :page-sizes="[10, 20, 50]"
        :total="total"
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        @current-change="reload()"
        @size-change="reload(1)"
      />
    </el-card>

    <!-- 新建 / 编辑 弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? t('po.edit') : t('po.new')"
      width="960px"
      :close-on-click-modal="false"
      destroy-on-close
      @closed="onDialogClosed"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <div class="form-grid">
          <el-form-item :label="t('po.supplier')" prop="supplierId">
            <el-select v-model="form.supplierId" filterable :placeholder="t('po.pickSupplier')" style="width: 100%">
              <el-option v-for="s in supplierOptions" :key="s.id" :label="`${s.name} (${s.code})`" :value="s.id" />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('po.currency')" prop="currency">
            <el-select v-model="form.currency" style="width: 100%">
              <el-option label="KES" value="KES" />
              <el-option label="USD" value="USD" />
              <el-option label="EUR" value="EUR" />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('po.orderDate')" prop="orderDate">
            <el-date-picker v-model="form.orderDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
          <el-form-item :label="t('po.expectedDate')">
            <el-date-picker v-model="form.expectedDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
        </div>

        <!-- 应付日预览 -->
        <el-form-item v-if="form.supplierId" :label="t('paymentStatus.dueDate')">
          <div class="terms-preview">
            <el-tag type="info" size="small">
              {{ t('customer.creditDays') }}: {{ selectedSupplierCreditDays }} {{ t('customer.daysSuffix') }}
              <span v-if="selectedSupplierTermsLabel"> · {{ selectedSupplierTermsLabel }}</span>
            </el-tag>
            <span class="arrow"> → </span>
            <strong class="due-date">{{ dueDatePreview || '-' }}</strong>
            <span class="dim small" style="margin-left: 8px">
              ({{ t('po.orderDate') }} + {{ selectedSupplierCreditDays }} {{ t('customer.daysSuffix') }})
            </span>
          </div>
        </el-form-item>

        <el-form-item :label="t('common.remark')">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>

        <div class="items-block">
          <div class="items-head">
            <span class="items-title">{{ t('po.items') }}</span>
            <el-button :icon="PlusIcon" link type="primary" @click="addItem">{{ t('po.addLine') }}</el-button>
          </div>
          <el-table :data="form.items" border size="small" :empty-text="t('po.emptyItems')">
            <el-table-column type="index" width="46" label="#" align="center" />
            <el-table-column :label="t('po.inputType')" width="140">
              <template #default="{ $index }">
                <el-select v-model="form.items[$index].inputType" filterable size="small" style="width: 100%">
                  <el-option v-for="opt in INPUT_TYPE_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column :label="t('po.itemDesc')" min-width="240">
              <template #default="{ $index }">
                <el-input v-model="form.items[$index].description" size="small" :placeholder="t('po.itemDescPlaceholder')" maxlength="255" />
              </template>
            </el-table-column>
            <el-table-column :label="t('po.quantity')" width="120">
              <template #default="{ $index }">
                <el-input-number v-model="form.items[$index].quantity" :min="0.001" :precision="3" :step="1" :controls="false" size="small" style="width: 100%" />
              </template>
            </el-table-column>
            <el-table-column :label="t('po.unit')" width="100">
              <template #default="{ $index }">
                <el-input v-model="form.items[$index].unit" size="small" maxlength="16" placeholder="bag/kg/L" />
              </template>
            </el-table-column>
            <el-table-column :label="t('po.unitPrice') + ` (${form.currency})`" width="130">
              <template #default="{ $index }">
                <el-input-number v-model="form.items[$index].unitPrice" :min="0" :precision="2" :step="100" :controls="false" size="small" style="width: 100%" />
              </template>
            </el-table-column>
            <el-table-column :label="t('po.amount')" width="120" align="right">
              <template #default="{ $index }">
                <strong>{{ fmtMoney(lineAmount(form.items[$index])) }}</strong>
              </template>
            </el-table-column>
            <el-table-column :label="t('common.remark')" min-width="120">
              <template #default="{ $index }">
                <el-input v-model="form.items[$index].remark" size="small" maxlength="255" />
              </template>
            </el-table-column>
            <el-table-column :label="t('common.actions')" width="60" align="center">
              <template #default="{ $index }">
                <el-button link type="danger" size="small" :disabled="form.items.length <= 1" @click="removeItem($index)">×</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="grand-total">
            {{ t('po.grandTotal') }}:
            <strong>{{ fmtMoney(grandTotal) }}</strong>
            <span class="dim"> {{ form.currency }}</span>
          </div>
        </div>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="onSubmit">{{ t('common.save') }}</el-button>
      </template>
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
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search as SearchIcon,
  Refresh as RefreshIcon,
  Plus as PlusIcon,
} from '@element-plus/icons-vue'
import {
  listPurchaseOrders,
  getPurchaseOrder,
  createPurchaseOrder,
  updatePurchaseOrder,
  confirmPurchaseOrder,
  receivePurchaseOrder,
  cancelPurchaseOrder,
  deletePurchaseOrder,
} from '@/api/purchaseOrder'
import { listSuppliers } from '@/api/supplier'
import VendorPaymentDialog from '@/components/VendorPaymentDialog.vue'

const { t } = useI18n()
const router = useRouter()

function goDetail(row) { router.push(`/procurement/orders/${row.id}`) }

// ----- enums -----
const INPUT_TYPE_OPTIONS = computed(() => [
  { value: 'labor',       label: t('po.inputLabor')       },
  { value: 'water',       label: t('po.inputWater')       },
  { value: 'electricity', label: t('po.inputElectricity') },
  { value: 'fertilizer',  label: t('po.inputFertilizer')  },
  { value: 'seed',        label: t('po.inputSeed')        },
  { value: 'pesticide',   label: t('po.inputPesticide')   },
  { value: 'equipment',   label: t('po.inputEquipment')   },
  { value: 'service',     label: t('po.inputService')     },
  { value: 'other',       label: t('po.inputOther')       },
])
function inputTypeLabel(v) { return INPUT_TYPE_OPTIONS.value.find(o => o.value === v)?.label || v }
const INPUT_TAG = {
  labor: 'primary', fertilizer: 'success', seed: 'success', pesticide: 'warning',
  water: 'info', electricity: 'info', equipment: '', service: 'info', other: ''
}
function inputTypeTag(v) { return INPUT_TAG[v] || '' }

const STATUS_TAG = {
  draft:             'info',
  confirmed:         'primary',
  partial_received:  'warning',
  received:          'success',
  cancelled:         'info',
}
function statusTag(v) { return STATUS_TAG[v] || 'info' }
function statusLabel(v) { return t(`poStatus.${v}`, v) }

const PAYMENT_TAG = { unpaid: 'info', partial: 'warning', paid: 'success' }
function paymentTag(v) { return PAYMENT_TAG[v] || 'info' }
function paymentLabel(v) { return t(`paymentStatus.${v}`, v) }

function isOverdue(row) {
  if (!row || row.paymentStatus === 'paid' || !row.dueDate) return false
  return row.dueDate < new Date().toISOString().slice(0, 10)
}
function isEditable(status) { return status === 'draft' || status === 'confirmed' }

function receivedClass(item) {
  if (!item) return ''
  const recv = Number(item.receivedQty || 0)
  const qty = Number(item.quantity || 0)
  if (qty <= 0) return 'dim'
  if (recv >= qty) return 'received-full'
  if (recv > 0) return 'received-partial'
  return 'dim'
}

// ----- 列表 -----
const loading = ref(false)
const list = ref([])
const total = ref(0)
const dateRange = ref(null)
const query = reactive({ code: '', supplierId: null, status: '', dateFrom: null, dateTo: null, page: 1, size: 20 })

function onDateChange(v) {
  query.dateFrom = v && v[0] || null
  query.dateTo = v && v[1] || null
  reload(1)
}
async function reload(page) {
  if (page) query.page = page
  loading.value = true
  try {
    const data = await listPurchaseOrders(query)
    list.value = data.list || []
    total.value = data.total || 0
  } finally { loading.value = false }
}
function resetQuery() {
  query.code = ''; query.supplierId = null; query.status = ''
  query.dateFrom = null; query.dateTo = null; dateRange.value = null
  reload(1)
}

// ----- 供应商下拉 -----
const supplierOptions = ref([])
async function loadSuppliers() {
  try {
    const data = await listSuppliers({ status: 'active', page: 1, size: 500 })
    supplierOptions.value = data.list || []
  } catch { supplierOptions.value = [] }
}
onMounted(async () => {
  await loadSuppliers()
  reload(1)
})

// ----- 展开行 (PO 明细) -----
const detailItems = reactive({})
const detailLoading = reactive({})
async function onExpand(row, expandedRows) {
  const expanded = expandedRows.some(r => r.id === row.id)
  if (!expanded) return
  detailLoading[row.id] = true
  try {
    const data = await getPurchaseOrder(row.id)
    detailItems[row.id] = data.items || []
  } catch {
    detailItems[row.id] = []
  } finally {
    detailLoading[row.id] = false
  }
}

// ----- 新建 / 编辑 -----
const dialogVisible = ref(false)
const editingId = ref(null)
const formRef = ref(null)
const saving = ref(false)
const todayIso = () => new Date().toISOString().slice(0, 10)

const emptyForm = () => ({
  supplierId: null,
  orderDate: todayIso(),
  expectedDate: todayIso(),
  currency: 'KES',
  fxRate: 1,
  remark: '',
  items: [{ inputType: 'fertilizer', description: '', quantity: 1, unit: 'bag', unitPrice: 0, remark: '' }],
})
const form = reactive(emptyForm())

const rules = computed(() => ({
  supplierId: [{ required: true, message: t('valid.required', { field: t('po.supplier') }), trigger: 'change' }],
  orderDate:  [{ required: true, message: t('valid.required', { field: t('po.orderDate') }), trigger: 'change' }],
  currency:   [{ required: true, message: t('valid.required', { field: t('po.currency') }), trigger: 'change' }],
}))

function fmtMoney(v) {
  if (v == null) return '0.00'
  return Number(v).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function lineAmount(row) {
  if (!row) return 0
  return (Number(row.quantity) || 0) * (Number(row.unitPrice) || 0)
}
const grandTotal = computed(() => form.items.reduce((s, r) => s + lineAmount(r), 0))

const selectedSupplier = computed(() => {
  if (!form.supplierId) return null
  return supplierOptions.value.find(s => s.id === form.supplierId) || null
})
const selectedSupplierCreditDays = computed(() => Number(selectedSupplier.value?.creditDays ?? 0))
const selectedSupplierTermsLabel = computed(() => selectedSupplier.value?.paymentTerms || '')
const dueDatePreview = computed(() => {
  if (!form.orderDate) return ''
  const d = new Date(form.orderDate)
  if (Number.isNaN(d.getTime())) return ''
  d.setDate(d.getDate() + selectedSupplierCreditDays.value)
  return d.toISOString().slice(0, 10)
})

function addItem() {
  form.items.push({ inputType: 'fertilizer', description: '', quantity: 1, unit: 'bag', unitPrice: 0, remark: '' })
}
function removeItem(i) { if (form.items.length > 1) form.items.splice(i, 1) }
function onDialogClosed() {
  formRef.value?.clearValidate()
  editingId.value = null
}

function onCreate() {
  editingId.value = null
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}

async function onEdit(row) {
  if (!isEditable(row.status)) {
    ElMessage.warning(t('po.notEditable'))
    return
  }
  editingId.value = row.id
  try {
    const data = await getPurchaseOrder(row.id)
    const h = data.order
    Object.assign(form, emptyForm(), {
      supplierId: h.supplierId,
      orderDate: h.orderDate,
      expectedDate: h.expectedDate,
      currency: h.currency,
      fxRate: h.fxRate || 1,
      remark: h.remark || '',
      items: (data.items || []).map(it => ({
        inputType: it.inputType, description: it.description,
        quantity: Number(it.quantity), unit: it.unit,
        unitPrice: Number(it.unitPrice), remark: it.remark,
      })),
    })
    if (!form.items.length) addItem()
    dialogVisible.value = true
  } catch {}
}

async function onSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  const validItems = form.items.filter(it => it.inputType && it.description && Number(it.quantity) > 0)
  if (!validItems.length) {
    ElMessage.warning(t('po.atLeastOneItem'))
    return
  }
  saving.value = true
  try {
    const payload = { ...form, items: validItems }
    if (editingId.value) {
      await updatePurchaseOrder(editingId.value, payload)
      ElMessage.success(t('common.updateSuccess'))
    } else {
      await createPurchaseOrder(payload)
      ElMessage.success(t('common.createSuccess'))
    }
    dialogVisible.value = false
    reload()
  } catch {} finally { saving.value = false }
}

// ----- 状态推进 -----
async function onConfirm(row) {
  await ElMessageBox.confirm(t('po.confirmConfirm', { code: row.code }), t('common.tip'), { type: 'warning' })
    .catch(() => Promise.reject('cancel'))
  try {
    await confirmPurchaseOrder(row.id)
    ElMessage.success(t('po.confirmedMsg'))
    reload()
  } catch (e) { if (e === 'cancel') return }
}

async function onReceive(row) {
  await ElMessageBox.confirm(t('po.confirmReceive', { code: row.code }), t('common.tip'), { type: 'warning' })
    .catch(() => Promise.reject('cancel'))
  try {
    await receivePurchaseOrder(row.id)
    ElMessage.success(t('po.receivedMsg'))
    reload()
  } catch (e) { if (e === 'cancel') return }
}

async function onCancel(row) {
  await ElMessageBox.confirm(t('po.confirmCancel', { code: row.code }), t('common.tip'), { type: 'warning' })
    .catch(() => Promise.reject('cancel'))
  try {
    await cancelPurchaseOrder(row.id)
    ElMessage.success(t('po.cancelledMsg'))
    reload()
  } catch (e) { if (e === 'cancel') return }
}

async function onDelete(row) {
  await ElMessageBox.confirm(t('po.confirmDelete', { code: row.code }), t('common.tip'), { type: 'warning' })
    .catch(() => Promise.reject('cancel'))
  try {
    await deletePurchaseOrder(row.id)
    ElMessage.success(t('common.deleteSuccess'))
    reload()
  } catch (e) { if (e === 'cancel') return }
}

// ----- 付款 -----
const payDialogVisible = ref(false)
const payTarget = ref(null)
function onOpenPayment(row) {
  payTarget.value = {
    id: row.id,
    code: row.code,
    supplierName: row.supplierName,
    totalAmount: row.totalAmount,
    paidAmount: row.paidAmount,
    currency: row.currency,
  }
  payDialogVisible.value = true
}
async function onPaymentSaved() {
  // 刷新当前行的明细 (如果展开) + 整个列表
  await reload()
}
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.filter-card :deep(.el-card__body),
.table-card :deep(.el-card__body) { padding: 16px; }
.toolbar { margin-bottom: 12px; display: flex; justify-content: flex-end; }
.pager { margin-top: 14px; justify-content: flex-end; }

.po-code {
  font-family: 'Consolas', monospace;
  background: #f5f7fa;
  padding: 2px 8px;
  border-radius: 4px;
  color: #fa8c16;
  font-size: 12px;
  font-weight: 600;
}
.link-code { text-decoration: none; }
.link-code .po-code:hover { background: #fff5e6; }
.cid { font-family: 'Consolas', monospace; color: #909399; font-size: 12px; }
.dim { color: #909399; }
.small { font-size: 11px; }
.overdue { color: #f56c6c; font-weight: 700; }

.received-full    { color: #1f7a35; font-weight: 600; }
.received-partial { color: #e6a23c; font-weight: 600; }

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
}

.terms-preview {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 4px 10px; background: #fff5e6;
  border: 1px dashed #f7c873; border-radius: 4px;
  font-size: 13px;
}
.terms-preview .arrow { color: #909399; margin: 0 4px; }
.terms-preview .due-date {
  color: #fa8c16; font-weight: 700; font-family: 'Consolas', monospace;
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
  background: #fff5e6;
  border-radius: 4px;
  color: #fa8c16;
}

.items-wrap {
  padding: 12px 18px;
  background: #f7f9fc;
}
</style>
