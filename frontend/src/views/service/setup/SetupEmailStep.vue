<template>
  <div class="step">
    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="hint"
    >
      <template #title>{{ t('service.emailHintTitle') }}</template>
      <div class="hint-body">
        <p>{{ t('service.emailHintLine1') }}</p>
        <p>{{ t('service.emailHintLine2') }}</p>
        <p>
          <a href="https://myaccount.google.com/apppasswords" target="_blank" rel="noopener">
            Gmail App Passwords →
          </a>
        </p>
      </div>
    </el-alert>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item :label="t('service.fldInboxName')" prop="name">
        <el-input v-model="form.name" :placeholder="t('service.fldInboxNameHint')" />
      </el-form-item>

      <el-form-item :label="t('service.fldEmail')" prop="email">
        <el-input v-model="form.email" placeholder="support@yourfarm.com" />
        <div v-if="detectedProvider" class="provider-tag">
          <el-icon><CheckIcon /></el-icon>
          {{ t('service.providerDetected', { name: detectedProvider }) }}
        </div>
      </el-form-item>

      <el-form-item :label="t('service.fldAppPassword')" prop="password">
        <el-input
          v-model="form.password"
          type="password"
          show-password
          :placeholder="t('service.fldAppPasswordHint')"
        />
      </el-form-item>

      <el-divider>
        <el-button text size="small" @click="showAdvanced = !showAdvanced">
          {{ showAdvanced ? t('service.hideAdvanced') : t('service.showAdvanced') }}
        </el-button>
      </el-divider>

      <template v-if="showAdvanced">
        <el-form-item :label="t('service.fldImapAddress')">
          <el-input
            v-model="form.imapAddress"
            :placeholder="advancedPlaceholder('imap')"
          />
        </el-form-item>
        <el-form-item :label="t('service.fldImapPort')">
          <el-input-number v-model="form.imapPort" :min="1" :max="65535" :placeholder="'993'" />
        </el-form-item>
        <el-form-item :label="t('service.fldSmtpAddress')">
          <el-input
            v-model="form.smtpAddress"
            :placeholder="advancedPlaceholder('smtp')"
          />
        </el-form-item>
        <el-form-item :label="t('service.fldSmtpPort')">
          <el-input-number v-model="form.smtpPort" :min="1" :max="65535" :placeholder="'587'" />
        </el-form-item>
      </template>
    </el-form>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { Check as CheckIcon } from '@element-plus/icons-vue'
import { setupEmailInbox } from '@/api/service'

const { t } = useI18n()

const emit = defineEmits(['submitted'])

const formRef = ref(null)
const showAdvanced = ref(false)

const form = ref({
  name: '',
  email: '',
  password: '',
  imapAddress: '',
  imapPort: null,
  smtpAddress: '',
  smtpPort: null,
})

const rules = {
  name:     [{ required: true, message: () => t('service.required'), trigger: 'blur' }],
  email:    [
    { required: true, message: () => t('service.required'), trigger: 'blur' },
    { type: 'email', message: () => t('service.invalidEmail'), trigger: 'blur' },
  ],
  password: [{ required: true, message: () => t('service.required'), trigger: 'blur' }],
}

const detectedProvider = computed(() => {
  const e = (form.value.email || '').toLowerCase()
  if (!e.includes('@')) return null
  const dom = e.split('@')[1]
  if (!dom) return null
  if (dom.endsWith('gmail.com') || dom.endsWith('googlemail.com')) return 'Gmail'
  if (dom.endsWith('outlook.com') || dom.endsWith('hotmail.com')
      || dom.endsWith('live.com') || dom.endsWith('office365.com')) return 'Microsoft 365'
  if (dom.endsWith('icloud.com') || dom.endsWith('me.com')) return 'iCloud'
  if (dom.endsWith('yahoo.com') || dom.endsWith('ymail.com')) return 'Yahoo'
  return null
})

function advancedPlaceholder(kind) {
  const p = detectedProvider.value
  if (!p) return kind === 'imap' ? 'imap.example.com' : 'smtp.example.com'
  if (p === 'Gmail')         return kind === 'imap' ? 'imap.gmail.com'        : 'smtp.gmail.com'
  if (p === 'Microsoft 365') return kind === 'imap' ? 'outlook.office365.com' : 'smtp.office365.com'
  if (p === 'iCloud')        return kind === 'imap' ? 'imap.mail.me.com'      : 'smtp.mail.me.com'
  if (p === 'Yahoo')         return kind === 'imap' ? 'imap.mail.yahoo.com'   : 'smtp.mail.yahoo.com'
  return kind === 'imap' ? 'imap.example.com' : 'smtp.example.com'
}

async function submit() {
  await formRef.value.validate()
  const payload = {
    name:         form.value.name,
    email:        form.value.email,
    password:     form.value.password,
    imapAddress:  form.value.imapAddress || null,
    imapPort:     form.value.imapPort || null,
    smtpAddress:  form.value.smtpAddress || null,
    smtpPort:     form.value.smtpPort || null,
  }
  const data = await setupEmailInbox(payload)
  emit('submitted', data)
}

defineExpose({ submit })
</script>

<style scoped>
.step { display: flex; flex-direction: column; gap: 16px; }
.hint { margin-bottom: 4px; }
.hint-body p { margin: 4px 0; color: #5b6b62; font-size: 12px; }
.hint-body a { color: #0f3a26; text-decoration: underline; }
.provider-tag {
  margin-top: 4px;
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: #2c8a4f;
}
</style>
