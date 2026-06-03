<template>
  <div class="conv-detail" v-if="conv">
    <!-- ===================== Header ===================== -->
    <header class="head">
      <div class="head-left">
        <div class="head-avatar">{{ initials(conv.customer?.name || conv.id) }}</div>
        <div class="head-name">
          <div class="head-name-line1">
            <span class="head-name-text">{{ conv.customer?.name || t('service.unknownContact') }}</span>
            <el-tag
              v-if="conv.customer?.code"
              size="small"
              type="success"
              effect="plain"
            >{{ conv.customer.code }}</el-tag>
          </div>
          <div class="head-name-line2">
            <span v-if="conv.customer?.contactPhone" class="head-meta">{{ conv.customer.contactPhone }}</span>
            <span class="head-meta-channel">{{ humanChannel(conv.channel) }} · #{{ conv.displayId }}</span>
          </div>
        </div>
      </div>
      <div class="head-right">
        <!-- Sprint 47: WhatsApp 24h service-window chip -->
        <el-tooltip
          v-if="conv.whatsAppPolicy?.managed"
          :content="waTooltip"
          placement="bottom"
        >
          <el-tag
            :type="waWithinWindow ? 'success' : 'info'"
            size="small"
            effect="plain"
            class="wa-chip"
          >
            <el-icon class="wa-chip-icon">
              <ClockIcon v-if="waWithinWindow" />
              <WarningIcon v-else />
            </el-icon>
            {{ waChipLabel }}
          </el-tag>
        </el-tooltip>

        <el-tag
          :type="conv.status === 'open' ? 'warning' : conv.status === 'resolved' ? 'success' : 'info'"
          size="small"
        >{{ t('service.status' + statusKey(conv.status)) }}</el-tag>
        <el-button
          v-if="conv.status === 'open'"
          size="small"
          type="success"
          @click="changeStatus('resolved')"
        >{{ t('service.markResolved') }}</el-button>
        <el-button
          v-else
          size="small"
          @click="changeStatus('open')"
        >{{ t('service.reopen') }}</el-button>
        <!-- Sprint 50d: copy a CSAT survey link to the clipboard so the
             agent can paste it into their next reply. v-perm gates by
             cs:csat:send. -->
        <el-button
          v-perm="'cs:csat:send'"
          size="small"
          type="primary"
          plain
          :loading="generatingCsat"
          :icon="StarIcon"
          @click="copyCsatLink"
        >{{ t('service.csatSendBtn') }}</el-button>
        <!-- Sprint 49.5: SUPER_ADMIN hard-delete. v-perm hides it for
             everyone else; backend also rejects with 403. -->
        <el-button
          v-perm="'cs:conversation:delete'"
          size="small"
          type="danger"
          plain
          :loading="deleting"
          :icon="DeleteIcon"
          @click="confirmDelete"
        >{{ t('service.deleteConversation') }}</el-button>
      </div>
    </header>

    <!-- ===================== WhatsApp policy banner (window expired) ===================== -->
    <div
      v-if="conv.whatsAppPolicy?.managed && !waWithinWindow"
      class="wa-banner"
    >
      <el-icon><WarningIcon /></el-icon>
      <span>{{ t('service.whatsAppBlockedBanner') }}</span>
    </div>

    <!-- ===================== Business context strip ===================== -->
    <div v-if="conv.customer" class="context">
      <div class="context-pill">
        <span class="context-label">{{ t('service.creditLevel') }}</span>
        <span class="context-value">{{ conv.customer.creditLevel || '—' }}</span>
      </div>
      <div class="context-pill">
        <span class="context-label">{{ t('service.paymentTerms') }}</span>
        <span class="context-value">{{ conv.customer.paymentTerms || '—' }}</span>
      </div>
      <div class="context-pill">
        <span class="context-label">{{ t('service.openOrders') }}</span>
        <span class="context-value">{{ conv.businessContext?.openOrderCount ?? 0 }}</span>
      </div>
      <div class="context-pill" :class="{ alert: (conv.businessContext?.overdueArInvoiceCount || 0) > 0 }">
        <span class="context-label">{{ t('service.overdueAr') }}</span>
        <span class="context-value">
          {{ conv.businessContext?.overdueArInvoiceCount ?? 0 }}
          <span v-if="conv.businessContext?.overdueArAmount" class="context-money">
            · KSh {{ formatMoney(conv.businessContext.overdueArAmount) }}
          </span>
        </span>
      </div>
      <div class="context-pill" :class="{ alert: (conv.businessContext?.openComplaintCount || 0) > 0 }">
        <span class="context-label">{{ t('service.openComplaints') }}</span>
        <span class="context-value">{{ conv.businessContext?.openComplaintCount ?? 0 }}</span>
      </div>
      <div v-if="conv.businessContext?.lastOrderDate" class="context-pill muted">
        <span class="context-label">{{ t('service.lastOrder') }}</span>
        <span class="context-value">{{ formatDate(conv.businessContext.lastOrderDate) }}</span>
      </div>
    </div>

    <!-- ===================== Message stream ===================== -->
    <main ref="streamRef" class="stream">
      <div
        v-for="msg in conv.messages"
        :key="msg.id"
        class="msg"
        :class="messageClasses(msg)"
      >
        <div class="bubble">
          <div class="bubble-content">{{ msg.content }}</div>
          <div class="bubble-time">{{ formatTime(msg.createdAt) }}</div>
        </div>
      </div>
    </main>

    <!-- ===================== Reply composer ===================== -->
    <footer class="composer">
      <div class="composer-tabs">
        <el-radio-group v-model="replyMode" size="small">
          <el-tooltip
            :content="t('service.whatsAppBlockedTooltip')"
            :disabled="!waReplyBlocked"
            placement="top"
          >
            <el-radio-button label="reply" :disabled="waReplyBlocked">
              {{ t('service.reply') }}
            </el-radio-button>
          </el-tooltip>
          <el-radio-button label="note">{{ t('service.privateNote') }}</el-radio-button>
        </el-radio-group>
        <span class="composer-hint">
          {{ replyMode === 'note' ? t('service.privateNoteHint') : t('service.replyHint') }}
        </span>
      </div>
      <el-input
        v-model="replyText"
        type="textarea"
        :rows="3"
        :placeholder="replyMode === 'note' ? t('service.privateNotePlaceholder') : t('service.replyPlaceholder')"
        @keydown.enter.ctrl="send"
        @keydown.enter.meta="send"
      />
      <div class="composer-actions">
        <div class="composer-actions-left">
          <el-dropdown
            v-if="templates.length"
            trigger="click"
            @command="onPickTemplate"
          >
            <el-button text :disabled="pickingTpl">
              <el-icon><DocumentIcon /></el-icon>
              {{ pickingTpl ? t('service.templateLoading') : t('service.template') }}
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item
                  v-for="tpl in templates"
                  :key="tpl.id"
                  :command="tpl.code"
                >
                  <div class="tpl-row">
                    <span class="tpl-name">{{ tpl.name }}</span>
                    <span class="tpl-meta">{{ tpl.channel }} · {{ tpl.lang }}</span>
                  </div>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <el-button text @click="askAi" :disabled="askingAi">
            <el-icon><MagicStickIcon /></el-icon>
            {{ askingAi ? t('service.aiThinking') : t('service.aiDraft') }}
          </el-button>
        </div>

        <el-button
          type="primary"
          :disabled="!replyText.trim() || sending"
          :loading="sending"
          @click="send"
        >
          {{ replyMode === 'note' ? t('service.saveNote') : t('service.sendReply') }}
        </el-button>
      </div>
    </footer>
  </div>

  <el-skeleton v-else :rows="10" animated style="padding: 20px" />
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  MagicStick as MagicStickIcon,
  Document as DocumentIcon,
  Clock as ClockIcon,
  Warning as WarningIcon,
  Delete as DeleteIcon,
  Star as StarIcon,
} from '@element-plus/icons-vue'
import {
  getConversation,
  replyToConversation,
  changeConversationStatus,
  aiAgentDiagnose,
  listSmsTemplates,
  renderSmsTemplate,
  deleteConversation,
  generateCsatLink,
} from '@/api/service'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const conv = ref(null)
const replyText = ref('')
const replyMode = ref('reply') // 'reply' or 'note'
const sending = ref(false)
const askingAi = ref(false)
const streamRef = ref(null)

