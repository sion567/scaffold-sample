import axios from 'axios'
import { ElLoading, ElMessage } from 'element-plus'
import { saveAs } from 'file-saver'
import { getToken } from '@/utils/auth.ts'
import errorCode from '@/utils/errorCode.ts'
import { blobValidate } from '@/utils/ct.ts'

const baseURL = import.meta.env.VITE_APP_BASE_API
let downloadLoadingInstance: ReturnType<typeof ElLoading.service> | undefined

export default {
  name(name: string, isDelete = true) {
    const url = baseURL + "/common/download?fileName=" + encodeURIComponent(name) + "&delete=" + isDelete
    axios({
      method: 'get',
      url: url,
      responseType: 'blob',
      headers: { 'Authorization': 'Bearer ' + getToken() }
    }).then((res) => {
      const isBlob = blobValidate(res.data)
      if (isBlob) {
        const blob = new Blob([res.data])
        this.saveAs(blob, decodeURIComponent(res.headers['download-filename'] as string))
      } else {
        this.printErrMsg(res.data)
      }
    })
  },
  resource(resource: string) {
    const url = baseURL + "/common/download/resource?resource=" + encodeURIComponent(resource)
    axios({
      method: 'get',
      url: url,
      responseType: 'blob',
      headers: { 'Authorization': 'Bearer ' + getToken() }
    }).then((res) => {
      const isBlob = blobValidate(res.data)
      if (isBlob) {
        const blob = new Blob([res.data])
        this.saveAs(blob, decodeURIComponent(res.headers['download-filename'] as string))
      } else {
        this.printErrMsg(res.data)
      }
    })
  },
  zip(url: string, name: string) {
    const downloadUrl = baseURL + url
    downloadLoadingInstance = ElLoading.service({ text: "正在下载数据，请稍候", background: "rgba(0, 0, 0, 0.7)", })
    axios({
      method: 'get',
      url: url,
      responseType: 'blob',
      headers: { 'Authorization': 'Bearer ' + getToken() }
    }).then((res) => {
      const isBlob = blobValidate(res.data)
      if (isBlob) {
        const blob = new Blob([res.data], { type: 'application/zip' })
        this.saveAs(blob, name)
      } else {
        this.printErrMsg(res.data)
      }
      downloadLoadingInstance?.close()
    }).catch((r) => {
      console.error(r)
      ElMessage.error('下载文件出现错误，请联系管理员！')
      downloadLoadingInstance?.close()
    })
  },
  saveAs(text: Blob, name?: string, opts?: Parameters<typeof saveAs>[2]) {
    saveAs(text, name, opts)
  },
  async printErrMsg(data: Blob) {
    const resText = await data.text()
    const rspObj = JSON.parse(resText) as { code?: number; msg?: string }
    const errMsg = errorCode[rspObj.code!] || rspObj.msg || errorCode['default']
    ElMessage.error(errMsg)
  }
}
