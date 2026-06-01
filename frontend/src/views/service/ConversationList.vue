<template>
  <div class="conv-page">
    <!-- ===================== Left: filters ===================== -->
    <aside class="filters">
      <h3 class="filters-title">{{ t('service.filters') }}</h3>

      <div class="filter-group">
        <label class="filter-label">{{ t('service.status') }}</label>
        <el-radio-group v-model="statusFilter" size="small" @change="reload">
          <el-radio-button label="open">{{ t('service.statusOpen') }}</el-radio-button>
          <el-radio-button label="pending">{{ t('service.statusPending') }}</el-radio-button>
          <el-radio-button label="resolved">{{ t('service.statusResolved') }}</el-radio-button>
        </el-radio-group>
      </div>

      <div class="filter-group">
        <label class="filter-label">{{ t('service.assignee') }}</label>
        <el-radio-group v-model="assigneeFilter" size="small" @change="reload">
          <el-radio-button label="">{{ t('service.assigneeAll') }}</el-radio-button>
          <el-radio-button label="me">{{ t('service.assigneeMe') }}</el-radio-button>
          <el-radio-button label="unassigned">{{ t('service.assigneeUnassigned') }}</el-radio-button>
        </el-radio-group>
      </div>

      <div class="filter-group">
        <label class="filter-label">{{ t('service.inbox') }}</label>
        <el-select
          v-model="inboxFilter"
          size="small"
          :placeholder="t('service.inboxAll')"
          clearable
          style="width: 100%"
          @change="reload"
        >
          <el-option
            v-for="inbox in inboxes"
            :key="inbox.id"
            :label="inbox.name"
            :value="inbox.id"
          />
        </el-select>
      </div>

      <div class="filters-foot">
        <el-button text @click="reload">
          <el-icon><RefreshIcon /></el-icon>
          {{ t('service.reload') }}
        </el-button>
        <el-button text @click="goToInboxes" class="filters-inboxes-btn">
          <el-icon><SettingIcon /></el-icon>
          {{ t('service.inboxesTitle') }}
        </el-button>
      </div>
    </aside>

    <!-- ===================== Middle: conversation list ===================== -->
    <section class="list-pane">
      <div class="list-head">
        <h3 class="list-title">{{ t('service.conversations') }}</h3>
        <el-tag v-if="!loading" size="small" type="info" effect="plain">
          {{ conversations.length }} {{ t('service.conversationsUnit') }}
        </el-tag>
      </div>

      <el-skeleton v-if="loading" :rows="6" animated />

      <el-empty v-else-if="conversations.length === 0" :description="t('service.empty')" />

      <ul v-else class="conv-list">
        <li
          v-for="conv in conversations"
          :key="conv.id"
          class="conv-row"
          :class="{ active: selectedId === conv.id, unread: (conv.unreadCount || 0) > 0 }"
          @click="openConversation(conv)"
        >
          <div class="conv-row-avatar">
            <div class="channel-icon" :title="conv.channel">{{ channelIcon(conv.channel) }}</div>
          </div>
          <div class="conv-row-main">
            <div class="conv-row-line1">
              <span class="conv-row-name">{{ conv.contactName || t('service.unknownContact') }}</span>
              <el-tag
                v-if="conv.agriosCustomerCode"
                size="small"
                type="success"
                effect="plain"
                class="agrios-tag"
              >{{ conv.agriosCustomerCode }}</el-tag>
              <span class="conv-row-time">{{ formatTime(conv.lastActivityAt) }}</span>
            </div>
            <div class="conv-row-line2">
              <span class="conv-row-preview">{{ conv.lastMessagePreview || '—' }}</span>
              <el-badge
                v-if="(conv.unreadCount || 0) > 0"
                :value="conv.unreadCount"
                class="unread-badge"
              />
            </div>
          </div>
        </li>
      </ul>
    </section>

    <!-- ===================== Right: detail (route-rendered) ===================== -->
    <section class="detail-pane">
      <router-view v-if="$route.params.id" />
      <el-empty v-else :description="t('service.selectAConversation')">
        <template #image>
          <el-icon style="font-size: 64px; color: #b9d1c1;"><ChatRoundIcon /></el-icon>
        </template>
      </el-empty>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import {
  Refresh as RefreshIcon,
  ChatRound as ChatRoundIcon,
  Setting as SettingIcon,
} from '@element-plus/icons-vue'
import { listConversations, listInboxes } from '@/api/service'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()

const statusFilter = ref('open')
const assigneeFilter = ref('')
const inboxFilter = ref(null)
const loading = ref(true)
const conversations = ref([])
const inboxes = ref([])

const selectedId = computed(() => Number(route.params.id) || null)

// Sprint 41e: simple polling. Every 10 s while the page is visible.
// Sprint 42+ will replace this with a WebSocket subscription on the
// Chatwoot ActionCable channel so updates feel instant.
let pollTimer = null

