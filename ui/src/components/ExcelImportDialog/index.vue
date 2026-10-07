<template>
  <el-dialog :title="title" v-model="visible" :width="width" append-to-body @close="handleClose">
    <el-upload ref="uploadRef" :limit="1" accept=".xlsx, .xls" :headers="headers" :action="uploadUrl" :disabled="isUploading" :on-progress="handleProgress" :on-change="handleFileChange" :on-remove="handleFileRemove" :on-success="handleSuccess" :auto-upload="false" drag>
      <el-icon class="el-icon--upload"><upload-filled /></el-icon>
      <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
      <template #tip>
        <div class="el-upload__tip text-center">
          <div class="el-upload__tip">
            <el-checkbox v-model="updateSupport"> {{ updateSupportLabel }} </el-checkbox>
          </div>
          <span>仅允许导入xls、xlsx格式文件。</span>
          <el-link v-if="templateUrl" type="primary" underline="never" style="font-size: 12px; vertical-align: baseline" @click="handleDownloadTemplate">下载模板</el-link>
        </div>
      </template>
    </el-upload>
    <template #footer>
      <div class="dialog-footer">
        <el-button type="primary" @click="handleSubmit">确 定</el-button>
        <el-button @click="visible = false">取 消</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import type { UploadFile, UploadInstance } from 'element-plus'
import { getToken } from '@/utils/auth'

const { proxy } = getCurrentInstance() as { proxy: ComponentPublicInstance }

interface Props {
  // 对话框标题
  title?: string
  // 对话框宽度
  width?: string
  // 上传接口地址（必传）
  action: string
  // 模板下载接口地址，不传则不显示下载模板链接
  templateAction?: string
  // 模板文件名前缀
  templateFileName?: string
  // 覆盖更新勾选框的说明文字
  updateSupportLabel?: string
}

const props = withDefaults(defineProps<Props>(), {
  title: '数据导入',
  width: '400px',
  templateAction: '',
  templateFileName: 'template',
  updateSupportLabel: '是否更新已经存在的数据'
})

const emit = defineEmits<{ (e: 'success'): void }>()

const uploadRef = ref<UploadInstance | null>(null)
const visible = ref(false)
const selectedFile = ref<UploadFile | null>(null)
const isUploading = ref(false)
const updateSupport = ref(false)
const headers = { Authorization: 'Bearer ' + getToken() }

const uploadUrl = computed(() => {
  return import.meta.env.VITE_APP_BASE_API + props.action + '?updateSupport=' + (updateSupport.value ? 1 : 0)
})

const templateUrl = computed(() => !!props.templateAction)

// 打开对话框（供父组件通过 ref 调用）
function open() {
  updateSupport.value = false
  isUploading.value = false
  visible.value = true
  nextTick(() => {
    selectedFile.value = null
    uploadRef.value?.clearFiles()
  })
}

// 关闭时清理
function handleClose() {
  isUploading.value = false
  selectedFile.value = null
  uploadRef.value?.clearFiles()
}

// 下载模板
function handleDownloadTemplate() {
  proxy.download(props.templateAction, {}, `${props.templateFileName}_${new Date().getTime()}.xlsx`)
}

// 上传进度
function handleProgress() {
  isUploading.value = true
}

/** 文件选择处理 */
const handleFileChange = (file: UploadFile, fileList: UploadFile[]) => {
  selectedFile.value = file
}

/** 文件删除处理 */
const handleFileRemove = (file: UploadFile, fileList: UploadFile[]) => {
  selectedFile.value = null
}

// 上传成功
/** 导入接口响应 */
interface ImportResult {
  code?: number
  msg?: string
}

function handleSuccess(response: ImportResult) {
  visible.value = false
  isUploading.value = false
  selectedFile.value = null
  uploadRef.value?.clearFiles()
  proxy.$alert("<div style='overflow:auto;overflow-x:hidden;max-height:70vh;padding:10px 20px 0;'>" + response.msg + '</div>', '导入结果', { dangerouslyUseHTMLString: true })
  emit('success')
}

// 提交上传
function handleSubmit() {
  const file = selectedFile.value
  if (!file || !file.name.toLowerCase().endsWith('.xls') && !file.name.toLowerCase().endsWith('.xlsx')) {
    proxy.$modal.msgError("请选择后缀为 “xls”或“xlsx”的文件。")
    return
  }
  if (!uploadRef.value) return
  uploadRef.value.submit()
}

defineExpose({ open })
</script>
