<template>
  <div class="step">
    <el-alert type="info" :closable="false" show-icon class="hint">
      <template #title>{{ t('service.smsHintTitle') }}</template>
      <div class="hint-body">
        <p>{{ t('service.smsHintLine1') }}</p>
        <ul>
          <li>{{ t('service.smsHintBullet1') }}</li>
          <li>{{ t('service.smsHintBullet2') }}</li>
          <li>{{ t('service.smsHintBullet3') }}</li>
        </ul>
        <p>
          <a href="https://account.africastalking.com" target="_blank" rel="noopener">
            account.africastalking.com →
          </a>
        </p>
      </div>
    </el-alert>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item :label="t('service.fldInboxName')" prop="name">
        <el-input v-model="form.name" :placeholder="t('service.fldInboxNameHint')" />
      </el-form-item>

      <el-form-item :label="t('service.fldAtUsername')" prop="username">
        <el-input v-model="form.username" placeholder="sandbox" />
        <div class="field-hint">{{ t('service.fldAtUsernameHint') }}</div>
      </el-form-item>

      <el-form-item :label="t('service.fldAtApiKey')" prop="apiKey">
        <el-input
          v-model="form.apiKey"
          type="password"
          show-password
          :placeholder="t('service.fldAtApiKeyHint')"
        />
      </el-form-item>

      <el-form-item :label="t('service.fldAtSenderId')">
        <el-input v-model="form.senderId" :placeholder="t('service.fldAtSenderIdHint')" />
        <div class="field-hint">{{ t('service.fldAtSenderIdNote') }}</div>
      </el-form-item>

      <el-form-item :label="t('service.fldAtSandbox')">
        <el-switch
          v-model="form.sandbox"
          :active-text="t('service.atSandboxOn')"
          :inactive-text="t('service.atSandboxOff')"
          inline-prompt
        />
        <div class="field-hint">{{ t('service.fldAtSandboxNote') }}</div>
      </el-form-item>

      <el-form-item :label="t('service.fldAtPublicUrl')">
        <el-input
          v-model="form.publicAgriosUrl"
          :placeholder="t('service.fldAtPublicUrlHint')"
        />
        <div class="field-hint">{{ t('service.fldAtPublicUrlNote') }}</div>
      </el-form-item>
    </el-form>

    <!-- After creation, surface the inbound webhook URL the operator must paste into AT dashboard -->
    <el-alert
      v-if="result"
      type="success"
      :closable="false"
      show-icon
      class="result"
    >
      <template #title>{{ t('service.smsResultTitle') }}</template>
      <div class="result-body">
        <p>{{ t('service.smsResultBody') }}</p>
        <el-input v-model="result.inboundWebhookUrl" readonly class="result-input">
          <template #append>
            <el-button @click="copyUrl">{{ t('service.copy') }}</el-button>
          </template>
        </el-input>
      </div>
    </el-alert>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { setupSmsInbox } from '@/api/service'

const { t } = useI18n()
const emit = defineEmits(['submitted'])

const formRef = ref(null)
const result = ref(null)

const form = ref({
  name: 'SMS via Africa\'s Talking',
  username: 'sandbox',
  apiKey: '',
  senderId: '',
  sandbox: true,
  publicAgriosUrl: '',
})

const rules = {
  name:     [{ required: true, message: () => t('service.required'), trigger: 'blur' }],
  username: [{ required: true, message: () => t('service.required'), trigger: 'blur' }],
  apiKey:   [{ required: true, message: () => t('service.required'), trigger: 'blur' }],
}

async function submit() {
  await formRef.value.validate()
  const { data } = await setupSmsInbox({ ...form.value })
  result.value = data
  // Hold the wizard open so the operator can copy the inbound webhook URL.
  // The 'submitted' event closes the wizard, so we delay it by one tick so
  // the operator sees the success card. If they want to close right away,
  // they can click cancel.
  setTimeout(() => emit('submitted', data?.inbox), 300)
}

function copyUrl() {
  if (!result.value?.inboundWebhookUrl) return
  navigator.clipboard.writeText(result.value.inboundWebhookUrl)
  ElMessage.success(t('service.copied'))
}

defineExpose({ submit })
</script>

<style scoped>
.step { display: flex; flex-direction: column; gap: 16px; }
.hint { margin-bottom: 4px; }
.hint-body p { margin: 4px 0; color: #5b6b62; font-size: 12px; }
.hint-body ul { margin: 6px 0 6px 18px; color: #5b6b62; font-size: 12px; }
.hint-body ul li { margin: 2px 0; }
.hint-body a { color: #0f3a26; text-decoration: underline; }
.field-hint { margin-top: 4px; font-size: 11px; color: #8a9690; }
.result { margin-top: 8px; }
.result-body p { margin: 4px 0 10px; font-size: 12px; color: #1c2e25; }
.result-input { font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; }
</style>