const templates = ref([])
const pickingTpl = ref(false)

// Sprint 47 — WhatsApp service-window state.
// `nowTick` is bumped every 30s so the countdown chip recomputes live
// without needing a full conversation reload.
const nowTick = ref(Date.now())
let nowTimer = null

const waWithinWindow = computed(() => {
  const p = conv.value?.whatsAppPolicy
  if (!p?.managed) return true            // non-WhatsApp inboxes are unrestricted
  if (!p.serviceWindowExpiresAt) return false
  return nowTick.value / 1000 < p.serviceWindowExpiresAt
})

const waReplyBlocked = computed(() => {
  const p = conv.value?.whatsAppPolicy
  return p?.managed && !waWithinWindow.value
})

const waChipLabel = computed(() => {
  const p = conv.value?.whatsAppPolicy
  if (!p?.managed) return ''
  if (!p.serviceWindowExpiresAt) return t('service.whatsAppNoWindow')
  const msLeft = p.serviceWindowExpiresAt * 1000 - nowTick.value
  if (msLeft <= 0) return t('service.whatsAppWindowExpired')
  const hours = Math.floor(msLeft / 3_600_000)
  const minutes = Math.floor((msLeft % 3_600_000) / 60_000)
  return t('service.whatsAppWindowRemaining', { h: hours, m: minutes })
})

