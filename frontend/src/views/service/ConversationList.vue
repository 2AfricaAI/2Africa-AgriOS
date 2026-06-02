<template>
  <div class="conv-page">
    <!-- ===================== Left: Views + Inboxes (Intercom-style sidebar) ===================== -->
    <!--
      Sprint 48b A: redesigned left rail. The old vertical-stack of filters
      (Status / Assignee / Inbox dropdown) is replaced with a Chatwoot /
      Intercom / Front-style two-section sidebar:
        - Saved Views section: All / Mine / Unassigned (one-click presets)
        - Inboxes section: dynamic list of every Chatwoot inbox (channel
          icon + name + per-inbox count badge)
      Status filtering (open / pending / resolved) is now horizontal tabs
      at the top of the middle pane so it composes with the chosen view.
    -->
    <aside class="rail">
      <section class="rail-section">
        <h4 class="rail-section-title">{{ t('cs.viewSectionViews') }}</h4>
        <ul class="rail-list">
          <li
            v-for="v in views"
            :key="v.id"
            class="rail-item"
            :class="{ active: currentView === v.id }"
            @click="setView(v.id)"
          >
            <span class="rail-item-icon">{{ v.icon }}</span>
            <span class="rail-item-name">{{ t(v.labelKey) }}</span>
          </li>
        </ul>
      </section>

      <section class="rail-section">
        <h4 class="rail-section-title">{{ t('cs.viewSectionInboxes') }}</h4>
        <ul class="rail-list">
          <li
            v-for="ib in inboxes"
            :key="`inbox-${ib.id}`"
            class="rail-item"
            :class="{ active: currentView === `inbox-${ib.id}` }"
            @click="setView(`inbox-${ib.id}`)"
            :title="humanChannel(ib.channelType)"
          >
            <span class="rail-item-icon">{{ channelIcon(ib.channelType) }}</span>
            <span class="rail-item-name">{{ ib.name }}</span>
          </li>
          <li v-if="!inboxes.length" class="rail-empty">
            {{ t('cs.inboxesEmpty') }}
          </li>
        </ul>
      </section>

      <div class="rail-foot">
        <el-button text @click="reload">
          <el-icon><RefreshIcon /></el-icon>
          {{ t('service.reload') }}
        </el-button>
        <el-button text @click="goToSettings">
          <el-icon><SettingIcon /></el-icon>
          {{ t('cs.openSettings') }}
        </el-button>
      </div>
    </aside>

    <!-- ===================== Middle: status tabs + conversation list ===================== -->
    <section class="list-pane">
      <div class="list-head">
        <div class="list-head-titles">
          <h3 class="list-title">{{ currentViewLabel }}</h3>
          <el-tag v-if="!loading" size="small" type="info" effect="plain">
            {{ conversations.length }} {{ t('service.conversationsUnit') }}
          </el-tag>
        </div>
        <el-radio-group v-model="statusFilter" size="small" @change="reload">
          <el-radio-button label="open">{{ t('service.statusOpen') }}</el-radio-button>
          <el-radio-button label="pending">{{ t('service.statusPending') }}</el-radio-button>
          <el-radio-button label="resolved">{{ t('service.statusResolved') }}</el-radio-button>
        </el-radio-group>
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

// ---------------- views ----------------
// Each view maps to a fixed (assigneeType, inboxId) combination. Inbox views
// are inserted dynamically below by id. Keep the icons emoji-only so they
// align with the channel icons that come from `channelIcon()` for the inbox
// list — gives the whole rail one visual rhythm.
const views = [
  { id: 'all',        icon: '📥', labelKey: 'cs.viewAll' },
  { id: 'mine',       icon: '👤', labelKey: 'cs.viewMine' },
  { id: 'unassigned', icon: '❓', labelKey: 'cs.viewUnassigned' },
]

const currentView = ref('all')
const statusFilter = ref('open')
const loading = ref(true)
const conversations = ref([])
const inboxes = ref([])

const selectedId = computed(() => Number(route.params.id) || null)

