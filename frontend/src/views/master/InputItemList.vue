<template>
  <div class="page">
    <!-- 过滤器 -->
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item :label="t('inputItem.code')">
          <el-input v-model="query.code" :placeholder="t('common.placeholder')" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item :label="t('inputItem.name')">
          <el-input v-model="query.name" :placeholder="t('common.placeholder')" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item :label="t('inputItem.type')">
          <el-select v-model="query.inputType" :placeholder="t('common.all')" clearable style="width: 140px">
            <el-option v-for="t_ in INPUT_TYPES" :key="t_.value" :label="t(`inputItem.type${cap(t_.value)}`)" :value="t_.value" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('inputItem.defaultSupplier')">
          <el-select v-model="query.supplierId" :placeholder="t('common.all')" clearable filterable style="width: 180px">
            <el-option v-for="s in suppliers" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('common.status')">
          <el-select v-model="query.status" :placeholder="t('common.all')" clearable style="width: 120px">
            <el-option :label="t('common.enable')" value="active" />
            <el-option :label="t('common.disable')" value="inactive" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="SearchIcon" @click="reload(1)">{{ t('common.search') }}</el-button>
          <el-button :icon="RefreshIcon" @click="onReset">{{ t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 工具栏 + 表格 -->
    <el-card shadow="never" class="table-card">
      <div class="toolbar">
        <el-button type="primary" :icon="PlusIcon" @click="onCreate">{{ t('inputItem.new') }}</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border stripe row-key="id">
        <el-table-column prop="code" :label="t('inputItem.code')" width="100" />
        <el-table-column :label="t('inputItem.name')" min-width="180">
          <template #default="{ row }">
            <div>{{ row.name }}</div>
            <div v-if="row.nameEn" class="muted">{{ row.nameEn }}</div>
          </template>
        </el-table-column>
        <el-table-column :label="t('inputItem.type')" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="typeTagColor(row.inputType)" size="small">
              {{ t(`inputItem.type${cap(row.inputType)}`) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="spec" :label="t('inputItem.spec')" width="100" />
        <el-table-column :label="t('inputItem.packing')" width="100" align="center">
          <template #default="{ row }">
            <span v-if="row.packQty">{{ row.packQty }}{{ row.packUnitLabel ? `/${row.packUnitLabel}` : '' }}</span>
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="unit" :label="t('inputItem.unit')" width="60" align="center" />
        <el-table-column prop="defaultWarehouseName" :label="t('inputItem.warehouse')" min-width="110" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.defaultWarehouseName">{{ row.defaultWarehouseName }}</span>
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('inputItem.minStock')" width="90" align="right">
          <template #default="{ row }">
            <span v-if="row.minStockQty != null">{{ row.minStockQty }}</span>
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('inputItem.phi')" width="80" align="center">
          <template #default="{ row }">
            <span v-if="row.inputType === 'pesticide'">
              <el-tag :type="row.phiDays >= 14 ? 'danger' : row.phiDays >= 7 ? 'warning' : 'info'" size="small">
                {{ row.phiDays }}d
              </el-tag>
            </span>
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="activeIngredient" :label="t('inputItem.activeIngredient')" min-width="140" show-overflow-tooltip />
        <el-table-column prop="registrationNo" :label="t('inputItem.regNo')" width="130" show-overflow-tooltip />
        <el-table-column prop="defaultSupplierName" :label="t('inputItem.defaultSupplier')" min-width="120" show-overflow-tooltip />
        <el-table-column :label="t('common.status')" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'info'" size="small">
              {{ row.status === 'active' ? t('common.enable') : t('common.disable') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.actions')" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link @click="onEdit(row)">{{ t('common.edit') }}</el-button>
            <el-button :type="row.status === 'active' ? 'warning' : 'success'" link @click="onToggle(row)">
              {{ row.status === 'active' ? t('common.actionDisable') : t('common.actionEnable') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          background layout="total, sizes, prev, pager, next, jumper"
          :total="total" v-model:current-page="query.page" v-model:page-size="query.size"
          :page-sizes="[10, 20, 50, 100]"
          @current-change="() => reload()" @size-change="() => reload(1)"
        />
      </div>
    </el-card>

    <!-- Create/Edit dialog -->
    <el-dialog
      v-model="dlg.visible"
      :title="dlg.id ? t('inputItem.editTitle') : t('inputItem.createTitle')"
      width="640px" :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item :label="t('inputItem.code')" prop="code">
          <el-input v-model="form.code" placeholder="e.g. II-0007" maxlength="32" />
        </el-form-item>
        <el-form-item :label="t('inputItem.name')" prop="name">
          <el-input v-model="form.name" maxlength="128" />
        </el-form-item>
        <el-form-item :label="t('inputItem.nameEn')">
          <el-input v-model="form.nameEn" maxlength="128" />
        </el-form-item>
        <el-form-item :label="t('inputItem.type')" prop="inputType">
          <el-select v-model="form.inputType" style="width: 100%">
            <el-option v-for="t_ in INPUT_TYPES" :key="t_.value" :label="t(`inputItem.type${cap(t_.value)}`)" :value="t_.value" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('inputItem.categoryL2')">
          <el-input v-model="form.categoryL2" :placeholder="t('inputItem.categoryL2Hint')" maxlength="64" />
        </el-form-item>
        <el-form-item :label="t('inputItem.spec')">
          <el-input v-model="form.spec" placeholder="e.g. 50kg/bag" maxlength="128" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item :label="t('inputItem.packQty')">
              <el-input-number v-model="form.packQty" :min="0.001" :precision="3" :controls="false" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="t('inputItem.packUnit')">
              <el-input v-model="form.packUnitLabel" placeholder="bag / bottle / box" maxlength="32" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item :label="t('inputItem.unit')" prop="unit">
          <el-input v-model="form.unit" placeholder="kg / L / pack / box / pcs" maxlength="16" />
        </el-form-item>
        <!-- pesticide-only fields -->
        <template v-if="form.inputType === 'pesticide'">
          <el-form-item :label="t('inputItem.activeIngredient')">
            <el-input v-model="form.activeIngredient" placeholder="e.g. Glyphosate 41%" maxlength="128" />
          </el-form-item>
          <el-form-item :label="t('inputItem.regNo')">
            <el-input v-model="form.registrationNo" placeholder="e.g. PCPB(CR)1234" maxlength="64" />
          </el-form-item>
          <el-form-item :label="t('inputItem.phi')" prop="phiDays">
            <el-input-number v-model="form.phiDays" :min="0" :max="365" />
            <span class="muted" style="margin-left: 10px">{{ t('inputItem.phiHint') }}</span>
          </el-form-item>
        </template>
        <el-form-item :label="t('inputItem.defaultSupplier')">
          <el-select v-model="form.defaultSupplierId" filterable clearable style="width: 100%">
            <el-option v-for="s in suppliers" :key="s.id" :label="`${s.code} · ${s.name}`" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('inputItem.defaultWarehouse')">
          <el-select v-model="form.defaultWarehouseId" filterable clearable style="width: 100%"
                     :placeholder="t('inputItem.selectWarehouse')">
            <el-option v-for="w in warehouses" :key="w.id" :label="`${w.code} · ${w.name}`" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('inputItem.minStock')">
          <el-input-number v-model="form.minStockQty" :min="0" :precision="3" :controls="false" style="width: 200px" />
          <span class="muted" style="margin-left: 8px">{{ form.unit || 'kg' }}</span>
        </el-form-item>
        <el-form-item :label="t('common.remark')">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="255" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg.visible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="onSubmit">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search as SearchIcon, Refresh as RefreshIcon, Plus as PlusIcon,
} from '@element-plus/icons-vue'
import {
  listInputItems, createInputItem, updateInputItem, toggleInputItemStatus,
} from '@/api/inputItem'
import { listSuppliers } from '@/api/supplier'
import { listWarehouses } from '@/api/warehouse'

const { t } = useI18n()

// Sprint 22.0 - 8 categories aligned with warehouse.purpose
const INPUT_TYPES = [
  { value: 'seed'         },
  { value: 'fertilizer'   },
  { value: 'pesticide'    },
  { value: 'construction' },
  { value: 'spare_parts'  },
  { value: 'tools'        },
  { value: 'packaging'    },
  { value: 'other'        },
]
// camelCase helper for i18n key 'inputItem.typeSpareParts' from 'spare_parts'
const cap = (s) => {
  if (!s) return ''
  return s.split('_').map(p => p.charAt(0).toUpperCase() + p.slice(1)).join('')
}
const typeTagColor = (t_) => ({
  seed:         'warning',
  fertilizer:   'success',
  pesticide:    'danger',
  construction: 'info',
  spare_parts:  'info',
  tools:        'info',
  packaging:    'primary',
  other:        'info',
})[t_] || 'info'

// ----- query -----
const query = reactive({
  code: '', name: '', inputType: '', supplierId: null, status: '',
  page: 1, size: 20,
})
const list = ref([])
const total = ref(0)
const loading = ref(false)

async function reload(p) {
  if (p) query.page = p
  loading.value = true
  try {
    const data = await listInputItems(query)
    list.value = data.list || []
    total.value = data.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}
function onReset() {
  Object.assign(query, {
    code: '', name: '', inputType: '', supplierId: null, status: '',
    page: 1, size: 20,
  })
  reload()
}

// ----- suppliers (for dropdown) -----
const suppliers = ref([])
async function loadSuppliers() {
  try {
    const data = await listSuppliers({ status: 'active', page: 1, size: 200 })
    suppliers.value = data.list || []
  } catch {/* ignore */}
}

// ----- warehouses (for default warehouse dropdown, Sprint 22.1.5) -----
const warehouses = ref([])
async function loadWarehouses() {
  try {
    const data = await listWarehouses({ status: 1, page: 1, size: 200 })
    warehouses.value = data.list || []
  } catch {/* ignore */}
}

// ----- create / edit -----
const dlg = reactive({ visible: false, id: null })
const form = reactive({
  code: '', name: '', nameEn: '', inputType: 'fertilizer',
  categoryL2: '', spec: '', packQty: null, packUnitLabel: '',
  unit: 'kg', activeIngredient: '', registrationNo: '',
  phiDays: 0, defaultSupplierId: null, defaultWarehouseId: null,
  minStockQty: null, status: 'active', remark: '',
})
const rules = {
  code:      [{ required: true, message: () => t('valid.required', { field: t('inputItem.code') }), trigger: 'blur' }],
  name:      [{ required: true, message: () => t('valid.required', { field: t('inputItem.name') }), trigger: 'blur' }],
  inputType: [{ required: true, message: () => t('valid.required', { field: t('inputItem.type') }), trigger: 'change' }],
  unit:      [{ required: true, message: () => t('valid.required', { field: t('inputItem.unit') }), trigger: 'blur' }],
}
const formRef = ref(null)
const saving = ref(false)

function onCreate() {
  Object.assign(form, {
    code: '', name: '', nameEn: '', inputType: 'fertilizer',
    categoryL2: '', spec: '', packQty: null, packUnitLabel: '',
    unit: 'kg', activeIngredient: '', registrationNo: '',
    phiDays: 0, defaultSupplierId: null, defaultWarehouseId: null,
    minStockQty: null, status: 'active', remark: '',
  })
  dlg.id = null
  dlg.visible = true
}
function onEdit(row) {
  Object.assign(form, {
    code: row.code, name: row.name, nameEn: row.nameEn || '', inputType: row.inputType,
    categoryL2: row.categoryL2 || '', spec: row.spec || '',
    packQty: row.packQty ?? null, packUnitLabel: row.packUnitLabel || '',
    unit: row.unit, activeIngredient: row.activeIngredient || '',
    registrationNo: row.registrationNo || '', phiDays: row.phiDays || 0,
    defaultSupplierId: row.defaultSupplierId || null,
    defaultWarehouseId: row.defaultWarehouseId || null,
    minStockQty: row.minStockQty ?? null,
    status: row.status, remark: row.remark || '',
  })
  dlg.id = row.id
  dlg.visible = true
}
async function onSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (dlg.id) {
      await updateInputItem(dlg.id, form)
      ElMessage.success(t('common.updateSuccess'))
    } else {
      await createInputItem(form)
      ElMessage.success(t('common.createSuccess'))
    }
    dlg.visible = false
    reload()
  } catch (e) {
    console.error(e)
  } finally {
    saving.value = false
  }
}

async function onToggle(row) {
  const next = row.status === 'active' ? 'inactive' : 'active'
  const action = next === 'active' ? t('common.actionEnable') : t('common.actionDisable')
  await ElMessageBox.confirm(
    t('inputItem.confirmToggle', { action, name: row.name }),
    t('common.tip'),
    { type: 'warning' },
  ).catch(() => Promise.reject('cancel'))
  await toggleInputItemStatus(row.id, next)
  ElMessage.success(t('common.operationSuccess'))
  reload()
}

onMounted(() => { loadSuppliers(); loadWarehouses(); reload() })
</script>

<style scoped>
.page { padding: 16px; }
.filter-card { margin-bottom: 12px; }
.toolbar { margin-bottom: 12px; }
.pager { margin-top: 12px; text-align: right; }
.muted { color: #909399; font-size: 11px; }
</style>
