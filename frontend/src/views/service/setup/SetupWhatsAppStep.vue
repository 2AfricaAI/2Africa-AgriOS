<template>
  <div class="step">
    <el-alert type="info" :closable="false" show-icon class="hint">
      <template #title>{{ t('service.whatsappHintTitle') }}</template>
      <div class="hint-body">
        <p>{{ t('service.whatsappHintLine1') }}</p>
        <ol>
          <li>{{ t('service.whatsappStep1') }}</li>
          <li>{{ t('service.whatsappStep2') }}</li>
          <li>{{ t('service.whatsappStep3') }}</li>
          <li>{{ t('service.whatsappStep4') }}</li>
        </ol>
        <p>
          <a href="https://business.facebook.com/wa/manage/" target="_blank" rel="noopener">
            Meta Business Manager →
          </a>
        </p>
      </div>
    </el-alert>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item :label="t('service.fldInboxName')" prop="name">
        <el-input v-model="form.name" :placeholder="t('service.fldInboxNameHint')" />
      </el-form-item>

      <el-form-item :label="t('service.fldPhoneNumber')" prop="phoneNumber">
        <el-input
          v-model="form.phoneNumber"
          placeholder="+254700123456"
        />
        <div class="field-hint">{{ t('service.fldPhoneNumberHint') }}</div>
      </el-form-item>

      <el-form-item :label="t('service.fldMetaApiToken')" prop="apiToken">
        <el-input
          v-model="form.apiToken"
          type="password"
          show-password
          :placeholder="t('service.fldMetaApiTokenHint')"
        />
      </el-form-item>

      <el-form-item :label="t('service.fldPhoneNumberId')" prop="phoneNumberId">
        <el-input
          v-model="form.phoneNumberId"
          :placeholder="t('service.fldPhoneNumberIdHint')"
        />
      </el-form-item>

      <el-form-item :label="t('service.fldBusinessAccountId')" prop="businessAccountId">
        <el-input
          v-model="form.businessAccountId"
          :placeholder="t('service.fldBusinessAccountIdHint')"
        />
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { setupWhatsAppInbox } from '@/api/service'

const { t } = useI18n()
const emit = defineEmits(['submitted'])

const formRef = ref(null)
const form = ref({
  name: '',
  phoneNumber: '',
  apiToken: '',
  phoneNumberId: '',
  businessAccountId: '',
})

const rules = {
  name:               [{ required: true, message: () => t('service.required'), trigger: 'blur' }],
  phoneNumber:        [{ required: true, message: () => t('service.required'), trigger: 'blur' }],
  apiToken:           [{ required: true, message: () => t('service.required'), trigger: 'blur' }],
  phoneNumberId:      [{ required: true, message: () => t('service.required'), trigger: 'blur' }],
  businessAccountId:  [{ required: true, message: () => t('service.required'), trigger: 'blur' }],
}

async function submit() {
  await formRef.value.validate()
  const data = await setupWhatsAppInbox({ ...form.value })
  emit('submitted', data)
}

defineExpose({ submit })
</script>

<style scoped>
.step { display: flex; flex-direction: column; gap: 16px; }
.hint { margin-bottom: 4px; }
.hint-body p { margin: 4px 0; color: #5b6b62; font-size: 12px; }
.hint-body ol { margin: 6px 0 6px 18px; color: #5b6b62; font-size: 12px; }
.hint-body ol li { margin: 2px 0; }
.hint-body a { color: #0f3a26; text-decoration: underline; }
.field-hint { margin-top: 4px; font-size: 11px; color: #8a9690; }
</style>
