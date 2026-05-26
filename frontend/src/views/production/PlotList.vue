<template>
  <div class="page">
    <!-- 过滤器 -->
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="query" @keyup.enter="reload(1)">
        <el-form-item :label="t('plot.code')">
          <el-input v-model="query.code" clearable :placeholder="t('crop.fuzzy')" style="width: 140px" @clear="reload(1)" />
        </el-form-item>
        <el-form-item :label="t('plot.name')">
          <el-input v-model="query.name" clearable :placeholder="t('crop.fuzzy')" style="width: 160px" @clear="reload(1)" />
        </el-form-item>
        <el-form-item :label="t('common.status')">
          <el-select v-model="query.status" clearable :placeholder="t('common.all')" style="width: 140px" @change="reload(1)">
            <el-option :label="t('plot.statusActive')"   value="active" />
            <el-option :label="t('plot.statusInactive')" value="inactive" />
            <el-option :label="t('plot.statusFallow')"   value="fallow" />
          </el-select>
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
        <el-button type="primary" :icon="PlusIcon" @click="onCreate">{{ t('plot.new') }}</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border stripe row-key="id">
        <el-table-column prop="id" label="ID" width="60" align="center">
          <template #default="{ row }"><span class="cid">#{{ row.id }}</span></template>
        </el-table-column>
        <el-table-column prop="code" :label="t('plot.code')" width="100">
          <template #default="{ row }"><code class="plot-code">{{ row.code }}</code></template>
        </el-table-column>
        <el-table-column prop="name" :label="t('plot.name')" min-width="180" />
        <el-table-column :label="t('plot.areaMu')" width="110" align="right">
          <template #default="{ row }">
            <strong>{{ Number(row.areaMu).toLocaleString(undefined, { maximumFractionDigits: 2 }) }}</strong>
            <span class="dim small"> mu</span>
          </template>
        </el-table-column>
        <el-table-column prop="ownerName" :label="t('plot.owner')" width="130">
          <template #default="{ row }">
            <span v-if="row.ownerName">{{ row.ownerName }}</span>
            <span v-else class="dim">staff #{{ row.ownerId }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('plot.allowedCrops')" min-width="200">
          <template #default="{ row }">
            <template v-if="allowedCropsOf(row).length">
              <el-tag
                v-for="id in allowedCropsOf(row)"
                :key="id"
                size="small"
                type="info"
                style="margin: 0 4px 2px 0"
              >
                {{ cropNameById(id) }}
              </el-tag>
            </template>
            <span v-else class="dim small">{{ t('plot.allowedCropsAny') }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="currentCropName" :label="t('plot.currentCrop')" min-width="120">
          <template #default="{ row }">
            <el-tag v-if="row.currentCropName" size="small" type="success">{{ row.currentCropName }}</el-tag>
            <span v-else class="dim">{{ t('plot.idle') }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.status')" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" :label="t('common.updatedAt')" width="160" />
        <el-table-column :label="t('common.actions')" width="280" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="onEdit(row)">{{ t('common.edit') }}</el-button>
            <el-dropdown trigger="click" @command="(s) => onChangeStatus(row, s)">
              <el-button link type="warning" size="small">
                {{ t('plot.changeStatus') }} <el-icon><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="row.status !== 'active'"   command="active">{{ t('plot.statusActive') }}</el-dropdown-item>
                  <el-dropdown-item v-if="row.status !== 'inactive'" command="inactive">{{ t('plot.statusInactive') }}</el-dropdown-item>
                  <el-dropdown-item v-if="row.status !== 'fallow'"   command="fallow">{{ t('plot.statusFallow') }}</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-button link type="danger" size="small" @click="onDelete(row)">{{ t('common.delete') }}</el-button>
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
      :title="editing ? t('plot.edit') : t('plot.new')"
      width="640px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item :label="t('plot.code')" prop="code">
          <el-input v-model="form.code" maxlength="32" placeholder="P-006" :disabled="!!editing" />
        </el-form-item>
        <el-form-item :label="t('plot.name')" prop="name">
          <el-input v-model="form.name" maxlength="64" :placeholder="t('plot.namePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('plot.areaMu')" prop="areaMu">
          <el-input-number v-model="form.areaMu" :min="0.01" :precision="2" :step="0.1" :controls="false" style="width: 160px" />
          <span class="dim" style="margin-left: 8px">mu</span>
        </el-form-item>
        <el-form-item :label="t('plot.location')">
          <el-input v-model="form.location" maxlength="120" :placeholder="t('plot.locationPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('plot.soilType')">
          <el-select v-model="form.soilType" clearable style="width: 100%">
            <el-option :label="t('plot.soilLoam')"   value="loam" />
            <el-option :label="t('plot.soilSand')"   value="sand" />
            <el-option :label="t('plot.soilClay')"   value="clay" />
            <el-option :label="t('plot.soilSaline')" value="saline" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('plot.irrigation')">
          <el-select v-model="form.irrigation" clearable style="width: 100%">
            <el-option :label="t('plot.irrigationDrip')"   value="drip" />
            <el-option :label="t('plot.irrigationSpray')"  value="spray" />
            <el-option :label="t('plot.irrigationFurrow')" value="furrow" />
            <el-option :label="t('plot.irrigationRainfed')" value="rainfed" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('plot.owner')" prop="ownerId">
          <el-input-number v-model="form.ownerId" :min="1" :step="1" :controls="false" style="width: 160px" />
          <span class="dim small" style="margin-left: 8px">staff_id ({{ t('plot.ownerHint') }})</span>
        </el-form-item>
        <el-form-item :label="t('plot.allowedCrops')">
          <el-select
            v-model="form.allowedCrops"
            multiple
            filterable
            collapse-tags
            collapse-tags-tooltip
            :placeholder="t('plot.allowedCropsPlaceholder')"
            style="width: 100%"
          >
            <el-option v-for="c in cropOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
          <div class="form-hint">{{ t('plot.allowedCropsHint') }}</div>
        </el-form-item>

        <el-form-item v-if="editing" :label="t('plot.currentCrop')">
          <el-tag v-if="currentCropDisplay" size="small" type="success">{{ currentCropDisplay }}</el-tag>
          <span v-else class="dim">{{ t('plot.idle') }}</span>
          <div class="form-hint">{{ t('plot.currentCropHint') }}</div>
        </el-form-item>
        <el-form-item :label="t('common.status')">
          <el-radio-group v-model="form.status">
            <el-radio-button label="active">{{ t('plot.statusActive') }}</el-radio-button>
            <el-radio-button label="inactive">{{ t('plot.statusInactive') }}</el-radio-button>
            <el-radio-button label="fallow">{{ t('plot.statusFallow') }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="t('common.remark')">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="255" />
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
  ArrowDown,
} from '@element-plus/icons-vue'
import {
  listPlots,
  getPlot,
  createPlot,
  updatePlot,
  changePlotStatus,
  deletePlot,
} from '@/api/plot'
import { listCrops } from '@/api/crop'
import { useAuthStore } from '@/stores/auth'

const { t } = useI18n()
const auth = useAuthStore()

const STATUS_TAG = { active: 'success', inactive: 'info', fallow: 'warning' }
function statusTag(v) { return STATUS_TAG[v] || 'info' }
function statusLabel(v) { return t(`plot.status${v ? v.charAt(0).toUpperCase() + v.slice(1) : 'Active'}`, v) }

// ----- 列表 -----
const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ code: '', name: '', status: '', page: 1, size: 20 })

