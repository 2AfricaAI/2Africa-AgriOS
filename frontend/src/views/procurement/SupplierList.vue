<template>
  <div class="page">
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="query" @keyup.enter="reload(1)">
        <el-form-item :label="t('common.keyword')">
          <el-input v-model="query.keyword" clearable :placeholder="t('supplier.keywordPlaceholder')"
                    style="width: 240px" @clear="reload(1)" />
        </el-form-item>
        <el-form-item :label="t('supplier.type')">
          <el-select v-model="query.type" clearable :placeholder="t('common.all')" style="width: 180px" @change="reload(1)">
            <el-option v-for="o in TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('common.status')">
          <el-select v-model="query.status" clearable :placeholder="t('common.all')" style="width: 140px" @change="reload(1)">
            <el-option :label="t('status.active')"   value="active" />
            <el-option :label="t('status.inactive')" value="inactive" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="SearchIcon" @click="reload(1)">{{ t('common.search') }}</el-button>
          <el-button :icon="RefreshIcon" @click="resetQuery">{{ t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <div class="toolbar">
        <el-button type="primary" :icon="PlusIcon" @click="onCreate">{{ t('supplier.new') }}</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border stripe row-key="id">
        <el-table-column prop="id" label="ID" width="60" align="center">
          <template #default="{ row }"><span class="cid">#{{ row.id }}</span></template>
        </el-table-column>
        <el-table-column prop="code" :label="t('supplier.code')" width="120">
          <template #default="{ row }"><code class="sup-code">{{ row.code }}</code></template>
        </el-table-column>
        <el-table-column prop="name" :label="t('supplier.name')" min-width="200" />
        <el-table-column :label="t('supplier.type')" width="150" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ typeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('supplier.contactName')" min-width="120">
          <template #default="{ row }">
            <span v-if="row.contactName">{{ row.contactName }}</span>
            <span v-else class="dim">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="contactPhone" :label="t('supplier.contactPhone')" min-width="140">
          <template #default="{ row }">
            <span v-if="row.contactPhone">{{ row.contactPhone }}</span>
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
        <el-table-column prop="sinceDate" :label="t('supplier.sinceDate')" width="110" />
        <el-table-column :label="t('common.status')" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'info'" size="small">
              {{ row.status === 'active' ? t('status.active') : t('status.inactive') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.actions')" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="onEdit(row)">
              {{ t('common.edit') }}
            </el-button>
            <el-button link :type="row.status === 'active' ? 'warning' : 'success'" size="small" @click="onToggleStatus(row)">
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
        layout="total, prev, pager, next, sizes"
        :page-sizes="[10, 20, 50]"
        :total="total"
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        @current-change="reload()"
        @size-change="reload(1)"
      />
    </el-card>

    <!-- 新建 / 编辑 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editing ? t('supplier.edit') : t('supplier.new')"
      width="640px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item :label="t('supplier.name')" prop="name">
          <el-input v-model="form.name" maxlength="120" />
        </el-form-item>
        <el-form-item :label="t('supplier.type')" prop="type">
          <el-select v-model="form.type" style="width: 100%">
            <el-option v-for="o in TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>

        <el-form-item :label="t('customer.creditDays')">
          <el-radio-group v-model="termsPreset" @change="onTermsPresetChange">
            <el-radio-button :label="0">{{ t('customer.termsCod') }}</el-radio-button>
            <el-radio-button :label="7">{{ t('customer.termsWeekly') }}</el-radio-button>
            <el-radio-button :label="15">{{ t('customer.termsHalfMonth') }}</el-radio-button>
            <el-radio-button :label="30">{{ t('customer.termsMonthly') }}</el-radio-button>
            <el-radio-button :label="-1">{{ t('customer.termsCustom') }}</el-radio-button>
          </el-radio-group>
          <div v-if="termsPreset === -1" style="margin-top: 8px">
            <el-input-number v-model="form.creditDays" :min="0" :max="365" :step="1" :controls="false" style="width: 120px" />
            <span class="dim" style="margin-left: 8px">{{ t('customer.daysSuffix') }}</span>
          </div>
        </el-form-item>

        <el-form-item :label="t('supplier.contactName')">
          <el-input v-model="form.contactName" maxlength="80" />
        </el-form-item>
        <el-form-item :label="t('supplier.contactPhone')">
          <el-input v-model="form.contactPhone" maxlength="32" placeholder="+254 7XX XXX XXX" />
        </el-form-item>
        <el-form-item :label="t('supplier.contactEmail')">
          <el-input v-model="form.contactEmail" maxlength="120" />
        </el-form-item>
        <el-form-item :label="t('supplier.taxId')">
          <el-input v-model="form.taxId" maxlength="64" placeholder="KRA PIN" />
        </el-form-item>
        <el-form-item :label="t('supplier.address')">
          <el-input v-model="form.address" maxlength="255" />
        </el-form-item>
        <el-form-item :label="t('supplier.sinceDate')">
          <el-date-picker v-model="form.sinceDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item :label="t('common.remark')">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="onSubmit">{{ t('common.save') }}</el-button>
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
  listSuppliers,
  createSupplier,
  updateSupplier,
  changeSupplierStatus,
  deleteSupplier,
} from '@/api/supplier'

const { t } = useI18n()

const TYPE_OPTIONS = computed(() => [
  { value: 'input_dealer',     label: t('supplier.typeInputDealer') },
  { value: 'labor_contractor', label: t('supplier.typeLaborContractor') },
  { value: 'utility',          label: t('supplier.typeUtility') },
  { value: 'equipment',        label: t('supplier.typeEquipment') },
  { value: 'service',          label: t('supplier.typeService') },
  { value: 'logistics',        label: t('supplier.typeLogistics') },
  { value: 'other',            label: t('supplier.typeOther') },
])
function typeLabel(v) { return TYPE_OPTIONS.value.find(o => o.value === v)?.label || v }

const TERMS_LABEL = { 0: 'COD', 7: '周结', 15: '半月结', 30: '月结' }
function termsTag(days) {
  if (days == null || days === 0) return 'success'
  if (days <= 7) return 'info'
  if (days <= 15) return 'warning'
  return 'danger'
}

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ keyword: '', type: '', status: '', page: 1, size: 20 })

async function reload(page) {
  if (page) query.page = page
  loading.value = true
  try {
    const data = await listSuppliers(query)
    list.value = data.list
    total.value = data.total
  } finally { loading.value = false }
}
function resetQuery() {
  query.keyword = ''; query.type = ''; query.status = ''
  reload(1)
}
onMounted(() => reload(1))

// ----- create / edit -----
const dialogVisible = ref(false)
const editing = ref(null)
const formRef = ref(null)
const saving = ref(false)
const termsPreset = ref(0)

const emptyForm = () => ({
  name: '', type: 'input_dealer',
  taxId: '', contactName: '', contactPhone: '', contactEmail: '', address: '',
  creditDays: 0, paymentTerms: 'COD',
  sinceDate: new Date().toISOString().slice(0, 10),
  remark: '',
})
const form = reactive(emptyForm())

const rules = computed(() => ({
  name: [{ required: true, message: t('valid.required', { field: t('supplier.name') }), trigger: 'blur' }],
  type: [{ required: true, message: t('valid.required', { field: t('supplier.type') }), trigger: 'change' }],
}))

function onTermsPresetChange(v) {
  if (v === -1) {
    // custom, leave the existing creditDays
  } else {
    form.creditDays = v
    form.paymentTerms = TERMS_LABEL[v]
  }
}

function onCreate() {
  editing.value = null
  Object.assign(form, emptyForm())
  termsPreset.value = 0
  dialogVisible.value = true
}
function onEdit(row) {
  editing.value = row.id
  Object.assign(form, emptyForm(), row)
  // 推断 preset
  if ([0, 7, 15, 30].includes(form.creditDays)) {
    termsPreset.value = form.creditDays
  } else {
    termsPreset.value = -1
  }
  dialogVisible.value = true
}

async function onSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (editing.value) {
      await updateSupplier(editing.value, form)
      ElMessage.success(t('common.updateSuccess'))
    } else {
      await createSupplier(form)
      ElMessage.success(t('common.createSuccess'))
    }
    dialogVisible.value = false
    reload()
  } catch {} finally { saving.value = false }
}

async function onToggleStatus(row) {
  const next = row.status === 'active' ? 'inactive' : 'active'
  await ElMessageBox.confirm(
    t('supplier.confirmToggle', { name: row.name, action: next === 'active' ? t('status.active') : t('status.inactive') }),
    t('common.tip'), { type: 'warning' }
  ).catch(() => Promise.reject('cancel'))
  try {
    await changeSupplierStatus(row.id, next)
    ElMessage.success(t('common.updateSuccess'))
    reload()
  } catch (e) { if (e === 'cancel') return }
}

async function onDelete(row) {
  await ElMessageBox.confirm(
    t('supplier.confirmDelete', { name: row.name }),
    t('common.tip'), { type: 'warning' }
  ).catch(() => Promise.reject('cancel'))
  try {
    await deleteSupplier(row.id)
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

.sup-code {
  font-family: 'Consolas', monospace;
  background: #f5f7fa;
  padding: 2px 8px;
  border-radius: 4px;
  color: #fa8c16; font-size: 12px; font-weight: 600;
}
.cid { font-family: 'Consolas', monospace; color: #909399; font-size: 12px; }
.dim { color: #909399; }
</style>
