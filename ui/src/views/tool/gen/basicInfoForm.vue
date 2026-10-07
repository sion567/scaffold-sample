<template>
  <el-form ref="basicInfoForm" :model="info" :rules="rules" label-width="150px">
    <el-row>
      <el-col :span="12">
        <el-form-item label="表名称" prop="tableName">
          <el-input placeholder="请输入仓库名称" :model-value="info.tableName" @update:model-value="(v: unknown) => emit('update-field', 'tableName', v)" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="表描述" prop="tableComment">
          <el-input placeholder="请输入" :model-value="info.tableComment" @update:model-value="(v: unknown) => emit('update-field', 'tableComment', v)" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="实体类名称" prop="className">
          <el-input placeholder="请输入" :model-value="info.className" @update:model-value="(v: unknown) => emit('update-field', 'className', v)" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="作者" prop="functionAuthor">
          <el-input placeholder="请输入" :model-value="info.functionAuthor" @update:model-value="(v: unknown) => emit('update-field', 'functionAuthor', v)" />
        </el-form-item>
      </el-col>
      <el-col :span="24">
        <el-form-item label="备注" prop="remark">
          <el-input type="textarea" :rows="3" :model-value="info.remark" @update:model-value="(v: unknown) => emit('update-field', 'remark', v)"></el-input>
        </el-form-item>
      </el-col>
    </el-row>
  </el-form>
</template>

<script setup lang="ts">
import type { FormRules } from 'element-plus'
import type { GenTable } from '@/api/tool/gen'

interface Props {
  /** 基础信息(生成表,父组件必传) */
  info: GenTable
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'update-field', key: string, value: unknown): void
}>()

// 表单校验
const rules = ref<FormRules>({
  tableName: [{ required: true, message: "请输入表名称", trigger: "blur" }],
  tableComment: [{ required: true, message: "请输入表描述", trigger: "blur" }],
  className: [{ required: true, message: "请输入实体类名称", trigger: "blur" }],
  functionAuthor: [{ required: true, message: "请输入作者", trigger: "blur" }]
})
</script>