const waTooltip = computed(() => {
  const p = conv.value?.whatsAppPolicy
  if (!p?.managed) return ''
  return waWithinWindow.value
    ? t('service.whatsAppWindowTooltip')
    : t('service.whatsAppExpiredTooltip')
})

async function load() {
  try {
    const data = await getConversation(route.params.id)
    conv.value = data
    await nextTick()
    scrollToBottom()
  } catch (err) {
    ElMessage.error(err?.message || t('service.loadFailed'))
  }
}

function scrollToBottom() {
  if (streamRef.value) {
    streamRef.value.scrollTop = streamRef.value.scrollHeight
  }
}

async function send() {
  if (!replyText.value.trim() || sending.value) return
  sending.value = true
  try {
    await replyToConversation(route.params.id, replyText.value.trim(), replyMode.value === 'note')
    ElMessage.success(
      replyMode.value === 'note' ? t('service.noteSaved') : t('service.replySent')
    )
    replyText.value = ''
    await load()
  } catch (err) {
    ElMessage.error(err?.message || t('service.sendFailed'))
  } finally {
    sending.value = false
  }
}

async function loadTemplates() {
  try {
    const data = await listSmsTemplates()
    templates.value = Array.isArray(data) ? data : []
  } catch {
    templates.value = []
  }
}

async function onPickTemplate(code) {
  pickingTpl.value = true
  try {
    const data = await renderSmsTemplate(code, Number(route.params.id))
    if (data?.rendered) {
      replyText.value = data.rendered
    }
  } catch (err) {
    ElMessage.error(err?.message || t('service.templateFailed'))
  } finally {
    pickingTpl.value = false
  }
}

async function askAi() {
  if (askingAi.value) return
  askingAi.value = true
  try {
    // Build a short context from the last 3 messages.
    const ctx = (conv.value?.messages || []).slice(-3)
      .map(m => (m.messageType === 0 ? 'Customer' : 'Agent') + ': ' + m.content).join('\n')
    const prompt = `You are a CSR for Albert's Farm. Suggest a concise, friendly reply to the customer based on the recent exchange. Reply in the customer's language.\n\nRecent exchange:\n${ctx}\n\nDraft a reply (1-3 sentences):`
    const data = await aiAgentDiagnose(prompt)
    if (data?.reply) {
      replyText.value = data.reply
    } else if (data?.error) {
      ElMessage.error(data.error)
    }
  } catch (err) {
    ElMessage.error(err?.message || t('service.aiFailed'))
  } finally {
    askingAi.value = false
  }
}

