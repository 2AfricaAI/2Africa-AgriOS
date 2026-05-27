<template>
  <div class="page">
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item :label="t('common.status')">
          <el-select v-model="query.status" :placeholder="t('common.all')" clearable style="width:120px">
            <el-option v-for="s in ['draft','confirmed','cancelled']" :key="s" :label="t(`scrap.status_${s}`)" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('scrap.warehouse')">
          <el-select v-model="query.warehouseId" :placeholder="t('common.all')" clearable filterable style="width:180px">
            <el-option v-for="w in warehouses" :key="w.id" :value="w.id" :label="`${w.code} · ${w.name}`" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('scrap.type')">
          <el-select v-model="query.scrapType" :placeholder="t('common.all')" clearable style="width:120px">
            <el-option v-for="st in ['damaged','expired','other']" :key="st" :label="t(`scrap.type_${st}`)" :value="st" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="SearchIcon" @click="reload(1)">{{ t('common.search') }}</el-button>
          <el-button :icon="RefreshIcon" @click="Object.assign(query,{status:'',warehouseId:null,scrapType:''});reload(1)">{{ t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    <el-card shadow="never" class="table-card">
      <div class="toolbar">
        <span class="table-title">{{ t('scrap.title') }}</span>
        <el-button type="primary" size="small" @click="newDlg.visible=true">{{ t('scrap.new') }}</el-button>
      </div>
      <el-table :data="list" v-loading="loading" border stripe row-key="id" @expand-change="onExpand">
        <el-table-column type="expand" width="36">
          <template #default="{row}">
            <div class="expand-wrap" v-loading="detailLoading[row.id]">
              <el-table v-if="detailItems[row.id]" :data="detailItems[row.id]" size="small" border>
                <el-table-column prop="inputItemCode" :label="t('inputItem.code')" width="100" />
                <el-table-column prop="inputItemName" :label="t('inputItem.name')" min-width="150" />
                <el-table-column :label="t('scrap.qty')" width="100" align="right">
                  <template #default="{row:r}"><strong class="text-red">{{ fmtQty(r.qty) }}</strong></template>
                </el-table-column>
                <el-table-column prop="reason" :label="t('scrap.reason')" min-width="160">
                  <template #default="{row:r}">{{ r.reason||'-' }}</template>
                </el-table-column>
              </el-table>
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="t('scrap.code')" width="180">
          <template #default="{row}"><code class="code-tag">{{ row.code }}</code></template>
        </el-table-column>
        <el-table-column prop="warehouseName" :label="t('scrap.warehouse')" min-width="130" />
        <el-table-column :label="t('scrap.type')" width="100" align="center">
          <template #default="{row}">
            <el-tag :type="row.scrapType==='expired'?'warning':'danger'" size="small">{{ t(`scrap.type_${row.scrapType}`) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('scrap.items')" width="60" align="center">
          <template #default="{row}">{{ row.itemCount }}</template>
        </el-table-column>
        <el-table-column :label="t('common.status')" width="100" align="center">
          <template #default="{row}">
            <el-tag :type="{draft:'info',confirmed:'success',cancelled:'info'}[row.status]" size="small" effect="dark">{{ t(`scrap.status_${row.status}`) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('scrap.createdAt')" width="150" align="center">
          <template #default="{row}">{{ row.createdAt?row.createdAt.replace('T',' ').slice(0,16):'-' }}</template>
        </el-table-column>
        <el-table-column :label="t('common.actions')" width="160" fixed="right" align="center">
          <template #default="{row}">
            <el-button v-if="row.status==='draft'" type="success" link size="small" @click="onConfirm(row)">{{ t('scrap.confirm') }}</el-button>
            <el-button v-if="row.status==='draft'" type="danger" link size="small" @click="onCancel(row)">{{ t('common.cancel') }}</el-button>
            <span v-if="row.status!=='draft'" class="dim">-</span>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager"><el-pagination background layout="total,sizes,prev,pager,next,jumper" :total="total" v-model:current-page="query.page" v-model:page-size="query.size" :page-sizes="[20,50,100]" @current-change="()=>reload()" @size-change="()=>reload(1)" /></div>
    </el-card>

    <el-dialog v-model="newDlg.visible" :title="t('scrap.new')" width="700px" :close-on-click-modal="false">
      <el-form label-width="100px">
        <el-form-item :label="t('scrap.warehouse')">
          <el-select v-model="newDlg.warehouseId" filterable style="width:100%">
            <el-option v-for="w in warehouses" :key="w.id" :value="w.id" :label="`${w.code} · ${w.name}`" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('scrap.type')">
          <el-select v-model="newDlg.scrapType" style="width:100%">
            <el-option v-for="st in ['damaged','expired','other']" :key="st" :label="t(`scrap.type_${st}`)" :value="st" />
          </el-select>
        </el-form-item>
      </el-form>
      <div style="margin-bottom:8px;display:flex;justify-content:space-between"><span style="font-weight:600">{{ t('scrap.items') }}</span><el-button link type="primary" @click="newDlg.items.push({inputItemId:null,qty:1,reason:''})">+ {{ t('scrap.addLine') }}</el-button></div>
      <el-table :data="newDlg.items" border size="small">
        <el-table-column :label="t('inputItem.name')" min-width="180">
          <template #default="{$index}">
            <el-select v-model="newDlg.items[$index].inputItemId" filterable size="small" style="width:100%">
              <el-option v-for="ii in inputItems" :key="ii.id" :value="ii.id" :label="`${ii.code} · ${ii.nameEn||ii.name}`" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column :label="t('scrap.qty')" width="120">
          <template #default="{$index}"><el-input-number v-model="newDlg.items[$index].qty" :min="0.001" :precision="3" :controls="false" size="small" style="width:100%" /></template>
        </el-table-column>
        <el-table-column :label="t('scrap.reason')" min-width="140">
          <template #default="{$index}"><el-input v-model="newDlg.items[$index].reason" size="small" maxlength="255" /></template>
        </el-table-column>
        <el-table-column width="50" align="center">
          <template #default="{$index}"><el-button link type="danger" size="small" :disabled="newDlg.items.length<=1" @click="newDlg.items.splice($index,1)">×</el-button></template>
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
import { listScrap, getScrap, createScrap, confirmScrap, cancelScrap } from '@/api/scrap'
import { listWarehouses } from '@/api/warehouse'
import { listInputItems } from '@/api/inputItem'

const { t } = useI18n()
function fmtQty(v) { return v==null?'-':Number(v).toLocaleString(undefined,{maximumFractionDigits:3}) }

const query = reactive({ status:'', warehouseId:null, scrapType:'', page:1, size:20 })
const list = ref([]); const total = ref(0); const loading = ref(false); const submitting = ref(false)
async function reload(p) { if(p) query.page=p; loading.value=true; try{const d=await listScrap(query);list.value=d.list||[];total.value=d.total||0}finally{loading.value=false} }

const warehouses = ref([]); const inputItems = ref([])
onMounted(async()=>{try{const[w,ii]=await Promise.all([listWarehouses({status:1,page:1,size:200}),listInputItems({status:'active',page:1,size:500})]);warehouses.value=w.list||[];inputItems.value=ii.list||[]}catch{};reload()})

const detailItems = reactive({}); const detailLoading = reactive({})
async function onExpand(row,expanded) { if(!expanded.some(r=>r.id===row.id))return; detailLoading[row.id]=true; try{const d=await getScrap(row.id);detailItems[row.id]=d.items||[]}finally{detailLoading[row.id]=false} }

const newDlg = reactive({visible:false,warehouseId:null,scrapType:'damaged',remark:'',items:[{inputItemId:null,qty:1,reason:''}]})
async function onCreateSubmit() { if(!newDlg.warehouseId){ElMessage.warning('Select warehouse');return}; const valid=newDlg.items.filter(it=>it.inputItemId&&it.qty>0); if(!valid.length){ElMessage.warning('Add items');return}; submitting.value=true; try{await createScrap(newDlg);ElMessage.success(t('common.createSuccess'));newDlg.visible=false;newDlg.items=[{inputItemId:null,qty:1,reason:''}];reload()}finally{submitting.value=false} }

async function onConfirm(row) { await ElMessageBox.confirm(t('scrap.confirmMsg',{code:row.code}),t('common.tip'),{type:'warning'}).catch(()=>Promise.reject('cancel')); try{await confirmScrap(row.id);ElMessage.success(t('scrap.confirmSuccess'));reload()}catch(e){if(e==='cancel')return} }
async function onCancel(row) { await ElMessageBox.confirm(t('scrap.cancelMsg',{code:row.code}),t('common.tip'),{type:'warning'}).catch(()=>Promise.reject('cancel')); try{await cancelScrap(row.id);ElMessage.success(t('scrap.cancelSuccess'));reload()}catch(e){if(e==='cancel')return} }
</script>

<style scoped>
.page{padding:16px}.filter-card{margin-bottom:12px}.toolbar{display:flex;justify-content:space-between;align-items:center;margin-bottom:12px}.table-title{font-size:16px;font-weight:600;color:#1f2937}.pager{margin-top:12px;text-align:right}.expand-wrap{padding:8px 12px;background:#fafafa}.code-tag{font-size:12px;background:#fee2e2;padding:2px 6px;border-radius:3px;color:#991b1b}.dim{color:#909399}.text-red{color:#ef4444;font-weight:600}
</style>
