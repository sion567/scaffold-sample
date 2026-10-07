<!-- 样例商品分类（树表案例） -->
<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="分类名称" prop="categoryName">
        <el-input v-model="queryParams.categoryName" placeholder="请输入分类名称" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['sample:category:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="info" plain icon="Sort" @click="toggleExpandAll">展开/折叠</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-if="refreshTable" v-loading="loading" :data="categoryList" row-key="categoryId" :default-expand-all="isExpandAll" :tree-props="{ children: 'children', hasChildren: 'hasChildren' }">
      <el-table-column label="分类名称" prop="categoryName" />
      <el-table-column label="排序" align="center" prop="orderNum" width="100" />
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : 'info'">{{ scope.row.status === '0' ? '正常' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="180" />
      <el-table-column label="操作" align="center" width="200">
        <template #default="scope">
          <el-button link type="primary" icon="Plus" @click="handleAdd(scope.row)" v-hasPermi="['sample:category:add']">新增</el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['sample:category:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['sample:category:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="categoryRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级分类" prop="parentId">
          <el-tree-select v-model="form.parentId" :data="categoryOptions" :props="{ value: 'categoryId', label: 'categoryName', children: 'children' }" value-key="categoryId" placeholder="选择上级分类" check-strictly />
        </el-form-item>
        <el-form-item label="分类名称" prop="categoryName">
          <el-input v-model="form.categoryName" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="显示排序" prop="orderNum">
          <el-input-number v-model="form.orderNum" :min="0" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio value="0">正常</el-radio>
            <el-radio value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Category" lang="ts">
import { listCategory, getCategory, treeCategory, addCategory, updateCategory, delCategory } from '@/api/sample/category.ts'
import type { ComponentPublicInstance } from 'vue'
import type { FormInstance } from 'element-plus'
import type { SampleCategory } from '@/api/sample/category'

const { proxy } = getCurrentInstance() as { proxy: ComponentPublicInstance }
const categoryList = ref<SampleCategory[]>([])
const categoryOptions = ref<SampleCategory[]>([])
const loading = ref(true)
const showSearch = ref(true)
const isExpandAll = ref(true)
const refreshTable = ref(true)
const title = ref('')
const open = ref(false)

const data = reactive({
  form: {} as SampleCategory,
  queryParams: { categoryName: undefined },
  rules: { categoryName: [{ required: true, message: '分类名称不能为空', trigger: 'blur' }] }
})
const { queryParams, form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listCategory(queryParams.value).then((res) => {
    categoryList.value = proxy.handleTree(res.data, 'categoryId', 'parentId')
    loading.value = false
  })
}
function getTree() { treeCategory().then((res) => { categoryOptions.value = res.data }) }
function toggleExpandAll() { refreshTable.value = false; isExpandAll.value = !isExpandAll.value; nextTick(() => { refreshTable.value = true }) }
function handleQuery() { getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function reset() {
  form.value = { categoryId: undefined, categoryName: undefined, parentId: 0, orderNum: 0, status: '0' }
  proxy.resetForm('categoryRef')
}
function handleAdd(row?: SampleCategory) {
  reset()
  getTree()
  if (row && row.categoryId) { form.value.parentId = row.categoryId } else { form.value.parentId = 0 }
  open.value = true; title.value = '新增分类'
}
function handleUpdate(row: SampleCategory) {
  reset(); getTree()
  getCategory(row.categoryId as number).then((res) => { form.value = res.data; open.value = true; title.value = '修改分类' })
}
function cancel() { open.value = false }
function submitForm() {
  (proxy.$refs['categoryRef'] as FormInstance).validate((valid: boolean) => {
    if (!valid) return
    if (form.value.categoryId != undefined) {
      updateCategory(form.value).then(() => { proxy.$modal.msgSuccess('修改成功'); open.value = false; getList() })
    } else {
      addCategory(form.value).then(() => { proxy.$modal.msgSuccess('新增成功'); open.value = false; getList() })
    }
  })
}
function handleDelete(row: SampleCategory) {
  proxy.$modal.confirm('是否确认删除分类“' + row.categoryName + '”？').then(() => delCategory(row.categoryId as number))
    .then(() => { getList(); proxy.$modal.msgSuccess('删除成功') })
}
getList()
</script>
