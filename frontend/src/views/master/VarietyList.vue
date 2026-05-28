<template>
  <div class="page">
    <!-- 过滤器 -->
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item :label="t('variety.crop')">
          <el-select
            v-model="query.cropId"
            :placeholder="t('common.all')"
            clearable
            filterable
            style="width: 180px"
          >
            <el-option
              v-for="c in crops"
              :key="c.id"
              :label="`${c.name} (${c.code})`"
              :value="c.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('variety.code')">
          <el-input v-model="query.code" :placeholder="t('crop.fuzzy')" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item :label="t('variety.name')">
          <el-input v-model="query.name" :placeholder="t('crop.fuzzy')" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item :label="t('common.status')">
          <el-select v-model="query.status" :placeholder="t('common.all')" clearable style="width: 120px">
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
        <el-button type="primary" :icon="PlusIcon" @click="onCreate">{{ t('variety.new') }}</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border stripe row-key="id">
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column :label="t('variety.crop')" min-width="150">
          <template #default="{ row }">
            <el-tag size="small" type="primary">
              {{ cropName(row.cropId) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="code" :label="t('variety.code')" width="120" />
        <el-table-column prop="name" :label="t('variety.name')" min-width="140" />
        <el-table-column prop="traits" :label="t('variety.traits')" min-width="200" show-overflow-tooltip />
        <el-table-column prop="shelfLifeDays" :label="t('variety.shelfLifeDays')" width="120" align="center">
          <template #default="{ row }">
            <span v-if="row.shelfLifeDays">{{ row.shelfLifeDays }} d</span>
            <span v-else class="dim small">{{ t('variety.useCropDefault') }}</span>
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
      :title="editing ? t('variety.editTitle') : t('variety.createTitle')"
      width="500"
      destroy-on-close
      @closed="onDialogClosed"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item :label="t('variety.crop')" prop="cropId">
          <el-select
            v-model="form.cropId"
            :placeholder="t('variety.selectCrop')"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="c in crops"
              :key="c.id"
              :label="`${c.name} (${c.code})`"
              :value="c.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('variety.code')" prop="code">
          <el-input v-model="form.code" :placeholder="t('variety.placeholderCode')" maxlength="32" />
          <div class="hint">{{ t('variety.codeUnique') }}</div>
        </el-form-item>
        <el-form-item :label="t('variety.name')" prop="name">
          <el-input v-model="form.name" :placeholder="t('variety.placeholderName')" maxlength="64" />
        </el-form-item>
        <el-form-item :label="t('variety.traits')">
          <el-input v-model="form.traits" type="textarea" :rows="2" maxlength="255" show-word-limit />
        </el-form-item>
        <el-form-item :label="t('variety.shelfLifeDays')">
          <el-input-number v-model="form.shelfLifeDays" :min="1" :max="365" controls-position="right" />
          <span class="dim small" style="margin-left:8px">{{ t('variety.shelfLifeHint') }}</span>
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
  listVarieties,
  createVariety,
  updateVariety,
  changeVarietyStatus,
} from '@/api/variety'
import { listCrops } from '@/api/crop'

const { t } = useI18n()

// ============================================================
// 作物清单(用于下拉 + id→name 映射)
// ============================================================
const crops = ref([])
const cropMap = computed(() => {
  const m = {}
  crops.value.forEach(c => { m[c.id] = c })
  return m
})

function cropName(id) {
  const c = cropMap.value[id]
  return c ? `${c.name} (${c.code})` : `#${id}`
}

async function loadCrops() {
  // 一次拉够,假设作物总数不超过 200(可配)
  const data = await listCrops({ page: 1, size: 200 })
  crops.value = data.list
}

// ============================================================
// 列表
// ============================================================
const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const query = reactive({ cropId: null, code: '', name: '', status: null })

async function reload(toPage) {
  if (toPage) page.value = toPage
  loading.value = true
  try {
    const data = await listVarieties({
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
  query.cropId = null
  query.code = ''
  query.name = ''
  query.status = null
  reload(1)
}

function onPageChange(p) { page.value = p; reload() }
function onSizeChange(s) { pageSize.value = s; reload(1) }

onMounted(async () => {
  await loadCrops()
  await reload(1)
})

// ============================================================
// 对话框
// ============================================================
const dialogVisible = ref(false)
const editing = ref(null)
const saving = ref(false)
const formRef = ref(null)

const emptyForm = () => ({ cropId: null, code: '', name: '', traits: '', shelfLifeDays: null })
const form = reactive(emptyForm())

const rules = computed(() => ({
  cropId: [{ required: true, message: t('variety.selectCrop'), trigger: 'change' }],
  code: [
    { required: true, message: t('valid.required', { field: t('variety.code') }), trigger: 'blur' },
    { max: 32, message: t('valid.maxLen', { field: t('variety.code'), n: 32 }), trigger: 'blur' },
  ],
  name: [
    { required: true, message: t('valid.required', { field: t('variety.name') }), trigger: 'blur' },
    { max: 64, message: t('valid.maxLen', { field: t('variety.name'), n: 64 }), trigger: 'blur' },
  ],
}))

function onCreate() {
  editing.value = null
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}

function onEdit(row) {
  editing.value = row.id
  Object.assign(form, {
    cropId: row.cropId,
    code: row.code,
    name: row.name,
    traits: row.traits || '',
    shelfLifeDays: row.shelfLifeDays,
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
      await updateVariety(editing.value, form)
      ElMessage.success(t('common.updateSuccess'))
    } else {
      await createVariety(form)
      ElMessage.success(t('common.createSuccess'))
    }
    dialogVisible.value = false
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
    t('variety.confirmToggle', { action, name: row.name }),
    t('common.tip'),
    { type: 'warning' },
  ).catch(() => Promise.reject('cancel'))
  try {
    await changeVarietyStatus(row.id, next)
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