async function changeStatus(status) {
  try {
    await changeConversationStatus(route.params.id, status)
    ElMessage.success(t('service.statusChanged'))
    await load()
  } catch (err) {
    ElMessage.error(err?.message || t('service.statusFailed'))
  }
}

// Sprint 50d — generate a CSAT survey link and copy it to clipboard so
// the agent can paste it into their next reply. Idempotent on backend
// (same conversation returns the same un-submitted token within TTL).
const generatingCsat = ref(false)
async function copyCsatLink() {
  if (!conv.value) return
  generatingCsat.value = true
  try {
    const data = await generateCsatLink(route.params.id)
    if (!data?.url) throw new Error('No URL returned')
    try {
      await navigator.clipboard.writeText(data.url)
      ElMessage.success(t('service.csatLinkCopied'))
    } catch {
      // Fallback for non-secure contexts (no Clipboard API). Show the
      // URL inline so the agent can copy it manually.
      ElMessage({
        message: t('service.csatLinkManual', { url: data.url }),
        type: 'success',
        duration: 6000,
        dangerouslyUseHTMLString: false,
      })
    }
  } catch (err) {
    ElMessage.error(err?.message || t('service.csatLinkFailed'))
  } finally {
    generatingCsat.value = false
  }
}

// Sprint 49.5 — destructive single-conversation delete.
// Two-step confirm: a modal first, then the actual API call. After a
// successful delete we navigate back to the conversation list rather
// than leaving the page on a now-dead conversation id.
const deleting = ref(false)
async function confirmDelete() {
  if (!conv.value) return
  const display = conv.value.customer?.name
    || `#${conv.value.displayId || conv.value.id}`
  try {
    await ElMessageBox.confirm(
      t('service.deleteConfirmMsg', { name: display }),
      t('service.deleteConfirmTitle'),
      {
        confirmButtonText: t('service.deleteConfirmYes'),
        cancelButtonText: t('common.cancel'),
        type: 'warning',
        confirmButtonClass: 'el-button--danger',
      },
    )
  } catch {
    return        // user cancelled
  }
  deleting.value = true
  try {
    await deleteConversation(route.params.id)
    ElMessage.success(t('service.deleteSuccess'))
    router.push({ name: 'customer-service' })
  } catch (err) {
    ElMessage.error(err?.message || t('service.deleteFailed'))
  } finally {
    deleting.value = false
  }
}

function messageClasses(msg) {
  if (msg.privateNote) return ['msg-note']
  if (msg.messageType === 0) return ['msg-incoming']
  return ['msg-outgoing']
}

