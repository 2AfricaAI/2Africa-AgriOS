<template>
  <div class="page">
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item :label="t('common.status')">
          <el-select v-model="query.status" :placeholder="t('common.all')" clearable style="width:120px">
            <el-option v-for="s in ['draft','counting','confirmed','cancelled']" :key="s" :label="t(`stocktake.status_${s}`)" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('stocktake.warehouse')">
          <el-select v-model="query.warehouseId" :placeholder="t('common.all')" clearable filterable style="width:180px">
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
        <span class="table-title">{{ t('stocktake.title') }}</span>
        <el-button type="primary" size="small" @click="newDlg.visible=true">{{ t('stocktake.new') }}</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border stripe row-key="id" @expand-change="onExpand">
        <el-table-column type="expand" width="36">
          <template #default="{ row }">
            <div class="expand-wrap" v-loading="detailLoading[row.id]">
              <el-table v-if="detailItems[row.id]" :data="detailItems[row.id]" size="small" border>
                <el-table-column prop="inputItemCode" :label="t('inputItem.code')" width="100" />
                <el-table-column prop="inputItemName" :label="t('inputItem.name')" min-width="150" />
                <el-table-column prop="unit" :label="t('inputItem.unit')" width="60" align="center" />
                <el-table-column :label="t('stocktake.systemQty')" width="110" align="right">
                  <template #default="{row:r}"><strong>{{ fmtQty(r.systemQty) }}</strong></template>
                </el-table-column>
                <el-table-column :label="t('stocktake.countQty')" width="110" align="right">
                  <template #default="{row:r}">
                    <span v-if="r.countQty!=null">{{ fmtQty(r.countQty) }}</span>
                    <span v-else class="dim">-</span>
                  </template>
                </el-table-column>
                <el-table-column :label="t('stocktake.diff')" width="100" align="right">
                  <template #default="{row:r}">
                    <span v-if="r.diffQty!=null" :class="r.diffQty>0?'text-green':r.diffQty<0?'text-red':''">
                      {{ r.diffQty > 0 ? '+' : '' }}{{ fmtQty(r.diffQty) }}
                    </span>
                    <span v-else class="dim">-</span>
                  </template>
                </el-table-column>
                <el-table-column prop="remark" :label="t('common.remark')" min-width="100">
                  <template #default="{row:r}">{{ r.remark||'-' }}</template>
                </el-table-column>
              </el-table>
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="t('stocktake.code')" width="180">
          <template #default="{row}"><code class="code-tag">{{ row.code }}</code></template>
        </el-table-column>
        <el-table-column prop="warehouseName" :label="t('stocktake.warehouse')" min-width="140" />
        <el-table-column :label="t('stocktake.type')" width="90" align="center">
          <template #default="{row}">
            <el-tag size="small">{{ t(`stocktake.type_${row.countType}`) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('stocktake.items')" width="60" align="center">
          <template #default="{row}">{{ row.itemCount }}</template>
        </el-table-column>
        <el-table-column :label="t('common.status')" width="100" align="center">
          <template #default="{row}">
            <el-tag :type="statusTag[row.status]" size="small" effect="dark">{{ t(`stocktake.status_${row.status}`) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('stocktake.createdAt')" width="150" align="center">
          <template #default="{row}">{{ fmtTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column :label="t('common.actions')" width="200" fixed="right" align="center">
          <template #default="{row}">
            <el-button v-if="row.status==='draft'||row.status==='counting'" type="warning" link size="small" @click="openCountDlg(row)">{{ t('stocktake.count') }}</el-button>
            <el-button v-if="row.status==='counting'" type="success" link size="small" @click="onConfirm(row)">{{ t('stocktake.confirm') }}</el-button>
            <el-button v-if="row.status!=='confirmed'" type="danger" link size="small" @click="onCancel(row)">{{ t('common.cancel') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total,sizes,prev,pager,next,jumper" :total="total"
          v-model:current-page="query.page" v-model:page-size="query.size" :page-sizes="[20,50,100]"
          @current-change="()=>reload()" @size-change="()=>reload(1)" />
      </div>
    </el-card>

    <!-- New stocktake -->
    <el-dialog v-model="newDlg.visible" :title="t('stocktake.new')" width="400px">
      <el-form label-width="90px">
        <el-form-item :label="t('stocktake.warehouse')">
          <el-select v-model="newDlg.warehouseId" filterable style="width:100%">
            <el-option v-for="w in warehouses" :key="w.id" :value="w.id" :label="`${w.code} · ${w.name}`" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('stocktake.type')">
          <el-select v-model="newDlg.countType" style="width:100%">
            <el-option v-for="ct in ['full','cycle','random']" :key="ct" :label="t(`stocktake.type_${ct}`)" :value="ct" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('common.remark')">
          <el-input v-model="newDlg.remark" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="newDlg.visible=false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="onCreateSubmit">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- Count dialog -->
    <el-dialog v-model="countDlg.visible" :title="t('stocktake.countTitle')" width="700px" :close-on-click-modal="false">
      <el-table :data="countDlg.items" border size="small">
        <el-table-column prop="inputItemCode" :label="t('inputItem.code')" width="100" />
        <el-table-column prop="inputItemName" :label="t('inputItem.name')" min-width="150" />
        <el-table-column :label="t('stocktake.systemQty')" width="110" align="right">
          <template #default="{row}"><strong>{{ fmtQty(row.systemQty) }}</strong></template>
        </el-table-column>
        <el-table-column :label="t('stocktake.countQty')" width="130">
          <template #default="{row,$index}">
            <el-input-number v-model="countDlg.items[$index].countQty" :min="0" :precision="3" :controls="false" size="small" style="width:100%" />
          </template>
        </el-table-column>
        <el-table-column :label="t('common.remark')" min-width="120">
          <template #default="{row,$index}">
            <el-input v-model="countDlg.items[$index].remark" size="small" maxlength="255" />
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="countDlg.visible=false">{{ t('common.cancel') }}</el-button>
        <el-button type="warning" :loading="submitting" @click="onCountSubmit">{{ t('stocktake.countSubmit') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search as SearchIcon, Refresh as RefreshIcon } from '@element-plus/icons-vue'
import { listStocktake, getStocktake, createStocktake, submitCounts, confirmStocktake, cancelStocktake } from '@/api/stocktake'
import { listWarehouses } from '@/api/warehouse'

const { t } = useI18n()
const statusTag = { draft:'info', counting:'warning', confirmed:'success', cancelled:'info' }
function fmtQty(v) { return v==null?'-':Number(v).toLocaleString(undefined,{maximumFractionDigits:3}) }
function fmtTime(v) { return v?v.replace('T',' ').slice(0,16):'-' }

const query = reactive({ status:'', warehouseId:null, countType:'', page:1, size:20 })
const list = ref([]); const total = ref(0); const loading = ref(false); const submitting = ref(false)
async function reload(p) { if(p) query.page=p; loading.value=true; try{const d=await listStocktake(query);list.value=d.list||[];total.value=d.total||0}finally{loading.value=false} }
function onReset() { Object.assign(query,{status:'',warehouseId:null,countType:'',page:1,size:20}); reload() }

const warehouses = ref([])
onMounted(async()=>{try{const d=await listWarehouses({status:1,page:1,size:200});warehouses.value=d.list||[]}catch{};reload()})

const detailItems = reactive({}); const detailLoading = reactive({})
async function onExpand(row,expanded) { if(!expanded.some(r=>r.id===row.id))return; detailLoading[row.id]=true; try{const d=await getStocktake(row.id);detailItems[row.id]=d.items||[]}finally{detailLoading[row.id]=false} }

const newDlg = reactive({visible:false,warehouseId:null,countType:'full',remark:''})
async function onCreateSubmit() { if(!newDlg.warehouseId){ElMessage.warning('Select warehouse');return}; submitting.value=true; try{await createStocktake(newDlg);ElMessage.success(t('common.createSuccess'));newDlg.visible=false;reload()}finally{submitting.value=false} }

const countDlg = reactive({visible:false,id:null,items:[]})
async function openCountDlg(row) { const d=await getStocktake(row.id); countDlg.id=row.id; countDlg.items=(d.items||[]).map(it=>({...it,countQty:it.countQty??it.systemQty,remark:it.remark||''})); countDlg.visible=true }
async function onCountSubmit() { submitting.value=true; try{await submitCounts(countDlg.id,{items:countDlg.items.map(it=>({itemId:it.id,countQty:it.countQty,remark:it.remark}))});ElMessage.success(t('stocktake.countSuccess'));countDlg.visible=false;reload()}finally{submitting.value=false} }

async function onConfirm(row) { await ElMessageBox.confirm(t('stocktake.confirmMsg',{code:row.code}),t('common.tip'),{type:'warning'}).catch(()=>Promise.reject('cancel')); try{await confirmStocktake(row.id);ElMessage.success(t('stocktake.confirmSuccess'));reload()}catch(e){if(e==='cancel')return} }
async function onCancel(row) { await ElMessageBox.confirm(t('stocktake.cancelMsg',{code:row.code}),t('common.tip'),{type:'warning'}).catch(()=>Promise.reject('cancel')); try{await cancelStocktake(row.id);ElMessage.success(t('stocktake.cancelSuccess'));reload()}catch(e){if(e==='cancel')return} }
</script>

<style scoped>
.page{padding:16px}.filter-card{margin-bottom:12px}.toolbar{display:flex;justify-content:space-between;align-items:center;margin-bottom:12px}.table-title{font-size:16px;font-weight:600;color:#1f2937}.pager{margin-top:12px;text-align:right}.expand-wrap{padding:8px 12px;background:#fafafa}.code-tag{font-size:12px;background:#ede9fe;padding:2px 6px;border-radius:3px;color:#6d28d9}.dim{color:#909399}.text-green{color:#16a34a;font-weight:600}.text-red{color:#ef4444;font-weight:600}
</style>
