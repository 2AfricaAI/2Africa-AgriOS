<template>
  <div class="page">
    <!-- 过滤器 -->
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item :label="t('crop.code')">
          <el-input v-model="query.code" :placeholder="t('crop.fuzzy')" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item :label="t('crop.name')">
          <el-input v-model="query.name" :placeholder="t('crop.fuzzy')" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item :label="t('crop.category')">
          <el-input v-model="query.category" :placeholder="t('crop.placeholderCategory')" clearable style="width: 160px" />
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
        <el-button type="primary" :icon="PlusIcon" @click="onCreate">{{ t('crop.new') }}</el-button>
        <el-button :icon="UploadFilled" @click="importOpen = true">{{ t('import.button') }}</el-button>
      </div>

      <ImportDialog
        v-model="importOpen"
        :title="t('menu.crops')"
        template-url="/v1/master/crops/import/template"
        import-url="/v1/master/crops/import"
        @imported="reload(1)"
      />

      <el-table :data="list" v-loading="loading" border stripe row-key="id">
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="code" :label="t('crop.code')" width="120" />
        <el-table-column prop="name" :label="t('crop.name')" min-width="120" />
        <el-table-column prop="category" :label="t('crop.category')" width="100" />
        <el-table-column prop="unit" :label="t('crop.unit')" width="80" align="center" />
        <el-table-column prop="cycleDays" :label="t('crop.cycleDays')" width="100" align="center" />
        <el-table-column prop="shelfLifeDays" :label="t('crop.shelfLifeDays')" width="110" align="center">
          <template #default="{ row }">
            <span v-if="row.shelfLifeDays">{{ row.shelfLifeDays }} d</span>
            <span v-else class="dim">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" :label="t('common.remark')" min-width="160" show-overflow-tooltip />
        <el-table-column :label="t('common.status')" width="100" align="center">
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

      <!-- 分页 -->
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
      :title="editing ? t('crop.editTitle') : t('crop.createTitle')"
      width="500"
      destroy-on-close
      @closed="onDialogClosed"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item :label="t('crop.code')" prop="code">
          <el-input v-model="form.code" :placeholder="t('crop.placeholderCode')" maxlength="32" />
        </el-form-item>
        <el-form-item :label="t('crop.name')" prop="name">
          <el-input v-model="form.name" :placeholder="t('crop.placeholderName')" maxlength="64" />
        </el-form-item>
        <el-form-item :label="t('crop.category')">
          <el-input v-model="form.category" :placeholder="t('crop.placeholderCategory')" maxlength="32" />
        </el-form-item>
        <el-form-item :label="t('crop.unit')">
          <el-input v-model="form.unit" :placeholder="t('crop.placeholderUnit')" maxlength="8" />
        </el-form-item>
        <el-form-item :label="t('crop.cycleDays')">
          <el-input-number v-model="form.cycleDays" :min="0" :max="9999" controls-position="right" />
        </el-form-item>
        <el-form-item :label="t('crop.shelfLifeDays')">
          <el-input-number v-model="form.shelfLifeDays" :min="1" :max="365" controls-position="right" />
          <span class="dim small" style="margin-left:8px">{{ t('crop.shelfLifeHint') }}</span>
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
  UploadFilled,
} from '@element-plus/icons-vue'
import {
  listCrops,
  createCrop,
  updateCrop,
  changeCropStatus,
} from '@/api/crop'
import ImportDialog from '@/components/ImportDialog.vue'

const { t } = useI18n()
const importOpen = ref(false)

// ============================================================
// 列表 / 分页 / 过滤
// ============================================================
const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)

const query = reactive({
  code: '',
  name: '',
  category: '',
  status: null,
})

async function reload(toPage) {
  if (toPage) page.value = toPage
  loading.value = true
  try {
    const data = await listCrops({
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
  query.code = ''
  query.name = ''
  query.category = ''
  query.status = null
  reload(1)
}

function onPageChange(p) {
  page.value = p
  reload()
}

function onSizeChange(s) {
  pageSize.value = s
  reload(1)
}

onMounted(() => reload(1))

// ============================================================
// 新建/编辑对话框
// ============================================================
const dialogVisible = ref(false)
const editing = ref(null)       // 编辑时存原对象 id;新建为 null
const saving = ref(false)
const formRef = ref(null)

const emptyForm = () => ({
  code: '',
  name: '',
  category: '',
  unit: 'kg',
  cycleDays: null,
  shelfLifeDays: null,
  remark: '',
})
const form = reactive(emptyForm())

const rules = computed(() => ({
  code: [
    { required: true, message: t('valid.required', { field: t('crop.code') }), trigger: 'blur' },
    { max: 32, message: t('valid.maxLen', { field: t('crop.code'), n: 32 }), trigger: 'blur' },
  ],
  name: [
    { required: true, message: t('valid.required', { field: t('crop.name') }), trigger: 'blur' },
    { max: 64, message: t('valid.maxLen', { field: t('crop.name'), n: 64 }), trigger: 'blur' },
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
    code: row.code,
    name: row.name,
    category: row.category || '',
    unit: row.unit || 'kg',
    cycleDays: row.cycleDays,
    shelfLifeDays: row.shelfLifeDays,
    remark: row.remark || '',
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
      await updateCrop(editing.value, form)
      ElMessage.success(t('common.updateSuccess'))
    } else {
      await createCrop(form)
      ElMessage.success(t('common.createSuccess'))
    }
    dialogVisible.value = false
    reload()
  } catch {
    // 错误已由 axios 拦截器 ElMessage 提示
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
    t('crop.confirmToggle', { action, name: row.name }),
    t('common.tip'),
    { type: 'warning' },
  ).catch(() => Promise.reject('cancel'))
  try {
    await changeCropStatus(row.id, next)
    ElMessage.success(t('common.operationSuccess'))
    reload()
  } catch (e) {
    if (e === 'cancel') return
  }
}
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.filter-card :deep(.el-card__body),
.table-card :deep(.el-card__body) {
  padding: 16px;
}

.toolbar {
  margin-bottom: 12px;
  display: flex;
  justify-content: flex-end;
}

.pager {
  margin-top: 14px;
  justify-content: flex-end;
}
</style>
