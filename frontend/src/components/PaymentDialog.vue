<template>
  <el-dialog
    v-model="visible"
    :title="t('payment.new')"
    width="520px"
    :close-on-click-modal="false"
    destroy-on-close
    @closed="reset"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
      <el-form-item v-if="orderInfo" :label="t('order.code')">
        <span><code class="oc">{{ orderInfo.code }}</code> · {{ orderInfo.customerName }}</span>
        <div class="dim small">
          {{ t('order.totalAmount') }}: {{ fmt(orderInfo.totalAmount) }} {{ orderInfo.currency }}
          · {{ t('paymentStatus.paidAmount') }}: {{ fmt(orderInfo.paidAmount) }}
          · <strong style="color:#e6a23c">{{ t('paymentStatus.outstanding') }}: {{ fmt(outstanding) }}</strong>
        </div>
      </el-form-item>

      <el-form-item :label="t('payment.amount')" prop="amount">
        <el-input-number v-model="form.amount" :min="0.01" :max="outstanding" :precision="2"
                         :step="100" :controls="false" style="width: 100%" />
      </el-form-item>

      <el-form-item :label="t('order.currency')" prop="currency">
        <el-select v-model="form.currency" style="width: 100%">
          <el-option label="KES" value="KES" />
          <el-option label="USD" value="USD" />
          <el-option label="EUR" value="EUR" />
        </el-select>
      </el-form-item>

      <el-form-item :label="t('payment.method')" prop="method">
        <el-select v-model="form.method" style="width: 100%">
          <el-option :label="t('payment.methodCash')"       value="cash" />
          <el-option :label="t('payment.methodBank')"       value="bank" />
          <el-option :label="t('payment.methodCheque')"     value="cheque" />
          <el-option :label="t('payment.methodLoopOnline')" value="loop_online" />
          <el-option :label="t('payment.methodLoopPos')"    value="loop_pos" />
        </el-select>
      </el-form-item>

      <el-form-item :label="t('payment.referenceNo')">
        <el-input v-model="form.referenceNo" maxlength="64"
                  :placeholder="refPlaceholder" />
      </el-form-item>

      <el-form-item v-if="form.method === 'loop_pos'" :label="t('payment.posTerminalId')">
        <el-input v-model="form.posTerminalId" maxlength="64" placeholder="e.g. TERM-001" />
      </el-form-item>

      <el-form-item v-if="['loop_online','loop_pos'].includes(form.method)" :label="t('payment.channel')">
        <el-select v-model="form.channel" clearable style="width: 100%">
          <el-option :label="t('payment.channelMpesa')" value="mpesa" />
          <el-option :label="t('payment.channelCard')"  value="card" />
          <el-option :label="t('payment.channelBank')"  value="bank" />
        </el-select>
      </el-form-item>

      <el-form-item :label="t('payment.paymentDate')" prop="paymentDate">
        <el-date-picker v-model="form.paymentDate" type="date" value-format="YYYY-MM-DD"
                        style="width: 100%" />
      </el-form-item>

      <el-form-item :label="t('common.remark')">
        <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="255" />
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
import { createPayment } from '@/api/finance'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  orderInfo: { type: Object, default: null },  // { id, code, customerName, totalAmount, paidAmount, currency }
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
  orderId: null,
  amount: null,
  currency: 'KES',
  fxRate: 1,
  method: 'cash',
  referenceNo: '',
  posTerminalId: '',
  channel: '',
  paymentDate: new Date().toISOString().slice(0, 10),
  remark: '',
})
const form = reactive(emptyForm())

const outstanding = computed(() => {
  if (!props.orderInfo) return 0
  return Math.max(0, Number(props.orderInfo.totalAmount || 0) - Number(props.orderInfo.paidAmount || 0))
})

const refPlaceholder = computed(() => {
  if (form.method === 'loop_online' || form.method === 'loop_pos') return 'Loop transaction ID'
  if (form.method === 'bank') return 'Bank reference'
  if (form.method === 'cheque') return 'Cheque number'
  return ''
})

const rules = computed(() => ({
  amount:      [{ required: true, message: t('valid.required', { field: t('payment.amount') }), trigger: 'change' }],
  currency:    [{ required: true, message: t('valid.required', { field: t('order.currency') }), trigger: 'change' }],
  method:      [{ required: true, message: t('valid.required', { field: t('payment.method') }), trigger: 'change' }],
  paymentDate: [{ required: true, message: t('valid.required', { field: t('payment.paymentDate') }), trigger: 'change' }],
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
  if (v && props.orderInfo) {
    reset()
    form.orderId = props.orderInfo.id
    form.amount = outstanding.value
    form.currency = props.orderInfo.currency || 'KES'
  }
})

async function onSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await createPayment({ ...form })
    ElMessage.success(t('payment.submitted'))
    visible.value = false
    emit('saved')
  } catch {} finally { saving.value = false }
}
</script>

<style scoped>
.oc {
  font-family: 'Consolas', monospace;
  background: #f5f7fa; padding: 2px 8px; border-radius: 4px;
  color: #1f7a35; font-size: 12px; font-weight: 600;
}
.dim { color: #909399; margin-top: 4px; }
.small { font-size: 12px; }
</style>
