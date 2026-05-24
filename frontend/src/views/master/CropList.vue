<template>
  <div class="page">
    <!-- 过滤器 -->
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="编码">
          <el-input v-model="query.code" placeholder="模糊查询" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="query.name" placeholder="模糊查询" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="query.category" placeholder="例: 叶菜/果蔬" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
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
        <el-button type="primary" :icon="PlusIcon" @click="onCreate">新建作物</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border stripe row-key="id">
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="code" label="编码" width="120" />
        <el-table-column prop="name" label="名称" min-width="120" />
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column prop="unit" label="单位" width="80" align="center" />
        <el-table-column prop="cycleDays" label="周期(天)" width="100" align="center" />
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right" align="center">
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
      :title="editing ? '编辑作物' : '新建作物'"
      width="500"
      destroy-on-close
      @closed="onDialogClosed"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="编码" prop="code">
          <el-input v-model="form.code" placeholder="例: CR-005" maxlength="32" />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="例: 辣椒" maxlength="64" />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="form.category" placeholder="例: 叶菜/果蔬/根茎" maxlength="32" />
        </el-form-item>
        <el-form-item label="单位">
          <el-input v-model="form.unit" placeholder="默认 kg" maxlength="8" />
        </el-form-item>
        <el-form-item label="周期(天)">
          <el-input-number v-model="form.cycleDays" :min="0" :max="9999" controls-position="right" />
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search as SearchIcon,
  Refresh as RefreshIcon,
  Plus as PlusIcon,
} from '@element-plus/icons-vue'
import {
  listCrops,
  createCrop,
  updateCrop,
  changeCropStatus,
} from '@/api/crop'

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
  remark: '',
})
const form = reactive(emptyForm())

const rules = {
  code: [
    { required: true, message: '请输入编码', trigger: 'blur' },
    { max: 32, message: '编码长度 ≤ 32', trigger: 'blur' },
  ],
  name: [
    { required: true, message: '请输入名称', trigger: 'blur' },
    { max: 64, message: '名称长度 ≤ 64', trigger: 'blur' },
  ],
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
    category: row.category || '',
    unit: row.unit || 'kg',
    cycleDays: row.cycleDays,
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
      ElMessage.success('修改成功')
    } else {
      await createCrop(form)
      ElMessage.success('创建成功')
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
  const action = next === 1 ? '启用' : '停用'
  await ElMessageBox.confirm(`确认${action}作物「${row.name}」?`, '提示', {
    type: 'warning',
  }).catch(() => Promise.reject('cancel'))
  try {
    await changeCropStatus(row.id, next)
    ElMessage.success(`${action}成功`)
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