async function reload(page) {
  if (page) query.page = page
  loading.value = true
  try {
    const data = await listPlots(query)
    list.value = data.list || []
    total.value = data.total || 0
  } finally { loading.value = false }
}
function resetQuery() {
  query.code = ''; query.name = ''; query.status = ''
  reload(1)
}

// ----- 作物下拉 (allowedCrops 多选) -----
const cropOptions = ref([])
const cropNameMap = computed(() => {
  const m = {}
  for (const c of cropOptions.value) m[c.id] = c.name
  return m
})
function cropNameById(id) { return cropNameMap.value[id] || `#${id}` }

/** 解析 PlotVO.allowedCropsJson 为 Array<number> */
function allowedCropsOf(row) {
  if (!row || !row.allowedCropsJson) return []
  try {
    const parsed = JSON.parse(row.allowedCropsJson)
    return Array.isArray(parsed) ? parsed : []
  } catch { return [] }
}

async function loadCrops() {
  try {
    const data = await listCrops({ page: 1, size: 200 })
    cropOptions.value = data.list || []
  } catch { cropOptions.value = [] }
}

onMounted(async () => {
  await Promise.all([reload(1), loadCrops()])
})

// ----- 新建 / 编辑 -----
const dialogVisible = ref(false)
const editing = ref(null)
const formRef = ref(null)
const saving = ref(false)
const currentCropDisplay = ref('')

