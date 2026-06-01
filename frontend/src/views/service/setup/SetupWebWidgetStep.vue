<template>
  <div class="step">
    <el-alert type="success" :closable="false" show-icon class="hint">
      <template #title>{{ t('service.webWidgetHintTitle') }}</template>
      <div class="hint-body">
        <p>{{ t('service.webWidgetHint') }}</p>
      </div>
    </el-alert>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item :label="t('service.fldInboxName')" prop="name">
        <el-input v-model="form.name" :placeholder="t('service.fldInboxNameHint')" />
      </el-form-item>

      <el-form-item :label="t('service.fldWebsiteUrl')" prop="websiteUrl">
        <el-input
          v-model="form.websiteUrl"
          placeholder="https://yourfarm.example.com"
        />
      </el-form-item>

      <el-form-item :label="t('service.fldWelcomeTitle')">
        <el-input v-model="form.welcomeTitle" placeholder="Hi there!" />
      </el-form-item>

      <el-form-item :label="t('service.fldWelcomeTagline')">
        <el-input
          v-model="form.welcomeTagline"
          type="textarea"
          :rows="2"
          placeholder="We're here to help."
        />
      </el-form-item>

      <el-form-item :label="t('service.fldWidgetColor')">
        <el-color-picker v-model="form.widgetColor" show-alpha :predefine="presetColors" />
        <span class="color-hint">{{ form.widgetColor || '#0F3A26' }}</span>
      </el-form-item>
    </el-form>

    <div class="preview" v-if="form.welcomeTitle || form.welcomeTagline">
      <div class="preview-label">{{ t('service.preview') }}</div>
      <div class="preview-card" :style="{ background: form.widgetColor || '#0F3A26' }">
        <div class="preview-title">{{ form.welcomeTitle || 'Hi there!' }}</div>
        <div class="preview-sub">{{ form.welcomeTagline || "We're here to help." }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { setupWebWidgetInbox } from '@/api/service'

const { t } = useI18n()
const emit = defineEmits(['submitted'])

const formRef = ref(null)
const form = ref({
  name: 'Website Chat',
  websiteUrl: '',
  welcomeTitle: 'Hi there!',
  welcomeTagline: "We're here to help.",
  widgetColor: '#0F3A26',
})

const rules = {
  name:       [{ required: true, message: () => t('service.required'), trigger: 'blur' }],
  websiteUrl: [
    { required: true, message: () => t('service.required'), trigger: 'blur' },
    { pattern: /^https?:\/\//, message: () => t('service.invalidUrl'), trigger: 'blur' },
  ],
}

const presetColors = [
  '#0F3A26', // AgriOS green
  '#009CE0', // Chatwoot default
  '#E67E22', // orange
  '#8E44AD', // purple
  '#34495E', // slate
  '#27AE60', // bright green
]

async function submit() {
  await formRef.value.validate()
  const data = await setupWebWidgetInbox({ ...form.value })
  emit('submitted', data)
}

defineExpose({ submit })
</script>

<style scoped>
.step { display: flex; flex-direction: column; gap: 16px; }
.hint { margin-bottom: 4px; }
.hint-body p { margin: 4px 0; color: #5b6b62; font-size: 12px; }
.color-hint {
  margin-left: 10px;
  font-size: 12px;
  color: #8a9690;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
}

.preview {
  border-top: 1px dashed #e6ece9;
  padding-top: 12px;
}
.preview-label {
  font-size: 11px;
  color: #8a9690;
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.preview-card {
  border-radius: 12px;
  padding: 16px 18px;
  color: #fff;
}
.preview-title {
  font-size: 16px;
  font-weight: 600;
}
.preview-sub {
  font-size: 13px;
  opacity: 0.9;
  margin-top: 4px;
}
</style>
