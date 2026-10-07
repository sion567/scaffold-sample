<template>
  <div class="component-upload-image">
    <el-upload
      multiple
      :disabled="disabled"
      :action="uploadImgUrl"
      list-type="picture-card"
      :on-success="handleUploadSuccess"
      :before-upload="handleBeforeUpload"
      :data="data"
      :limit="limit"
      :on-error="handleUploadError"
      :on-exceed="handleExceed"
      ref="imageUpload"
      :before-remove="handleDelete"
      :show-file-list="true"
      :headers="headers"
      :file-list="fileList"
      :on-preview="handlePictureCardPreview"
      :class="{ hide: fileList.length >= limit }"
    >
      <el-icon class="avatar-uploader-icon"><plus /></el-icon>
    </el-upload>
    <!-- 上传提示 -->
    <div class="el-upload__tip" v-if="showTip && !disabled">
      请上传
      <template v-if="fileSize">
        大小不超过 <b style="color: #f56c6c">{{ fileSize }}MB</b>
      </template>
      <template v-if="fileType">
        格式为 <b style="color: #f56c6c">{{ fileType.join("/") }}</b>
      </template>
      的文件
    </div>

    <el-dialog
      v-model="dialogVisible"
      title="预览"
      width="800px"
      append-to-body
    >
      <img
        :src="dialogImageUrl"
        style="display: block; max-width: 100%; margin: 0 auto"
      />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import type { UploadFile, UploadRawFile, UploadUserFile } from 'element-plus'
import { getToken } from '@/utils/auth'
import { isExternal } from '@/utils/validate'
import Sortable from 'sortablejs'

interface ImageUploadProps {
  /** 值（逗号分隔 url 字符串，或图片对象数组） */
  modelValue?: string | Record<string, unknown> | (string | UploadUserFile)[]
  // 上传接口地址
  action?: string
  // 上传携带的参数
  data?: Record<string, unknown>
  // 图片数量限制
  limit?: number
  // 大小限制(MB)
  fileSize?: number
  // 文件类型, 例如['png', 'jpg', 'jpeg']
  fileType?: string[]
  // 是否显示提示
  isShowTip?: boolean
  // 禁用组件（仅查看图片）
  disabled?: boolean
  // 拖动排序
  drag?: boolean
}

