<template>
  <div class="page">
    <el-card shadow="never" class="table-card">
      <div class="toolbar">
        <div>
          <span class="table-title">{{ t('sysRole.title') }}</span>
          <span class="dim small" style="margin-left: 8px">{{ t('sysRole.assignHint') }}</span>
        </div>
        <el-button
          v-if="auth.isSuperAdmin"
          type="primary"
          size="default"
          :icon="PlusIcon"
          @click="openNewRole"
        >
          {{ t('sysRole.newCustom') }}
        </el-button>
      </div>

      <el-table :data="roles" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="code" :label="t('sysRole.code')" width="180">
          <template #default="{ row }"><code class="code">{{ row.code }}</code></template>
        </el-table-column>
        <el-table-column prop="name" :label="t('sysRole.name')" width="180" />
        <el-table-column :label="t('sysRole.type')" width="120" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isBuiltIn === 1" size="small" type="info">{{ t('sysRole.builtIn') }}</el-tag>
            <el-tag v-else size="small" type="success">{{ t('sysRole.custom') }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('sysRole.dataScope')" width="120" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="scopeTag(row.dataScope)">{{ t(`sysRole.scope_${row.dataScope}`, row.dataScope) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" :label="t('common.remark')" min-width="200" show-overflow-tooltip />
        <el-table-column :label="t('common.actions')" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="openAssign(row)">
              {{ t('sysRole.editPermissions') }}
            </el-button>
            <el-button
              v-if="row.isBuiltIn !== 1"
              size="small"
              type="primary"
              link
              @click="openRename(row)"
            >
              {{ t('common.edit') }}
            </el-button>
            <el-button
              v-if="row.isBuiltIn !== 1"
              size="small"
              type="danger"
              link
              @click="onDelete(row)"
            >
              {{ t('common.delete') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- ============================================================ -->
    <!-- Module-access assignment dialog (Stripe-style 11 x 3 matrix) -->
    <!-- ============================================================ -->
    <el-dialog v-model="assignVisible" :title="assignTitle" width="700px" destroy-on-close>
      <div v-loading="assignLoading">
        <div class="copy-toolbar">
          <span class="dim small">{{ t('sysRole.copyFrom') }}:</span>
          <el-button
            v-for="r in builtInRoles"
            :key="r.id"
            size="small"
            @click="copyFromRole(r)"
            :disabled="!editingRole || r.id === editingRole.id"
          >
            {{ r.code }}
          </el-button>
        </div>
        <el-table :data="moduleRows" border size="small" :max-height="420">
          <el-table-column :label="t('sysRole.module')" width="180">
            <template #default="{ row }">
              <span class="module-name">{{ t(`module.${row.module}`, row.module) }}</span>
            </template>
          </el-table-column>
          <el-table-column align="center" width="160">
            <template #header>
              <el-radio v-model="bulkPick" label="none" @change="setAll('none')">
                <span class="dim small">{{ t('access.none') }}</span>
              </el-radio>
            </template>
            <template #default="{ row }">
              <el-radio v-model="access[row.module]" value="none">&nbsp;</el-radio>
            </template>
          </el-table-column>
          <el-table-column align="center" width="160">
            <template #header>
              <el-radio v-model="bulkPick" label="read" @change="setAll('read')">
                <span class="dim small">{{ t('access.read') }}</span>
              </el-radio>
            </template>
            <template #default="{ row }">
              <el-radio v-model="access[row.module]" value="read">&nbsp;</el-radio>
            </template>
          </el-table-column>
          <el-table-column align="center">
            <template #header>
              <el-radio v-model="bulkPick" label="write" @change="setAll('write')">
                <span class="dim small">{{ t('access.write') }}</span>
              </el-radio>
            </template>
            <template #default="{ row }">
              <el-radio v-model="access[row.module]" value="write">&nbsp;</el-radio>
            </template>
          </el-table-column>
        </el-table>
        <p class="dim small foot-note">{{ t('sysRole.affectAllUsersHint') }}</p>
      </div>
      <template #footer>
        <el-button @click="assignVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="saveAccess">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- ============================================================ -->
    <!-- Create / rename custom role dialog                            -->
    <!-- ============================================================ -->
    <el-dialog v-model="roleFormVisible" :title="roleFormTitle" width="520px" destroy-on-close>
      <el-form ref="roleFormRef" :model="roleForm" :rules="roleFormRules" label-width="120px" status-icon>
        <el-form-item :label="t('sysRole.code')" prop="code">
          <el-input
            v-model="roleForm.code"
            placeholder="ACCOUNTANT"
            :disabled="!!editingRole"
            style="text-transform: uppercase"
          />
          <div class="dim small">{{ t('sysRole.codeHint') }}</div>
        </el-form-item>
        <el-form-item :label="t('sysRole.name')" prop="name">
          <el-input v-model="roleForm.name" :placeholder="t('sysRole.nameHint')" />
        </el-form-item>
        <el-form-item :label="t('sysRole.dataScope')" prop="dataScope">
          <el-select v-model="roleForm.dataScope" style="width: 100%">
            <el-option value="self" :label="t('sysRole.scope_self')" />
            <el-option value="group" :label="t('sysRole.scope_group')" />
            <el-option value="all" :label="t('sysRole.scope_all')" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('common.remark')" prop="remark">
          <el-input v-model="roleForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleFormVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="savingRole" @click="saveRoleForm">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus as PlusIcon } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import {
  listRoles, createRole, updateRole, deleteRole,
  listModules, getModuleAccess, setModuleAccess, getRole,
} from '@/api/sysRole'

const { t } = useI18n()
const auth = useAuthStore()

const roles = ref([])
const loading = ref(false)
const modules = ref([])
const builtInRoles = computed(() => roles.value.filter(r => r.isBuiltIn === 1))

function scopeTag(s) { return ({ all: 'danger', group: 'warning', self: 'info' }[s]) || 'info' }

async function refresh() {
  loading.value = true
  try {
    const [roleList, moduleList] = await Promise.all([listRoles(), listModules()])
    roles.value = roleList
    modules.value = moduleList
  } finally { loading.value = false }
}

onMounted(refresh)

// ============================================================
// Module-access assignment dialog
// ============================================================
const assignVisible = ref(false)
const assignLoading = ref(false)
const saving = ref(false)
const editingRole = ref(null)
const access = reactive({})            // { master: 'write', sales: 'read', ... }
const bulkPick = ref('')               // for the column-header "set all to X" radios

const assignTitle = computed(() =>
  editingRole.value ? t('sysRole.editTitle', { role: editingRole.value.code }) : '',
)

const moduleRows = computed(() => modules.value.map(m => ({ module: m })))

async function openAssign(row) {
  editingRole.value = row
  assignVisible.value = true
  assignLoading.value = true
  bulkPick.value = ''
  Object.keys(access).forEach(k => delete access[k])
  try {
    const current = await getModuleAccess(row.id)
    modules.value.forEach(m => { access[m] = current[m] || 'none' })
  } catch {
    ElMessage.error(t('sysRole.loadFailed'))
  } finally {
    assignLoading.value = false
  }
}

function setAll(level) {
  modules.value.forEach(m => { access[m] = level })
}

async function copyFromRole(role) {
  assignLoading.value = true
  try {
    const map = await getModuleAccess(role.id)
    modules.value.forEach(m => { access[m] = map[m] || 'none' })
    ElMessage.success(t('sysRole.copiedFrom', { role: role.code }))
  } finally {
    assignLoading.value = false
  }
}

async function saveAccess() {
  saving.value = true
  try {
    await setModuleAccess(editingRole.value.id, { ...access })
    ElMessage.success(t('sysRole.assignSaved'))
    assignVisible.value = false
  } catch {
    ElMessage.error(t('sysRole.assignFailed'))
  } finally {
    saving.value = false
  }
}

// ============================================================
// Custom role create / rename dialog
// ============================================================
const roleFormVisible = ref(false)
const savingRole = ref(false)
const roleFormRef = ref(null)
const roleForm = reactive({ code: '', name: '', dataScope: 'self', remark: '' })

const roleFormTitle = computed(() =>
  editingRole.value && roleFormVisible.value
    ? t('sysRole.editRoleTitle', { role: editingRole.value.code })
    : t('sysRole.newCustom'),
)

const roleFormRules = {
  code: [
    { required: true, message: t('sysRole.codeRequired'), trigger: 'blur' },
    { pattern: /^[A-Z][A-Z0-9_]{1,31}$/, message: t('sysRole.codePatternHint'), trigger: 'blur' },
  ],
  name: [{ required: true, message: t('sysRole.nameRequired'), trigger: 'blur' }],
}

function openNewRole() {
  editingRole.value = null
  Object.assign(roleForm, { code: '', name: '', dataScope: 'self', remark: '' })
  roleFormVisible.value = true
}

async function openRename(row) {
  editingRole.value = row
  const fresh = await getRole(row.id)
  Object.assign(roleForm, {
    code: fresh.code,
    name: fresh.name,
    dataScope: fresh.dataScope || 'self',
    remark: fresh.remark || '',
  })
  roleFormVisible.value = true
}

async function saveRoleForm() {
  await roleFormRef.value?.validate()
  savingRole.value = true
  try {
    if (editingRole.value) {
      await updateRole(editingRole.value.id, roleForm)
      ElMessage.success(t('sysRole.roleUpdated'))
    } else {
      await createRole(roleForm)
      ElMessage.success(t('sysRole.roleCreated'))
    }
    roleFormVisible.value = false
    await refresh()
  } catch (e) {
    if (e?.message) ElMessage.error(e.message)
  } finally {
    savingRole.value = false
  }
}

async function onDelete(row) {
  await ElMessageBox.confirm(
    t('sysRole.deleteConfirm', { role: row.code }),
    t('common.tip'),
    { type: 'warning' },
  ).catch(() => null)
  try {
    await deleteRole(row.id)
    ElMessage.success(t('sysRole.roleDeleted'))
    await refresh()
  } catch (e) {
    if (e?.message) ElMessage.error(e.message)
  }
}
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.table-card :deep(.el-card__body) { padding: 16px; }
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.table-title { font-size: 14px; font-weight: 600; color: #1f2937; }
.code { font-family: 'Monaco', 'Consolas', monospace; font-size: 12px; background: #f5f7fa; padding: 2px 8px; border-radius: 4px; color: #1f7a35; }
.dim { color: #94a3b8; }
.small { font-size: 12px; }
.module-name { font-weight: 500; }
.copy-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
  padding: 8px 12px;
  background: #f8fafc;
  border-radius: 4px;
  border: 1px solid #e5e7eb;
}
.foot-note { margin-top: 12px; }

/* Make the header radios look like clickable column-headers */
:deep(.el-table th .el-radio) { margin-right: 0; }
:deep(.el-table th .el-radio__label) { padding-left: 6px; font-weight: 500; }
</style>
