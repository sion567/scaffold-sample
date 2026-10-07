<!-- 样例订单（主子表 + 工作流审批案例） -->
<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="订单号" prop="orderNo">
        <el-input v-model="queryParams.orderNo" placeholder="请输入订单号" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="订单状态" clearable style="width: 200px">
          <el-option v-for="(label, value) in statusMap" :key="value" :label="label" :value="value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['sample:order:add']">新增</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="orderList">
      <el-table-column label="订单号" align="center" prop="orderNo" />
      <el-table-column label="客户ID" align="center" prop="customerId" width="80" />
      <el-table-column label="订单总额" align="center" prop="totalAmount" />
      <el-table-column label="状态" align="center" prop="status">
        <template #default="scope">
          <el-tag :type="statusTag[scope.row.status]">{{ statusMap[scope.row.status] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="审批意见" align="center" prop="auditRemark" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" width="330">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleInfo(scope.row)" v-hasPermi="['sample:order:query']">详情</el-button>
          <el-button link type="primary" icon="Edit" v-if="scope.row.status === '0' || scope.row.status === '3'" @click="handleUpdate(scope.row)" v-hasPermi="['sample:order:edit']">修改</el-button>
          <el-button link type="success" v-if="scope.row.status === '0' || scope.row.status === '3'" @click="handleSubmit(scope.row)" v-hasPermi="['sample:order:submit']">提交审批</el-button>
          <el-button link type="warning" v-if="scope.row.status === '1'" @click="handleAudit(scope.row, true)" v-hasPermi="['sample:order:audit']">通过</el-button>
          <el-button link type="danger" v-if="scope.row.status === '1'" @click="handleAudit(scope.row, false)" v-hasPermi="['sample:order:audit']">驳回</el-button>
          <el-button link type="primary" v-if="scope.row.status !== '0'" @click="handleFlow(scope.row)">流转</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 订单编辑（主子表） -->
    <el-dialog :title="title" v-model="open" width="760px" append-to-body>
      <el-form ref="orderRef" :model="form" :rules="rules" label-width="80px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="订单号" prop="orderNo">
              <el-input v-model="form.orderNo" placeholder="请输入订单号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="客户ID" prop="customerId">
              <el-input-number v-model="form.customerId" :min="1" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="订单明细" prop="items">
          <el-button type="primary" icon="Plus" size="small" @click="addItem">添加明细</el-button>
          <el-table :data="form.items" size="small" style="width: 100%; margin-top: 6px">
            <el-table-column label="商品名称">
              <template #default="s"><el-input v-model="s.row.productName" placeholder="商品名称" /></template>
            </el-table-column>
            <el-table-column label="数量" width="120">
              <template #default="s"><el-input-number v-model="s.row.quantity" :min="1" size="small" /></template>
            </el-table-column>
            <el-table-column label="单价" width="140">
              <template #default="s"><el-input-number v-model="s.row.price" :min="0" :precision="2" size="small" /></template>
            </el-table-column>
            <el-table-column label="操作" width="80">
              <template #default="s">
                <el-button link type="danger" @click="form.items!.splice(s.$index, 1)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </template>
    </el-dialog>

    <!-- 流转历史 -->
    <el-dialog title="审批流转历史" v-model="flowOpen" width="640px" append-to-body>
      <el-table :data="flowList" size="small">
        <el-table-column label="节点" align="center" prop="nodeCode" width="90" />
        <el-table-column label="动作" align="center" prop="action" width="100" />
        <el-table-column label="操作人" align="center" prop="operator" width="100" />
        <el-table-column label="意见" align="center" prop="remark" />
        <el-table-column label="时间" align="center" prop="operateTime" width="160" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup name="SampleOrder" lang="ts">
import { listOrder, getOrder, addOrder, updateOrder, delOrder, submitOrder, auditOrder, flowHistory } from '@/api/sample/order.ts'
import type { ComponentPublicInstance } from 'vue'
import type { FormInstance } from 'element-plus'
import type { SampleOrder, FlowHistory } from '@/api/sample/order'

const { proxy } = getCurrentInstance() as { proxy: ComponentPublicInstance }
const statusMap: Record<string, string> = { '0': '待提交', '1': '审批中', '2': '已通过', '3': '已驳回' }
const statusTag: Record<string, string> = { '0': 'info', '1': 'warning', '2': 'success', '3': 'danger' }

const orderList = ref<SampleOrder[]>([])
const flowList = ref<FlowHistory[]>([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const title = ref('')
const open = ref(false)
const flowOpen = ref(false)

const data = reactive({
  form: {} as SampleOrder,
  queryParams: { pageNum: 1, pageSize: 10, orderNo: undefined, status: undefined },
  rules: { orderNo: [{ required: true, message: '订单号不能为空', trigger: 'blur' }] }
})
const { queryParams, form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listOrder(queryParams.value).then((res) => { orderList.value = res.rows; total.value = res.total; loading.value = false })
}
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function reset() {
  form.value = { orderId: undefined, orderNo: undefined, customerId: 1, remark: undefined, items: [] }
  proxy.resetForm('orderRef')
}
function handleAdd() { reset(); open.value = true; title.value = '新增订单' }
function handleUpdate(row: SampleOrder) {
  reset()
  getOrder(row.orderId as number).then((res) => { form.value = res.data; open.value = true; title.value = '修改订单' })
}
function handleInfo(row: SampleOrder) {
  getOrder(row.orderId as number).then((res) => { form.value = res.data; open.value = true; title.value = '订单详情' })
}
function handleFlow(row: SampleOrder) {
  flowHistory(row.orderId as number).then((res) => { flowList.value = res.data; flowOpen.value = true })
}
function addItem() { form.value.items!.push({ productName: undefined, quantity: 1, price: 0 }) }
function cancel() { open.value = false }
function submitForm() {
  (proxy.$refs['orderRef'] as FormInstance).validate((valid: boolean) => {
    if (!valid) return
    if (form.value.orderId != undefined) {
      updateOrder(form.value).then(() => { proxy.$modal.msgSuccess('修改成功'); open.value = false; getList() })
    } else {
      addOrder(form.value).then(() => { proxy.$modal.msgSuccess('新增成功'); open.value = false; getList() })
    }
  })
}
function handleSubmit(row: SampleOrder) {
  proxy.$modal.confirm('提交订单“' + row.orderNo + '”进入审批流？').then(() => submitOrder(row.orderId as number))
    .then((res) => { proxy.$modal.msgSuccess(res.data || '已提交'); getList() })
}
function handleAudit(row: SampleOrder, pass: boolean) {
  proxy.$modal.prompt(pass ? '通过意见' : '驳回原因').then(({ value }: { value: string }) =>
    auditOrder(row.orderId as number, pass, value || '').then((res) => { proxy.$modal.msgSuccess(res.data || '操作成功'); getList() }))
}
function handleDelete(row: SampleOrder) {
  proxy.$modal.confirm('是否确认删除订单“' + row.orderNo + '”？').then(() => delOrder(row.orderId as number))
    .then(() => { getList(); proxy.$modal.msgSuccess('删除成功') })
}
getList()
</script>
