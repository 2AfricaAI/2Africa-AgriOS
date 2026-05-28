<template>
  <div class="page">
    <!-- Filters -->
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item :label="t('complaint.category')">
          <el-select v-model="query.category" :placeholder="t('common.all')" clearable style="width: 150px">
            <el-option v-for="c in CATEGORIES" :key="c" :label="t(`complaint.cat_${c}`)" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('complaint.severity')">
          <el-select v-model="query.severity" :placeholder="t('common.all')" clearable style="width: 130px">
            <el-option v-for="s in SEVERITIES" :key="s" :label="t(`complaint.sev_${s}`)" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('complaint.status')">
          <el-select v-model="query.status" :placeholder="t('common.all')" clearable style="width: 170px">
            <el-option v-for="s in STATUSES" :key="s" :label="t(`complaint.st_${s}`)" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="SearchIcon" @click="reload(1)">{{ t('common.search') }}</el-button>
          <el-button :icon="RefreshIcon" @click="onReset">{{ t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Table -->
    <el-card shadow="never" class="table-card">
      <div class="toolbar">
        <span class="table-title">{{ t('complaint.title') }}</span>
        <el-button type="primary" size="small" @click="openCreate">{{ t('complaint.newComplaint') }}</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border stripe row-key="id">
        <el-table-column :label="t('complaint.code')" width="180">
          <template #default="{ row }"><code class="code-tag">{{ row.code }}</code></template>
        </el-table-column>
        <el-table-column prop="reportedAt" :label="t('complaint.reportedAt')" width="160" />
        <el-table-column :label="t('complaint.customer')" min-width="160">
          <template #default="{ row }">
            <span v-if="row.customerName">{{ row.customerName }}</span>
            <el-tag v-else size="small" type="info">{{ t('complaint.internal') }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('complaint.orderRef')" width="150">
          <template #default="{ row }"><code v-if="row.orderCode" class="link-code" @click="goOrder(row.orderId)">{{ row.orderCode }}</code><span v-else class="dim">-</span></template>
        </el-table-column>
        <el-table-column :label="t('complaint.batchRef')" width="160">
          <template #default="{ row }"><code v-if="row.batchCode" class="link-code" @click="goBatch(row.batchId)">{{ row.batchCode }}</code><span v-else class="dim">-</span></template>
        </el-table-column>
        <el-table-column :label="t('complaint.category')" width="120">
          <template #default="{ row }">
            <el-tag size="small">{{ t(`complaint.cat_${row.category}`, row.category) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('complaint.severity')" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="sevTag(row.severity)" effect="dark" size="small">{{ t(`complaint.sev_${row.severity}`, row.severity) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('complaint.status')" width="160" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" effect="dark" size="small">{{ t(`complaint.st_${row.status}`, row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('complaint.description')" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.description }}</template>
        </el-table-column>
        <el-table-column :label="t('common.actions')" width="240" fixed="right" align="center">
          <template #default="{ row }">
            <el-button v-if="row.status === 'open'" link type="primary" size="small" @click="onTransition(row, 'investigating')">{{ t('complaint.act_investigate') }}</el-button>
            <el-button v-if="['open','investigating'].includes(row.status)" link type="success" size="small" @click="openResolve(row)">{{ t('complaint.act_resolve') }}</el-button>
            <el-button v-if="row.status === 'resolved'" link size="small" @click="onTransition(row, 'closed')">{{ t('complaint.act_close') }}</el-button>
            <el-button v-if="canEscalate(row)" link type="danger" size="small" @click="openEscalate(row)">{{ t('complaint.act_recall') }}</el-button>
            <el-button v-if="row.status === 'open'" link size="small" @click="onDelete(row)">{{ t('common.delete') }}</el-button>
          </template>
        </el-table-column>
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

    <!-- New/edit dialog -->
    <el-dialog v-model="dlg" :title="t('complaint.newComplaint')" width="640px" @closed="onClose">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" status-icon>
        <el-form-item :label="t('complaint.customer')">
          <el-select v-model="form.customerId" filterable clearable :placeholder="t('complaint.placeholderCustomer')" style="width: 100%">
            <el-option v-for="c in customers" :key="c.id" :label="`${c.name} (${c.code})`" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('complaint.orderRef')">
          <el-input v-model.number="form.orderId" type="number" :placeholder="t('complaint.placeholderOrderId')" />
        </el-form-item>
        <el-form-item :label="t('complaint.batchRef')">
          <el-input v-model.number="form.batchId" type="number" :placeholder="t('complaint.placeholderBatchId')" />
        </el-form-item>
        <el-form-item :label="t('complaint.category')" prop="category">
          <el-select v-model="form.category" style="width: 100%">
            <el-option v-for="c in CATEGORIES" :key="c" :label="t(`complaint.cat_${c}`)" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('complaint.severity')" prop="severity">
          <el-select v-model="form.severity" style="width: 100%">
            <el-option v-for="s in SEVERITIES" :key="s" :label="t(`complaint.sev_${s}`)" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('complaint.channel')" prop="channel">
          <el-select v-model="form.channel" style="width: 100%">
            <el-option v-for="c in CHANNELS" :key="c" :label="t(`complaint.ch_${c}`)" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('complaint.reportedAt')" prop="reportedAt">
          <el-date-picker v-model="form.reportedAt" type="datetime" style="width: 100%" :placeholder="t('complaint.reportedAt')" value-format="YYYY-MM-DDTHH:mm:ss" />
        </el-form-item>
        <el-form-item :label="t('complaint.description')" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4" maxlength="2000" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="onSubmit">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- Resolve dialog -->
    <el-dialog v-model="resolveDlg" :title="t('complaint.resolveTitle')" width="520px">
      <el-form :model="resolveForm" label-width="120px">
        <el-form-item :label="t('complaint.resolution')">
          <el-input v-model="resolveForm.resolution" type="textarea" :rows="4" maxlength="1000" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resolveDlg = false">{{ t('common.cancel') }}</el-button>
        <el-button type="success" @click="onResolveSubmit">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- Escalate to recall dialog -->
    <el-dialog v-model="escalateDlg" :title="t('complaint.escalateTitle')" width="560px">
      <el-alert type="warning" :closable="false" show-icon style="margin-bottom: 12px">
        {{ t('complaint.escalateHint') }}
      </el-alert>
      <el-form :model="escalateForm" label-width="120px">
        <el-form-item :label="t('complaint.batchRef')">
          <el-input v-model.number="escalateForm.batchId" type="number" disabled />
        </el-form-item>
        <el-form-item :label="t('recall.reason')">
          <el-input v-model="escalateForm.reason" type="textarea" :rows="4" maxlength="2000" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="escalateDlg = false">{{ t('common.cancel') }}</el-button>
        <el-button type="danger" :loading="escalating" @click="onEscalateSubmit">{{ t('complaint.act_recall') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search as SearchIcon, Refresh as RefreshIcon } from '@element-plus/icons-vue'
import { listComplaints, createComplaint, transitionComplaint, deleteComplaint } from '@/api/complaint'
import { triggerRecall } from '@/api/recall'
import { listCustomers } from '@/api/customer'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()

const CATEGORIES = ['quality', 'quantity', 'late', 'safety', 'wrong_product', 'other']
const SEVERITIES = ['low', 'medium', 'high', 'critical']
const STATUSES = ['open', 'investigating', 'resolved', 'closed', 'escalated_to_recall']
const CHANNELS = ['phone', 'email', 'app', 'onsite', 'other']

const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const loading = ref(false)
const query = reactive({ category: '', severity: '', status: '' })

const customers = ref([])
async function loadCustomers() {
  const data = await listCustomers({ page: 1, size: 500 })
  customers.value = data.list
}

function sevTag(s) { return ({ low: 'info', medium: '', high: 'warning', critical: 'danger' })[s] || 'info' }
function statusTag(s) {
  return ({ open: 'danger', investigating: 'warning', resolved: 'success', closed: 'info', escalated_to_recall: 'danger' })[s] || 'info'
}
function canEscalate(row) {
  return row.batchId && ['open', 'investigating'].includes(row.status)
}

async function reload(toPage) {
  if (toPage) page.value = toPage
  loading.value = true
  try {
    const data = await listComplaints({ ...query, page: page.value, size: pageSize.value })
    list.value = data.list
    total.value = data.total
  } finally { loading.value = false }
}
function onReset() { query.category = ''; query.severity = ''; query.status = ''; reload(1) }
function onPageChange(p) { page.value = p; reload() }
function onSizeChange(s) { pageSize.value = s; reload(1) }

// ----- new dialog -----
const dlg = ref(false)
const saving = ref(false)
const formRef = ref(null)
const emptyForm = () => ({
  customerId: null,
  orderId: null,
  batchId: null,
  skuId: null,
  category: 'quality',
  severity: 'medium',
  channel: 'phone',
  reportedAt: new Date().toISOString().slice(0, 19),
  description: '',
})
const form = reactive(emptyForm())
const rules = {
  category: [{ required: true, message: t('valid.required', { field: t('complaint.category') }) }],
  severity: [{ required: true, message: t('valid.required', { field: t('complaint.severity') }) }],
  channel:  [{ required: true, message: t('valid.required', { field: t('complaint.channel') }) }],
  reportedAt: [{ required: true, message: t('valid.required', { field: t('complaint.reportedAt') }) }],
  description: [{ required: true, message: t('valid.required', { field: t('complaint.description') }) }],
}

function openCreate(prefill = {}) {
  Object.assign(form, emptyForm(), prefill)
  dlg.value = true
}
function onClose() { Object.assign(form, emptyForm()); formRef.value?.clearValidate() }

async function onSubmit() {
  await formRef.value.validate().catch(() => null)
  saving.value = true
  try {
    await createComplaint({ ...form })
    ElMessage.success(t('common.saveSuccess'))
    dlg.value = false
    reload(1)
  } finally { saving.value = false }
}

// ----- resolve dialog -----
const resolveDlg = ref(false)
const resolveTarget = ref(null)
const resolveForm = reactive({ resolution: '' })
function openResolve(row) {
  resolveTarget.value = row
  resolveForm.resolution = row.resolution || ''
  resolveDlg.value = true
}
async function onResolveSubmit() {
  await transitionComplaint(resolveTarget.value.id, 'resolved', resolveForm.resolution)
  ElMessage.success(t('common.saveSuccess'))
  resolveDlg.value = false
  reload()
}

// ----- transition shortcut -----
async function onTransition(row, to) {
  await ElMessageBox.confirm(t('complaint.confirmTransition', { to: t(`complaint.st_${to}`) }), t('common.tip'), { type: 'info' }).catch(() => null)
  await transitionComplaint(row.id, to)
  ElMessage.success(t('common.saveSuccess'))
  reload()
}

async function onDelete(row) {
  await ElMessageBox.confirm(t('complaint.confirmDelete', { code: row.code }), t('common.tip'), { type: 'warning' }).catch(() => null)
  await deleteComplaint(row.id)
  ElMessage.success(t('common.deleteSuccess'))
  reload()
}

// ----- escalate to recall -----
const escalateDlg = ref(false)
const escalating = ref(false)
const escalateTarget = ref(null)
const escalateForm = reactive({ batchId: null, reason: '' })
function openEscalate(row) {
  escalateTarget.value = row
  escalateForm.batchId = row.batchId
  escalateForm.reason = `Escalated from complaint ${row.code}: ${row.description}`
  escalateDlg.value = true
}
async function onEscalateSubmit() {
  if (!escalateForm.reason) { ElMessage.warning(t('valid.required', { field: t('recall.reason') })); return }
  escalating.value = true
  try {
    const recallId = await triggerRecall({
      batchId: escalateForm.batchId,
      reason: escalateForm.reason,
      scope: 'batch_only',
      sourceComplaintId: escalateTarget.value.id,
    })
    ElMessage.success(t('complaint.escalateSuccess'))
    escalateDlg.value = false
    reload()
    setTimeout(() => router.push(`/qc/recalls?highlight=${recallId}`), 400)
  } finally { escalating.value = false }
}

// ----- helpers -----
function goOrder(id) { if (id) router.push(`/sales/orders/${id}`) }
function goBatch(id) { if (id) router.push(`/production/batches/${id}`) }

onMounted(async () => {
  await loadCustomers()
  // Honour ?prefillOrder=... ?prefillCustomer=... ?prefillBatch=... from OrderDetail link
  const q = route.query
  if (q.prefillOrder || q.prefillCustomer || q.prefillBatch) {
    openCreate({
      orderId: q.prefillOrder ? Number(q.prefillOrder) : null,
      customerId: q.prefillCustomer ? Number(q.prefillCustomer) : null,
      batchId: q.prefillBatch ? Number(q.prefillBatch) : null,
    })
  }
  reload(1)
})
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.filter-card :deep(.el-card__body),
.table-card :deep(.el-card__body) { padding: 16px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.table-title { font-size: 14px; font-weight: 600; color: #1f2937; }
.code-tag { font-family: 'Monaco', 'Consolas', monospace; font-size: 12px; background: #f5f7fa; padding: 2px 8px; border-radius: 4px; color: #b91c1c; }
.link-code { cursor: pointer; color: #2563eb; }
.link-code:hover { text-decoration: underline; }
.dim { color: #94a3b8; }
.pager { margin-top: 14px; justify-content: flex-end; }
</style>
