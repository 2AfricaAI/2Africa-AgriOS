<template>
  <el-drawer
    :model-value="visible"
    @update:model-value="$emit('update:visible', $event)"
    direction="rtl"
    size="520px"
    :close-on-click-modal="false"
    :show-close="true"
  >
    <template #header>
      <div class="wiz-head">
        <span class="wiz-title">{{ t('service.wizardTitle') }}</span>
        <span v-if="step !== 'pick'" class="wiz-step">{{ humanChannel(picked) }}</span>
      </div>
    </template>

    <!-- ======== Step 1: pick channel ======== -->
    <div v-if="step === 'pick'" class="picker">
      <p class="picker-hint">{{ t('service.pickChannelHint') }}</p>
      <ul class="channel-grid">
        <li
          v-for="c in channels"
          :key="c.id"
          class="channel-card"
          :class="{ disabled: !c.ready }"
          @click="c.ready && pick(c.id)"
        >
          <div class="channel-icon">{{ c.icon }}</div>
          <div class="channel-meta">
            <div class="channel-name">{{ t(c.labelKey) }}</div>
            <div class="channel-desc">{{ t(c.descKey) }}</div>
          </div>
          <el-tag
            v-if="!c.ready"
            size="small"
            type="info"
            effect="plain"
            class="channel-soon"
          >{{ t('service.comingSoon') }}</el-tag>
        </li>
      </ul>
    </div>

    <!-- ======== Step 2: channel-specific form ======== -->
    <SetupEmailStep
      v-else-if="step === 'form' && picked === 'email'"
      ref="formRef"
      @submitted="onCreated"
    />
    <SetupWhatsAppStep
      v-else-if="step === 'form' && picked === 'whatsapp'"
      ref="formRef"
      @submitted="onCreated"
    />
    <SetupWebWidgetStep
      v-else-if="step === 'form' && picked === 'webwidget'"
      ref="formRef"
      @submitted="onCreated"
    />

    <!-- ======== Footer ======== -->
    <template #footer>
      <div class="wiz-footer">
        <el-button v-if="step !== 'pick'" text @click="back">
          {{ t('common.back') }}
        </el-button>
        <el-button @click="close">{{ t('common.cancel') }}</el-button>
        <el-button
          v-if="step === 'form'"
          type="primary"
          :loading="submitting"
          @click="submit"
        >
          {{ t('service.createInbox') }}
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import SetupEmailStep from './setup/SetupEmailStep.vue'
import SetupWhatsAppStep from './setup/SetupWhatsAppStep.vue'
import SetupWebWidgetStep from './setup/SetupWebWidgetStep.vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
})
const emit = defineEmits(['update:visible', 'created'])

const { t } = useI18n()

const step = ref('pick')        // 'pick' | 'form'
const picked = ref(null)         // 'email' | 'whatsapp' | 'webwidget' | ...
const submitting = ref(false)
const formRef = ref(null)

// Sprint 42 ships 3 channels; the rest are placeholders so the operator can
// see what's coming in Sprint 43+.
const channels = [
  { id: 'email',     icon: '✉️', labelKey: 'service.chEmail',     descKey: 'service.chEmailDesc',     ready: true  },
  { id: 'whatsapp',  icon: '🟢', labelKey: 'service.chWhatsapp',  descKey: 'service.chWhatsappDesc',  ready: true  },
  { id: 'webwidget', icon: '🌐', labelKey: 'service.chWebWidget', descKey: 'service.chWebWidgetDesc', ready: true  },
  { id: 'sms',       icon: '📱', labelKey: 'service.chSms',       descKey: 'service.chSmsDesc',       ready: false },
  { id: 'phone',     icon: '☎️', labelKey: 'service.chPhone',     descKey: 'service.chPhoneDesc',     ready: false },
  { id: 'twitter',   icon: '🐦', labelKey: 'service.chTwitter',   descKey: 'service.chTwitterDesc',   ready: false },
  { id: 'facebook',  icon: '📘', labelKey: 'service.chFacebook',  descKey: 'service.chFacebookDesc',  ready: false },
  { id: 'tiktok',    icon: '🎵', labelKey: 'service.chTiktok',    descKey: 'service.chTiktokDesc',    ready: false },
  { id: 'slack',     icon: '💼', labelKey: 'service.chSlack',     descKey: 'service.chSlackDesc',     ready: false },
  { id: 'telegram',  icon: '✈️', labelKey: 'service.chTelegram',  descKey: 'service.chTelegramDesc',  ready: false },
]

function pick(id) {
  picked.value = id
  step.value = 'form'
}

function back() {
  step.value = 'pick'
  picked.value = null
}

function close() {
  emit('update:visible', false)
}

async function submit() {
  if (!formRef.value) return
  submitting.value = true
  try {
    await formRef.value.submit()
  } catch (err) {
    ElMessage.error(err?.message || t('service.createFailed'))
  } finally {
    submitting.value = false
  }
}

function onCreated(inbox) {
  emit('created', inbox)
  emit('update:visible', false)
}

function humanChannel(id) {
  const c = channels.find(x => x.id === id)
  return c ? t(c.labelKey) : ''
}

// Reset state when drawer reopens
watch(() => props.visible, (v) => {
  if (v) {
    step.value = 'pick'
    picked.value = null
  }
})
</script>

<style scoped>
.wiz-head {
  display: flex;
  align-items: center;
  gap: 10px;
}
.wiz-title {
  font-size: 16px;
  font-weight: 600;
  color: #0f3a26;
}
.wiz-step {
  font-size: 13px;
  color: #5b6b62;
  background: #e9f3ed;
  padding: 2px 8px;
  border-radius: 10px;
}

.picker-hint {
  margin: 0 0 16px 0;
  color: #5b6b62;
  font-size: 13px;
}
.channel-grid {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.channel-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 14px;
  border: 1px solid #e6ece9;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.15s ease;
}
.channel-card:hover:not(.disabled) {
  border-color: #0f3a26;
  background: #f6faf8;
  transform: translateX(2px);
}
.channel-card.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.channel-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: #e9f3ed;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  flex-shrink: 0;
}
.channel-meta {
  flex: 1;
  min-width: 0;
}
.channel-name {
  font-size: 14px;
  font-weight: 600;
  color: #1c2e25;
}
.channel-desc {
  margin-top: 3px;
  font-size: 12px;
  color: #5b6b62;
  overflow: hidden;
  text-overflow: ellipsis;
}
.channel-soon {
  flex-shrink: 0;
}

.wiz-footer {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}
</style>