const emptyForm = () => ({
  code: '',
  name: '',
  areaMu: 1,
  location: '',
  soilType: 'loam',
  irrigation: 'drip',
  ownerId: auth.userId || 1,
  allowedCrops: [],
  status: 'active',
  remark: '',
})
const form = reactive(emptyForm())

const rules = computed(() => ({
  code:    [{ required: true, message: t('valid.required', { field: t('plot.code') }), trigger: 'blur' }],
  name:    [{ required: true, message: t('valid.required', { field: t('plot.name') }), trigger: 'blur' }],
  areaMu:  [{ required: true, message: t('valid.required', { field: t('plot.areaMu') }), trigger: 'change' }],
  ownerId: [{ required: true, message: t('valid.required', { field: t('plot.owner') }), trigger: 'change' }],
}))

function onCreate() {
  editing.value = null
  currentCropDisplay.value = ''
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}

function onEdit(row) {
  editing.value = row.id
  currentCropDisplay.value = row.currentCropName || ''
  Object.assign(form, emptyForm(), {
    code: row.code,
    name: row.name,
    areaMu: row.areaMu,
    // PlotVO 不带 location/soilType/irrigation/remark/allowedCrops，需要拉详情
    ownerId: row.ownerId,
    status: row.status,
  })
  // 异步加载详情补齐 form
  loadDetail(row.id)
  dialogVisible.value = true
}

async function loadDetail(id) {
  try {
    const d = await getPlot(id)
    if (d) {
      form.location    = d.location || ''
      form.soilType    = d.soilType || 'loam'
      form.irrigation  = d.irrigation || 'drip'
      form.allowedCrops = Array.isArray(d.allowedCrops) ? d.allowedCrops : []
      form.remark      = d.remark || ''
    }
  } catch {}
}

async function onSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (editing.value) {
      await updatePlot(editing.value, form)
      ElMessage.success(t('common.updateSuccess'))
    } else {
      await createPlot(form)
      ElMessage.success(t('common.createSuccess'))
    }
    dialogVisible.value = false
    reload()
  } catch {} finally { saving.value = false }
}

async function onChangeStatus(row, status) {
  const label = statusLabel(status)
  await ElMessageBox.confirm(
    t('plot.confirmChangeStatus', { name: row.name, action: label }),
    t('common.tip'), { type: 'warning' }
  ).catch(() => Promise.reject('cancel'))
  try {
    await changePlotStatus(row.id, status)
    ElMessage.success(t('common.updateSuccess'))
    reload()
  } catch (e) { if (e === 'cancel') return }
}

async function onDelete(row) {
  await ElMessageBox.confirm(
    t('plot.confirmDelete', { name: row.name }),
    t('common.tip'), { type: 'warning' }
  ).catch(() => Promise.reject('cancel'))
  try {
    await deletePlot(row.id)
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

.plot-code {
  font-family: 'Consolas', monospace;
  background: #f5f7fa;
  padding: 2px 8px;
  border-radius: 4px;
  color: #1f7a35; font-size: 12px; font-weight: 600;
}
.cid { font-family: 'Consolas', monospace; color: #909399; font-size: 12px; }
.dim { color: #909399; }
.small { font-size: 11px; }
.form-hint {
  font-size: 11px; color: #909399; line-height: 1.4; margin-top: 2px;
}
</style>
