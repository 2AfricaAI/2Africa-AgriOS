<template>
  <div class="page">
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item :label="t('sysUser.username')">
          <el-input v-model="query.username" clearable :placeholder="t('sysUser.placeholderUsername')" style="width: 200px" />
        </el-form-item>
        <el-form-item :label="t('sysUser.status')">
          <el-select v-model="query.status" :placeholder="t('common.all')" clearable style="width: 140px">
            <el-option v-for="s in STATUSES" :key="s" :label="t(`sysUser.st_${s}`)" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="SearchIcon" @click="reload(1)">{{ t('common.search') }}</el-button>
          <el-button :icon="RefreshIcon" @click="onReset">{{ t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <div class="toolbar">
        <span class="table-title">{{ t('sysUser.title') }}</span>
        <el-button type="primary" size="small" @click="openCreate">{{ t('sysUser.newUser') }}</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border stripe row-key="id">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" :label="t('sysUser.username')" width="140">
          <template #default="{ row }"><code class="code">{{ row.username }}</code></template>
        </el-table-column>
        <el-table-column prop="nickname" :label="t('sysUser.nickname')" width="160" />
        <el-table-column prop="phone" :label="t('sysUser.phone')" width="160" />
        <el-table-column prop="email" :label="t('sysUser.email')" min-width="200" show-overflow-tooltip />
        <el-table-column :label="t('sysUser.roles')" min-width="220">
          <template #default="{ row }">
            <el-tag v-for="r in row.roles" :key="r.id" size="small" type="primary" effect="plain" style="margin-right:4px">
              {{ r.name }}
            </el-tag>
            <span v-if="!row.roles?.length" class="dim">-</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('sysUser.status')" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small" effect="dark">{{ t(`sysUser.st_${row.status}`, row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginAt" :label="t('sysUser.lastLogin')" width="170" />
        <el-table-column :label="t('common.actions')" width="280" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">{{ t('common.edit') }}</el-button>
            <el-button link size="small" @click="openResetPwd(row)">{{ t('sysUser.resetPwd') }}</el-button>
            <el-button v-if="row.status === 'active'" link type="warning" size="small" @click="onChangeStatus(row, 'disabled')">{{ t('sysUser.disable') }}</el-button>
            <el-button v-else link type="success" size="small" @click="onChangeStatus(row, 'active')">{{ t('sysUser.enable') }}</el-button>
            <el-button v-if="row.username !== 'admin'" link type="danger" size="small" @click="onDelete(row)">{{ t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pager"
        background
        layout="total, sizes, prev, pager, next, jumper"
        :page-sizes="[10, 20, 50]"
        :total="total"
        :page-size="pageSize"
        :current-page="page"
        @size-change="onSizeChange"
        @current-change="onPageChange"
      />
    </el-card>

    <!-- New / edit dialog -->
    <el-dialog v-model="dlg" :title="editing ? t('sysUser.editTitle') : t('sysUser.newUser')" width="600px" @closed="onClose">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item :label="t('sysUser.username')" prop="username">
          <el-input v-model="form.username" :disabled="!!editing" />
        </el-form-item>
        <el-form-item :label="t('sysUser.nickname')">
          <el-input v-model="form.nickname" />
        </el-form-item>
        <el-form-item v-if="!editing" :label="t('sysUser.password')" prop="password">
          <el-input v-model="form.password" type="password" show-password />
          <div class="hint">{{ t('sysUser.pwdHint') }}</div>
        </el-form-item>
        <el-form-item :label="t('sysUser.phone')">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item :label="t('sysUser.email')">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item :label="t('sysUser.roles')" prop="roleIds">
          <el-select v-model="form.roleIds" multiple :placeholder="t('sysUser.placeholderRoles')" style="width: 100%">
            <el-option v-for="r in roles" :key="r.id" :label="`${r.name} (${r.code})`" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('sysUser.status')">
          <el-select v-model="form.status" style="width: 200px">
            <el-option v-for="s in STATUSES" :key="s" :label="t(`sysUser.st_${s}`)" :value="s" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="onSubmit">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- Reset password dialog -->
    <el-dialog v-model="resetDlg" :title="t('sysUser.resetPwdTitle')" width="460px">
      <el-form label-width="120px">
        <el-form-item :label="t('sysUser.username')">
          <code class="code">{{ resetTarget?.username }}</code>
        </el-form-item>
        <el-form-item :label="t('sysUser.newPwd')">
          <el-input v-model="resetForm.password" type="password" show-password />
          <div class="hint">{{ t('sysUser.pwdHint') }}</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetDlg = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="resetSaving" @click="onResetSubmit">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search as SearchIcon, Refresh as RefreshIcon } from '@element-plus/icons-vue'
import {
  listUsers, createUser, updateUser, changeUserStatus,
  resetUserPassword, deleteUser,
} from '@/api/sysUser'
import { listRoles } from '@/api/sysRole'

const { t } = useI18n()
const STATUSES = ['active', 'locked', 'disabled']

const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const loading = ref(false)
const query = reactive({ username: '', status: '' })

const roles = ref([])
async function loadRoles() { roles.value = await listRoles() }

function statusTag(s) { return ({ active: 'success', locked: 'warning', disabled: 'info' }[s]) || 'info' }

async function reload(toPage) {
  if (toPage) page.value = toPage
  loading.value = true
  try {
    const data = await listUsers({ ...query, page: page.value, size: pageSize.value })
    list.value = data.list
    total.value = data.total
  } finally { loading.value = false }
}
function onReset() { query.username = ''; query.status = ''; reload(1) }
function onPageChange(p) { page.value = p; reload() }
function onSizeChange(s) { pageSize.value = s; reload(1) }

// ----- new/edit dialog -----
const dlg = ref(false)
const editing = ref(null)
const saving = ref(false)
const formRef = ref(null)
const emptyForm = () => ({ username: '', nickname: '', password: '', phone: '', email: '', roleIds: [], status: 'active' })
const form = reactive(emptyForm())

const rules = {
  username: [
    { required: true, message: t('valid.required', { field: t('sysUser.username') }) },
    { min: 3, max: 64, message: t('valid.length', { min: 3, max: 64 }) },
  ],
  password: [{ required: true, validator: (rule, value, cb) => {
    if (editing.value) return cb()
    if (!value || value.length < 8) return cb(new Error(t('sysUser.pwdHint')))
    cb()
  }}],
  roleIds: [{ validator: (rule, value, cb) => {
    if (!value || value.length === 0) cb(new Error(t('sysUser.atLeastOneRole'))); else cb()
  }}],
}

function openCreate() {
  editing.value = null
  Object.assign(form, emptyForm())
  dlg.value = true
}
function openEdit(row) {
  editing.value = row.id
  Object.assign(form, {
    username: row.username,
    nickname: row.nickname || '',
    password: '',
    phone: row.phone || '',
    email: row.email || '',
    roleIds: (row.roles || []).map(r => r.id),
    status: row.status,
  })
  dlg.value = true
}
function onClose() { editing.value = null; Object.assign(form, emptyForm()); formRef.value?.clearValidate() }

async function onSubmit() {
  await formRef.value.validate().catch(() => null)
  saving.value = true
  try {
    if (editing.value) {
      await updateUser(editing.value, { ...form })
    } else {
      await createUser({ ...form })
    }
    ElMessage.success(t('common.saveSuccess'))
    dlg.value = false
    reload()
  } finally { saving.value = false }
}

// ----- status toggle -----
async function onChangeStatus(row, status) {
  await ElMessageBox.confirm(
    t('sysUser.confirmStatus', { username: row.username, status: t(`sysUser.st_${status}`) }),
    t('common.tip'),
    { type: 'warning' },
  ).catch(() => null)
  await changeUserStatus(row.id, status)
  ElMessage.success(t('common.saveSuccess'))
  reload()
}

// ----- reset password -----
const resetDlg = ref(false)
const resetSaving = ref(false)
const resetTarget = ref(null)
const resetForm = reactive({ password: '' })
function openResetPwd(row) {
  resetTarget.value = row
  resetForm.password = ''
  resetDlg.value = true
}
async function onResetSubmit() {
  if (!resetForm.password || resetForm.password.length < 8) {
    ElMessage.warning(t('sysUser.pwdHint'))
    return
  }
  resetSaving.value = true
  try {
    await resetUserPassword(resetTarget.value.id, resetForm.password)
    ElMessage.success(t('sysUser.resetSuccess'))
    resetDlg.value = false
  } finally { resetSaving.value = false }
}

// ----- delete -----
async function onDelete(row) {
  await ElMessageBox.confirm(
    t('sysUser.confirmDelete', { username: row.username }),
    t('common.tip'),
    { type: 'warning' },
  ).catch(() => null)
  await deleteUser(row.id)
  ElMessage.success(t('common.deleteSuccess'))
  reload()
}

onMounted(async () => { await loadRoles(); reload(1) })
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.filter-card :deep(.el-card__body),
.table-card :deep(.el-card__body) { padding: 16px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.table-title { font-size: 14px; font-weight: 600; color: #1f2937; }
.code { font-family: 'Monaco', 'Consolas', monospace; font-size: 12px; background: #f5f7fa; padding: 2px 8px; border-radius: 4px; color: #1f7a35; }
.dim { color: #94a3b8; }
.pager { margin-top: 14px; justify-content: flex-end; }
.hint { font-size: 12px; color: #94a3b8; margin-top: 4px; }
</style>
