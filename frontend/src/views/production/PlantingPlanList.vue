<template>
  <div class="page">
    <!-- 过滤器 -->
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="地块">
          <el-select v-model="query.plotId" placeholder="全部" clearable filterable style="width: 180px">
            <el-option
              v-for="p in plots"
              :key="p.id"
              :label="`${p.name} (${p.code})`"
              :value="p.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="作物">
          <el-select v-model="query.cropId" placeholder="全部" clearable filterable style="width: 180px">
            <el-option
              v-for="c in crops"
              :key="c.id"
              :label="`${c.name} (${c.code})`"
              :value="c.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px">
            <el-option
              v-for="s in STATUS_OPTIONS"
              :key="s.value"
              :label="s.label"
              :value="s.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="编码">
          <el-input v-model="query.code" placeholder="模糊查询" clearable style="width: 160px" />
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
        <el-button type="primary" :icon="PlusIcon" @click="onCreate">新建种植计划</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border stripe row-key="id">
        <el-table-column prop="code" label="计划编码" width="140" />
        <el-table-column label="地块" min-width="160">
          <template #default="{ row }">
            <el-tag size="small">{{ row.plotName }}</el-tag>
            <span class="dim">({{ row.plotCode }})</span>
          </template>
        </el-table-column>
        <el-table-column label="作物 / 品种" min-width="180">
          <template #default="{ row }">
            <div>
              <el-tag size="small" type="primary">{{ row.cropName }}</el-tag>
              <span class="dim">{{ row.cropCode }}</span>
            </div>
            <div v-if="row.varietyId" style="margin-top: 4px">
              <el-tag size="small" type="info">{{ row.varietyName }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="面积(亩)" width="100" align="right">
          <template #default="{ row }">{{ Number(row.areaMu).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="计划周期" width="200">
          <template #default="{ row }">
            <div>{{ row.planStartDate }}</div>
            <div class="dim">↓ {{ row.planHarvestDate }}</div>
          </template>
        </el-table-column>
        <el-table-column label="目标产量(kg)" width="120" align="right">
          <template #default="{ row }">
            <span v-if="row.targetYieldKg == null" class="dim">-</span>
            <span v-else>{{ Number(row.targetYieldKg).toLocaleString() }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small" effect="dark">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="onEdit(row)">编辑</el-button>
            <el-dropdown @command="(s) => onChangeStatus(row, s)" style="margin: 0 6px">
              <el-button link type="warning" size="small">
                改状态<el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    v-for="s in STATUS_OPTIONS"
                    :key="s.value"
                    :command="s.value"
                    :disabled="s.value === row.status"
                  >
                    <el-tag :type="s.tag" size="small" style="margin-right: 6px">
                      {{ s.label }}
                    </el-tag>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-popconfirm
              title="确认删除该计划?"
              @confirm="onDelete(row)"
            >
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
      :title="editing ? '编辑种植计划' : '新建种植计划'"
      width="640"
      destroy-on-close
      @closed="onDialogClosed"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="计划编码" prop="code">
          <el-input v-model="form.code" placeholder="例: PL-26-0001" maxlength="32" />
          <div class="hint">建议格式 PL-{两位年份}-{4位序号},如 PL-26-0001</div>
        </el-form-item>

        <el-form-item label="地块" prop="plotId">
          <el-select v-model="form.plotId" placeholder="选择地块" filterable style="width: 100%">
            <el-option
              v-for="p in plots"
              :key="p.id"
              :label="`${p.name} (${p.code})  ·  ${p.areaMu || '-'} 亩`"
              :value="p.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="作物" prop="cropId">
          <el-select
            v-model="form.cropId"
            placeholder="选择作物"
            filterable
            style="width: 100%"
            @change="onCropChange"
          >
            <el-option
              v-for="c in crops"
              :key="c.id"
              :label="`${c.name} (${c.code})`"
              :value="c.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="品种">
          <el-select
            v-model="form.varietyId"
            :placeholder="form.cropId ? '可选 - 该作物下的品种' : '请先选择作物'"
            clearable
            filterable
            :disabled="!form.cropId"
            style="width: 100%"
          >
            <el-option
              v-for="v in filteredVarieties"
              :key="v.id"
              :label="`${v.name} (${v.code})`"
              :value="v.id"
            />
          </el-select>
          <div v-if="form.cropId && filteredVarieties.length === 0" class="hint">
            该作物下还没有品种,可以留空,或先去"主数据 - 品种"补
          </div>
        </el-form-item>

        <el-form-item label="种植面积" prop="areaMu">
          <el-input-number
            v-model="form.areaMu"
            :min="0.01"
            :precision="2"
            :step="0.5"
            controls-position="right"
            style="width: 100%"
          />
          <span class="unit">亩</span>
        </el-form-item>

        <el-form-item label="计划周期" prop="planDates">
          <el-date-picker
            v-model="planDateRange"
            type="daterange"
            range-separator="→"
            start-placeholder="计划起始"
            end-placeholder="计划采收"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="目标产量">
          <el-input-number
            v-model="form.targetYieldKg"
            :min="0"
            :precision="2"
            :step="100"
            controls-position="right"
            style="width: 100%"
          />
          <span class="unit">kg</span>
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="255" show-word-limit />
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
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Search as SearchIcon,
  Refresh as RefreshIcon,
  Plus as PlusIcon,
  ArrowDown,
} from '@element-plus/icons-vue'
import {
  listPlantingPlans,
  createPlantingPlan,
  updatePlantingPlan,
  changePlantingPlanStatus,
  deletePlantingPlan,
} from '@/api/plantingPlan'
import { listPlots } from '@/api/plot'
import { listCrops } from '@/api/crop'
import { listVarieties } from '@/api/variety'

// ============================================================
// 状态字典
// ============================================================
const STATUS_OPTIONS = [
  { value: 'draft',       label: '草稿',   tag: 'info' },
  { value: 'planned',     label: '已排期', tag: 'primary' },
  { value: 'in_progress', label: '种植中', tag: 'warning' },
  { value: 'harvested',   label: '已采收', tag: 'success' },
  { value: 'completed',   label: '已完成', tag: 'success' },
  { value: 'cancelled',   label: '已取消', tag: 'danger' },
]
const STATUS_MAP = Object.fromEntries(STATUS_OPTIONS.map(s => [s.value, s]))
function statusLabel(v) { return STATUS_MAP[v]?.label || v }
function statusTag(v) { return STATUS_MAP[v]?.tag || 'info' }

// ============================================================
// 关联数据 (地块/作物/品种) - 一次拉够,前端复用
// ============================================================
const plots = ref([])
const crops = ref([])
const varieties = ref([])

async function loadRelated() {
  const [pData, cData, vData] = await Promise.all([
    listPlots({ page: 1, size: 500 }),
    listCrops({ page: 1, size: 500 }),
    listVarieties({ page: 1, size: 1000 }),
  ])
  plots.value = pData.list
  crops.value = cData.list
  varieties.value = vData.list
}

const filteredVarieties = computed(() => {
  if (!form.cropId) return []
  return varieties.value.filter(v => v.cropId === form.cropId)
})

// ============================================================
// 列表
// ============================================================
const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const query = reactive({ plotId: null, cropId: null, status: '', code: '' })

async function reload(toPage) {
  if (toPage) page.value = toPage
  loading.value = true
  try {
    const data = await listPlantingPlans({
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
  query.plotId = null
  query.cropId = null
  query.status = ''
  query.code = ''
  reload(1)
}

function onPageChange(p) { page.value = p; reload() }
function onSizeChange(s) { pageSize.value = s; reload(1) }

onMounted(async () => {
  await loadRelated()
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
  plotId: null,
  cropId: null,
  varietyId: null,
  areaMu: null,
  planStartDate: null,
  planHarvestDate: null,
  targetYieldKg: null,
  remark: '',
})
const form = reactive(emptyForm())

// 日期范围控件 ↔ 后端两个独立字段的双向同步
const planDateRange = ref(null)
watch(planDateRange, (v) => {
  if (Array.isArray(v) && v.length === 2) {
    form.planStartDate = v[0]
    form.planHarvestDate = v[1]
  } else {
    form.planStartDate = null
    form.planHarvestDate = null
  }
})

const rules = {
  code: [
    { required: true, message: '请输入计划编码', trigger: 'blur' },
    { max: 32, message: '编码长度 ≤ 32', trigger: 'blur' },
  ],
  plotId: [{ required: true, message: '请选择地块', trigger: 'change' }],
  cropId: [{ required: true, message: '请选择作物', trigger: 'change' }],
  areaMu: [{ required: true, message: '请输入种植面积', trigger: 'change' }],
  planDates: [
    {
      validator: (_r, _v, cb) => {
        if (!form.planStartDate || !form.planHarvestDate) {
          cb(new Error('请选择计划周期'))
        } else {
          cb()
        }
      },
      trigger: 'change',
    },
  ],
}

// 选作物时,如果原品种不属于新作物,清空品种
function onCropChange() {
  if (form.varietyId) {
    const v = varieties.value.find(x => x.id === form.varietyId)
    if (!v || v.cropId !== form.cropId) {
      form.varietyId = null
    }
  }
}

function onCreate() {
  editing.value = null
  Object.assign(form, emptyForm())
  planDateRange.value = null
  dialogVisible.value = true
}

function onEdit(row) {
  editing.value = row.id
  Object.assign(form, {
    code: row.code,
    plotId: row.plotId,
    cropId: row.cropId,
    varietyId: row.varietyId,
    areaMu: Number(row.areaMu),
    planStartDate: row.planStartDate,
    planHarvestDate: row.planHarvestDate,
    targetYieldKg: row.targetYieldKg != null ? Number(row.targetYieldKg) : null,
    remark: row.remark || '',
  })
  planDateRange.value = [row.planStartDate, row.planHarvestDate]
  dialogVisible.value = true
}

function onDialogClosed() {
  editing.value = null
  Object.assign(form, emptyForm())
  planDateRange.value = null
  formRef.value?.clearValidate()
}

async function onSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    if (editing.value) {
      await updatePlantingPlan(editing.value, form)
      ElMessage.success('修改成功')
    } else {
      await createPlantingPlan(form)
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
// 状态切换 / 删除
// ============================================================
async function onChangeStatus(row, newStatus) {
  if (newStatus === row.status) return
  try {
    await changePlantingPlanStatus(row.id, newStatus)
    ElMessage.success(`状态已改为「${statusLabel(newStatus)}」`)
    reload()
  } catch {}
}

async function onDelete(row) {
  try {
    await deletePlantingPlan(row.id)
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
.dim { color: #909399; font-size: 12px; margin-left: 4px; }
.hint { font-size: 12px; color: #909399; margin-top: 4px; }
.unit { margin-left: 8px; color: #606266; font-size: 13px; }
</style>
