<template>
  <div class="inbox-list-page">
    <!-- ===================== Header ===================== -->
    <header class="page-head">
      <div>
        <h2 class="page-title">{{ t('service.inboxesTitle') }}</h2>
        <p class="page-sub">{{ t('service.inboxesSub') }}</p>
      </div>
      <div class="head-actions">
        <el-button text @click="reload">
          <el-icon><RefreshIcon /></el-icon>
          {{ t('service.reload') }}
        </el-button>
        <el-button type="primary" size="default" @click="openWizard">
          <el-icon><PlusIcon /></el-icon>
          {{ t('service.addInbox') }}
        </el-button>
      </div>
    </header>

    <!-- ===================== Inbox grid ===================== -->
    <el-skeleton v-if="loading" :rows="6" animated />

    <div v-else-if="inboxes.length === 0" class="empty">
      <el-empty :description="t('service.noInboxes')">
        <el-button type="primary" @click="openWizard">
          {{ t('service.addInbox') }}
        </el-button>
      </el-empty>
    </el-skeleton>

    <ul v-else class="grid">
      <li v-for="ib in inboxes" :key="ib.id" class="card">
        <div class="card-head">
          <div class="card-icon">{{ channelIcon(ib.channelType) }}</div>
          <div class="card-meta">
            <div class="card-name">{{ ib.name }}</div>
            <div class="card-channel">{{ humanChannel(ib.channelType) }}</div>
          </div>
          <el-dropdown trigger="click">
            <el-button text size="small" class="card-menu-btn">
              <el-icon><MoreFilledIcon /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="openInChatwoot(ib)">
                  <el-icon><LinkIcon /></el-icon>
                  {{ t('service.openInChatwoot') }}
                </el-dropdown-item>
                <el-dropdown-item divided @click="confirmDelete(ib)">
                  <el-icon><DeleteIcon /></el-icon>
                  <span class="text-danger">{{ t('service.deleteInbox') }}</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
        <div class="card-body">
          <div v-if="ib.email" class="card-line">
            <el-icon><MessageIcon /></el-icon>
            <span>{{ ib.email }}</span>
          </div>
          <div v-if="ib.phoneNumber" class="card-line">
            <el-icon><PhoneIcon /></el-icon>
            <span>{{ ib.phoneNumber }}</span>
          </div>
          <div class="card-line muted">
            <el-icon><CheckIcon /></el-icon>
            <span>ID #{{ ib.id }}</span>
          </div>
        </div>
      </li>
    </ul>

    <!-- ===================== Setup wizard ===================== -->
    <InboxSetupWizard
      v-model:visible="wizardVisible"
      @created="onCreated"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Refresh as RefreshIcon,
  Plus as PlusIcon,
  MoreFilled as MoreFilledIcon,
  Delete as DeleteIcon,
  Link as LinkIcon,
  Message as MessageIcon,
  Phone as PhoneIcon,
  Check as CheckIcon,
} from '@element-plus/icons-vue'
import { listInboxes, deleteInbox } from '@/api/service'
import InboxSetupWizard from './InboxSetupWizard.vue'

const { t } = useI18n()

const inboxes = ref([])
const loading = ref(true)
const wizardVisible = ref(false)

const CHATWOOT_URL = import.meta.env.VITE_CHATWOOT_URL || 'http://localhost:3000'

async function reload() {
  loading.value = true
  try {
    const { data } = await listInboxes()
    inboxes.value = Array.isArray(data) ? data : []
  } catch (err) {
    ElMessage.error(err?.message || t('service.loadFailed'))
  } finally {
    loading.value = false
  }
}

function openWizard() {
  wizardVisible.value = true
}

function onCreated(inbox) {
  ElMessage.success(t('service.inboxCreated', { name: inbox?.name || '' }))
  reload()
}

async function confirmDelete(inbox) {
  try {
    await ElMessageBox.confirm(
      t('service.deleteConfirm', { name: inbox.name }),
      t('common.warning'),
      { type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await deleteInbox(inbox.id)
    ElMessage.success(t('service.inboxDeleted'))
    reload()
  } catch (err) {
    ElMessage.error(err?.message || t('service.deleteFailed'))
  }
}

function openInChatwoot(inbox) {
  const url = `${CHATWOOT_URL}/app/accounts/1/settings/inboxes/${inbox.id}`
  window.open(url, '_blank', 'noopener')
}

function channelIcon(type) {
  if (!type) return '💬'
  if (type.includes('Email'))           return '✉️'
  if (type.includes('Whatsapp'))        return '🟢'
  if (type.includes('WebWidget'))       return '🌐'
  if (type.includes('TwitterProfile') || type.includes('Twitter')) return '🐦'
  if (type.includes('FacebookPage') || type.includes('Facebook'))  return '📘'
  if (type.includes('Telegram'))        return '✈️'
  if (type.includes('Sms') || type.includes('Twilio')) return '📱'
  if (type.includes('Line'))            return '💚'
  if (type.includes('Api'))             return '🧪'
  return '💬'
}

function humanChannel(type) {
  if (!type) return '—'
  return type.replace(/^Channel::/, '')
}

onMounted(reload)
</script>

<style scoped>
.inbox-list-page {
  padding: 0 4px;
}

.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 16px;
  gap: 12px;
}
.page-title {
  margin: 0;
  font-size: 18px;
  color: #0f3a26;
}
.page-sub {
  margin: 4px 0 0 0;
  color: #5b6b62;
  font-size: 13px;
}
.head-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.empty {
  margin-top: 40px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
  list-style: none;
  padding: 0;
  margin: 0;
}

.card {
  background: #fff;
  border: 1px solid #e6ece9;
  border-radius: 10px;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  transition: box-shadow 0.15s ease, transform 0.15s ease;
}
.card:hover {
  box-shadow: 0 6px 18px rgba(15, 58, 38, 0.08);
  transform: translateY(-1px);
}
.card-head {
  display: flex;
  align-items: center;
  gap: 10px;
}
.card-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: #e9f3ed;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}
.card-meta {
  flex: 1;
  min-width: 0;
}
.card-name {
  font-weight: 600;
  color: #1c2e25;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.card-channel {
  margin-top: 2px;
  font-size: 11px;
  color: #8a9690;
}
.card-menu-btn { flex-shrink: 0; }

.card-body {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.card-line {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #5b6b62;
  word-break: break-all;
}
.card-line.muted { color: #8a9690; }
.text-danger { color: #c45a4d; }
</style>
