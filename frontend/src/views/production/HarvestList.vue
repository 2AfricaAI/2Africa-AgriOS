<template>
  <div class="page">
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="计划">
          <el-select v-model="query.planId" placeholder="全部" clearable filterable style="width: 240px">
            <el-option
              v-for="p in plans"
              :key="p.id"
              :label="`${p.code} · ${p.plotName} · ${p.cropName}`"
              :value="p.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="→"
            start-placeholder="起始"
            end-placeholder="止"
            value-format="YYYY-MM-DD"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="SearchIcon" @click="reload(1)">查询</el-button>
          <el-button :icon="RefreshIcon" @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <div class="toolbar">
        <el-button type="primary" :icon="PlusIcon" @click="onCreate">新建采收记录</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border stripe row-key="id">
        <el-table-column prop="code" label="采收单号" width="160" />
        <el-table-column prop="harvestDate" label="采收日期" width="110" />
        <el-table-column label="关联" min-width="220">
          <template #default="{ row }">
            <div class="dim" style="margin-bottom: 2px">{{ row.planCode }}</div>
            <el-tag size="small">{{ row.plotName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="作物 / 品种" min-width="160">
          <template #default="{ row }">
            <el-tag size="small" type="primary">{{ row.cropName }}</el-tag>
            <span v-if="row.varietyName" class="dim" style="margin-left: 4px">{{ row.varietyName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="采收量 (kg)" width="120" align="right">
          <template #default="{ row }">
            <strong>{{ Number(row.qtyKg).toLocaleString(undefined, { minimumFractionDigits: 2 }) }}</strong>
          </template>
        </el-table-column>
        <el-table-column label="批次号 (自动)" min-width="170">
          <template #default="{ row }">
            <code class="batch-code">{{ row.batchCode }}</code>
          </template>
        </el-table-column>
        <el-table-column label="操作人" width="110">
          <template #default="{ row }">
            <span v-if="row.operatorName">{{ row.operatorName }}</span>
            <span v-else class="dim">#{{ row.operatorId }}</span>
          </template>
        </el-table-column>
        <el-table-column label="照片" width="140">
          <template #default="{ row }">
            <div v-if="row.photos?.length" class="photo-row">
              <el-image
                v-for="(p, i) in row.photos.slice(0, 3)"
                :key="p.id"
                :src="p.downloadUrl"
                :preview-src-list="row.photos.map(x => x.downloadUrl)"
                :initial-index="i"
                fit="cover"
                class="thumb"
                preview-teleported
              />
              <span v-if="row.photos.length > 3" class="more">+{{ row.photos.length - 3 }}</span>
            </div>
            <span v-else class="dim">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="100" fixed="right" align="center">
          <template #default="{ row }">
            <el-popconfirm title="删除采收记录同时会软删对应 batch?" @confirm="onDelete(row)">
              <template #reference>
                <el-button link type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pager"
        background
        layout="total, sizes, prev, pager, next, jumper"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        :page-size="pageSize"
        :current-page="page"
        @size-change="onSizeChange"
        @current-change="onPageChange"
      />
    </el-card>

    <!-- 新建对话框 -->
    <el-dialog
      v-model="dialogVisible"
      title="新建采收记录"
      width="640"
      destroy-on-close
      @closed="onDialogClosed"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="关联计划" prop="planId">
          <el-select v-model="form.planId" placeholder="选择计划" filterable style="width: 100%">
            <el-option
              v-for="p in plans"
              :key="p.id"
              :label="`${p.code} · ${p.plotName} · ${p.cropName}`"
              :value="p.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="采收日期" prop="harvestDate">
          <el-date-picker
            v-model="form.harvestDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="采收量" prop="qtyKg">
          <el-input-number
            v-model="form.qtyKg"
            :min="0.001"
            :precision="3"
            :step="10"
            controls-position="right"
            style="width: 100%"
          />
          <span class="unit">kg</span>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="255" show-word-limit />
        </el-form-item>
        <el-form-item label="照片">
          <FileUploader
            v-model="photos"
            biz-type="harvest_photo"
            accept="image/*"
            :limit="9"
            :max-size-mb="10"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSubmit">
          保存并自动产生 batch
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Search as SearchIcon,
  Refresh as RefreshIcon,
  Plus as PlusIcon,
} from '@element-plus/icons-vue'
import { listHarvests, createHarvest, deleteHarvest } from '@/api/harvest'
import { listPlantingPlans } from '@/api/plantingPlan'
import FileUploader from '@/components/FileUploader.vue'

const plans = ref([])
async function loadPlans() {
  const data = await listPlantingPlans({ page: 1, size: 500 })
  plans.value = data.list
}

// list
const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const query = reactive({ planId: null, dateFrom: null, dateTo: null })

const dateRange = ref(null)
watch(dateRange, (v) => {
  if (Array.isArray(v) && v.length === 2) {
    query.dateFrom = v[0]; query.dateTo = v[1]
  } else { query.dateFrom = null; query.dateTo = null }
})

async function reload(toPage) {
  if (toPage) page.value = toPage
  loading.value = true
  try {
    const data = await listHarvests({ ...query, page: page.value, size: pageSize.value })
    list.value = data.list
    total.value = data.total
  } finally { loading.value = false }
}
function onReset() {
  query.planId = null
  dateRange.value = null
  reload(1)
}
function onPageChange(p) { page.value = p; reload() }
function onSizeChange(s) { pageSize.value = s; reload(1) }

onMounted(async () => { await loadPlans(); await reload(1) })

// dialog
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref(null)
const photos = ref([])

const emptyForm = () => ({ planId: null, harvestDate: null, qtyKg: null, remark: '' })
const form = reactive(emptyForm())

const rules = {
  planId:      [{ required: true, message: '请选择计划', trigger: 'change' }],
  harvestDate: [{ required: true, message: '请选择采收日期', trigger: 'change' }],
  qtyKg:       [{ required: true, message: '请输入采收量', trigger: 'change' }],
}

function onCreate() {
  Object.assign(form, emptyForm())
  photos.value = []
  dialogVisible.value = true
}
function onDialogClosed() {
  Object.assign(form, emptyForm())
  photos.value = []
  formRef.value?.clearValidate()
}

async function onSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const payload = {
      ...form,
      photos: photos.value.map(p => p.id).filter(Boolean),
      clientUuid: crypto.randomUUID(),
    }
    await createHarvest(payload)
    ElMessage.success('采收记录已创建,批次已自动产生')
    dialogVisible.value = false
    reload()
  } catch {}
  finally { saving.value = false }
}

async function onDelete(row) {
  try {
    await deleteHarvest(row.id)
    ElMessage.success('已删除')
    reload()
  } catch {}
}
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.filter-card :deep(.el-card__body),
.table-card :deep(.el-card__body) { padding: 16px; }
.toolbar { margin-bottom: 12px; display: flex; justify-content: flex-end; }
.pager { margin-top: 14px; justify-content: flex-end; }
.dim { color: #909399; font-size: 12px; }
.unit { margin-left: 8px; color: #606266; font-size: 13px; }

.batch-code {
  font-family: 'Consolas', monospace;
  background: #f5f7fa;
  padding: 2px 8px;
  border-radius: 4px;
  color: #d63384;
  font-size: 12px;
}

.photo-row { display: flex; align-items: center; gap: 4px; }
.thumb {
  width: 36px; height: 36px;
  border-radius: 4px; cursor: pointer;
  border: 1px solid #ebeef5;
}
.more { font-size: 11px; color: #909399; margin-left: 4px; }
</style>
