<template>
  <div class="page">
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item :label="t('qc.type')">
          <el-select v-model="query.inspectionType" :placeholder="t('common.all')" clearable style="width: 140px">
            <el-option v-for="t_ in TYPES" :key="t_" :label="t(`qc.type_${t_}`)" :value="t_" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('qc.result')">
          <el-select v-model="query.result" :placeholder="t('common.all')" clearable style="width: 150px">
            <el-option v-for="r in RESULTS" :key="r" :label="t(`qc.result_${r}`)" :value="r" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="SearchIcon" @click="reload(1)">{{ t('common.search') }}</el-button>
          <el-button :icon="RefreshIcon" @click="onReset">{{ t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <div class="toolbar">
        <span class="table-title">{{ t('qc.title') }}</span>
        <el-button type="primary" size="small" @click="openCreate">{{ t('qc.newInspection') }}</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border stripe row-key="id" @expand-change="onExpand">
        <el-table-column type="expand" width="36">
          <template #default="{ row }">
            <div class="expand-wrap" v-loading="detailLoading[row.id]">
              <el-table v-if="detailItems[row.id]" :data="detailItems[row.id]" size="small" border>
                <el-table-column type="index" label="#" width="40" align="center" />
                <el-table-column prop="checkPoint" :label="t('qc.checkPoint')" min-width="140" />
                <el-table-column prop="expectedValue" :label="t('qc.expected')" min-width="120" />
                <el-table-column prop="actualValue" :label="t('qc.actual')" min-width="120" />
                <el-table-column :label="t('qc.itemResult')" width="90" align="center">
                  <template #default="{ row: r }">
                    <el-tag :type="r.result === 'pass' ? 'success' : r.result === 'fail' ? 'danger' : 'info'" size="small">
                      {{ t(`qc.itemResult_${r.result || 'pending'}`) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="remark" :label="t('common.remark')" min-width="120" />
              </el-table>
            </div>
          </template>
        </el-table-column>

        <el-table-column :label="t('qc.code')" width="180">
          <template #default="{ row }"><code class="code-tag">{{ row.code }}</code></template>
        </el-table-column>
        <el-table-column :label="t('qc.type')" width="120">
          <template #default="{ row }">
            <el-tag size="small">{{ t(`qc.type_${row.inspectionType}`, row.inspectionType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('qc.refType')" width="130">
          <template #default="{ row }">{{ row.refType || '-' }}</template>
        </el-table-column>
        <el-table-column prop="refCode" :label="t('qc.refCode')" width="170">
          <template #default="{ row }">{{ row.refCode || '-' }}</template>
        </el-table-column>
        <el-table-column prop="inspectDate" :label="t('qc.date')" width="110" align="center" />
        <el-table-column prop="inspectorName" :label="t('qc.inspector')" width="120">
          <template #default="{ row }">{{ row.inspectorName || '-' }}</template>
        </el-table-column>
        <el-table-column :label="t('qc.result')" width="130" align="center">
          <template #default="{ row }">
            <el-tag :type="resultTag(row.result)" effect="dark" size="small">
              {{ t(`qc.result_${row.result}`, row.result) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('qc.items')" width="70" align="center">
          <template #default="{ row }">{{ row.itemCount }}</template>
        </el-table-column>
        <el-table-column :label="t('common.actions')" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">{{ t('common.edit') }}</el-button>
            <el-button link type="danger" size="small" @click="onDelete(row)">{{ t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, sizes, prev, pager, next, jumper"
          :total="total" v-model:current-page="query.page" v-model:page-size="query.size"
          :page-sizes="[20,50,100]"
          @current-change="() => reload()" @size-change="() => reload(1)" />
      </div>
    </el-card>

    <!-- Create/Edit dialog -->
    <el-dialog v-model="dlg.visible" :title="dlg.id ? t('qc.editTitle') : t('qc.newInspection')" width="900px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <div style="display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 12px">
          <el-form-item :label="t('qc.type')" prop="inspectionType">
            <el-select v-model="form.inspectionType" style="width: 100%">
              <el-option v-for="t_ in TYPES" :key="t_" :label="t(`qc.type_${t_}`)" :value="t_" />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('qc.date')" prop="inspectDate">
            <el-date-picker v-model="form.inspectDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
          <el-form-item :label="t('qc.result')" prop="result">
            <el-select v-model="form.result" style="width: 100%">
              <el-option v-for="r in RESULTS" :key="r" :label="t(`qc.result_${r}`)" :value="r" />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('qc.refType')">
            <el-input v-model="form.refType" placeholder="warehouse_inbound / activity / batch" maxlength="32" />
          </el-form-item>
          <el-form-item :label="t('qc.refCode')">
            <el-input v-model="form.refCode" maxlength="64" />
          </el-form-item>
          <el-form-item :label="'ref_id'">
            <el-input-number v-model="form.refId" :min="0" :controls="false" style="width: 100%" />
          </el-form-item>
        </div>
        <el-form-item :label="t('qc.resultRemark')">
          <el-input v-model="form.resultRemark" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>

        <el-form-item :label="t('qc.items')">
          <div style="width: 100%">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px">
              <span style="font-size: 12px; color: #909399">{{ t('qc.itemsHint') }}</span>
              <el-button link type="primary" size="small" @click="form.items.push({checkPoint:'',expectedValue:'',actualValue:'',result:'pending',remark:''})">+ {{ t('qc.addItem') }}</el-button>
            </div>
            <el-table v-if="form.items.length" :data="form.items" border size="small">
              <el-table-column :label="t('qc.checkPoint')" min-width="140">
                <template #default="{ $index }">
                  <el-input v-model="form.items[$index].checkPoint" size="small" maxlength="64" />
                </template>
              </el-table-column>
              <el-table-column :label="t('qc.expected')" width="130">
                <template #default="{ $index }">
                  <el-input v-model="form.items[$index].expectedValue" size="small" maxlength="128" />
                </template>
              </el-table-column>
              <el-table-column :label="t('qc.actual')" width="130">
                <template #default="{ $index }">
                  <el-input v-model="form.items[$index].actualValue" size="small" maxlength="128" />
                </template>
              </el-table-column>
              <el-table-column :label="t('qc.itemResult')" width="110">
                <template #default="{ $index }">
                  <el-select v-model="form.items[$index].result" size="small" style="width: 100%">
                    <el-option v-for="r in ['pending','pass','fail']" :key="r" :label="t(`qc.itemResult_${r}`)" :value="r" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column :label="t('common.remark')" min-width="120">
                <template #default="{ $index }">
                  <el-input v-model="form.items[$index].remark" size="small" maxlength="255" />
                </template>
              </el-table-column>
              <el-table-column width="50" align="center">
                <template #default="{ $index }">
                  <el-button link type="danger" size="small" @click="form.items.splice($index, 1)">×</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg.visible=false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="onSubmit">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search as SearchIcon, Refresh as RefreshIcon } from '@element-plus/icons-vue'
import { listQc, getQc, createQc, updateQc, deleteQc } from '@/api/qcInspection'

const { t } = useI18n()
const TYPES = ['incoming', 'in_process', 'outgoing']
const RESULTS = ['pending', 'pass', 'conditional_pass', 'fail']
const resultTag = (r) => ({ pass:'success', conditional_pass:'warning', fail:'danger', pending:'info' }[r] || 'info')

const query = reactive({ inspectionType:'', result:'', refType:'', dateFrom:null, dateTo:null, page:1, size:20 })
const list = ref([]); const total = ref(0); const loading = ref(false)
async function reload(p) { if(p) query.page=p; loading.value=true; try{const d=await listQc(query);list.value=d.list||[];total.value=d.total||0}finally{loading.value=false} }
function onReset() { Object.assign(query,{inspectionType:'',result:'',refType:'',dateFrom:null,dateTo:null,page:1,size:20}); reload() }

const detailItems = reactive({}); const detailLoading = reactive({})
async function onExpand(row,expanded) { if(!expanded.some(r=>r.id===row.id))return; detailLoading[row.id]=true; try{const d=await getQc(row.id);detailItems[row.id]=d.items||[]}finally{detailLoading[row.id]=false} }

// dialog
const dlg = reactive({ visible:false, id:null })
const formRef = ref(null)
const saving = ref(false)
const today = () => new Date().toISOString().slice(0,10)
const emptyForm = () => ({
  inspectionType: 'incoming', refType:'', refId: null, refCode:'',
  inspectDate: today(), result: 'pending', resultRemark:'', remark:'',
  items: [{ checkPoint:'', expectedValue:'', actualValue:'', result:'pending', remark:'' }],
})
const form = reactive(emptyForm())
const rules = {
  inspectionType: [{ required: true, message: 'required', trigger: 'change' }],
  inspectDate:    [{ required: true, message: 'required', trigger: 'change' }],
}
function openCreate() { Object.assign(form, emptyForm()); dlg.id=null; dlg.visible=true }
async function openEdit(row) {
  const d = await getQc(row.id)
  Object.assign(form, emptyForm(), {
    inspectionType: d.header.inspectionType,
    refType: d.header.refType || '',
    refId: d.header.refId,
    refCode: d.header.refCode || '',
    inspectDate: d.header.inspectDate,
    result: d.header.result,
    resultRemark: d.header.resultRemark || '',
    remark: d.header.remark || '',
    items: (d.items || []).map(it => ({...it})),
  })
  if (!form.items.length) form.items = [{ checkPoint:'', expectedValue:'', actualValue:'', result:'pending', remark:'' }]
  dlg.id = row.id; dlg.visible = true
}
async function onSubmit() {
  const valid = await formRef.value.validate().catch(()=>false)
  if (!valid) return
  saving.value = true
  try {
    if (dlg.id) {
      await updateQc(dlg.id, form)
      ElMessage.success(t('common.updateSuccess'))
    } else {
      await createQc(form)
      ElMessage.success(t('common.createSuccess'))
    }
    dlg.visible = false
    reload()
  } finally { saving.value = false }
}

async function onDelete(row) {
  await ElMessageBox.confirm(t('qc.deleteConfirm', { code: row.code }), t('common.tip'), { type:'warning' }).catch(()=>Promise.reject('c'))
  try { await deleteQc(row.id); ElMessage.success(t('common.deleteSuccess')); reload() } catch(e){if(e==='c')return}
}

onMounted(reload)
</script>

<style scoped>
.page{padding:16px}.filter-card{margin-bottom:12px}.toolbar{display:flex;justify-content:space-between;align-items:center;margin-bottom:12px}.table-title{font-size:16px;font-weight:600;color:#1f2937}.pager{margin-top:12px;text-align:right}.expand-wrap{padding:8px 12px;background:#fafafa}.code-tag{font-size:12px;background:#dcfce7;padding:2px 6px;border-radius:3px;color:#14532d}
</style>
