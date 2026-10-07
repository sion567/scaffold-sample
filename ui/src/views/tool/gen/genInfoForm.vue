<template>
  <el-form ref="genInfoForm" :model="info" :rules="rules" label-width="150px">
    <el-row>
      <el-col :span="12">
        <el-form-item prop="tplCategory">
          <template #label>生成模板</template>
          <el-select :model-value="info.tplCategory" @update:model-value="(v: unknown) => emit('update-field', 'tplCategory', v)" @change="tplSelectChange">
            <el-option label="单表（增删改查）" value="crud" />
            <el-option label="树表（增删改查）" value="tree" />
            <el-option label="主子表（增删改查）" value="sub" />
          </el-select>
        </el-form-item>
      </el-col>

      <el-col :span="12">
        <el-form-item prop="packageName">
          <template #label>
            生成包路径
            <el-tooltip content="生成在哪个java包下，例如 com.ct.system" placement="top">
              <el-icon><question-filled /></el-icon>
            </el-tooltip>
          </template>
          <el-input :model-value="info.packageName" @update:model-value="(v: unknown) => emit('update-field', 'packageName', v)" />
        </el-form-item>
      </el-col>

      <el-col :span="12">
        <el-form-item prop="moduleName">
          <template #label>
            生成模块名
            <el-tooltip content="可理解为子系统名，例如 system" placement="top">
              <el-icon><question-filled /></el-icon>
            </el-tooltip>
          </template>
          <el-input :model-value="info.moduleName" @update:model-value="(v: unknown) => emit('update-field', 'moduleName', v)" />
        </el-form-item>
      </el-col>

      <el-col :span="12">
        <el-form-item prop="businessName">
          <template #label>
            生成业务名
            <el-tooltip content="可理解为功能英文名，例如 user" placement="top">
              <el-icon><question-filled /></el-icon>
            </el-tooltip>
          </template>
          <el-input :model-value="info.businessName" @update:model-value="(v: unknown) => emit('update-field', 'businessName', v)" />
        </el-form-item>
      </el-col>

      <el-col :span="12">
        <el-form-item prop="functionName">
          <template #label>
            生成功能名
            <el-tooltip content="用作类描述，例如 用户" placement="top">
              <el-icon><question-filled /></el-icon>
            </el-tooltip>
          </template>
          <el-input :model-value="info.functionName" @update:model-value="(v: unknown) => emit('update-field', 'functionName', v)" />
        </el-form-item>
      </el-col>

      <el-col :span="12">
        <el-form-item prop="formColNum">
          <template #label>
            表单布局
            <el-tooltip content="选择表单的栅格布局方式" placement="top">
              <el-icon><question-filled /></el-icon>
            </el-tooltip>
          </template>
          <el-select :model-value="info.formColNum" @update:model-value="(v: unknown) => emit('update-field', 'formColNum', v)">
            <el-option label="单列" :value="1" />
            <el-option label="双列" :value="2" />
            <el-option label="三列" :value="3" />
          </el-select>
        </el-form-item>
      </el-col>

      <el-col :span="12">
        <el-form-item prop="genView">
          <template #label>扩展功能</template>
          <el-checkbox :model-value="info.view" @update:model-value="(v: unknown) => emit('update-field', 'view', v)">生成详情页</el-checkbox>
        </el-form-item>
      </el-col>

      <el-col :span="12">
        <el-form-item prop="genType">
          <template #label>
            生成代码方式
            <el-tooltip content="默认为zip压缩包下载，也可以自定义生成路径" placement="top">
              <el-icon><question-filled /></el-icon>
            </el-tooltip>
          </template>
          <el-radio :model-value="info.genType" @update:model-value="(v: unknown) => emit('update-field', 'genType', v)" value="0">zip压缩包</el-radio>
          <el-radio :model-value="info.genType" @update:model-value="(v: unknown) => emit('update-field', 'genType', v)" value="1">自定义路径</el-radio>
        </el-form-item>
      </el-col>

      <el-col :span="12">
        <el-form-item>
          <template #label>
            上级菜单
            <el-tooltip content="分配到指定菜单下，例如 系统管理" placement="top">
              <el-icon><question-filled /></el-icon>
            </el-tooltip>
          </template>
          <el-tree-select
            :model-value="info.parentMenuId" @update:model-value="(v: unknown) => emit('update-field', 'parentMenuId', v)"
            :data="menuOptions"
            :props="{ value: 'menuId', label: 'menuName', children: 'children' }"
            placeholder="请选择系统菜单"
            check-strictly
          />
        </el-form-item>
      </el-col>

      <el-col :span="24" v-if="info.genType == '1'">
        <el-form-item prop="genPath">
          <template #label>
            自定义路径
            <el-tooltip content="填写磁盘绝对路径，若不填写，则生成到当前Web项目下" placement="top">
              <el-icon><question-filled /></el-icon>
            </el-tooltip>
          </template>
          <el-input :model-value="info.genPath" @update:model-value="(v: unknown) => emit('update-field', 'genPath', v)">
            <template #append>
              <el-dropdown>
                <el-button type="primary">
                  最近路径快速选择
                  <i class="el-icon-arrow-down el-icon--right"></i>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="emit('update-field', 'genPath', '/')">恢复默认的生成基础路径</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
          </el-input>
        </el-form-item>
      </el-col>
    </el-row>
    
    <template v-if="info.tplCategory == 'tree'">
      <h4 class="form-header">其他信息</h4>
      <el-row v-show="info.tplCategory == 'tree'">
        <el-col :span="12">
          <el-form-item>
            <template #label>
              树编码字段
              <el-tooltip content="树显示的编码字段名， 如：dept_id" placement="top">
                <el-icon><question-filled /></el-icon>
              </el-tooltip>
            </template>
            <el-select :model-value="info.treeCode" @update:model-value="(v: unknown) => emit('update-field', 'treeCode', v)" placeholder="请选择">
              <el-option
                v-for="(column, index) in info.columns"
                :key="index"
                :label="column.columnName + '：' + column.columnComment"
                :value="column.columnName"
              ></el-option>
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item>
            <template #label>
              树父编码字段
              <el-tooltip content="树显示的父编码字段名， 如：parent_Id" placement="top">
                <el-icon><question-filled /></el-icon>
              </el-tooltip>
            </template>
            <el-select :model-value="info.treeParentCode" @update:model-value="(v: unknown) => emit('update-field', 'treeParentCode', v)" placeholder="请选择">
              <el-option
                v-for="(column, index) in info.columns"
                :key="index"
                :label="column.columnName + '：' + column.columnComment"
                :value="column.columnName"
              ></el-option>
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item>
            <template #label>
              树名称字段
              <el-tooltip content="树节点的显示名称字段名， 如：dept_name" placement="top">
                <el-icon><question-filled /></el-icon>
              </el-tooltip>
            </template>
            <el-select :model-value="info.treeName" @update:model-value="(v: unknown) => emit('update-field', 'treeName', v)" placeholder="请选择">
              <el-option
                v-for="(column, index) in info.columns"
                :key="index"
                :label="column.columnName + '：' + column.columnComment"
                :value="column.columnName"
              ></el-option>
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
    </template>

    <template v-if="info.tplCategory == 'sub'">
      <h4 class="form-header">关联信息</h4>
      <el-row>
        <el-col :span="12">
          <el-form-item>
            <template #label>
              关联子表的表名
              <el-tooltip content="关联子表的表名， 如：sys_user" placement="top">
                <el-icon><question-filled /></el-icon>
              </el-tooltip>
            </template>
            <el-select :model-value="info.subTableName" @update:model-value="(v: unknown) => emit('update-field', 'subTableName', v)" placeholder="请选择" @change="subSelectChange">
              <el-option
                v-for="(table, index) in tables"
                :key="index"
                :label="table.tableName + '：' + table.tableComment"
                :value="table.tableName"
              ></el-option>
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item>
            <template #label>
              子表关联的外键名
              <el-tooltip content="子表关联的外键名， 如：user_id" placement="top">
                <el-icon><question-filled /></el-icon>
              </el-tooltip>
            </template>
            <el-select :model-value="info.subTableFkName" @update:model-value="(v: unknown) => emit('update-field', 'subTableFkName', v)" placeholder="请选择">
              <el-option
                v-for="(column, index) in subColumns"
                :key="index"
                :label="column.columnName + '：' + column.columnComment"
                :value="column.columnName"
              ></el-option>
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
    </template>

  </el-form>
