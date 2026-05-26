<template>
  <el-dialog
    v-model="visible"
    :title="t('collection.new')"
    width="560px"
    :close-on-click-modal="false"
    destroy-on-close
    @closed="reset"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
      <el-form-item v-if="customerInfo" :label="t('ar.customer')">
        <el-tag size="small" type="primary">{{ customerInfo.name }}</el-tag>
        <code class="dim small" style="margin-left: 6px">{{ customerInfo.code }}</code>
        <div v-if="customerInfo.outstanding != null" class="dim small" style="margin-top: 4px">
          {{ t('paymentStatus.outstanding') }}:
          <strong style="color:#e6a23c">{{ fmt(customerInfo.outstanding) }} KES</strong>
        </div>
      </el-form-item>

      <el-form-item :label="t('collection.logDate')" prop="logDate">
        <el-date-picker v-model="form.logDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
      </el-form-item>

      <el-form-item :label="t('collection.channel')" prop="channel">
        <el-select v-model="form.channel" style="width: 100%">
          <el-option :label="t('collection.channelPhone')"    value="phone" />
          <el-option :label="t('collection.channelWhatsapp')" value="whatsapp" />
          <el-option :label="t('collection.channelSms')"      value="sms" />
          <el-option :label="t('collection.channelEmail')"    value="email" />
          <el-option :label="t('collection.channelVisit')"    value="visit" />
          <el-option :label="t('collection.channelOther')"    value="other" />
        </el-select>
      </el-form-item>

      <el-form-item :label="t('collection.contactPerson')">
        <el-input v-model="form.contactPerson" maxlength="80" :placeholder="t('collection.contactPersonPlaceholder')" />
      </el-form-item>

      <el-form-item :label="t('collection.outcome')" prop="outcome">
        <el-select v-model="form.outcome" style="width: 100%">
          <el-option :label="t('collection.outcomePromised')" value="promised" />
          <el-option :label="t('collection.outcomeRefused')"  value="refused" />
          <el-option :label="t('collection.outcomeNoAnswer')" value="no_answer" />
          <el-option :label="t('collection.outcomeDisputed')" value="disputed" />
          <el-option :label="t('collection.outcomePaid')"     value="paid" />
          <el-option :label="t('collection.outcomeOther')"    value="other" />
        </el-select>
      </el-form-item>

      <el-form-item
        v-if="form.outcome === 'promised'"
        :label="t('collection.promisedDate')"
        prop="promisedDate"
      >
        <el-date-picker v-model="form.promisedDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
      </el-form-item>

      <el-form-item v-if="form.outcome === 'promised'" :label="t('collection.promisedAmount')">
        <el-input-number v-model="form.promisedAmount" :min="0" :precision="2" :step="1000"
                         :controls="false" style="width: 100%" />
      </el-form-item>

      <el-form-item :label="t('collection.nextActionDate')">
        <el-date-picker v-model="form.nextActionDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
      </el-form-item>

      <el-form-item :label="t('collection.content')">
        <el-input v-model="form.content" type="textarea" :rows="3" maxlength="2000"
                  :placeholder="t('collection.contentPlaceholder')" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">{{ t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="saving" @click="onSubmit">
        {{ t('common.save') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { createCollection } from '@/api/finance'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  /** { id, code, name, outstanding? } */
  customerInfo: { type: Object, default: null },
  /** 可选 — 如果是针对单张订单跟催 */
  orderId: { type: Number, default: null },
})
const emit = defineEmits(['update:modelValue', 'saved'])

const { t } = useI18n()
const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const formRef = ref(null)
const saving = ref(false)

const emptyForm = () => ({
  customerId: null,
  orderId: null,
  logDate: new Date().toISOString().slice(0, 10),
  channel: 'phone',
  contactPerson: '',
  outcome: 'promised',
  promisedDate: null,
  promisedAmount: null,
  content: '',
  nextActionDate: null,
})
const form = reactive(emptyForm())

const rules = computed(() => ({
  logDate:      [{ required: true, message: t('valid.required', { field: t('collection.logDate') }), trigger: 'change' }],
  channel:      [{ required: true, message: t('valid.required', { field: t('collection.channel') }), trigger: 'change' }],
  outcome:      [{ required: true, message: t('valid.required', { field: t('collection.outcome') }), trigger: 'change' }],
  promisedDate: [{
    validator(_, v, cb) {
      if (form.outcome === 'promised' && !v) {
        cb(new Error(t('valid.required', { field: t('collection.promisedDate') })))
      } else { cb() }
    },
    trigger: 'change',
  }],
}))

function fmt(v) {
  if (v == null) return '0.00'
  return Number(v).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function reset() {
  Object.assign(form, emptyForm())
  formRef.value?.clearValidate()
}

watch(visible, (v) => {
  if (v && props.customerInfo) {
    reset()
    form.customerId = props.customerInfo.id
    form.orderId    = props.orderId
  }
})

async function onSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await createCollection({ ...form })
    ElMessage.success(t('collection.saved'))
    visible.value = false
    emit('saved')
  } catch {} finally { saving.value = false }
}
</script>

<style scoped>
.dim { color: #909399; }
.small { font-size: 12px; }
</style>
