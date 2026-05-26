<template>
  <div class="page">
    <!-- 过滤器 -->
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item :label="t('activity.plan')">
          <el-select v-model="query.planId" :placeholder="t('common.all')" clearable filterable style="width: 200px">
            <el-option
              v-for="p in plans"
              :key="p.id"
              :label="`${p.code} · ${p.plotName} · ${p.cropName}`"
              :value="p.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('activity.type')">
          <el-select v-model="query.activityType" :placeholder="t('common.all')" clearable style="width: 140px">
            <el-option
              v-for="opt in TYPE_OPTIONS"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('activity.date')">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            :range-separator="t('date.rangeSep')"
            :start-placeholder="t('date.start')"
            :end-placeholder="t('date.end')"
            value-format="YYYY-MM-DD"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item :label="t('activity.audit')">
          <el-select v-model="query.auditStatus" :placeholder="t('common.all')" clearable style="width: 130px">
            <el-option
              v-for="a in AUDIT_OPTIONS"
              :key="a.value"
              :label="a.label"
              :value="a.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="SearchIcon" @click="reload(1)">{{ t('common.search') }}</el-button>
          <el-button :icon="RefreshIcon" @click="onReset">{{ t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never" class="table-card">
      <div class="toolbar">
        <el-button type="primary" :icon="PlusIcon" @click="onCreate">{{ t('activity.new') }}</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border stripe row-key="id">
        <el-table-column prop="occurDate" :label="t('activity.occurDate')" width="110" />
        <el-table-column :label="t('activity.type')" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.activityType)" size="small" effect="dark">
              {{ typeLabel(row.activityType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('activity.relation')" min-width="220">
          <template #default="{ row }">
            <div class="dim" style="margin-bottom: 2px">{{ row.planCode }}</div>
            <div>
              <el-tag size="small">{{ row.plotName }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="t('activity.operator')" width="110">
          <template #default="{ row }">
            <span v-if="row.operatorName">{{ row.operatorName }}</span>
            <span v-else class="dim">#{{ row.operatorId }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('activity.photos')" width="160">
          <template #default="{ row }">
            <div v-if="row.photos?.length" class="photo-row">
              <el-image
                v-for="(p, i) in row.photos.slice(0, 3)"
                :key="p.id"
                :src="p.downloadUrl"
                :preview-src-list="row.photos.map(x => x.downloadUrl)"
                :initial-index="i"
                fit="cover"
                class="thumb"
                preview-teleported
              />
              <span v-if="row.photos.length > 3" class="more">+{{ row.photos.length - 3 }}</span>
            </div>
            <span v-else class="dim">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" :label="t('common.remark')" min-width="180" show-overflow-tooltip />
        <el-table-column :label="t('activity.audit')" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="auditTag(row.auditStatus)" size="small">
              {{ auditLabel(row.auditStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.actions')" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="onEdit(row)">{{ t('common.edit') }}</el-button>
            <el-dropdown @command="(s) => onAudit(row, s)" style="margin: 0 6px">
              <el-button link type="warning" size="small">
                {{ t('activity.audit') }}<el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    v-for="a in AUDIT_OPTIONS"
                    :key="a.value"
                    :command="a.value"
                    :disabled="a.value === row.auditStatus"
                  >
                    <el-tag :type="a.tag" size="small" style="margin-right: 6px">{{ a.label }}</el-tag>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-popconfirm :title="t('activity.confirmDelete')" @confirm="onDelete(row)">
              <template #reference>
                <el-button link type="danger" size="small">{{ t('common.delete') }}</el-button>
              </template>
            </el-popconfirm>
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
      :title="editing ? t('activity.editTitle') : t('activity.createTitle')"
      width="640"
      destroy-on-close
      @closed="onDialogClosed"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item :label="t('activity.plan')" prop="planId">
          <el-select v-model="form.planId" :placeholder="t('activity.planPlaceholder')" filterable style="width: 100%">
            <el-option
              v-for="p in plans"
              :key="p.id"
              :label="`${p.code} · ${p.plotName} · ${p.cropName}`"
              :value="p.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item :label="t('activity.occurDate')" prop="occurDate">
          <el-date-picker
            v-model="form.occurDate"
            type="date"
            value-format="YYYY-MM-DD"
            :placeholder="t('activity.datePlaceholder')"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item :label="t('activity.type')" prop="activityType">
          <el-select v-model="form.activityType" :placeholder="t('common.selectPlaceholder')" style="width: 100%">
            <el-option
              v-for="opt in TYPE_OPTIONS"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item :label="t('activity.gps')">
          <el-input v-model="form.locationGps" :placeholder="t('activity.gpsPlaceholder')" maxlength="64" />
        </el-form-item>

        <el-form-item :label="t('common.remark')">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>

        <!-- Sprint 11 - 成本字段 (V2.0 Phase 2 P&L) -->
        <el-divider content-position="left">
          <span class="cost-divider">💰 {{ t('activity.costsBlock') }}</span>
        </el-divider>

        <div class="cost-row">
          <el-form-item :label="t('activity.laborCost')" class="cost-field">
            <el-input-number v-model="form.laborCost" :min="0" :precision="2" :controls="false" style="width: 100%" />
          </el-form-item>
          <el-form-item :label="t('activity.fertilizerCost')" class="cost-field">
            <el-input-number v-model="form.fertilizerCost" :min="0" :precision="2" :controls="false" style="width: 100%" />
          </el-form-item>
        </div>
        <div class="cost-row">
          <el-form-item :label="t('activity.waterCost')" class="cost-field">
            <el-input-number v-model="form.waterCost" :min="0" :precision="2" :controls="false" style="width: 100%" />
          </el-form-item>
          <el-form-item :label="t('activity.electricityCost')" class="cost-field">
            <el-input-number v-model="form.electricityCost" :min="0" :precision="2" :controls="false" style="width: 100%" />
          </el-form-item>
        </div>
        <div class="cost-row">
          <el-form-item :label="t('activity.otherCost')" class="cost-field">
            <el-input-number v-model="form.otherCost" :min="0" :precision="2" :controls="false" style="width: 100%" />
          </el-form-item>
          <el-form-item :label="t('order.currency')" class="cost-field">
            <el-select v-model="form.costCurrency" style="width: 100%">
              <el-option label="KES" value="KES" />
              <el-option label="USD" value="USD" />
              <el-option label="EUR" value="EUR" />
            </el-select>
          </el-form-item>
        </div>

        <el-form-item :label="t('activity.photos')">
          <FileUploader
            v-model="photos"
            biz-type="activity_photo"
            accept="image/*"
            :limit="9"
            :max-size-mb="10"
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
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search as SearchIcon,
  Refresh as RefreshIcon,
  Plus as PlusIcon,
  ArrowDown,
} from '@element-plus/icons-vue'
import {
  listActivities,
  createActivity,
  updateActivity,
  auditActivity,
  deleteActivity,
} from '@/api/activity'
import { listPlantingPlans } from '@/api/plantingPlan'
import FileUploader from '@/components/FileUploader.vue'

const { t } = useI18n()

// ============================================================
// 字典
// ============================================================
const TYPE_OPTIONS = computed(() => [
  { value: 'sow',       label: t('activity.typeSeed'),      tag: 'success' },
  { value: 'fertilize', label: t('activity.typeFertilize'), tag: 'warning' },
  { value: 'spray',     label: t('activity.typeSpray'),     tag: 'danger' },
  { value: 'weed',      label: t('activity.typeWeed'),      tag: 'info' },
  { value: 'water',     label: t('activity.typeWater'),     tag: 'primary' },
  { value: 'prune',     label: t('activity.typePrune'),     tag: 'info' },
  { value: 'other',     label: t('activity.typeOther'),     tag: 'info' },
])
const TYPE_MAP = computed(() => Object.fromEntries(TYPE_OPTIONS.value.map(opt => [opt.value, opt])))
function typeLabel(v) { return TYPE_MAP.value[v]?.label || v }
function typeTag(v)   { return TYPE_MAP.value[v]?.tag || 'info' }

const AUDIT_OPTIONS = computed(() => [
  { value: 'pending',  label: t('activity.auditPending'),  tag: 'info' },
  { value: 'approved', label: t('activity.auditApproved'), tag: 'success' },
  { value: 'rejected', label: t('activity.auditRejected'), tag: 'danger' },
])
const AUDIT_MAP = computed(() => Object.fromEntries(AUDIT_OPTIONS.value.map(a => [a.value, a])))
function auditLabel(v) { return AUDIT_MAP.value[v]?.label || v }
function auditTag(v)   { return AUDIT_MAP.value[v]?.tag || 'info' }

// ============================================================
// 关联数据
// ============================================================
const plans = ref([])
async function loadPlans() {
  const data = await listPlantingPlans({ page: 1, size: 500 })
  plans.value = data.list
}

// ============================================================
// 列表
// ============================================================
const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const query = reactive({
  planId: null,
  activityType: '',
  auditStatus: '',
  dateFrom: null,
  dateTo: null,
})

// 日期范围控件 ↔ query 双向
const dateRange = ref(null)
watch(dateRange, (v) => {
  if (Array.isArray(v) && v.length === 2) {
    query.dateFrom = v[0]
    query.dateTo = v[1]
  } else {
    query.dateFrom = null
    query.dateTo = null
  }
})

async function reload(toPage) {
  if (toPage) page.value = toPage
  loading.value = true
  try {
    const data = await listActivities({
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
  query.planId = null
  query.activityType = ''
  query.auditStatus = ''
  query.dateFrom = null
  query.dateTo = null
  dateRange.value = null
  reload(1)
}

function onPageChange(p) { page.value = p; reload() }
function onSizeChange(s) { pageSize.value = s; reload(1) }

onMounted(async () => {
  await loadPlans()
  await reload(1)
})

// ============================================================
// 对话框
// ============================================================
const dialogVisible = ref(false)
const editing = ref(null)
const saving = ref(false)
const formRef = ref(null)

const photos = ref([])   // FileUploader v-model: 完整 FileVO 数组

const emptyForm = () => ({
  planId: null,
  activityType: '',
  occurDate: null,
  locationGps: '',
  remark: '',
  // Sprint 11 cost fields (V2.0 Phase 2 P&L)
  laborCost: 0,
  waterCost: 0,
  electricityCost: 0,
  fertilizerCost: 0,
  otherCost: 0,
  costCurrency: 'KES',
})
const form = reactive(emptyForm())

const rules = computed(() => ({
  planId:       [{ required: true, message: t('activity.pickPlan'), trigger: 'change' }],
  activityType: [{ required: true, message: t('activity.pickType'), trigger: 'change' }],
  occurDate:    [{ required: true, message: t('activity.pickOccurDate'), trigger: 'change' }],
}))

function onCreate() {
  editing.value = null
  Object.assign(form, emptyForm())
  photos.value = []
  dialogVisible.value = true
}

function onEdit(row) {
  editing.value = row.id
  Object.assign(form, {
    planId: row.planId,
    activityType: row.activityType,
    occurDate: row.occurDate,
    locationGps: row.locationGps || '',
    remark: row.remark || '',
    laborCost: Number(row.laborCost) || 0,
    waterCost: Number(row.waterCost) || 0,
    electricityCost: Number(row.electricityCost) || 0,
    fertilizerCost: Number(row.fertilizerCost) || 0,
    otherCost: Number(row.otherCost) || 0,
    costCurrency: row.costCurrency || 'KES',
  })
  // 后端返回的 photos 是 FileVO[],可以直接给 FileUploader
  photos.value = row.photos || []
  dialogVisible.value = true
}

function onDialogClosed() {
  editing.value = null
  Object.assign(form, emptyForm())
  photos.value = []
  formRef.value?.clearValidate()
}

async function onSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    // 提取 file IDs (后端只要 IDs)
    const payload = {
      ...form,
      photos: photos.value.map(p => p.id).filter(Boolean),
      clientUuid: editing.value ? null : crypto.randomUUID(),
    }
    if (editing.value) {
      await updateActivity(editing.value, payload)
      ElMessage.success(t('common.updateSuccess'))
    } else {
      await createActivity(payload)
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
// 审核 / 删除
// ============================================================
async function onAudit(row, newStatus) {
  if (newStatus === row.auditStatus) return
  let remark = ''
  if (newStatus === 'rejected') {
    const { value } = await ElMessageBox.prompt(t('activity.rejectPrompt'), t('activity.rejectTitle'), {
      confirmButtonText: t('common.ok'),
      cancelButtonText: t('common.cancel'),
      inputType: 'textarea',
      inputPlaceholder: t('activity.rejectPlaceholder'),
    }).catch(() => ({ value: undefined }))
    if (value === undefined) return
    remark = value
  }
  try {
    await auditActivity(row.id, newStatus, remark)
    ElMessage.success(t('activity.auditDone', { label: auditLabel(newStatus) }))
    reload()
  } catch {}
}

async function onDelete(row) {
  try {
    await deleteActivity(row.id)
    ElMessage.success(t('common.deleteSuccess'))
    reload()
  } catch {}
}
</script>

<style scoped>
.cost-divider { font-size: 13px; color: #1f7a35; font-weight: 600; }
.cost-row { display: flex; gap: 14px; }
.cost-row .cost-field { flex: 1; }

.page { display: flex; flex-direction: column; gap: 16px; }
.filter-card :deep(.el-card__body),
.table-card :deep(.el-card__body) { padding: 16px; }
.toolbar { margin-bottom: 12px; display: flex; justify-content: flex-end; }
.pager { margin-top: 14px; justify-content: flex-end; }
.dim { color: #909399; font-size: 12px; }

.photo-row {
  display: flex;
  align-items: center;
  gap: 4px;
}
.thumb {
  width: 36px;
  height: 36px;
  border-radius: 4px;
  cursor: pointer;
  border: 1px solid #ebeef5;
}
.more {
  font-size: 11px;
  color: #909399;
  margin-left: 4px;
}
</style>
