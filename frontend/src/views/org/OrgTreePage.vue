<template>
  <!--
    Sprint 51 Day 4 -- Organization tree management.
    Three columns: tree (left), node detail (center), members + tags (right).
  -->
  <div class="org-page" v-loading="loading">
    <!-- ============== Left: el-tree ============== -->
    <aside class="rail">
      <div class="rail-head">
        <h3 class="rail-title">{{ t('org.treeTitle') }}</h3>
        <el-button
          size="small"
          type="primary"
          plain
          :icon="PlusIcon"
          @click="openCreate(null)"
        >{{ t('org.newRoot') }}</el-button>
      </div>
      <div class="rail-filter">
        <el-input
          v-model="filterText"
          :placeholder="t('org.filterPlaceholder')"
          clearable
          size="small"
        />
        <el-checkbox v-model="includeInactive" @change="reload" size="default">
          {{ t('org.showInactive') }}
        </el-checkbox>
      </div>
      <el-tree
        ref="treeRef"
        class="rail-tree"
        :data="treeData"
        :props="{ label: 'name', children: 'children' }"
        node-key="id"
        :filter-node-method="filterNode"
        :expand-on-click-node="false"
        :default-expand-all="true"
        :highlight-current="true"
        @node-click="onNodeClick"
      >
        <template #default="{ data }">
          <div class="tree-node" :class="{ inactive: !data.active }">
            <span class="tree-type-chip" :class="`chip-${data.type.toLowerCase()}`">
              {{ data.type }}
            </span>
            <span class="tree-name">{{ data.name }}</span>
            <span v-if="!data.active" class="tree-inactive-tag">{{ t('org.inactive') }}</span>
          </div>
        </template>
      </el-tree>
    </aside>

    <!-- ============== Center: node detail ============== -->
    <section class="detail">
      <template v-if="!selected">
        <el-empty :description="t('org.selectHint')" />
      </template>
      <template v-else>
        <div class="detail-head">
          <div>
            <h3 class="detail-title">{{ selected.name }}</h3>
            <div class="detail-sub">
              <el-tag size="small" :type="typeTagType(selected.type)" effect="plain">
                {{ selected.type }}
              </el-tag>
              <span class="detail-code">{{ selected.code }}</span>
            </div>
          </div>
          <div class="detail-actions">
            <el-button
              v-if="!isPhysical(selected.type) && selected.type !== 'GROUP'"
              size="small"
              type="primary"
              plain
              :icon="PlusIcon"
              @click="openCreate(selected.id)"
            >{{ t('org.addChild') }}</el-button>
            <el-button
              size="small"
              :type="selected.active ? 'warning' : 'success'"
              plain
              :icon="selected.active ? PauseIcon : VideoPlayIcon"
              @click="toggleActive"
            >
              {{ selected.active ? t('org.deactivate') : t('org.activate') }}
            </el-button>
            <el-button
              v-if="!isPhysical(selected.type) && selected.type !== 'GROUP'"
              size="small"
              type="danger"
              plain
              :icon="DeleteIcon"
              @click="confirmDelete"
            >{{ t('common.delete') }}</el-button>
          </div>
        </div>

        <el-form
          ref="detailFormRef"
          :model="detailForm"
          label-width="120px"
          label-position="left"
          class="detail-form"
        >
          <el-form-item :label="t('org.f.name')">
            <el-input v-model="detailForm.name" />
          </el-form-item>
          <el-form-item :label="t('org.f.location')">
            <el-input v-model="detailForm.location" :placeholder="t('org.f.locationPh')" />
          </el-form-item>
          <el-form-item :label="t('org.f.costCenter')">
            <el-input v-model="detailForm.costCenter" />
          </el-form-item>
          <el-form-item :label="t('org.f.manager')">
            <el-select
              v-model="detailForm.managerId"
              filterable
              clearable
              :placeholder="t('org.f.managerPh')"
            >
              <el-option
                v-for="u in userOptions"
                :key="u.id"
                :label="`${u.nickname || u.username} (${u.username})`"
                :value="u.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('org.f.sortNo')">
            <el-input-number v-model="detailForm.sortNo" :min="0" :max="9999" />
          </el-form-item>
          <el-form-item :label="t('org.f.description')">
            <el-input
              v-model="detailForm.description"
              type="textarea"
              :rows="3"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="saveDetail">{{ t('common.save') }}</el-button>
            <el-button @click="resetDetailForm">{{ t('common.cancel') }}</el-button>
          </el-form-item>
        </el-form>
      </template>
    </section>

    <!-- ============== Right: members + tags ============== -->
    <aside class="side" v-if="selected">
      <div class="side-section">
        <div class="side-head">
          <h4>{{ t('org.membersTitle') }}</h4>
          <el-button
            size="small"
            plain
            :icon="PlusIcon"
            @click="openMemberAdd"
          >{{ t('org.addMember') }}</el-button>
        </div>
        <div v-if="!members.length" class="side-empty">
          {{ t('org.noMembers') }}
        </div>
        <ul v-else class="member-list">
          <li v-for="m in members" :key="m.id" class="member-row">
            <div>
              <div class="member-name">
                {{ userLabel(m.userId) }}
                <el-tag v-if="m.isPrimary" size="small" effect="plain" type="success">primary</el-tag>
                <el-tag v-if="m.isManager" size="small" effect="plain" type="warning">manager</el-tag>
              </div>
              <div class="member-meta">
                {{ m.position || '—' }} · {{ m.effectiveFrom }}
                <span v-if="m.effectiveTo"> → {{ m.effectiveTo }}</span>
              </div>
            </div>
            <el-button
              v-if="!m.effectiveTo"
              size="small"
              text
              type="danger"
              @click="closeMember(m)"
            >{{ t('org.closeMember') }}</el-button>
          </li>
        </ul>
      </div>

      <div class="side-section">
        <div class="side-head">
          <h4>{{ t('org.tagsTitle') }}</h4>
        </div>
        <div class="tag-list">
          <el-tag
            v-for="tag in nodeTags"
            :key="tag.id"
            closable
            size="small"
            class="tag-chip"
            @close="detachTagFromNode(tag.id)"
          >
            {{ tag.code }}
          </el-tag>
          <el-select
            v-model="newTagId"
            size="small"
            filterable
            clearable
            :placeholder="t('org.attachTag')"
            style="width: 200px;"
            @change="attachTagToNode"
          >
            <el-option
              v-for="tag in availableTags"
              :key="tag.id"
              :label="tag.code"
              :value="tag.id"
            />
          </el-select>
        </div>
      </div>
    </aside>

    <!-- ============== Create node dialog ============== -->
    <el-dialog
      v-model="createDlg.open"
      :title="t('org.dlgCreateTitle')"
      width="520px"
    >
      <el-form :model="createDlg.form" label-width="110px" label-position="left">
        <el-form-item :label="t('org.f.parent')">
          <el-select v-model="createDlg.form.parentId" filterable :placeholder="t('org.f.parentPh')">
            <el-option v-for="n in allNodes" :key="n.id"
                       :label="`${n.name} [${n.type}]`" :value="n.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('org.f.type')">
          <el-select v-model="createDlg.form.type">
            <el-option v-for="t in typeOptions" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('org.f.code')">
          <el-input v-model="createDlg.form.code" :placeholder="t('org.f.codePh')" />
        </el-form-item>
        <el-form-item :label="t('org.f.name')">
          <el-input v-model="createDlg.form.name" />
        </el-form-item>
        <el-form-item :label="t('org.f.location')">
          <el-input v-model="createDlg.form.location" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDlg.open = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="createDlg.loading" @click="submitCreate">
          {{ t('common.save') }}
        </el-button>
      </template>
    </el-dialog>

    <!-- ============== Add member dialog ============== -->
    <el-dialog
      v-model="memberDlg.open"
      :title="t('org.dlgMemberTitle')"
      width="480px"
    >
      <el-form :model="memberDlg.form" label-width="110px" label-position="left">
        <el-form-item :label="t('org.f.user')">
          <el-select v-model="memberDlg.form.userId" filterable :placeholder="t('org.f.userPh')">
            <el-option
              v-for="u in userOptions"
              :key="u.id"
              :label="`${u.nickname || u.username} (${u.username})`"
              :value="u.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('org.f.position')">
          <el-input v-model="memberDlg.form.position" />
        </el-form-item>
        <el-form-item :label="t('org.f.from')">
          <el-date-picker v-model="memberDlg.form.effectiveFrom" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label=" ">
          <el-checkbox v-model="memberDlg.form.isPrimary">{{ t('org.f.primary') }}</el-checkbox>
          <el-checkbox v-model="memberDlg.form.isManager">{{ t('org.f.manager') }}</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="memberDlg.open = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="memberDlg.loading" @click="submitMember">
          {{ t('common.save') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus as PlusIcon,
  Delete as DeleteIcon,
  VideoPause as PauseIcon,
  VideoPlay as VideoPlayIcon,
} from '@element-plus/icons-vue'

import {
  getOrgTree, listOrgNodes,
  createOrgNode, updateOrgNode, deleteOrgNode, setOrgNodeActive,
  listMembershipsByNode, assignMembership, closeMembership,
  listOrgTags, listTagsForNode, attachTag, detachTag,
} from '@/api/org'
import { listUsers } from '@/api/sysUser'

const { t } = useI18n()

// -------- state --------
const loading = ref(false)
const treeRef = ref(null)
const treeData = ref([])
const allNodes = ref([])         // flat list, used by parent picker
const filterText = ref('')
const includeInactive = ref(false)
const selected = ref(null)
const detailForm = ref({ name: '', location: '', costCenter: '', managerId: null, sortNo: 0, description: '' })

const members = ref([])
const tagsAll = ref([])
const nodeTagIds = ref([])
const newTagId = ref(null)

const userOptions = ref([])
const userIndex = ref({})

// -------- dialogs --------
const createDlg = ref({ open: false, loading: false, form: blankCreate() })
const memberDlg = ref({ open: false, loading: false, form: blankMember() })

function blankCreate() {
  return { parentId: null, type: 'DEPT', code: '', name: '', location: '' }
}
function blankMember() {
  return {
    userId: null, position: '',
    effectiveFrom: new Date().toISOString().slice(0, 10),
    isPrimary: false, isManager: false,
  }
}

const typeOptions = [
  'DEPT', 'TEAM', 'PROJECT', 'FARM', 'PACKHOUSE', 'PROCESSING', 'WAREHOUSE',
]

const PHYSICAL_TYPES = ['FARM', 'PACKHOUSE', 'PROCESSING', 'WAREHOUSE']
function isPhysical(type) { return PHYSICAL_TYPES.includes(type) }
function typeTagType(type) {
  if (isPhysical(type)) return 'success'
  if (type === 'GROUP') return 'warning'
  return 'info'
}

const nodeTags = computed(() =>
  nodeTagIds.value
    .map(id => tagsAll.value.find(t => t.id === id))
    .filter(Boolean)
)
const availableTags = computed(() =>
  tagsAll.value.filter(t => !nodeTagIds.value.includes(t.id))
)

// -------- data flow --------
async function reload() {
  loading.value = true
  try {
    const [treeResp, flatResp, tagsResp, usersResp] = await Promise.all([
      getOrgTree(includeInactive.value),
      listOrgNodes(includeInactive.value),
      listOrgTags(null, false),
      listUsers({ pageSize: 200 }),
    ])
    treeData.value = treeResp || []
    allNodes.value = flatResp || []
    tagsAll.value = tagsResp || []
    const users = usersResp?.rows || usersResp || []
    userOptions.value = users
    userIndex.value = Object.fromEntries(users.map(u => [u.id, u]))
  } catch (err) {
    ElMessage.error(err?.message || t('org.loadFailed'))
  } finally {
    loading.value = false
  }
}

function onNodeClick(data) {
  selected.value = data
  resetDetailForm()
  loadNodeAux(data.id)
}

function resetDetailForm() {
  if (!selected.value) return
  detailForm.value = {
    name: selected.value.name || '',
    location: selected.value.location || '',
    costCenter: selected.value.costCenter || '',
    managerId: selected.value.managerId || null,
    sortNo: selected.value.sortNo || 0,
    description: selected.value.description || '',
  }
}

async function loadNodeAux(nodeId) {
  try {
    const [m, tagIds] = await Promise.all([
      listMembershipsByNode(nodeId, true),
      listTagsForNode(nodeId),
    ])
    members.value = m || []
    nodeTagIds.value = tagIds || []
  } catch (err) {
    ElMessage.warning(err?.message || t('org.auxFailed'))
  }
}

async function saveDetail() {
  if (!selected.value) return
  try {
    const updated = await updateOrgNode(selected.value.id, detailForm.value)
    ElMessage.success(t('common.saved'))
    Object.assign(selected.value, updated)
    await reload()
  } catch (err) {
    ElMessage.error(err?.message || t('common.saveFailed'))
  }
}

async function toggleActive() {
  if (!selected.value) return
  try {
    const updated = await setOrgNodeActive(selected.value.id, !selected.value.active)
    selected.value.active = updated.active
    ElMessage.success(t('common.saved'))
    await reload()
  } catch (err) {
    ElMessage.error(err?.message || t('common.saveFailed'))
  }
}

async function confirmDelete() {
  if (!selected.value) return
  try {
    await ElMessageBox.confirm(
      t('org.deleteConfirm', { name: selected.value.name }),
      t('common.confirm'),
      { type: 'warning' },
    )
  } catch { return }
  try {
    await deleteOrgNode(selected.value.id)
    ElMessage.success(t('common.deleted'))
    selected.value = null
    await reload()
  } catch (err) {
    ElMessage.error(err?.message || t('common.deleteFailed'))
  }
}

// -------- create dialog --------
function openCreate(parentId) {
  createDlg.value.form = blankCreate()
  createDlg.value.form.parentId = parentId
  createDlg.value.open = true
}
async function submitCreate() {
  createDlg.value.loading = true
  try {
    await createOrgNode(createDlg.value.form)
    ElMessage.success(t('common.saved'))
    createDlg.value.open = false
    await reload()
  } catch (err) {
    ElMessage.error(err?.message || t('common.saveFailed'))
  } finally {
    createDlg.value.loading = false
  }
}

// -------- members --------
function openMemberAdd() {
  memberDlg.value.form = blankMember()
  memberDlg.value.open = true
}
async function submitMember() {
  memberDlg.value.loading = true
  try {
    await assignMembership({
      ...memberDlg.value.form,
      nodeId: selected.value.id,
      isPrimary: memberDlg.value.form.isPrimary ? 1 : 0,
      isManager: memberDlg.value.form.isManager ? 1 : 0,
    })
    ElMessage.success(t('common.saved'))
    memberDlg.value.open = false
    await loadNodeAux(selected.value.id)
  } catch (err) {
    ElMessage.error(err?.message || t('common.saveFailed'))
  } finally {
    memberDlg.value.loading = false
  }
}
async function closeMember(m) {
  try {
    await closeMembership(m.id, new Date().toISOString().slice(0, 10))
    ElMessage.success(t('common.saved'))
    await loadNodeAux(selected.value.id)
  } catch (err) {
    ElMessage.error(err?.message || t('common.saveFailed'))
  }
}
function userLabel(uid) {
  const u = userIndex.value[uid]
  return u ? (u.nickname || u.username) : `#${uid}`
}

// -------- tags --------
async function attachTagToNode(tagId) {
  if (!tagId || !selected.value) return
  try {
    await attachTag(tagId, selected.value.id)
    newTagId.value = null
    nodeTagIds.value.push(tagId)
    ElMessage.success(t('common.saved'))
  } catch (err) {
    ElMessage.error(err?.message || t('common.saveFailed'))
  }
}
async function detachTagFromNode(tagId) {
  if (!selected.value) return
  try {
    await detachTag(tagId, selected.value.id)
    nodeTagIds.value = nodeTagIds.value.filter(id => id !== tagId)
    ElMessage.success(t('common.saved'))
  } catch (err) {
    ElMessage.error(err?.message || t('common.saveFailed'))
  }
}

// -------- filter --------
function filterNode(value, data) {
  if (!value) return true
  return (data.name || '').toLowerCase().includes(value.toLowerCase())
      || (data.code || '').toLowerCase().includes(value.toLowerCase())
}
watch(filterText, v => treeRef.value?.filter(v))

onMounted(reload)
</script>

<style scoped>
.org-page {
  display: grid;
  grid-template-columns: 320px 1fr 320px;
  gap: 12px;
  height: calc(100vh - 100px);
}

/* ----- left rail ----- */
.rail {
  background: #fff;
  border: 1px solid #e6ece9;
  border-radius: 8px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow-y: auto;
}
.rail-head { display: flex; align-items: center; justify-content: space-between; }
.rail-title { margin: 0; font-size: 14px; color: #0f3a26; }
.rail-filter { display: flex; gap: 8px; align-items: center; }
.rail-tree { font-size: 13px; }
.tree-node {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
}
.tree-type-chip {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 3px;
  background: #eef2f0;
  color: #5b6b62;
  font-weight: 600;
}
.chip-group { background: #fff3cd; color: #856404; }
.chip-farm { background: #d1e7dd; color: #155724; }
.chip-packhouse, .chip-processing, .chip-warehouse { background: #cfe2ff; color: #084298; }
.chip-dept { background: #e2e3e5; color: #41464b; }
.tree-name { color: #1c2e25; }
.tree-inactive-tag { color: #8a9690; font-size: 11px; margin-left: 4px; }
.tree-node.inactive .tree-name { color: #b9c4be; }

/* ----- center detail ----- */
.detail {
  background: #fff;
  border: 1px solid #e6ece9;
  border-radius: 8px;
  padding: 16px 20px;
  overflow-y: auto;
}
.detail-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 16px;
  gap: 12px;
}
.detail-title { margin: 0 0 4px; color: #0f3a26; }
.detail-sub { display: flex; gap: 8px; align-items: center; }
.detail-code { font-family: 'SF Mono', Menlo, monospace; color: #5b6b62; font-size: 12px; }
.detail-actions { display: flex; gap: 8px; flex-wrap: wrap; }
.detail-form { max-width: 560px; margin-top: 8px; }

/* ----- right side ----- */
.side {
  background: #fff;
  border: 1px solid #e6ece9;
  border-radius: 8px;
  padding: 14px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.side-section { display: flex; flex-direction: column; gap: 8px; }
.side-head { display: flex; align-items: center; justify-content: space-between; }
.side-head h4 { margin: 0; color: #0f3a26; font-size: 13px; }
.side-empty { color: #8a9690; font-size: 12px; }
.member-list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 8px; }
.member-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 8px;
  background: #f8faf9;
  border-radius: 6px;
  font-size: 12px;
}
.member-name { display: flex; gap: 4px; align-items: center; font-weight: 600; color: #1c2e25; }
.member-meta { color: #5b6b62; font-size: 11px; margin-top: 2px; }
.tag-list { display: flex; flex-wrap: wrap; gap: 6px; align-items: center; }
.tag-chip { font-size: 11px; }
</style>
