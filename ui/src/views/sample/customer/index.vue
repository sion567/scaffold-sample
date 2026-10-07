<!-- 样例客户（单表 CRUD 案例，与 gen 生成物同构） -->
<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="客户姓名" prop="customerName">
        <el-input v-model="queryParams.customerName" placeholder="请输入客户姓名" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="客户状态" clearable style="width: 200px">
          <el-option v-for="dict in sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['sample:customer:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['sample:customer:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="customerList">
      <el-table-column label="客户ID" align="center" prop="customerId" width="80" />
      <el-table-column label="客户姓名" align="center" prop="customerName" />
      <el-table-column label="手机号" align="center" prop="phone" />
      <el-table-column label="邮箱" align="center" prop="email" />
      <el-table-column label="状态" align="center" prop="status">
        <template #default="scope">
          <dict-tag :options="sys_normal_disable" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" width="160">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['sample:customer:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['sample:customer:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="customerRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="客户姓名" prop="customerName">
          <el-input v-model="form.customerName" placeholder="请输入客户姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in sys_normal_disable" :key="dict.value" :value="dict.value">{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Customer" lang="ts">
import { listCustomer, getCustomer, addCustomer, updateCustomer, delCustomer } from '@/api/sample/customer.ts'
import type { ComponentPublicInstance } from 'vue'
import type { FormInstance } from 'element-plus'
import type { SampleCustomer } from '@/api/sample/customer'

const { proxy } = getCurrentInstance() as { proxy: ComponentPublicInstance }
const { sys_normal_disable } = proxy.useDict('sys_normal_disable')

const customerList = ref<SampleCustomer[]>([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const title = ref('')
const open = ref(false)

const data = reactive({
  form: {} as SampleCustomer,
  queryParams: { pageNum: 1, pageSize: 10, customerName: undefined, status: undefined },
  rules: { customerName: [{ required: true, message: '客户姓名不能为空', trigger: 'blur' }] }
})
const { queryParams, form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listCustomer(queryParams.value).then((res) => {
    customerList.value = res.rows
    total.value = res.total
    loading.value = false
  })
}

function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }

function reset() {
  form.value = { customerId: undefined, customerName: undefined, phone: undefined, email: undefined, status: '0', remark: undefined }
  proxy.resetForm('customerRef')
}

function handleAdd() { reset(); open.value = true; title.value = '新增客户' }
function handleUpdate(row: SampleCustomer) {
  reset()
  getCustomer(row.customerId as number).then((res) => { form.value = res.data; open.value = true; title.value = '修改客户' })
}
function cancel() { open.value = false }

function submitForm() {
  (proxy.$refs['customerRef'] as FormInstance).validate((valid: boolean) => {
    if (!valid) return
    if (form.value.customerId != undefined) {
      updateCustomer(form.value).then(() => { proxy.$modal.msgSuccess('修改成功'); open.value = false; getList() })
    } else {
      addCustomer(form.value).then(() => { proxy.$modal.msgSuccess('新增成功'); open.value = false; getList() })
    }
  })
}

function handleDelete(row: SampleCustomer) {
  proxy.$modal.confirm('是否确认删除客户“' + row.customerName + '”？').then(() => delCustomer(row.customerId as number))
    .then(() => { getList(); proxy.$modal.msgSuccess('删除成功') })
}

function handleExport() {
  proxy.download('/sample/customer/export', { ...queryParams.value }, `customer_${Date.now()}.xlsx`)
}

getList()
</script>
