<!-- 样例库存（单表 CRUD 案例） -->
<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="商品名称" prop="productName">
        <el-input v-model="queryParams.productName" placeholder="请输入商品名称" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="仓库" prop="warehouse">
        <el-input v-model="queryParams.warehouse" placeholder="请输入仓库" clearable style="width: 160px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['sample:stock:add']">新增</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="stockList">
      <el-table-column label="商品名称" align="center" prop="productName" />
      <el-table-column label="库存数量" align="center" prop="quantity">
        <template #default="scope">
          <el-tag :type="scope.row.quantity < 10 ? 'danger' : 'success'">{{ scope.row.quantity }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="仓库" align="center" prop="warehouse" />
      <el-table-column label="更新时间" align="center" prop="updateTime" width="160" />
      <el-table-column label="操作" align="center" width="160">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['sample:stock:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['sample:stock:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="stockRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="商品名称" prop="productName">
          <el-input v-model="form.productName" placeholder="请输入商品名称" />
        </el-form-item>
        <el-form-item label="库存数量" prop="quantity">
          <el-input-number v-model="form.quantity" :min="0" />
        </el-form-item>
        <el-form-item label="仓库" prop="warehouse">
          <el-input v-model="form.warehouse" placeholder="请输入仓库" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Stock" lang="ts">
import { listStock, getStock, addStock, updateStock, delStock } from '@/api/sample/stock.ts'
import type { ComponentPublicInstance } from 'vue'
import type { FormInstance } from 'element-plus'
import type { SampleStock } from '@/api/sample/stock'

const { proxy } = getCurrentInstance() as { proxy: ComponentPublicInstance }
const stockList = ref<SampleStock[]>([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const title = ref('')
const open = ref(false)

const data = reactive({
  form: {} as SampleStock,
  queryParams: { pageNum: 1, pageSize: 10, productName: undefined, warehouse: undefined },
  rules: { productName: [{ required: true, message: '商品名称不能为空', trigger: 'blur' }] }
})
const { queryParams, form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listStock(queryParams.value).then((res) => { stockList.value = res.rows; total.value = res.total; loading.value = false })
}
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function reset() { form.value = { stockId: undefined, productName: undefined, quantity: 0, warehouse: undefined }; proxy.resetForm('stockRef') }
function handleAdd() { reset(); open.value = true; title.value = '新增库存' }
function handleUpdate(row: SampleStock) { reset(); getStock(row.stockId as number).then((res) => { form.value = res.data; open.value = true; title.value = '修改库存' }) }
function cancel() { open.value = false }
function submitForm() {
  (proxy.$refs['stockRef'] as FormInstance).validate((valid: boolean) => {
    if (!valid) return
    if (form.value.stockId != undefined) {
      updateStock(form.value).then(() => { proxy.$modal.msgSuccess('修改成功'); open.value = false; getList() })
    } else {
      addStock(form.value).then(() => { proxy.$modal.msgSuccess('新增成功'); open.value = false; getList() })
    }
  })
}
function handleDelete(row: SampleStock) {
  proxy.$modal.confirm('是否确认删除商品“' + row.productName + '”的库存？').then(() => delStock(row.stockId as number))
    .then(() => { getList(); proxy.$modal.msgSuccess('删除成功') })
}
getList()
</script>
