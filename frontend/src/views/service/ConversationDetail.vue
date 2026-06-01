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
      </div>
    </header>

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
          <el-radio-button label="reply">{{ t('service.reply') }}</el-radio-button>
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
        <el-button text @click="askAi" :disabled="askingAi">
          <el-icon><MagicStickIcon /></el-icon>
          {{ askingAi ? t('service.aiThinking') : t('service.aiDraft') }}
        </el-button>
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
import { ref, watch, nextTick, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { MagicStick as MagicStickIcon } from '@element-plus/icons-vue'
import {
  getConversation,
  replyToConversation,
  changeConversationStatus,
  aiAgentDiagnose,
} from '@/api/service'

const { t } = useI18n()
const route = useRoute()

const conv = ref(null)
const replyText = ref('')
const replyMode = ref('reply') // 'reply' or 'note'
const sending = ref(false)
const askingAi = ref(false)
const streamRef = ref(null)

async function load() {
  try {
    const { data } = await getConversation(route.params.id)
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

async function askAi() {
  if (askingAi.value) return
  askingAi.value = true
  try {
    // Build a short context from the last 3 messages.
    const ctx = (conv.value?.messages || []).slice(-3)
      .map(m => (m.messageType === 0 ? 'Customer' : 'Agent') + ': ' + m.content).join('\n')
    const prompt = `You are a CSR for Albert's Farm. Suggest a concise, friendly reply to the customer based on the recent exchange. Reply in the customer's language.\n\nRecent exchange:\n${ctx}\n\nDraft a reply (1-3 sentences):`
    const { data } = await aiAgentDiagnose(prompt)
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

onMounted(load)
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
</style>