function formatTime(epochSec) {
  if (!epochSec) return ''
  const d = new Date(epochSec * 1000)
  return d.toLocaleString('en-GB', {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function formatMoney(v) {
  if (v === null || v === undefined) return '0'
  const n = Number(v)
  if (isNaN(n)) return String(v)
  return n.toLocaleString('en-GB', { maximumFractionDigits: 0 })
}

function formatDate(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  if (isNaN(d.getTime())) return iso
  const now = new Date()
  const days = Math.floor((now - d) / 86_400_000)
  if (days === 0) return 'Today'
  if (days === 1) return 'Yesterday'
  if (days < 7) return `${days}d ago`
  return d.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' })
}

function initials(s) {
  if (!s) return '?'
  return String(s).trim().slice(0, 2).toUpperCase()
}

function humanChannel(c) {
  if (!c) return '—'
  return c.replace(/^Channel::/, '')
}

function statusKey(s) {
  if (!s) return 'Open'
  return s.charAt(0).toUpperCase() + s.slice(1)
}

watch(() => route.params.id, (id) => {
  if (id) {
    conv.value = null
    load()
  }
})

// Auto-switch to private note when the window expires (or starts that way).
// Keeps the user from typing a reply that the backend would refuse anyway.
watch(waReplyBlocked, (blocked) => {
  if (blocked && replyMode.value === 'reply') {
    replyMode.value = 'note'
  }
})

onMounted(() => {
  load()
  loadTemplates()
  nowTimer = setInterval(() => { nowTick.value = Date.now() }, 30_000)
})

onBeforeUnmount(() => {
  if (nowTimer) clearInterval(nowTimer)
})
</script>

<style scoped>
.conv-detail {
  display: flex;
  flex-direction: column;
  height: 100%;
}

/* ----- header ----- */
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  border-bottom: 1px solid #eef2f0;
  flex-shrink: 0;
}
.head-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.head-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: #0f3a26;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 15px;
}
.head-name-line1 {
  display: flex;
  align-items: center;
  gap: 8px;
}
.head-name-text {
  font-size: 15px;
  font-weight: 600;
  color: #1c2e25;
}
.head-name-line2 {
  margin-top: 2px;
  font-size: 12px;
  color: #5b6b62;
  display: flex;
  gap: 8px;
}
.head-meta-channel { color: #8a9690; }
.head-right {
  display: flex;
  gap: 8px;
  align-items: center;
}

/* ----- business context strip ----- */
.context {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 10px 18px;
  background: #f8faf9;
  border-bottom: 1px solid #eef2f0;
  flex-shrink: 0;
}
.context-pill {
  background: #fff;
  border: 1px solid #e6ece9;
  border-radius: 16px;
  padding: 4px 10px;
  display: flex;
  align-items: baseline;
  gap: 6px;
  font-size: 12px;
}
.context-pill.alert {
  background: #fff3e6;
  border-color: #f5a14d;
  color: #b35a00;
}
.context-pill.muted {
  background: transparent;
  border-color: transparent;
  padding-left: 4px;
}
.context-label { color: #8a9690; }
.context-value { font-weight: 600; color: #1c2e25; }
.context-pill.alert .context-value { color: #b35a00; }
.context-pill.muted .context-value { color: #5b6b62; font-weight: 500; }
.context-money { color: #b35a00; font-weight: 600; margin-left: 2px; }

/* ----- message stream ----- */
.stream {
  flex: 1;
  overflow-y: auto;
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  background: #fafcfb;
}
.msg {
  display: flex;
  max-width: 80%;
}
.msg-incoming { justify-content: flex-start; }
.msg-outgoing { justify-content: flex-end; margin-left: auto; }
.msg-note     { justify-content: center; margin-left: auto; margin-right: auto; max-width: 90%; }
.bubble {
  border-radius: 12px;
  padding: 8px 12px;
  font-size: 14px;
  line-height: 1.45;
  word-break: break-word;
  box-shadow: 0 1px 2px rgba(0,0,0,0.04);
}
.msg-incoming .bubble { background: #fff; border: 1px solid #e6ece9; }
.msg-outgoing .bubble { background: #0f3a26; color: #fff; }
.msg-note     .bubble { background: #fff7e6; border: 1px dashed #f0b86e; color: #6b4a12; font-style: italic; }
.bubble-content { white-space: pre-wrap; }
.bubble-time {
  font-size: 10px;
  margin-top: 4px;
  opacity: 0.7;
  text-align: right;
}

/* ----- composer ----- */
.composer {
  border-top: 1px solid #eef2f0;
  padding: 12px 18px 14px;
  background: #fff;
  flex-shrink: 0;
}
.composer-tabs {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}
.composer-hint {
  font-size: 11px;
  color: #8a9690;
}
.composer-actions {
  margin-top: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.composer-actions-left {
  display: flex;
  gap: 4px;
  align-items: center;
}
.tpl-row {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 2px 0;
}
.tpl-name { font-size: 13px; color: #1c2e25; font-weight: 500; }
.tpl-meta { font-size: 11px; color: #8a9690; }

/* ----- Sprint 47: WhatsApp service-window chip + banner ----- */
.wa-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.wa-chip-icon {
  font-size: 12px;
}
.wa-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 18px;
  background: #fff3e6;
  border-bottom: 1px solid #f5a14d;
  color: #b35a00;
  font-size: 12px;
  flex-shrink: 0;
}
.wa-banner .el-icon { flex-shrink: 0; }
</style>
