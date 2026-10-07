/**
* v-copyText 复制文本内容
* Copyright (c) 2022 ct
*/
import type { Directive } from 'vue'

/** 挂载复制相关缓存字段的元素（beforeMount 时动态写入） */
interface CopyElement extends HTMLElement {
  /** 待复制文本 */
  $copyValue: string
  /** 复制成功回调 */
  $copyCallback?: (value: string) => void
  /** 解绑事件 */
  $destroyCopy?: () => void
}

/** v-copyText 的值：普通复制传文本，:callback 修饰符传复制成功回调 */
type CopyTextValue = string | ((value: string) => void)

const copyText: Directive<CopyElement, CopyTextValue> = {
  beforeMount(el, { value, arg }) {
    if (arg === "callback") {
      el.$copyCallback = value as (value: string) => void
    } else {
      el.$copyValue = value as string
      const handler = () => {
        copyTextToClipboard(el.$copyValue)
        if (el.$copyCallback) {
          el.$copyCallback(el.$copyValue)
        }
      }
      el.addEventListener("click", handler)
      el.$destroyCopy = () => el.removeEventListener("click", handler)
    }
  }
}

export default copyText

function copyTextToClipboard(input: string, { target = document.body }: { target?: HTMLElement } = {}) {
  const element = document.createElement('textarea')
  const previouslyFocusedElement = document.activeElement

  element.value = input

  // Prevent keyboard from showing on mobile
  element.setAttribute('readonly', '')

  element.style.contain = 'strict'
  element.style.position = 'absolute'
  element.style.left = '-9999px'
  element.style.fontSize = '12pt' // Prevent zooming on iOS

  const selection = document.getSelection()
  if (!selection) return false
  const originalRange = selection.rangeCount > 0 && selection.getRangeAt(0)

  target.append(element)
  element.select()

  // Explicit selection workaround for iOS
  element.selectionStart = 0
  element.selectionEnd = input.length

  let isSuccess = false
  try {
    isSuccess = document.execCommand('copy')
  } catch { }

  element.remove()

  if (originalRange) {
    selection.removeAllRanges()
    selection.addRange(originalRange)
  }

  // Get the focus back on the previously focused element, if any
  if (previouslyFocusedElement) {
    (previouslyFocusedElement as HTMLElement).focus()
  }

  return isSuccess
}
