<template>
  <div class="page">
    <!-- ======== Filter bar (with user_type tabs on top) ======== -->
    <el-card shadow="never" class="filter-card">
      <el-tabs v-model="query.userType" @tab-change="reload(1)">
        <el-tab-pane :label="t('sysUser.allUsers')" name="" />
        <el-tab-pane :label="t('userType.STAFF')" name="STAFF" />
        <el-tab-pane :label="t('userType.PARTNER')" name="PARTNER" />
        <el-tab-pane :label="t('userType.CUSTOMER')" name="CUSTOMER" />
      </el-tabs>
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

    <!-- ======== Table ======== -->
    <el-card shadow="never" class="table-card">
      <div class="toolbar">
        <span class="table-title">{{ t('sysUser.title') }}</span>
        <el-dropdown trigger="click" @command="onNewCommand">
          <el-button type="primary" size="default">
            {{ t('sysUser.newUser') }} <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="staff">
                <el-icon><UserFilled /></el-icon>{{ t('sysUser.newStaff') }}
                <span class="cmd-hint">{{ t('sysUser.newStaffHint') }}</span>
              </el-dropdown-item>
              <el-dropdown-item command="partner">
                <el-icon><Connection /></el-icon>{{ t('sysUser.newPartner') }}
                <span class="cmd-hint">{{ t('sysUser.newPartnerHint') }}</span>
              </el-dropdown-item>
              <el-dropdown-item command="customer">
                <el-icon><Shop /></el-icon>{{ t('sysUser.newCustomer') }}
                <span class="cmd-hint">{{ t('sysUser.newCustomerHint') }}</span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>

      <el-table :data="list" v-loading="loading" border stripe row-key="id">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column :label="t('sysUser.type')" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="typeTag(row.userType)">{{ t(`userType.${row.userType || 'STAFF'}`) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="username" :label="t('sysUser.username')" width="140">
          <template #default="{ row }"><code class="code">{{ row.username }}</code></template>
        </el-table-column>
        <el-table-column prop="nickname" :label="t('sysUser.nickname')" width="160" />
        <el-table-column :label="t('sysUser.org')" min-width="180">
          <template #default="{ row }">
            <span v-if="row.orgName" class="dim-strong">{{ row.orgName }}</span>
            <span v-else-if="row.userType === 'CUSTOMER' && row.linkedCustomerId">
              <el-tag size="small" type="info" effect="plain">customer #{{ row.linkedCustomerId }}</el-tag>
            </span>
            <span v-else class="dim">-</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('sysUser.roles')" min-width="220">
          <template #default="{ row }">
            <el-tag v-for="r in row.roles" :key="r.id" size="small" type="primary" effect="plain" style="margin-right:4px">
              {{ r.code }}
            </el-tag>
            <el-tag v-for="s in row.partnerSubtypes || []" :key="`s-${s}`" size="small" type="warning" effect="plain" style="margin-right:4px">
              {{ s }}
            </el-tag>
            <span v-if="!row.roles?.length && !row.partnerSubtypes?.length" class="dim">-</span>
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
            <el-button v-if="row.userType === 'PARTNER'" link size="small" @click="openScopes(row)">{{ t('sysUser.scopes') }}</el-button>
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

    <!-- ====================== STAFF dialog ====================== -->
    <el-dialog v-model="staffDlg" :title="editing ? t('sysUser.editTitle') : t('sysUser.newStaff')" width="600px" @closed="resetStaffForm">
      <el-form ref="staffFormRef" :model="staffForm" :rules="staffRules" label-width="120px">
        <el-form-item :label="t('sysUser.username')" prop="username">
          <el-input v-model="staffForm.username" :disabled="!!editing" />
        </el-form-item>
        <el-form-item :label="t('sysUser.nickname')">
          <el-input v-model="staffForm.nickname" />
        </el-form-item>
        <el-form-item v-if="!editing" :label="t('sysUser.password')" prop="password">
          <el-input v-model="staffForm.password" type="password" show-password />
          <div class="hint">{{ t('sysUser.pwdHint') }}</div>
        </el-form-item>
        <el-form-item :label="t('sysUser.phone')">
          <el-input v-model="staffForm.phone" />
        </el-form-item>
        <el-form-item :label="t('sysUser.email')">
          <el-input v-model="staffForm.email" />
        </el-form-item>
        <el-form-item :label="t('sysUser.roles')" prop="roleIds">
          <el-select v-model="staffForm.roleIds" multiple :placeholder="t('sysUser.placeholderRoles')" style="width: 100%">
            <el-option
              v-for="r in staffRoles"
              :key="r.id"
              :label="`${r.name} (${r.code})`"
              :value="r.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('sysUser.status')">
          <el-select v-model="staffForm.status" style="width: 200px">
            <el-option v-for="s in STATUSES" :key="s" :label="t(`sysUser.st_${s}`)" :value="s" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="staffDlg = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="onSubmitStaff">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- ====================== PARTNER dialog ====================== -->
    <el-dialog v-model="partnerDlg" :title="t('sysUser.newPartner')" width="680px" @closed="resetPartnerForm">
      <el-form ref="partnerFormRef" :model="partnerForm" :rules="partnerRules" label-width="140px">
        <el-form-item :label="t('sysUser.username')" prop="username">
          <el-input v-model="partnerForm.username" placeholder="kephis_agro1" />
        </el-form-item>
        <el-form-item :label="t('sysUser.nickname')" prop="nickname">
          <el-input v-model="partnerForm.nickname" placeholder="James Mwangi" />
        </el-form-item>
        <el-form-item :label="t('sysUser.orgName')" prop="orgName">
          <el-input v-model="partnerForm.orgName" placeholder="KEPHIS / Equity Bank / ..." />
          <div class="hint">{{ t('sysUser.orgNameHint') }}</div>
        </el-form-item>
        <el-form-item :label="t('sysUser.partnerSubtypes')" prop="subtypes">
          <el-select v-model="partnerForm.subtypes" multiple style="width: 100%">
            <el-option
              v-for="s in availableSubtypes"
              :key="s"
              :label="`${s} - ${t(`partnerSubtype.${s}`, '')}`"
              :value="s"
            />
          </el-select>
          <div class="hint">{{ t('sysUser.partnerSubtypesHint') }}</div>
        </el-form-item>
        <el-form-item :label="t('sysUser.password')" prop="password">
          <el-input v-model="partnerForm.password" type="password" show-password />
          <div class="hint">{{ t('sysUser.pwdHint') }}</div>
        </el-form-item>
        <el-form-item :label="t('sysUser.phone')">
          <el-input v-model="partnerForm.phone" />
        </el-form-item>
        <el-form-item :label="t('sysUser.email')">
          <el-input v-model="partnerForm.email" />
        </el-form-item>

        <el-divider content-position="left">{{ t('sysUser.scopes') }}</el-divider>
        <div class="hint" style="margin: 0 0 12px 140px">{{ t('sysUser.scopesHint') }}</div>
        <div v-for="(s, idx) in partnerForm.scopes" :key="idx" class="scope-row">
          <el-select v-model="s.scopeType" style="width: 140px">
            <el-option label="PLOT" value="PLOT" />
            <el-option label="CUSTOMER" value="CUSTOMER" />
            <el-option label="WAREHOUSE" value="WAREHOUSE" />
            <el-option label="DATE_WINDOW" value="DATE_WINDOW" />
          </el-select>
          <el-select
            v-if="s.scopeType === 'PLOT'"
            v-model="s.scopeId"
            filterable
            :placeholder="t('sysUser.pickPlot')"
            style="width: 200px"
          >
            <el-option v-for="p in plots" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
          <el-select
            v-else-if="s.scopeType === 'CUSTOMER'"
            v-model="s.scopeId"
            filterable
            :placeholder="t('sysUser.pickCustomer')"
            style="width: 200px"
          >
            <el-option v-for="c in customers" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
          <span v-else class="dim small" style="width: 200px; text-align: center">-</span>

          <el-date-picker
            v-model="s.validFrom"
            type="date"
            :placeholder="t('sysUser.validFrom')"
            style="width: 150px"
            value-format="YYYY-MM-DD"
          />
          <el-date-picker
            v-model="s.validTo"
            type="date"
            :placeholder="t('sysUser.validTo')"
            style="width: 150px"
            value-format="YYYY-MM-DD"
          />
          <el-button link type="danger" :icon="DeleteIcon" @click="removeScopeRow(idx)" />
        </div>
        <el-button size="small" type="primary" link :icon="PlusIcon" @click="addScopeRow">
          {{ t('sysUser.addScope') }}
        </el-button>
      </el-form>
      <template #footer>
        <el-button @click="partnerDlg = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="onSubmitPartner">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- ====================== CUSTOMER dialog ====================== -->
    <el-dialog v-model="customerDlg" :title="t('sysUser.newCustomer')" width="560px" @closed="resetCustomerForm">
      <el-form ref="customerFormRef" :model="customerForm" :rules="customerRules" label-width="160px">
        <el-form-item :label="t('sysUser.linkedCustomer')" prop="linkedCustomerId">
          <el-select v-model="customerForm.linkedCustomerId" filterable :placeholder="t('sysUser.pickCustomer')" style="width: 100%">
            <el-option v-for="c in customers" :key="c.id" :label="`${c.name} (#${c.id})`" :value="c.id" />
          </el-select>
          <div class="hint">{{ t('sysUser.linkedCustomerHint') }}</div>
        </el-form-item>
        <el-form-item :label="t('sysUser.username')" prop="username">
          <el-input v-model="customerForm.username" placeholder="acme_buyer" />
        </el-form-item>
        <el-form-item :label="t('sysUser.nickname')" prop="nickname">
          <el-input v-model="customerForm.nickname" />
        </el-form-item>
        <el-form-item :label="t('sysUser.password')" prop="password">
          <el-input v-model="customerForm.password" type="password" show-password />
          <div class="hint">{{ t('sysUser.pwdHint') }}</div>
        </el-form-item>
        <el-form-item :label="t('sysUser.phone')">
          <el-input v-model="customerForm.phone" />
        </el-form-item>
        <el-form-item :label="t('sysUser.email')">
          <el-input v-model="customerForm.email" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="customerDlg = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="onSubmitCustomer">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- ====================== Scope-edit dialog ====================== -->
    <el-dialog v-model="scopeDlg" :title="t('sysUser.editScopesTitle', { user: scopeTarget?.username })" width="680px">
      <div v-loading="scopeLoading">
        <div v-for="(s, idx) in scopeRows" :key="idx" class="scope-row">
          <el-select v-model="s.scopeType" style="width: 140px">
            <el-option label="PLOT" value="PLOT" />
            <el-option label="CUSTOMER" value="CUSTOMER" />
            <el-option label="WAREHOUSE" value="WAREHOUSE" />
            <el-option label="DATE_WINDOW" value="DATE_WINDOW" />
          </el-select>
          <el-select v-if="s.scopeType === 'PLOT'" v-model="s.scopeId" filterable style="width: 200px">
            <el-option v-for="p in plots" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
          <el-select v-else-if="s.scopeType === 'CUSTOMER'" v-model="s.scopeId" filterable style="width: 200px">
            <el-option v-for="c in customers" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
          <span v-else class="dim small" style="width: 200px; text-align: center">-</span>
          <el-date-picker v-model="s.validFrom" type="date" style="width: 150px" value-format="YYYY-MM-DD" :placeholder="t('sysUser.validFrom')" />
          <el-date-picker v-model="s.validTo" type="date" style="width: 150px" value-format="YYYY-MM-DD" :placeholder="t('sysUser.validTo')" />
          <el-button link type="danger" :icon="DeleteIcon" @click="scopeRows.splice(idx, 1)" />
        </div>
        <el-button size="small" type="primary" link :icon="PlusIcon" @click="scopeRows.push({ scopeType: 'PLOT', scopeId: null })">
          {{ t('sysUser.addScope') }}
        </el-button>
      </div>
      <template #footer>
        <el-button @click="scopeDlg = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="onSubmitScopes">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- ====================== Reset password dialog ====================== -->
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
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search as SearchIcon, Refresh as RefreshIcon,
  Plus as PlusIcon, Delete as DeleteIcon, ArrowDown,
  UserFilled, Connection, Shop,
} from '@element-plus/icons-vue'
import {
  listUsers, createUser, updateUser, changeUserStatus,
  resetUserPassword, deleteUser,
  listPartnerSubtypes, createPartner, createCustomer,
  getUserScopes, setUserScopes,
} from '@/api/sysUser'
import { listRoles } from '@/api/sysRole'
import { listCustomers } from '@/api/customer'
import { listPlots } from '@/api/plot'

const { t } = useI18n()
const STATUSES = ['active', 'locked', 'disabled']

// ----- Data
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const loading = ref(false)
const query = reactive({ username: '', status: '', userType: '' })

const allRoles = ref([])
// Staff form only offers non-partner / non-customer roles
const partnerRoleCodes = new Set(['AGRONOMIST','GAP_AUDITOR','BANK_OFFICER','LANDLORD','INSURANCE','CUSTOMER_SELF'])
const staffRoles = computed(() => allRoles.value.filter(r => !partnerRoleCodes.has(r.code)))
const availableSubtypes = ref([])
const customers = ref([])
const plots = ref([])

function statusTag(s) { return ({ active: 'success', locked: 'warning', disabled: 'info' }[s]) || 'info' }
function typeTag(t) { return ({ STAFF: 'primary', PARTNER: 'warning', CUSTOMER: 'success' }[t]) || 'info' }

async function reload(toPage) {
  if (toPage) page.value = toPage
  loading.value = true
  try {
    const data = await listUsers({ ...query, page: page.value, size: pageSize.value })
    list.value = data.list
    total.value = data.total
  } finally { loading.value = false }
}
function onReset() {
  query.username = ''; query.status = ''; query.userType = ''
  reload(1)
}
function onPageChange(p) { page.value = p; reload() }
function onSizeChange(s) { pageSize.value = s; reload(1) }

// ============================================================
// Dialog: split-dropdown "New" -> staff/partner/customer
// ============================================================
const staffDlg = ref(false)
const partnerDlg = ref(false)
const customerDlg = ref(false)
const editing = ref(null)
const saving = ref(false)

async function onNewCommand(cmd) {
  editing.value = null
  // Lazy-load options for the dialog being opened
  if (cmd === 'partner') {
    if (!plots.value.length)     plots.value     = await loadAll(listPlots)
    if (!customers.value.length) customers.value = await loadAll(listCustomers)
    if (!availableSubtypes.value.length) availableSubtypes.value = await listPartnerSubtypes()
    partnerDlg.value = true
  } else if (cmd === 'customer') {
    if (!customers.value.length) customers.value = await loadAll(listCustomers)
    customerDlg.value = true
  } else {
    staffDlg.value = true
  }
}

async function loadAll(fn) {
  // helper for "pull every row" — backend pagination is fine for small dicts
  const data = await fn({ page: 1, size: 500 })
  return data.list || data || []
}

// ============================================================
// Staff form
// ============================================================
const staffFormRef = ref(null)
const emptyStaff = () => ({ username: '', nickname: '', password: '', phone: '', email: '', roleIds: [], status: 'active' })
const staffForm = reactive(emptyStaff())
const staffRules = {
  username: [
    { required: true, message: t('valid.required', { field: t('sysUser.username') }) },
    { min: 3, max: 64, message: t('valid.length', { min: 3, max: 64 }) },
  ],
  password: [{ validator: (rule, value, cb) => {
    if (editing.value) return cb()
    if (!value || value.length < 8) return cb(new Error(t('sysUser.pwdHint')))
    cb()
  }}],
  roleIds: [{ validator: (rule, value, cb) => {
    if (!value || value.length === 0) cb(new Error(t('sysUser.atLeastOneRole'))); else cb()
  }}],
}
function resetStaffForm() { Object.assign(staffForm, emptyStaff()); staffFormRef.value?.clearValidate(); editing.value = null }

function openEdit(row) {
  // Re-use staff dialog for editing all 3 types; only STAFF roleIds list shown.
  editing.value = row.id
  Object.assign(staffForm, {
    username: row.username,
    nickname: row.nickname || '',
    password: '',
    phone: row.phone || '',
    email: row.email || '',
    roleIds: (row.roles || []).map(r => r.id),
    status: row.status,
  })
  staffDlg.value = true
}

async function onSubmitStaff() {
  await staffFormRef.value.validate().catch(() => null)
  saving.value = true
  try {
    if (editing.value) {
      await updateUser(editing.value, { ...staffForm })
    } else {
      await createUser({ ...staffForm })
    }
    ElMessage.success(t('common.saveSuccess'))
    staffDlg.value = false
    reload()
  } finally { saving.value = false }
}

// ============================================================
// Partner form
// ============================================================
const partnerFormRef = ref(null)
const emptyPartner = () => ({
  username: '', nickname: '', password: '', phone: '', email: '',
  orgName: '', subtypes: [], scopes: [],
})
const partnerForm = reactive(emptyPartner())
const partnerRules = {
  username: [{ required: true, message: t('valid.required', { field: t('sysUser.username') }) }],
  nickname: [{ required: true, message: t('valid.required', { field: t('sysUser.nickname') }) }],
  orgName:  [{ required: true, message: t('valid.required', { field: t('sysUser.orgName') }) }],
  subtypes: [{ validator: (r, v, cb) => v?.length ? cb() : cb(new Error(t('sysUser.atLeastOneSubtype'))) }],
  password: [{ validator: (r, v, cb) =>
    (!v || v.length < 8) ? cb(new Error(t('sysUser.pwdHint'))) : cb() }],
}
function resetPartnerForm() { Object.assign(partnerForm, emptyPartner()); partnerFormRef.value?.clearValidate() }
function addScopeRow() { partnerForm.scopes.push({ scopeType: 'PLOT', scopeId: null, validFrom: null, validTo: null }) }
function removeScopeRow(idx) { partnerForm.scopes.splice(idx, 1) }

async function onSubmitPartner() {
  await partnerFormRef.value.validate().catch(() => null)
  saving.value = true
  try {
    // Drop the scopeId for DATE_WINDOW rows where the field is hidden
    const scopes = partnerForm.scopes.map(s => ({
      scopeType: s.scopeType,
      scopeId: s.scopeType === 'DATE_WINDOW' ? null : s.scopeId,
      validFrom: s.validFrom || null,
      validTo: s.validTo || null,
    }))
    await createPartner({ ...partnerForm, scopes })
    ElMessage.success(t('common.saveSuccess'))
    partnerDlg.value = false
    reload()
  } finally { saving.value = false }
}

// ============================================================
// Customer form
// ============================================================
const customerFormRef = ref(null)
const emptyCustomer = () => ({
  username: '', nickname: '', password: '', phone: '', email: '',
  linkedCustomerId: null,
})
const customerForm = reactive(emptyCustomer())
const customerRules = {
  linkedCustomerId: [{ required: true, message: t('valid.required', { field: t('sysUser.linkedCustomer') }) }],
  username: [{ required: true, message: t('valid.required', { field: t('sysUser.username') }) }],
  nickname: [{ required: true, message: t('valid.required', { field: t('sysUser.nickname') }) }],
  password: [{ validator: (r, v, cb) =>
    (!v || v.length < 8) ? cb(new Error(t('sysUser.pwdHint'))) : cb() }],
}
function resetCustomerForm() { Object.assign(customerForm, emptyCustomer()); customerFormRef.value?.clearValidate() }

async function onSubmitCustomer() {
  await customerFormRef.value.validate().catch(() => null)
  saving.value = true
  try {
    await createCustomer({ ...customerForm })
    ElMessage.success(t('common.saveSuccess'))
    customerDlg.value = false
    reload()
  } finally { saving.value = false }
}

// ============================================================
// Scope-edit dialog (for existing PARTNER users)
// ============================================================
const scopeDlg = ref(false)
const scopeLoading = ref(false)
const scopeTarget = ref(null)
const scopeRows = ref([])
async function openScopes(row) {
  scopeTarget.value = row
  scopeDlg.value = true
  scopeLoading.value = true
  try {
    if (!plots.value.length)     plots.value     = await loadAll(listPlots)
    if (!customers.value.length) customers.value = await loadAll(listCustomers)
    scopeRows.value = (await getUserScopes(row.id)).map(s => ({
      scopeType: s.scopeType,
      scopeId: s.scopeId,
      validFrom: s.validFrom,
      validTo: s.validTo,
    }))
  } finally { scopeLoading.value = false }
}
async function onSubmitScopes() {
  saving.value = true
  try {
    await setUserScopes(scopeTarget.value.id, scopeRows.value.map(s => ({
      scopeType: s.scopeType,
      scopeId: s.scopeType === 'DATE_WINDOW' ? null : s.scopeId,
      validFrom: s.validFrom || null,
      validTo: s.validTo || null,
    })))
    ElMessage.success(t('common.saveSuccess'))
    scopeDlg.value = false
    reload()
  } finally { saving.value = false }
}

// ============================================================
// Status / reset / delete (reused from Sprint 34)
// ============================================================
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

const resetDlg = ref(false)
const resetSaving = ref(false)
const resetTarget = ref(null)
const resetForm = reactive({ password: '' })
function openResetPwd(row) { resetTarget.value = row; resetForm.password = ''; resetDlg.value = true }
async function onResetSubmit() {
  if (!resetForm.password || resetForm.password.length < 8) {
    ElMessage.warning(t('sysUser.pwdHint')); return
  }
  resetSaving.value = true
  try {
    await resetUserPassword(resetTarget.value.id, resetForm.password)
    ElMessage.success(t('sysUser.resetSuccess'))
    resetDlg.value = false
  } finally { resetSaving.value = false }
}

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

onMounted(async () => {
  allRoles.value = await listRoles()
  reload(1)
})
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.filter-card :deep(.el-card__body),
.table-card :deep(.el-card__body) { padding: 16px; }
.filter-card :deep(.el-tabs__header) { margin-bottom: 16px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.table-title { font-size: 14px; font-weight: 600; color: #1f2937; }
.code { font-family: 'Monaco', 'Consolas', monospace; font-size: 12px; background: #f5f7fa; padding: 2px 8px; border-radius: 4px; color: #1f7a35; }
.dim { color: #94a3b8; }
.dim-strong { color: #475569; font-weight: 500; }
.small { font-size: 12px; }
.pager { margin-top: 14px; justify-content: flex-end; }
.hint { font-size: 12px; color: #94a3b8; margin-top: 4px; }
.cmd-hint { color: #94a3b8; font-size: 11px; margin-left: 8px; }
.scope-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  padding-left: 140px;
}
</style>