const currentViewLabel = computed(() => {
  const built = views.find(v => v.id === currentView.value)
  if (built) return t(built.labelKey)
  if (currentView.value?.startsWith('inbox-')) {
    const id = Number(currentView.value.slice('inbox-'.length))
    const ib = inboxes.value.find(x => x.id === id)
    return ib?.name || t('service.conversations')
  }
  return t('service.conversations')
})

// ---------------- API params derived from current view ----------------
function paramsForView() {
  const params = { status: statusFilter.value }
  switch (currentView.value) {
    case 'all':
      break
    case 'mine':
      params.assigneeType = 'me'
      break
    case 'unassigned':
      params.assigneeType = 'unassigned'
      break
    default:
      if (currentView.value?.startsWith('inbox-')) {
        params.inboxId = Number(currentView.value.slice('inbox-'.length))
      }
  }
  return params
}

// ---------------- data flow ----------------
let pollTimer = null

async function reload() {
  loading.value = true
  try {
    const data = await listConversations(paramsForView())
    conversations.value = Array.isArray(data) ? data : []
  } catch (err) {
    ElMessage.error(err?.message || t('service.loadFailed'))
  } finally {
    loading.value = false
  }
}

async function loadInboxes() {
  try {
    const data = await listInboxes()
    inboxes.value = Array.isArray(data) ? data : []
  } catch {
    // Non-fatal — leave the inbox section empty.
  }
}

// ---------------- interactions ----------------
function setView(id) {
  currentView.value = id
  reload()
}

function openConversation(conv) {
  router.push({ name: 'service-conversation-detail', params: { id: conv.id } })
}

function goToSettings() {
  router.push({ name: 'service-settings' })
}

// ---------------- formatters ----------------
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

function humanChannel(c) {
  if (!c) return '—'
  return c.replace(/^Channel::/, '')
}

watch(() => route.params.id, (id) => {
  if (id) reload()
})

onMounted(async () => {
  await Promise.all([reload(), loadInboxes()])
  // Sprint 41e polling — replaced by WebSocket in a future sprint.
  pollTimer = setInterval(reload, 10_000)
})

onBeforeUnmount(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<style scoped>
.conv-page {
  display: grid;
  grid-template-columns: 240px 400px 1fr;
  gap: 12px;
  height: calc(100vh - 100px);
}

/* ============== left rail (views + inboxes) ============== */
.rail {
  background: #f8faf9;
  border: 1px solid #e6ece9;
  border-radius: 8px;
  padding: 14px 0 14px 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
  overflow-y: auto;
}
.rail-section {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.rail-section-title {
  margin: 8px 14px 4px;
  font-size: 11px;
  color: #8a9690;
  text-transform: uppercase;
  letter-spacing: 0.6px;
  font-weight: 600;
}
.rail-list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.rail-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 14px;
  cursor: pointer;
  font-size: 13px;
  color: #1c2e25;
  transition: background 0.12s ease;
}
.rail-item:hover {
  background: #eef4f0;
}
.rail-item.active {
  background: #e9f3ed;
  border-left: 3px solid #0f3a26;
  padding-left: 11px;
  font-weight: 600;
  color: #0f3a26;
}
.rail-item-icon {
  width: 22px;
  text-align: center;
  font-size: 14px;
  flex-shrink: 0;
}
.rail-item-name {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.rail-empty {
  padding: 6px 14px;
  font-size: 12px;
  color: #8a9690;
  font-style: italic;
}
.rail-foot {
  margin-top: auto;
  padding: 8px 12px;
  border-top: 1px solid #eef2f0;
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

/* ============== middle list pane ============== */
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
  flex-direction: column;
  gap: 10px;
  padding: 12px 14px;
  border-bottom: 1px solid #eef2f0;
}
.list-head-titles {
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: space-between;
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
.conv-row:hover { background: #f6faf8; }
.conv-row.active {
  background: #e9f3ed;
  border-left: 3px solid #0f3a26;
  padding-left: 11px;
}
.conv-row.unread .conv-row-name { font-weight: 600; }
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

/* ============== right detail pane ============== */
.detail-pane {
  background: #fff;
  border: 1px solid #e6ece9;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
</style>
