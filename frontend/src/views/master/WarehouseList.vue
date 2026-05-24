<template>
  <div class="page">
    <!-- 过滤器 -->
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="类型">
          <el-select v-model="query.type" placeholder="全部" clearable style="width: 140px">
            <el-option
              v-for="t in TYPE_OPTIONS"
              :key="t.value"
              :label="t.label"
              :value="t.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="父节点">
          <el-select
            v-model="query.parentId"
            placeholder="全部"
            clearable
            filterable
            style="width: 200px"
          >
            <el-option label="(顶层节点)" :value="0" />
            <el-option
              v-for="w in allWarehouses"
              :key="w.id"
              :label="`${w.name} (${w.code})`"
              :value="w.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="编码">
          <el-input v-model="query.code" placeholder="模糊查询" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="query.name" placeholder="模糊查询" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 110px">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="SearchIcon" @click="reload(1)">查询</el-button>
          <el-button :icon="RefreshIcon" @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 工具栏 + 表格 -->
    <el-card shadow="never" class="table-card">
      <div class="toolbar">
        <el-button type="primary" :icon="PlusIcon" @click="onCreate">新建仓库/库位</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border stripe row-key="id">
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="code" label="编码" width="120" />
        <el-table-column prop="name" label="名称" min-width="140" />
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="typeTagColor(row.type)" size="small">
              {{ typeLabel(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="父节点" min-width="160">
          <template #default="{ row }">
            <span v-if="!row.parentId || row.parentId === 0" style="color: #909399">(顶层)</span>
            <span v-else>{{ warehouseName(row.parentId) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="capacityKg" label="容量(kg)" width="120" align="right">
          <template #default="{ row }">
            <span v-if="row.capacityKg == null" style="color: #c0c4cc">-</span>
            <span v-else>{{ Number(row.capacityKg).toLocaleString() }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="onEdit(row)">编辑</el-button>
            <el-button
              link
              :type="row.status === 1 ? 'warning' : 'success'"
              size="small"
              @click="onToggleStatus(row)"
            >
              {{ row.status === 1 ? '停用' : '启用' }}
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
      :title="editing ? '编辑仓库/库位' : '新建仓库/库位'"
      width="520"
      destroy-on-close
      @closed="onDialogClosed"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="编码" prop="code">
          <el-input v-model="form.code" placeholder="例: W03" maxlength="32" />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="例: 二号包装仓" maxlength="64" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择" style="width: 100%">
            <el-option
              v-for="t in TYPE_OPTIONS"
              :key="t.value"
              :label="t.label"
              :value="t.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="父节点">
          <el-select
            v-model="form.parentId"
            placeholder="(默认为顶层节点)"
            clearable
            filterable
            style="width: 100%"
          >
            <el-option label="(顶层节点)" :value="0" />
            <el-option
              v-for="w in selectableParents"
              :key="w.id"
              :label="`${w.name} (${w.code})`"
              :value="w.id"
            />
          </el-select>
          <div class="hint">编辑时不能选自己作为父节点</div>
        </el-form-item>
        <el-form-item label="容量(kg)">
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
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
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

// ============================================================
// 类型字典(跟后端 schema 对齐: normal/cold/quarantine)
// ============================================================
const TYPE_OPTIONS = [
  { value: 'normal',     label: '常温',     tag: 'success' },
  { value: 'cold',       label: '冷藏',     tag: 'primary' },
  { value: 'quarantine', label: '隔离/检疫', tag: 'warning' },
]
const TYPE_MAP = Object.fromEntries(TYPE_OPTIONS.map(t => [t.value, t]))
function typeLabel(v) { return TYPE_MAP[v]?.label || v }
function typeTagColor(v) { return TYPE_MAP[v]?.tag || 'info' }

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
const query = reactive({ type: '', parentId: null, code: '', name: '', status: null })

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
  parentId: 0,
  capacityKg: null,
})
const form = reactive(emptyForm())

// 编辑时父节点下拉里要排除"自己"
const selectableParents = computed(() => {
  return allWarehouses.value.filter(w => w.id !== editing.value)
})

const rules = {
  code: [
    { required: true, message: '请输入编码', trigger: 'blur' },
    { max: 32, message: '编码长度 ≤ 32', trigger: 'blur' },
  ],
  name: [
    { required: true, message: '请输入名称', trigger: 'blur' },
    { max: 64, message: '名称长度 ≤ 64', trigger: 'blur' },
  ],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
}

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
      ElMessage.success('修改成功')
    } else {
      await createWarehouse(form)
      ElMessage.success('创建成功')
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
  const action = next === 1 ? '启用' : '停用'
  await ElMessageBox.confirm(`确认${action}仓库「${row.name}」?`, '提示', {
    type: 'warning',
  }).catch(() => Promise.reject('cancel'))
  try {
    await changeWarehouseStatus(row.id, next)
    ElMessage.success(`${action}成功`)
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
