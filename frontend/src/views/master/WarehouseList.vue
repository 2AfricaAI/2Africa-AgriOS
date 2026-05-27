<template>
  <div class="page">
    <!-- 过滤器 -->
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item :label="t('wh.type')">
          <el-select v-model="query.type" :placeholder="t('common.all')" clearable style="width: 140px">
            <el-option
              v-for="opt in TYPE_OPTIONS"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('wh.parentNode')">
          <el-select
            v-model="query.parentId"
            :placeholder="t('common.all')"
            clearable
            filterable
            style="width: 200px"
          >
            <el-option :label="t('wh.topNode')" :value="0" />
            <el-option
              v-for="w in allWarehouses"
              :key="w.id"
              :label="`${w.name} (${w.code})`"
              :value="w.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('wh.code')">
          <el-input v-model="query.code" :placeholder="t('crop.fuzzy')" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item :label="t('wh.name')">
          <el-input v-model="query.name" :placeholder="t('crop.fuzzy')" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item :label="t('common.status')">
          <el-select v-model="query.status" :placeholder="t('common.all')" clearable style="width: 110px">
            <el-option :label="t('common.enable')" :value="1" />
            <el-option :label="t('common.disable')" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="SearchIcon" @click="reload(1)">{{ t('common.search') }}</el-button>
          <el-button :icon="RefreshIcon" @click="onReset">{{ t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 工具栏 + 表格 -->
    <el-card shadow="never" class="table-card">
      <div class="toolbar">
        <el-button type="primary" :icon="PlusIcon" @click="onCreate">{{ t('wh.newFull') }}</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border stripe row-key="id">
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="code" :label="t('wh.code')" width="120" />
        <el-table-column prop="name" :label="t('wh.name')" min-width="140" />
        <el-table-column :label="t('wh.type')" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="typeTagColor(row.type)" size="small">
              {{ typeLabel(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('wh.purpose')" width="160" align="center">
          <template #default="{ row }">
            <el-tag :type="purposeTagColor(row.purpose)" size="small">
              {{ purposeLabel(row.purpose) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('wh.parentNode')" min-width="160">
          <template #default="{ row }">
            <span v-if="!row.parentId || row.parentId === 0" style="color: #909399">{{ t('wh.topNodeShort') }}</span>
            <span v-else>{{ warehouseName(row.parentId) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="capacityKg" :label="t('wh.capacityKg')" width="120" align="right">
          <template #default="{ row }">
            <span v-if="row.capacityKg == null" style="color: #c0c4cc">-</span>
            <span v-else>{{ Number(row.capacityKg).toLocaleString() }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.status')" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? t('common.enable') : t('common.disable') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" :label="t('common.createdAt')" width="170" />
        <el-table-column :label="t('common.actions')" width="120" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="onEdit(row)">{{ t('common.edit') }}</el-button>
            <el-button
              link
              :type="row.status === 1 ? 'warning' : 'success'"
              size="small"
              @click="onToggleStatus(row)"
            >
              {{ row.status === 1 ? t('common.actionDisable') : t('common.actionEnable') }}
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
      :title="editing ? t('wh.editTitleFull') : t('wh.createTitleFull')"
      width="520"
      destroy-on-close
      @closed="onDialogClosed"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item :label="t('wh.code')" prop="code">
          <el-input v-model="form.code" :placeholder="t('wh.placeholderCode')" maxlength="32" />
        </el-form-item>
        <el-form-item :label="t('wh.name')" prop="name">
          <el-input v-model="form.name" :placeholder="t('wh.placeholderName')" maxlength="64" />
        </el-form-item>
        <el-form-item :label="t('wh.type')" prop="type">
          <el-select v-model="form.type" :placeholder="t('common.selectPlaceholder')" style="width: 100%">
            <el-option
              v-for="opt in TYPE_OPTIONS"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('wh.purpose')" prop="purpose">
          <el-select v-model="form.purpose" :placeholder="t('common.selectPlaceholder')" style="width: 100%">
            <el-option
              v-for="opt in PURPOSE_OPTIONS"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('wh.parentNode')">
          <el-select
            v-model="form.parentId"
            :placeholder="t('wh.parentPlaceholder')"
            clearable
            filterable
            style="width: 100%"
          >
            <el-option :label="t('wh.topNode')" :value="0" />
            <el-option
              v-for="w in selectableParents"
              :key="w.id"
              :label="`${w.name} (${w.code})`"
              :value="w.id"
            />
          </el-select>
          <div class="hint">{{ t('wh.parentSelfTip') }}</div>
        </el-form-item>
        <el-form-item :label="t('wh.capacityKg')">
          <el-input-number
            v-model="form.capacityKg"
            :min="0"
            :precision="2"
            :step="100"
            controls-position="right"
            style="width: 100%"
          />
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
import { ref, reactive, onMounted, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search as SearchIcon,
  Refresh as RefreshIcon,
  Plus as PlusIcon,
} from '@element-plus/icons-vue'
import {
  listWarehouses,
  createWarehouse,
  updateWarehouse,
  changeWarehouseStatus,
} from '@/api/warehouse'

const { t } = useI18n()

// ============================================================
// 类型字典(跟后端 schema 对齐: normal/cold/quarantine)
// ============================================================
const TYPE_OPTIONS = computed(() => [
  { value: 'normal',     label: t('wh.typeNormal'),     tag: 'success' },
  { value: 'cold',       label: t('wh.typeCold'),       tag: 'primary' },
  { value: 'quarantine', label: t('wh.typeQuarantine'), tag: 'warning' },
])
const TYPE_MAP = computed(() =>
  Object.fromEntries(TYPE_OPTIONS.value.map(opt => [opt.value, opt]))
)
function typeLabel(v) { return TYPE_MAP.value[v]?.label || v }
function typeTagColor(v) { return TYPE_MAP.value[v]?.tag || 'info' }

// Sprint 22.0 - business purpose (9 categories, separated by GAP requirement)
const PURPOSE_OPTIONS = computed(() => [
  { value: 'finished_goods',       label: t('wh.purposeFinishedGoods'),      tag: 'success' },
  { value: 'seed_storage',         label: t('wh.purposeSeedStorage'),        tag: 'warning' },
  { value: 'fertilizer_storage',   label: t('wh.purposeFertilizerStorage'),  tag: 'success' },
  { value: 'pesticide_storage',    label: t('wh.purposePesticideStorage'),   tag: 'danger'  },
  { value: 'construction_storage', label: t('wh.purposeConstructionStorage'),tag: 'info'    },
  { value: 'spare_parts_storage',  label: t('wh.purposeSparePartsStorage'),  tag: 'info'    },
  { value: 'tools_storage',        label: t('wh.purposeToolsStorage'),       tag: 'info'    },
  { value: 'packaging_storage',    label: t('wh.purposePackagingStorage'),   tag: 'primary' },
  { value: 'other_storage',        label: t('wh.purposeOtherStorage'),       tag: 'info'    },
])
const PURPOSE_MAP = computed(() =>
  Object.fromEntries(PURPOSE_OPTIONS.value.map(opt => [opt.value, opt]))
)
function purposeLabel(v) { return PURPOSE_MAP.value[v]?.label || v }
function purposeTagColor(v) { return PURPOSE_MAP.value[v]?.tag || 'info' }

// ============================================================
// 全量仓库列表(用于父节点下拉 + id→name 映射)
// ============================================================
const allWarehouses = ref([])
const warehouseMap = computed(() => {
  const m = {}
  allWarehouses.value.forEach(w => { m[w.id] = w })
  return m
})

function warehouseName(id) {
  const w = warehouseMap.value[id]
  return w ? `${w.name} (${w.code})` : `#${id}`
}

async function loadAllWarehouses() {
  const data = await listWarehouses({ page: 1, size: 500 })
  allWarehouses.value = data.list
}

// ============================================================
// 列表
// ============================================================
const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const query = reactive({ type: '', purpose: '', parentId: null, code: '', name: '', status: null })

async function reload(toPage) {
  if (toPage) page.value = toPage
  loading.value = true
  try {
    const data = await listWarehouses({
      ...query,
      page: page.value,
      size: pageSize.value,
    })
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function onReset() {
  query.type = ''
  query.purpose = ''
  query.parentId = null
  query.code = ''
  query.name = ''
  query.status = null
  reload(1)
}

function onPageChange(p) { page.value = p; reload() }
function onSizeChange(s) { pageSize.value = s; reload(1) }

onMounted(async () => {
  await loadAllWarehouses()
  await reload(1)
})

// ============================================================
// 对话框
// ============================================================
const dialogVisible = ref(false)
const editing = ref(null)
const saving = ref(false)
const formRef = ref(null)

const emptyForm = () => ({
  code: '',
  name: '',
  type: 'normal',
  purpose: 'finished_goods',
  parentId: 0,
  capacityKg: null,
})
const form = reactive(emptyForm())

// 编辑时父节点下拉里要排除"自己"
const selectableParents = computed(() => {
  return allWarehouses.value.filter(w => w.id !== editing.value)
})

const rules = computed(() => ({
  code: [
    { required: true, message: t('valid.required', { field: t('wh.code') }), trigger: 'blur' },
    { max: 32, message: t('valid.maxLen', { field: t('wh.code'), n: 32 }), trigger: 'blur' },
  ],
  name: [
    { required: true, message: t('valid.required', { field: t('wh.name') }), trigger: 'blur' },
    { max: 64, message: t('valid.maxLen', { field: t('wh.name'), n: 64 }), trigger: 'blur' },
  ],
  type: [{ required: true, message: t('wh.selectType'), trigger: 'change' }],
  purpose: [{ required: true, message: t('wh.selectPurpose'), trigger: 'change' }],
}))

function onCreate() {
  editing.value = null
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}

function onEdit(row) {
  editing.value = row.id
  Object.assign(form, {
    code: row.code,
    name: row.name,
    type: row.type || 'normal',
    purpose: row.purpose || 'finished_goods',
    parentId: row.parentId || 0,
    capacityKg: row.capacityKg != null ? Number(row.capacityKg) : null,
  })
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
      await updateWarehouse(editing.value, form)
      ElMessage.success(t('common.updateSuccess'))
    } else {
      await createWarehouse(form)
      ElMessage.success(t('common.createSuccess'))
    }
    dialogVisible.value = false
    await loadAllWarehouses()  // 列表里新加节点可能要作为其他节点的 parent
    reload()
  } catch {
    // axios 拦截器已提示
  } finally {
    saving.value = false
  }
}

// ============================================================
// 启用/停用
// ============================================================
async function onToggleStatus(row) {
  const next = row.status === 1 ? 0 : 1
  const action = next === 1 ? t('common.actionEnable') : t('common.actionDisable')
  await ElMessageBox.confirm(
    t('wh.confirmToggle', { action, name: row.name }),
    t('common.tip'),
    { type: 'warning' },
  ).catch(() => Promise.reject('cancel'))
  try {
    await changeWarehouseStatus(row.id, next)
    ElMessage.success(t('common.operationSuccess'))
    reload()
  } catch (e) {
    if (e === 'cancel') return
  }
}
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.filter-card :deep(.el-card__body),
.table-card :deep(.el-card__body) { padding: 16px; }
.toolbar { margin-bottom: 12px; display: flex; justify-content: flex-end; }
.pager { margin-top: 14px; justify-content: flex-end; }
.hint { font-size: 12px; color: #909399; margin-top: 4px; }
</style>