const props = withDefaults(defineProps<ImageUploadProps>(), {
  modelValue: undefined,
  action: '/common/upload',
  data: undefined,
  limit: 5,
  fileSize: 5,
  fileType: () => ['png', 'jpg', 'jpeg'],
  isShowTip: true,
  disabled: false,
  drag: true
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

const { proxy } = getCurrentInstance() as { proxy: ComponentPublicInstance }
const number = ref(0)
const uploadList = ref<UploadUserFile[]>([])
const dialogImageUrl = ref('')
const dialogVisible = ref(false)
const baseUrl = import.meta.env.VITE_APP_BASE_API || ''
const uploadImgUrl = ref(import.meta.env.VITE_APP_BASE_API + props.action) // 上传的图片服务器地址
const headers = ref<Record<string, string>>({ Authorization: 'Bearer ' + getToken() })
const fileList = ref<UploadUserFile[]>([])
const showTip = computed(
  () => props.isShowTip && (props.fileType || props.fileSize)
)

watch(() => props.modelValue, val => {
  if (val) {
    // 首先将值转为数组
    const list = Array.isArray(val) ? val : (props.modelValue as string).split(',')
    // 然后将数组转为对象数组
    fileList.value = list.map(item => {
      if (typeof item === 'string') {
        if (item.indexOf(baseUrl) === -1 && !isExternal(item)) {
          item = { name: baseUrl + item, url: baseUrl + item }
        } else {
          item = { name: item, url: item }
        }
      }
      return item
    })
  } else {
    fileList.value = []
    return []
  }
},{ deep: true, immediate: true })

// 上传前loading加载
function handleBeforeUpload(file: UploadRawFile) {
  let isImg = false
  if (props.fileType.length) {
    let fileExtension = ''
    if (file.name.lastIndexOf('.') > -1) {
      fileExtension = file.name.slice(file.name.lastIndexOf('.') + 1)
    }
    isImg = props.fileType.some((type) => {
      if (file.type.indexOf(type) > -1) return true
      if (fileExtension && fileExtension.indexOf(type) > -1) return true
      return false
    })
  } else {
    isImg = file.type.indexOf('image') > -1
  }
  if (!isImg) {
    proxy.$modal.msgError(`文件格式不正确，请上传${props.fileType.join('/')}图片格式文件!`)
    return false
  }
  if (file.name.includes(',')) {
    proxy.$modal.msgError('文件名不正确，不能包含英文逗号!')
    return false
  }
  if (props.fileSize) {
    const isLt = file.size / 1024 / 1024 < props.fileSize
    if (!isLt) {
      proxy.$modal.msgError(`上传头像图片大小不能超过 ${props.fileSize} MB!`)
      return false
    }
  }
  proxy.$modal.loading('正在上传图片，请稍候...')
  number.value++
}

// 文件个数超出
function handleExceed() {
  proxy.$modal.msgError(`上传文件数量不能超过 ${props.limit} 个!`)
}

/** 上传接口响应（后端 /common/upload 返回字段） */
interface UploadResult {
  code: number
  msg?: string
  fileName?: string
  newFileName?: string
  originalFilename?: string
  url?: string
}

// 上传成功回调
function handleUploadSuccess(res: UploadResult, file?: UploadFile) {
  if (res.code === 200) {
    uploadList.value.push({ name: res.fileName ?? '', url: res.fileName })
    uploadedSuccessfully()
  } else {
    number.value--
    proxy.$modal.closeLoading()
    proxy.$modal.msgError(res.msg ?? '')
    ;(proxy.$refs.imageUpload as { handleRemove: (file?: UploadFile) => void }).handleRemove(file)
    uploadedSuccessfully()
  }
}

// 删除图片
function handleDelete(file: UploadFile) {
  const findex = fileList.value.map(f => f.name).indexOf(file.name)
  if (findex > -1 && uploadList.value.length === number.value) {
    fileList.value.splice(findex, 1)
    emit('update:modelValue', listToString(fileList.value))
    return false
  }
}

// 上传结束处理
function uploadedSuccessfully() {
  if (number.value > 0 && uploadList.value.length === number.value) {
    fileList.value = fileList.value.filter(f => f.url !== undefined).concat(uploadList.value)
    uploadList.value = []
    number.value = 0
    emit('update:modelValue', listToString(fileList.value))
    proxy.$modal.closeLoading()
  }
}

// 上传失败
function handleUploadError() {
  proxy.$modal.msgError('上传图片失败')
  proxy.$modal.closeLoading()
}

// 预览
function handlePictureCardPreview(file: UploadFile) {
  dialogImageUrl.value = file.url ?? ''
  dialogVisible.value = true
}

// 对象转成指定字符串分隔
function listToString(list: UploadUserFile[], separator?: string) {
  let strs = ''
  separator = separator || ','
  for (let i in list) {
    if (list[i].url) {
      strs += list[i].url + separator
    }
  }
  return strs != '' ? strs.substr(0, strs.length - 1) : ''
}

// 初始化拖拽排序
onMounted(() => {
  if (props.drag && !props.disabled) {
    nextTick(() => {
      const imageUploadEl = proxy.$refs.imageUpload as { $el?: HTMLElement } | undefined
      const element = imageUploadEl?.$el?.querySelector('.el-upload-list') as HTMLElement
      Sortable.create(element, {
        onEnd: (evt) => {
          if (evt.oldIndex === undefined || evt.newIndex === undefined) return
          const movedItem = fileList.value.splice(evt.oldIndex, 1)[0]
          fileList.value.splice(evt.newIndex, 0, movedItem)
          emit('update:modelValue', listToString(fileList.value))
        }
      })
    })
  }
})
</script>

<style scoped lang="scss">
// .el-upload--picture-card 控制加号部分
:deep(.hide .el-upload--picture-card) {
    display: none;
}

:deep(.el-upload.el-upload--picture-card.is-disabled) {
  display: none !important;
} 
</style>