async function reload() {
  loading.value = true
  try {
    const params = { status: statusFilter.value }
    if (assigneeFilter.value) params.assigneeType = assigneeFilter.value
    if (inboxFilter.value) params.inboxId = inboxFilter.value
    const { data } = await listConversations(params)
    conversations.value = Array.isArray(data) ? data : []
  } catch (err) {
    ElMessage.error(err?.message || t('service.loadFailed'))
  } finally {
    loading.value = false
  }
}

async function loadInboxes() {
  try {
    const { data } = await listInboxes()
    inboxes.value = Array.isArray(data) ? data : []
  } catch {
    // Non-fatal — just leave the inbox filter empty.
  }
}

function openConversation(conv) {
  router.push({ name: 'service-conversation-detail', params: { id: conv.id } })
}

function goToInboxes() {
  router.push({ name: 'service-inboxes' })
}

function formatTime(epochSec) {
  if (!epochSec) return ''
  const d = new Date(epochSec * 1000)
  const now = new Date()
  const sameDay = d.toDateString() === now.toDateString()
  if (sameDay) {
    return d.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' })
  }
  return d.toLocaleDateString('en-GB', { day: '2-digit', month: 'short' })
}

function channelIcon(channel) {
  if (!channel) return '💬'
  if (channel.includes('Email')) return '✉️'
  if (channel.includes('Whatsapp')) return '🟢'
  if (channel.includes('WebWidget')) return '🌐'
  if (channel.includes('Twitter') || channel.includes('TwitterProfile')) return '🐦'
  if (channel.includes('Facebook')) return '📘'
  if (channel.includes('TikTok')) return '🎵'
  if (channel.includes('Sms')) return '📱'
  if (channel.includes('Telegram')) return '✈️'
  if (channel.includes('Slack')) return '💼'
  if (channel.includes('Api')) return '🧪'
  return '💬'
}

watch(() => route.params.id, (id) => {
  if (id) {
    // Detail page mounted — make sure the list is fresh too.
    reload()
  }
})

onMounted(async () => {
  await Promise.all([reload(), loadInboxes()])
  pollTimer = setInterval(reload, 10_000)
})

onBeforeUnmount(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<style scoped>
.conv-page {
  display: grid;
  grid-template-columns: 220px 380px 1fr;
  gap: 12px;
  height: calc(100vh - 100px);
}

/* ----- filters ----- */
.filters {
  background: #f8faf9;
  border: 1px solid #e6ece9;
  border-radius: 8px;
  padding: 14px 14px 18px;
  display: flex;
  flex-direction: column;
  gap: 18px;
  overflow-y: auto;
}
.filters-title {
  margin: 0;
  font-size: 14px;
  color: #0f3a26;
  letter-spacing: 0.3px;
}
.filter-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.filter-label {
  font-size: 12px;
  color: #5b6b62;
  font-weight: 500;
}
.filters-foot {
  margin-top: auto;
}

/* ----- list ----- */
.list-pane {
  background: #fff;
  border: 1px solid #e6ece9;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-bottom: 1px solid #eef2f0;
}
.list-title {
  margin: 0;
  font-size: 14px;
  color: #0f3a26;
}
.conv-list {
  list-style: none;
  padding: 0;
  margin: 0;
  overflow-y: auto;
  flex: 1;
}
.conv-row {
  display: flex;
  align-items: stretch;
  gap: 10px;
  padding: 12px 14px;
  border-bottom: 1px solid #f1f4f3;
  cursor: pointer;
  transition: background 0.12s ease;
}
.conv-row:hover {
  background: #f6faf8;
}
.conv-row.active {
  background: #e9f3ed;
  border-left: 3px solid #0f3a26;
  padding-left: 11px;
}
.conv-row.unread .conv-row-name {
  font-weight: 600;
}
.conv-row-avatar {
  flex: 0 0 36px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.channel-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: #e9f3ed;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}
.conv-row-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}
.conv-row-line1 {
  display: flex;
  align-items: center;
  gap: 6px;
}
.conv-row-name {
  font-size: 14px;
  color: #1c2e25;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.agrios-tag {
  font-size: 10px;
  height: 18px;
  line-height: 16px;
  padding: 0 4px;
}
.conv-row-time {
  margin-left: auto;
  font-size: 11px;
  color: #8a9690;
  flex-shrink: 0;
}
.conv-row-line2 {
  display: flex;
  align-items: center;
  gap: 6px;
}
.conv-row-preview {
  flex: 1;
  font-size: 12px;
  color: #5b6b62;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.unread-badge :deep(.el-badge__content) {
  background: #0f3a26;
}

/* ----- detail ----- */
.detail-pane {
  background: #fff;
  border: 1px solid #e6ece9;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
</style>
