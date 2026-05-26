<template>
  <div class="page">
    <!-- 过滤器 -->
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item :label="t('common.keyword')">
          <el-input
            v-model="query.keyword"
            :placeholder="t('customer.keywordPlaceholder')"
            clearable
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item :label="t('customer.type')">
          <el-select v-model="query.type" :placeholder="t('common.all')" clearable style="width: 150px">
            <el-option
              v-for="opt in TYPE_OPTIONS"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('common.status')">
          <el-select v-model="query.status" :placeholder="t('common.all')" clearable style="width: 120px">
            <el-option :label="t('status.active')" value="active" />
            <el-option :label="t('status.inactive')" value="inactive" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="SearchIcon" @click="reload(1)">{{ t('common.search') }}</el-button>
          <el-button :icon="RefreshIcon" @click="onReset">{{ t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <div class="toolbar">
        <el-button type="primary" :icon="PlusIcon" @click="onCreate">{{ t('customer.new') }}</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border stripe row-key="id">
        <el-table-column prop="id" label="ID" width="60" align="center">
          <template #default="{ row }">
            <span class="cid">#{{ row.id }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="code" :label="t('customer.code')" width="120">
          <template #default="{ row }"><code class="cus-code">{{ row.code }}</code></template>
        </el-table-column>
        <el-table-column prop="name" :label="t('customer.name')" min-width="200" />
        <el-table-column :label="t('customer.type')" width="120" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ typeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('customer.contactName')" min-width="120">
          <template #default="{ row }">
            <span v-if="row.contactName">{{ row.contactName }}</span>
            <span v-else class="dim">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="contactPhone" :label="t('customer.contactPhone')" min-width="140">
          <template #default="{ row }">
            <span v-if="row.contactPhone">{{ row.contactPhone }}</span>
            <span v-else class="dim">-</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('customer.creditLevel')" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.creditLevel" size="small" :type="creditTag(row.creditLevel)" effect="dark">
              {{ row.creditLevel }}
            </el-tag>
            <span v-else class="dim">-</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('customer.paymentTerms')" width="140" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="termsTag(row.creditDays)">
              {{ row.paymentTerms || `Net ${row.creditDays || 0}` }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sinceDate" :label="t('customer.sinceDate')" width="110" />
        <el-table-column :label="t('common.status')" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'info'" size="small">
              {{ row.status === 'active' ? t('status.active') : t('status.inactive') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.actions')" width="260" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="onEdit(row)">
              {{ t('common.edit') }}
            </el-button>
            <el-button link type="success" size="small" @click="onOpenStatement(row)">
              📄 {{ t('statement.btn') }}
            </el-button>
            <el-button
              link
              :type="row.status === 'active' ? 'warning' : 'success'"
              size="small"
              @click="onToggleStatus(row)"
            >
              {{ row.status === 'active' ? t('status.inactive') : t('status.active') }}
            </el-button>
            <el-button link type="danger" size="small" @click="onDelete(row)">
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
      :title="editing ? t('customer.editTitle') : t('customer.createTitle')"
      width="560"
      destroy-on-close
      @closed="onDialogClosed"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item :label="t('customer.name')" prop="name">
          <el-input v-model="form.name" :placeholder="t('customer.placeholderName')" maxlength="128" />
        </el-form-item>
        <el-form-item :label="t('customer.type')" prop="type">
          <el-select v-model="form.type" style="width: 100%">
            <el-option
              v-for="opt in TYPE_OPTIONS"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('customer.contactName')">
          <el-input v-model="form.contactName" :placeholder="t('customer.placeholderContact')" maxlength="64" />
        </el-form-item>
        <el-form-item :label="t('customer.contactPhone')">
          <el-input v-model="form.contactPhone" :placeholder="t('customer.placeholderPhone')" maxlength="20" />
        </el-form-item>
        <el-form-item :label="t('customer.creditLevel')">
          <el-select v-model="form.creditLevel" clearable style="width: 100%">
            <el-option label="A" value="A" />
            <el-option label="B" value="B" />
            <el-option label="C" value="C" />
            <el-option label="D" value="D" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('customer.paymentTerms')">
          <el-select v-model="termsPreset" @change="onTermsPresetChange" style="width: 100%">
            <el-option :label="t('customer.termsCod')"       :value="0"  />
            <el-option :label="t('customer.termsWeekly')"    :value="7"  />
            <el-option :label="t('customer.termsHalfMonth')" :value="15" />
            <el-option :label="t('customer.termsMonthly')"   :value="30" />
            <el-option :label="t('customer.termsCustom')"    :value="-1" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="termsPreset === -1" :label="t('customer.creditDays')">
          <el-input-number v-model="form.creditDays" :min="0" :max="365" :controls="false"
                           style="width: 100%" />
          <span class="dim" style="margin-left: 6px">{{ t('customer.daysSuffix') }}</span>
        </el-form-item>
        <el-form-item :label="t('customer.sinceDate')">
          <el-date-picker
            v-model="form.sinceDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item :label="t('common.remark')">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="255" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="onSubmit">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- 对账单下载弹窗 -->
    <el-dialog
      v-model="stmtDialogVisible"
      :title="t('statement.title')"
      width="480px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div v-if="stmtTarget" class="stmt-target">
        <el-tag size="small" type="primary">{{ stmtTarget.name }}</el-tag>
        <code class="cus-code" style="margin-left: 6px">{{ stmtTarget.code }}</code>
      </div>
      <el-form label-width="100px" style="margin-top: 14px">
        <el-form-item :label="t('statement.period')">
          <el-date-picker
            v-model="stmtRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            :start-placeholder="t('statement.from')"
            :end-placeholder="t('statement.to')"
            style="width: 100%"
            :shortcuts="stmtShortcuts"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stmtDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="stmtDownloading" @click="onDownloadStatement">
          📥 {{ t('statement.download') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search as SearchIcon,
  Refresh as RefreshIcon,
  Plus as PlusIcon,
} from '@element-plus/icons-vue'
import {
  listCustomers,
  createCustomer,
  updateCustomer,
  changeCustomerStatus,
  deleteCustomer,
} from '@/api/customer'
import { downloadStatementPdf } from '@/api/finance'

const { t } = useI18n()

const TYPE_OPTIONS = computed(() => [
  { value: 'supermarket', label: t('customer.typeSupermarket') },
  { value: 'restaurant',  label: t('customer.typeRestaurant')  },
  { value: 'ecommerce',   label: t('customer.typeEcommerce')   },
  { value: 'wholesale',   label: t('customer.typeWholesale')   },
  { value: 'export',      label: t('customer.typeExport')      },
  { value: 'other',       label: t('customer.typeOther')       },
])
function typeLabel(v) {
  return TYPE_OPTIONS.value.find(o => o.value === v)?.label || v
}

function creditTag(level) {
  switch (level) {
    case 'A': return 'success'
    case 'B': return 'primary'
    case 'C': return 'warning'
    case 'D': return 'danger'
    default:  return 'info'
  }
}
function termsTag(days) {
  const d = Number(days) || 0
  if (d === 0)  return 'success'
  if (d <= 7)   return 'primary'
  if (d <= 15)  return 'info'
  if (d <= 30)  return 'warning'
  return 'danger'
}

// ----- list / pagination -----
const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const query = reactive({ keyword: '', type: '', status: '' })

async function reload(toPage) {
  if (toPage) page.value = toPage
  loading.value = true
  try {
    const data = await listCustomers({ ...query, page: page.value, size: pageSize.value })
    list.value = data.list
    total.value = data.total
  } finally { loading.value = false }
}
function onReset() {
  query.keyword = ''; query.type = ''; query.status = ''
  reload(1)
}
function onPageChange(p) { page.value = p; reload() }
function onSizeChange(s) { pageSize.value = s; reload(1) }
onMounted(() => reload(1))

// ----- create / edit dialog -----
const dialogVisible = ref(false)
const editing = ref(null)
const saving = ref(false)
const formRef = ref(null)

const emptyForm = () => ({
  name: '', type: 'supermarket', contactName: '', contactPhone: '',
  creditLevel: '', creditDays: 0, paymentTerms: 'COD',
  sinceDate: null, remark: '',
})
const form = reactive(emptyForm())

// 账期预设 (0/7/15/30/-1=自定义)
const termsPreset = ref(0)
const TERMS_LABEL = { 0: 'COD', 7: 'Weekly', 15: 'Half-month', 30: 'Monthly' }
function onTermsPresetChange(v) {
  if (v === -1) {
    // 自定义: 保留当前 creditDays 让用户编辑, paymentTerms 标记自定义
    form.paymentTerms = 'Custom'
  } else {
    form.creditDays = v
    form.paymentTerms = TERMS_LABEL[v] || 'COD'
  }
}

const rules = computed(() => ({
  name: [
    { required: true, message: t('valid.required', { field: t('customer.name') }), trigger: 'blur' },
    { max: 128, message: t('valid.maxLen', { field: t('customer.name'), n: 128 }), trigger: 'blur' },
  ],
  type: [
    { required: true, message: t('valid.required', { field: t('customer.type') }), trigger: 'change' },
  ],
}))

function onCreate() {
  editing.value = null
  Object.assign(form, emptyForm())
  termsPreset.value = 0
  dialogVisible.value = true
}
function onEdit(row) {
  editing.value = row.id
  const cd = Number(row.creditDays) || 0
  Object.assign(form, {
    name: row.name,
    type: row.type,
    contactName: row.contactName || '',
    contactPhone: row.contactPhone || '',
    creditLevel: row.creditLevel || '',
    creditDays: cd,
    paymentTerms: row.paymentTerms || TERMS_LABEL[cd] || 'Custom',
    sinceDate: row.sinceDate || null,
    remark: row.remark || '',
  })
  // 匹配 preset, 否则用 custom
  termsPreset.value = [0, 7, 15, 30].includes(cd) ? cd : -1
  dialogVisible.value = true
}
function onDialogClosed() {
  editing.value = null
  Object.assign(form, emptyForm())
  formRef.value?.clearValidate()
}

async function onSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (editing.value) {
      await updateCustomer(editing.value, form)
      ElMessage.success(t('common.updateSuccess'))
    } else {
      await createCustomer(form)
      ElMessage.success(t('common.createSuccess'))
    }
    dialogVisible.value = false
    reload()
  } catch {} finally { saving.value = false }
}

// ----- statement download -----
const stmtDialogVisible = ref(false)
const stmtTarget = ref(null)
const stmtRange = ref(defaultStmtRange())
const stmtDownloading = ref(false)

function defaultStmtRange() {
  // 默认本月 1 号 → 今天
  const today = new Date()
  const first = new Date(today.getFullYear(), today.getMonth(), 1)
  const fmt = d => d.toISOString().slice(0, 10)
  return [fmt(first), fmt(today)]
}
const stmtShortcuts = [
  {
    text: t('statement.shortcutCurrentMonth'),
    value: () => {
      const d = new Date(); const s = new Date(d.getFullYear(), d.getMonth(), 1)
      return [s, d]
    },
  },
  {
    text: t('statement.shortcutLastMonth'),
    value: () => {
      const d = new Date()
      const s = new Date(d.getFullYear(), d.getMonth() - 1, 1)
      const e = new Date(d.getFullYear(), d.getMonth(), 0)
      return [s, e]
    },
  },
  {
    text: t('statement.shortcutLast30'),
    value: () => {
      const e = new Date(); const s = new Date()
      s.setDate(s.getDate() - 30)
      return [s, e]
    },
  },
]

function onOpenStatement(row) {
  stmtTarget.value = { id: row.id, code: row.code, name: row.name }
  stmtRange.value = defaultStmtRange()
  stmtDialogVisible.value = true
}

async function onDownloadStatement() {
  if (!stmtTarget.value) return
  if (!stmtRange.value || stmtRange.value.length !== 2) {
    ElMessage.warning(t('valid.required', { field: t('statement.period') }))
    return
  }
  const [from, to] = stmtRange.value
  stmtDownloading.value = true
  try {
    const blob = await downloadStatementPdf(stmtTarget.value.id, { from, to })
    // 触发浏览器下载
    const url = window.URL.createObjectURL(new Blob([blob], { type: 'application/pdf' }))
    const a = document.createElement('a')
    a.href = url
    a.download = `statement-${stmtTarget.value.code}-${from}-${to}.pdf`
    document.body.appendChild(a)
    a.click()
    a.remove()
    setTimeout(() => window.URL.revokeObjectURL(url), 1000)
    ElMessage.success(t('statement.downloadSuccess'))
    stmtDialogVisible.value = false
  } catch (e) {
    ElMessage.error(t('statement.downloadFail'))
  } finally {
    stmtDownloading.value = false
  }
}

// ----- status toggle -----
async function onToggleStatus(row) {
  const next = row.status === 'active' ? 'inactive' : 'active'
  const action = next === 'active' ? t('status.active') : t('status.inactive')
  await ElMessageBox.confirm(
    t('customer.confirmToggle', { action, name: row.name }),
    t('common.tip'),
    { type: 'warning' }
  ).catch(() => Promise.reject('cancel'))
  try {
    await changeCustomerStatus(row.id, next)
    ElMessage.success(t('common.operationSuccess'))
    reload()
  } catch (e) { if (e === 'cancel') return }
}

// ----- delete -----
async function onDelete(row) {
  await ElMessageBox.confirm(
    t('customer.confirmDelete', { name: row.name }),
    t('common.tip'),
    { type: 'warning' }
  ).catch(() => Promise.reject('cancel'))
  try {
    await deleteCustomer(row.id)
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

.cus-code {
  font-family: 'Consolas', monospace;
  background: #f5f7fa;
  padding: 2px 8px;
  border-radius: 4px;
  color: #1f7a35;
  font-size: 12px;
  font-weight: 600;
}
.cid {
  font-family: 'Consolas', monospace;
  color: #909399; font-size: 12px;
}
.stmt-target {
  padding: 8px 12px;
  background: #f7faf8;
  border-left: 3px solid #1f7a35;
  border-radius: 2px;
}
.dim { color: #909399; }
</style>
