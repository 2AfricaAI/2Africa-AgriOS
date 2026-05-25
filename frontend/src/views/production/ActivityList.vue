<template>
  <div class="page">
    <!-- 过滤器 -->
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="计划">
          <el-select v-model="query.planId" placeholder="全部" clearable filterable style="width: 200px">
            <el-option
              v-for="p in plans"
              :key="p.id"
              :label="`${p.code} · ${p.plotName} · ${p.cropName}`"
              :value="p.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="query.activityType" placeholder="全部" clearable style="width: 140px">
            <el-option
              v-for="t in TYPE_OPTIONS"
              :key="t.value"
              :label="t.label"
              :value="t.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="→"
            start-placeholder="起始"
            end-placeholder="止"
            value-format="YYYY-MM-DD"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item label="审核">
          <el-select v-model="query.auditStatus" placeholder="全部" clearable style="width: 130px">
            <el-option
              v-for="a in AUDIT_OPTIONS"
              :key="a.value"
              :label="a.label"
              :value="a.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="SearchIcon" @click="reload(1)">查询</el-button>
          <el-button :icon="RefreshIcon" @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never" class="table-card">
      <div class="toolbar">
        <el-button type="primary" :icon="PlusIcon" @click="onCreate">新建农事记录</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border stripe row-key="id">
        <el-table-column prop="occurDate" label="发生日期" width="110" />
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.activityType)" size="small" effect="dark">
              {{ typeLabel(row.activityType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="关联" min-width="220">
          <template #default="{ row }">
            <div class="dim" style="margin-bottom: 2px">{{ row.planCode }}</div>
            <div>
              <el-tag size="small">{{ row.plotName }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作人" width="110">
          <template #default="{ row }">
            <span v-if="row.operatorName">{{ row.operatorName }}</span>
            <span v-else class="dim">#{{ row.operatorId }}</span>
          </template>
        </el-table-column>
        <el-table-column label="照片" width="160">
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
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        <el-table-column label="审核" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="auditTag(row.auditStatus)" size="small">
              {{ auditLabel(row.auditStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="onEdit(row)">编辑</el-button>
            <el-dropdown @command="(s) => onAudit(row, s)" style="margin: 0 6px">
              <el-button link type="warning" size="small">
                审核<el-icon class="el-icon--right"><ArrowDown /></el-icon>
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
            <el-popconfirm title="确认删除该农事记录?" @confirm="onDelete(row)">
              <template #reference>
                <el-button link type="danger" size="small">删除</el-button>
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
      :title="editing ? '编辑农事记录' : '新建农事记录'"
      width="640"
      destroy-on-close
      @closed="onDialogClosed"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="关联计划" prop="planId">
          <el-select v-model="form.planId" placeholder="选择种植计划" filterable style="width: 100%">
            <el-option
              v-for="p in plans"
              :key="p.id"
              :label="`${p.code} · ${p.plotName} · ${p.cropName}`"
              :value="p.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="发生日期" prop="occurDate">
          <el-date-picker
            v-model="form.occurDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="活动类型" prop="activityType">
          <el-select v-model="form.activityType" placeholder="请选择" style="width: 100%">
            <el-option
              v-for="t in TYPE_OPTIONS"
              :key="t.value"
              :label="t.label"
              :value="t.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="GPS 坐标">
          <el-input v-model="form.locationGps" placeholder="-1.2864, 36.8172  (可选)" maxlength="64" />
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>

        <el-form-item label="照片">
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
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
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

// ============================================================
// 字典
// ============================================================
const TYPE_OPTIONS = [
  { value: 'sow',       label: 'Sowing',      tag: 'success' },
  { value: 'fertilize', label: 'Fertilizing', tag: 'warning' },
  { value: 'spray',     label: 'Spraying',    tag: 'danger' },
  { value: 'weed',      label: 'Weeding',     tag: 'info' },
  { value: 'water',     label: 'Watering',    tag: 'primary' },
  { value: 'prune',     label: 'Pruning',     tag: 'info' },
  { value: 'other',     label: 'Other',       tag: 'info' },
]
const TYPE_MAP = Object.fromEntries(TYPE_OPTIONS.map(t => [t.value, t]))
function typeLabel(v) { return TYPE_MAP[v]?.label || v }
function typeTag(v)   { return TYPE_MAP[v]?.tag || 'info' }

const AUDIT_OPTIONS = [
  { value: 'pending',  label: '待审核', tag: 'info' },
  { value: 'approved', label: '已通过', tag: 'success' },
  { value: 'rejected', label: '已驳回', tag: 'danger' },
]
const AUDIT_MAP = Object.fromEntries(AUDIT_OPTIONS.map(a => [a.value, a]))
function auditLabel(v) { return AUDIT_MAP[v]?.label || v }
function auditTag(v)   { return AUDIT_MAP[v]?.tag || 'info' }

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
})
const form = reactive(emptyForm())

const rules = {
  planId:       [{ required: true, message: '请选择种植计划', trigger: 'change' }],
  activityType: [{ required: true, message: '请选择活动类型', trigger: 'change' }],
  occurDate:    [{ required: true, message: '请选择发生日期', trigger: 'change' }],
}

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
      ElMessage.success('修改成功')
    } else {
      await createActivity(payload)
      ElMessage.success('创建成功')
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
    const { value } = await ElMessageBox.prompt('请填写驳回原因', '驳回审核', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputPlaceholder: '说明驳回原因, 可空',
    }).catch(() => ({ value: undefined }))
    if (value === undefined) return
    remark = value
  }
  try {
    await auditActivity(row.id, newStatus, remark)
    ElMessage.success(`已${auditLabel(newStatus)}`)
    reload()
  } catch {}
}

async function onDelete(row) {
  try {
    await deleteActivity(row.id)
    ElMessage.success('已删除')
    reload()
  } catch {}
}
</script>

<style scoped>
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
