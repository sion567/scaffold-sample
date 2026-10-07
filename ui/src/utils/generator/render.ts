import { defineComponent, h, resolveComponent } from 'vue'
import type { PropType } from 'vue'
import { makeMap } from '@/utils/index.ts'
import type { ElementConfig } from './config.ts'

const isAttr = makeMap(
  'accept,accept-charset,accesskey,action,align,alt,async,autocomplete,' +
  'autofocus,autoplay,autosave,bgcolor,border,buffered,challenge,charset,' +
  'checked,cite,class,code,codebase,color,cols,colspan,content,http-equiv,' +
  'name,contenteditable,contextmenu,controls,coords,data,datetime,default,' +
  'defer,dir,dirname,disabled,download,draggable,dropzone,enctype,method,for,' +
  'form,formaction,headers,height,hidden,high,href,hreflang,http-equiv,' +
  'icon,id,ismap,itemprop,keytype,kind,label,lang,language,list,loop,low,' +
  'manifest,max,maxlength,media,method,GET,POST,min,multiple,email,file,' +
  'muted,name,novalidate,open,optimum,pattern,ping,placeholder,poster,' +
  'preload,radiogroup,readonly,rel,required,reversed,rows,rowspan,sandbox,' +
  'scope,scoped,seamless,selected,shape,size,type,text,password,sizes,span,' +
  'spellcheck,src,srcdoc,srclang,srcset,start,step,style,summary,tabindex,' +
  'target,title,type,usemap,value,width,wrap' + 'prefix-icon'
)
const isNotProps = makeMap(
  'layout,prepend,regList,tag,document,changeTag,defaultValue'
)

/** 子节点/插槽渲染函数（参数为 h 函数、元素配置与触发的配置键） */
type ChildRender = (hFn: typeof h, conf: ElementConfig, key: string) => unknown

function useVModel(props: { defaultValue?: unknown }, emit: (event: 'update:modelValue', ...args: unknown[]) => void) {
  return {
    modelValue: props.defaultValue,
    'onUpdate:modelValue': (val: unknown) => emit('update:modelValue', val),
  }
}
const componentChild: Record<string, Record<string, ChildRender>> = {
  'el-button': {
    default(h, conf, key) {
      return conf[key]
    },
  },
  'el-select': {
    options(h, conf, key) {
      return conf.options!.map(item => h(resolveComponent('el-option'), {
        label: item.label,
        value: item.value,
      }))
    }
  },
  'el-radio-group': {
    options(h, conf, key) {
      return conf.optionType === 'button' ? conf.options!.map(item => h(resolveComponent('el-checkbox-button'), {
        label: item.value,
      }, () => item.label)) : conf.options!.map(item => h(resolveComponent('el-radio'), {
        label: item.value,
        border: conf.border,
      }, () => item.label))
    }
  },
  'el-checkbox-group': {
    options(h, conf, key) {
      return conf.optionType === 'button' ? conf.options!.map(item => h(resolveComponent('el-checkbox-button'), {
        label: item.value,
      }, () => item.label)) : conf.options!.map(item => h(resolveComponent('el-checkbox'), {
        label: item.value,
        border: conf.border,
      }, () => item.label))
    }
  },
  'el-upload': {
    'list-type': (h, conf, key) => {
      const option: Record<string, unknown> = {}
      // if (conf.showTip) {
      //   tip = h('div', {
      //     class: "el-upload__tip"
      //   }, () => '只能上传不超过' + conf.fileSize + conf.sizeUnit + '的' + conf.accept + '文件')
      // }
      if (conf['list-type'] === 'picture-card') {
        return h(resolveComponent('el-icon'), option, () => h(resolveComponent('Plus')))
      } else {
        // option.size = "small"
        option.type = "primary"
        option.icon = "Upload"
        return h(resolveComponent('el-button'), option, () => conf.buttonText)
      }
    },

  }
}
const componentSlot: Record<string, Record<string, ChildRender>> = {
  'el-upload': {
    'tip': (h, conf, key) => {
      if (conf.showTip) {
        return () => h('div', {
          class: "el-upload__tip"
        }, '只能上传不超过' + conf.fileSize + conf.sizeUnit + '的' + conf.accept + '文件')
      }
    },
  }
}
export default defineComponent({
  props: {
    conf: {
      type: Object as PropType<ElementConfig>,
      required: true,
    },
  },

  // 使用 render 函数
  render() {
    const dataObject: Record<string, Record<string, unknown>> = {
      attrs: {},
      props: {},
      on: {},
      style: {}
    }
    const confClone = JSON.parse(JSON.stringify(this.conf)) as ElementConfig
    const children: unknown[] = []
    const slot: Record<string, unknown> = {}
    const childObjs = componentChild[confClone.tag!]
    if (childObjs) {
      Object.keys(childObjs).forEach(key => {
        const childFunc = childObjs[key]
        if (confClone[key]) {
          children.push(childFunc(h, confClone, key))
        }
      })
    }
    const slotObjs = componentSlot[confClone.tag!]
    if (slotObjs) {
      Object.keys(slotObjs).forEach(key => {
        const childFunc = slotObjs[key]
        if (confClone[key]) {
          slot[key] = childFunc(h, confClone, key)
        }
      })
    }
    Object.keys(confClone).forEach(key => {
      const val = confClone[key]
      if (dataObject[key]) {
        ;(dataObject as Record<string, unknown>)[key] = val
      } else if (isAttr(key)) {
        dataObject.attrs[key] = val
      } else if (!isNotProps(key)) {
        dataObject.props[key] = val
      }
    })
    if(children.length > 0){
      slot.default = () => children
    }

    return h(resolveComponent(this.conf.tag!),
      {
        modelValue: this.$attrs.modelValue,
        ...dataObject.props,
        ...dataObject.attrs,
        style: {
          ...dataObject.style
        },
      } as Parameters<typeof h>[1]
      , slot as Parameters<typeof h>[2])
  }
})