</template>

<script setup lang="ts">
import { listMenu } from "@/api/system/menu"
import type { ComponentPublicInstance } from 'vue'
import type { FormRules } from 'element-plus'
import type { GenTable, GenTableColumn } from '@/api/tool/gen'

const subColumns = ref<GenTableColumn[]>([])
const menuOptions = ref<GenTable[]>([])
const { proxy } = getCurrentInstance() as { proxy: ComponentPublicInstance }

interface Props {
  /** 生成表信息(父组件必传) */
  info: GenTable
  /** 主表及子表列表 */
  tables?: GenTable[] | null
}

const props = withDefaults(defineProps<Props>(), {
  tables: null
})

const emit = defineEmits<{
  (e: 'update-field', key: string, value: unknown): void
}>()

// 表单校验
const rules = ref<FormRules>({
  tplCategory: [{ required: true, message: "请选择生成模板", trigger: "blur" }],
  packageName: [{ required: true, message: "请输入生成包路径", trigger: "blur" }],
  moduleName: [{ required: true, message: "请输入生成模块名", trigger: "blur" }],
  businessName: [{ required: true, message: "请输入生成业务名", trigger: "blur" }],
  functionName: [{ required: true, message: "请输入生成功能名", trigger: "blur" }]
})

function subSelectChange(value: string) {
  emit('update-field', 'subTableFkName', "")
}

function tplSelectChange(value: string) {
  if (value !== "sub") {
    emit('update-field', 'subTableName', "")
    emit('update-field', 'subTableFkName', "")
  }
}

function setSubTableColumns(value?: string) {
  for (const table of props.tables ?? []) {
    if (value === table.tableName) {
      subColumns.value = table.columns ?? []
      break
    }
  }
}

/** 查询菜单下拉树结构 */
function getMenuTreeselect() {
  listMenu().then(response => {
    menuOptions.value = proxy.handleTree(response.data, "menuId")
  })
}

onMounted(() => {
  getMenuTreeselect()
})

watch(() => props.info.subTableName, val => {
  setSubTableColumns(val)
})
</script>
