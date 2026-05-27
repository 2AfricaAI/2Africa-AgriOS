<template>
  <div class="page">
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item :label="t('common.status')">
          <el-select v-model="query.status" :placeholder="t('common.all')" clearable style="width: 120px">
            <el-option v-for="s in ['draft','picked','confirmed','cancelled']" :key="s"
              :label="t(`outbound.status_${s}`)" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('outbound.warehouse')">
          <el-select v-model="query.warehouseId" :placeholder="t('common.all')" clearable filterable style="width: 180px">
            <el-option v-for="w in warehouses" :key="w.id" :value="w.id" :label="`${w.code} · ${w.name}`" />
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
        <span class="table-title">{{ t('outbound.title') }}</span>
        <el-button type="primary" size="small" @click="newDlg.visible = true">{{ t('outbound.newManual') }}</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border stripe row-key="id" @expand-change="onExpand">
        <el-table-column type="expand" width="36">
          <template #default="{ row }">
            <div class="expand-wrap" v-loading="detailLoading[row.id]">
              <el-table v-if="detailItems[row.id]" :data="detailItems[row.id]" size="small" border>
                <el-table-column prop="inputItemCode" :label="t('inputItem.code')" width="100" />
                <el-table-column prop="inputItemName" :label="t('inputItem.name')" min-width="150" />
                <el-table-column prop="unit" :label="t('inputItem.unit')" width="60" align="center" />
                <el-table-column :label="t('outbound.requested')" width="100" align="right">
                  <template #default="{ row: r }"><strong>{{ fmtQty(r.requestedQty) }}</strong></template>
                </el-table-column>
                <el-table-column :label="t('outbound.picked')" width="100" align="right">
                  <template #default="{ row: r }">
                    <span v-if="r.pickedQty != null">{{ fmtQty(r.pickedQty) }}</span>
                    <span v-else class="dim">-</span>
                  </template>
                </el-table-column>
                <el-table-column :label="t('outbound.actual')" width="100" align="right">
                  <template #default="{ row: r }">
                    <span v-if="r.actualQty != null" class="text-red">{{ fmtQty(r.actualQty) }}</span>
                    <span v-else class="dim">-</span>
                  </template>
                </el-table-column>
                <el-table-column prop="remark" :label="t('common.remark')" min-width="100">
                  <template #default="{ row: r }">{{ r.remark || '-' }}</template>
                </el-table-column>
              </el-table>
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="t('outbound.code')" width="180">
          <template #default="{ row }"><code class="code-tag">{{ row.code }}</code></template>
        </el-table-column>
        <el-table-column :label="t('outbound.source')" width="130" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ t(`outbound.src_${row.sourceType}`, row.sourceType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="warehouseName" :label="t('outbound.warehouse')" min-width="130" show-overflow-tooltip />
        <el-table-column :label="t('outbound.items')" width="60" align="center">
          <template #default="{ row }">{{ row.itemCount }}</template>
        </el-table-column>
        <el-table-column :label="t('common.status')" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag[row.status]" size="small" effect="dark">{{ t(`outbound.status_${row.status}`) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('outbound.createdAt')" width="150" align="center">
          <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column :label="t('common.actions')" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button v-if="row.status==='draft'" type="warning" link size="small" @click="openPickDlg(row)">{{ t('outbound.pick') }}</el-button>
            <el-button v-if="row.status==='picked'" type="success" link size="small" @click="onConfirm(row)">{{ t('outbound.confirm') }}</el-button>
            <el-button v-if="row.status!=='confirmed'" type="danger" link size="small" @click="onCancel(row)">{{ t('common.cancel') }}</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination background layout="total, sizes, prev, pager, next, jumper"
          :total="total" v-model:current-page="query.page" v-model:page-size="query.size"
          :page-sizes="[20,50,100]" @current-change="()=>reload()" @size-change="()=>reload(1)" />
      </div>
    </el-card>

    <!-- Pick dialog -->
    <el-dialog v-model="pickDlg.visible" :title="t('outbound.pickTitle')" width="700px" :close-on-click-modal="false">
      <el-table :data="pickDlg.items" border size="small">
        <el-table-column prop="inputItemCode" :label="t('inputItem.code')" width="100" />
        <el-table-column prop="inputItemName" :label="t('inputItem.name')" min-width="150" />
        <el-table-column :label="t('outbound.requested')" width="100" align="right">
          <template #default="{ row }"><strong>{{ fmtQty(row.requestedQty) }}</strong></template>
        </el-table-column>
        <el-table-column :label="t('outbound.picked')" width="130">
          <template #default="{ row, $index }">
            <el-input-number v-model="pickDlg.items[$index].pickedQty" :min="0" :precision="3" :controls="false" size="small" style="width:100%" />
          </template>
        </el-table-column>
        <el-table-column :label="t('common.remark')" min-width="120">
          <template #default="{ row, $index }">
            <el-input v-model="pickDlg.items[$index].remark" size="small" maxlength="255" />
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="pickDlg.visible=false">{{ t('common.cancel') }}</el-button>
        <el-button type="warning" :loading="submitting" @click="onPickSubmit">{{ t('outbound.pickSubmit') }}</el-button>
      </template>
    </el-dialog>

    <!-- New manual outbound dialog -->
    <el-dialog v-model="newDlg.visible" :title="t('outbound.newManual')" width="700px" :close-on-click-modal="false">
      <el-form label-width="100px">
        <el-form-item :label="t('outbound.warehouse')">
          <el-select v-model="newDlg.warehouseId" filterable style="width:100%">
            <el-option v-for="w in warehouses" :key="w.id" :value="w.id" :label="`${w.code} · ${w.name}`" />
          </el-select>
        </el-form-item>
      </el-form>
      <div style="margin-bottom:8px;display:flex;justify-content:space-between;align-items:center">
        <span style="font-weight:600">{{ t('outbound.items') }}</span>
        <el-button link type="primary" @click="newDlg.items.push({inputItemId:null,requestedQty:1})">+ {{ t('outbound.addLine') }}</el-button>
      </div>
      <el-table :data="newDlg.items" border size="small">
        <el-table-column :label="t('inputItem.name')" min-width="200">
          <template #default="{ $index }">
            <el-select v-model="newDlg.items[$index].inputItemId" filterable size="small" style="width:100%">
              <el-option v-for="ii in inputItems" :key="ii.id" :value="ii.id" :label="`${ii.code} · ${ii.nameEn||ii.name}`" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column :label="t('outbound.requested')" width="130">
          <template #default="{ $index }">
            <el-input-number v-model="newDlg.items[$index].requestedQty" :min="0.001" :precision="3" :controls="false" size="small" style="width:100%" />
          </template>
        </el-table-column>
        <el-table-column width="50" align="center">
          <template #default="{ $index }">
            <el-button link type="danger" size="small" :disabled="newDlg.items.length<=1" @click="newDlg.items.splice($index,1)">×</el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="newDlg.visible=false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="onCreateSubmit">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search as SearchIcon, Refresh as RefreshIcon } from '@element-plus/icons-vue'
import { listOutbound, getOutbound, createOutbound, pickOutbound, confirmOutbound, cancelOutbound } from '@/api/outbound'
import { listWarehouses } from '@/api/warehouse'
import { listInputItems } from '@/api/inputItem'

const { t } = useI18n()
const statusTag = { draft:'info', picked:'warning', confirmed:'success', cancelled:'info' }
function fmtQty(v) { return v==null?'-':Number(v).toLocaleString(undefined,{maximumFractionDigits:3}) }
function fmtTime(v) { return v?v.replace('T',' ').slice(0,16):'-' }

const query = reactive({ status:'', warehouseId:null, sourceType:'', page:1, size:20 })
const list = ref([]); const total = ref(0); const loading = ref(false); const submitting = ref(false)
async function reload(p) { if(p) query.page=p; loading.value=true; try { const d=await listOutbound(query); list.value=d.list||[]; total.value=d.total||0 } finally { loading.value=false } }
function onReset() { Object.assign(query,{status:'',warehouseId:null,sourceType:'',page:1,size:20}); reload() }

const warehouses = ref([]); const inputItems = ref([])
async function loadDropdowns() {
  const [w,ii] = await Promise.all([listWarehouses({status:1,page:1,size:200}), listInputItems({status:'active',page:1,size:500})])
  warehouses.value=w.list||[]; inputItems.value=ii.list||[]
}

const detailItems = reactive({}); const detailLoading = reactive({})
async function onExpand(row,expanded) { if(!expanded.some(r=>r.id===row.id))return; detailLoading[row.id]=true; try{const d=await getOutbound(row.id);detailItems[row.id]=d.items||[]}finally{detailLoading[row.id]=false} }

// pick dialog
const pickDlg = reactive({ visible:false, id:null, items:[] })
async function openPickDlg(row) { const d=await getOutbound(row.id); pickDlg.id=row.id; pickDlg.items=(d.items||[]).map(it=>({...it,pickedQty:it.pickedQty??it.requestedQty,remark:it.remark||''})); pickDlg.visible=true }
async function onPickSubmit() { submitting.value=true; try { await pickOutbound(pickDlg.id,{items:pickDlg.items.map(it=>({itemId:it.id,pickedQty:it.pickedQty,remark:it.remark}))}); ElMessage.success(t('outbound.pickSuccess')); pickDlg.visible=false; reload() } finally { submitting.value=false } }

async function onConfirm(row) { await ElMessageBox.confirm(t('outbound.confirmMsg',{code:row.code}),t('common.tip'),{type:'warning'}).catch(()=>Promise.reject('cancel')); try{await confirmOutbound(row.id);ElMessage.success(t('outbound.confirmSuccess'));reload()}catch(e){if(e==='cancel')return} }
async function onCancel(row) { await ElMessageBox.confirm(t('outbound.cancelMsg',{code:row.code}),t('common.tip'),{type:'warning'}).catch(()=>Promise.reject('cancel')); try{await cancelOutbound(row.id);ElMessage.success(t('outbound.cancelSuccess'));reload()}catch(e){if(e==='cancel')return} }

// new manual
const newDlg = reactive({ visible:false, warehouseId:null, items:[{inputItemId:null,requestedQty:1}] })
async function onCreateSubmit() { if(!newDlg.warehouseId){ElMessage.warning('Select warehouse');return}; const valid=newDlg.items.filter(it=>it.inputItemId&&it.requestedQty>0); if(!valid.length){ElMessage.warning('Add items');return}; submitting.value=true; try{await createOutbound({sourceType:'manual',warehouseId:newDlg.warehouseId,items:valid});ElMessage.success(t('common.createSuccess'));newDlg.visible=false;newDlg.items=[{inputItemId:null,requestedQty:1}];reload()}finally{submitting.value=false} }

onMounted(()=>{loadDropdowns();reload()})
</script>

<style scoped>
.page{padding:16px}.filter-card{margin-bottom:12px}.toolbar{display:flex;justify-content:space-between;align-items:center;margin-bottom:12px}.table-title{font-size:16px;font-weight:600;color:#1f2937}.pager{margin-top:12px;text-align:right}.expand-wrap{padding:8px 12px;background:#fafafa}.code-tag{font-size:12px;background:#fef3c7;padding:2px 6px;border-radius:3px;color:#92400e}.dim{color:#909399}.text-red{color:#ef4444;font-weight:600}
</style>
