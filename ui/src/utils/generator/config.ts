/** 表单项正则校验规则 */
export interface RegListConfig {
  /** 正则表达式（字符串源码，生成代码时 new RegExp(pattern)） */
  pattern: string
  /** 校验失败提示 */
  message: string
}

/** 表单组件选项（下拉/单选/多选/级联通用） */
export interface ComponentOption {
  label: string
  value: string | number
  id?: number
  disabled?: boolean
  children?: ComponentOption[]
}

/**
 * 生成器元素统一配置
 * 字段为 inputComponents/selectComponents/layoutComponents 及运行期绘制数据的全集，
 * 除布局键外均可选（不同组件只使用各自的子集）
 */
export interface ElementConfig {
  /** 布局类型：表单项 / 行容器 */
  layout?: 'colFormItem' | 'rowFormItem'
  /** 组件标签（如 el-input） */
  tag?: string
  tagIcon?: string
  label?: string | null
  /** 双向绑定字段名 */
  vModel?: string
  formId?: number
  renderKey?: number
  /** 行容器组件名 */
  componentName?: string
  span?: number
  labelWidth?: string | number | null
  style?: { width?: string }
  placeholder?: string
  defaultValue?: unknown
  disabled?: boolean
  required?: boolean
  regList?: RegListConfig[]
  changeTag?: boolean
  document?: string
  layoutTree?: boolean
  gutter?: number
  /** 行容器 el-row 的 type / justify / align */
  type?: string
  justify?: string
  align?: string
  children?: ElementConfig[]

  // el-input 系列
  clearable?: boolean
  prepend?: string
  append?: string
  'prefix-icon'?: string
  'suffix-icon'?: string
  maxlength?: number | string | null
  'show-word-limit'?: boolean
  readonly?: boolean
  autosize?: { minRows: number; maxRows: number }
  'show-password'?: boolean

  // el-input-number / el-slider / el-rate
  min?: number | string | null
  max?: number | string | null
  step?: number | string | null
  'step-strictly'?: boolean
  precision?: number | string | null
  'controls-position'?: string
  'show-stops'?: boolean
  range?: boolean
  'allow-half'?: boolean
  'show-text'?: boolean
  'show-score'?: boolean

  // el-select / el-cascader
  filterable?: boolean
  multiple?: boolean
  options?: ComponentOption[]
  dataType?: string
  labelKey?: string
  valueKey?: string
  childrenKey?: string
  separator?: string
  /** 级联组件的级联 props（注意嵌套同名） */
  props?: { props?: Record<string, unknown> }
  'show-all-levels'?: boolean

  // el-radio-group / el-checkbox-group
  optionType?: string
  border?: boolean
  size?: string

  // el-switch
  'active-text'?: string
  'inactive-text'?: string
  'active-color'?: string | null
  'inactive-color'?: string | null
  'active-value'?: unknown
  'inactive-value'?: unknown

  // el-time-picker / el-date-picker
  format?: string
  'value-format'?: string
  'is-range'?: boolean
  'range-separator'?: string
  'start-placeholder'?: string
  'end-placeholder'?: string
  'picker-options'?: { selectableRange?: string }

  // el-color-picker
  'show-alpha'?: boolean
  'color-format'?: string

  // el-upload
  action?: string
  accept?: string
  name?: string
  'auto-upload'?: boolean
  showTip?: boolean
  buttonText?: string
  fileSize?: number
  sizeUnit?: string
  'list-type'?: string
  tip?: string

  // el-button
  default?: string
  icon?: string

  /** 运行期允许携带任意自定义键（render/js 按键名动态读取配置） */
  [key: string]: unknown
}

/** 表单全局配置（生成器右侧面板的 formConf） */
export interface FormConf {
  formRef: string
  formModel: string
  size: string
  labelPosition: string
  labelWidth: number
  formRules: string
  gutter: number
  disabled: boolean
  span: number
  formBtns: boolean
  /** 表单设计器扩展：未聚焦组件边框（RightPanel 开关，DraggableItem 高亮用） */
  unFocusedComponentBorder?: boolean
}

/** 组装代码时传入的完整配置：全局配置 + 字段列表 */
export type FormDesignConf = FormConf & { fields: ElementConfig[] }

export const formConf: FormConf = {
  formRef: 'formRef',
  formModel: 'formData',
  size: 'default',
  labelPosition: 'right',
  labelWidth: 100,
  formRules: 'rules',
  gutter: 15,
  disabled: false,
  span: 24,
  formBtns: true,
}

export const inputComponents: ElementConfig[] = [
  {
    label: '单行文本',
    tag: 'el-input',
    tagIcon: 'input',
    type: 'text',
    placeholder: '请输入',
    defaultValue: undefined,
    span: 24,
    labelWidth: null,
    style: { width: '100%' },
    clearable: true,
    prepend: '',
    append: '',
    'prefix-icon': '',
    'suffix-icon': '',
    maxlength: null,
    'show-word-limit': false,
    readonly: false,
    disabled: false,
    required: true,
    regList: [],
    changeTag: true,
    document: 'https://element-plus.org/zh-CN/component/input',
  },
  {
    label: '多行文本',
    tag: 'el-input',
    tagIcon: 'textarea',
    type: 'textarea',
    placeholder: '请输入',
    defaultValue: undefined,
    span: 24,
    labelWidth: null,
    autosize: {
      minRows: 4,
      maxRows: 4,
    },
    style: { width: '100%' },
    maxlength: null,
    'show-word-limit': false,
    readonly: false,
    disabled: false,
    required: true,
    regList: [],
    changeTag: true,
    document: 'https://element-plus.org/zh-CN/component/input',
  },
  {
    label: '密码',
    tag: 'el-input',
    tagIcon: 'password',
    type: 'password',
    placeholder: '请输入',
    defaultValue: undefined,
    span: 24,
    'show-password': true,
    labelWidth: null,
    style: { width: '100%' },
    clearable: true,
    prepend: '',
    append: '',
    'prefix-icon': '',
    'suffix-icon': '',
    maxlength: null,
    'show-word-limit': false,
    readonly: false,
    disabled: false,
    required: true,
    regList: [],
    changeTag: true,
    document: 'https://element-plus.org/zh-CN/component/input',
  },
  {
    label: '计数器',
    tag: 'el-input-number',
    tagIcon: 'number',
    placeholder: '',
    defaultValue: undefined,
    span: 24,
    labelWidth: null,
    min: undefined,
    max: undefined,
    step: undefined,
    'step-strictly': false,
    precision: undefined,
    'controls-position': '',
    disabled: false,
    required: true,
    regList: [],
    changeTag: true,
    document: 'https://element-plus.org/zh-CN/component/input-number',
  },
] satisfies ElementConfig[]

export const selectComponents: ElementConfig[] = [
  {
    label: '下拉选择',
    tag: 'el-select',
    tagIcon: 'select',
    placeholder: '请选择',
    defaultValue: undefined,
    span: 24,
    labelWidth: null,
    style: { width: '100%' },
    clearable: true,
    disabled: false,
    required: true,
    filterable: false,
    multiple: false,
    options: [
      {
        label: '选项一',
        value: 1,
      },
      {
        label: '选项二',
        value: 2,
      },
    ],
    regList: [],
    changeTag: true,
    document: 'https://element-plus.org/zh-CN/component/select',
  },
  {
    label: '级联选择',
    tag: 'el-cascader',
    tagIcon: 'cascader',
    placeholder: '请选择',
    defaultValue: [],
    span: 24,
    labelWidth: null,
    style: { width: '100%' },
    props: {
      props: {
        multiple: false,
      },
    },
    'show-all-levels': true,
    disabled: false,
    clearable: true,
    filterable: false,
    required: true,
    options: [
      {
        id: 1,
        value: 1,
        label: '选项1',
        children: [
          {
            id: 2,
            value: 2,
            label: '选项1-1',
          },
        ],
      },
    ],
    dataType: 'dynamic',
    labelKey: 'label',
    valueKey: 'value',
    childrenKey: 'children',
    separator: '/',
    regList: [],
    changeTag: true,
    document: 'https://element-plus.org/zh-CN/component/cascader',
  },
  {
    label: '单选框组',
    tag: 'el-radio-group',
    tagIcon: 'radio',
    defaultValue: 0,
    span: 24,
    labelWidth: null,
    style: {},
    optionType: 'default',
    border: false,
    size: 'default',
    disabled: false,
    required: true,
    options: [
      {
        label: '选项一',
        value: 1,
      },
      {
        label: '选项二',
        value: 2,
      },
    ],
    regList: [],
    changeTag: true,
    document: 'https://element-plus.org/zh-CN/component/radio',
  },
  {
    label: '多选框组',
    tag: 'el-checkbox-group',
    tagIcon: 'checkbox',
    defaultValue: [],
    span: 24,
    labelWidth: null,
    style: {},
    optionType: 'default',
    border: false,
    size: 'default',
    disabled: false,
    required: true,
    options: [
      {
        label: '选项一',
        value: 1,
      },
      {
        label: '选项二',
        value: 2,
      },
    ],
    regList: [],
    changeTag: true,
    document: 'https://element-plus.org/zh-CN/component/checkbox',
  },
  {
    label: '开关',
    tag: 'el-switch',
    tagIcon: 'switch',
    defaultValue: false,
    span: 24,
    labelWidth: null,
    style: {},
    disabled: false,
    required: true,
    'active-text': '',
    'inactive-text': '',
    'active-color': null,
    'inactive-color': null,
    'active-value': true,
    'inactive-value': false,
    regList: [],
    changeTag: true,
    document: 'https://element-plus.org/zh-CN/component/switch',
  },
  {
    label: '滑块',
    tag: 'el-slider',
    tagIcon: 'slider',
    defaultValue: null,
    span: 24,
    labelWidth: null,
    disabled: false,
    required: true,
    min: 0,
    max: 100,
    step: 1,
    'show-stops': false,
    range: false,
    regList: [],
    changeTag: true,
    document: 'https://element-plus.org/zh-CN/component/slider',
  },
  {
    label: '时间选择',
    tag: 'el-time-picker',
    tagIcon: 'time',
    placeholder: '请选择',
    defaultValue: '',
    span: 24,
    labelWidth: null,
    style: { width: '100%' },
    disabled: false,
    clearable: true,
    required: true,
    format: 'HH:mm:ss',
    'value-format': 'HH:mm:ss',
    regList: [],
    changeTag: true,
    document: 'https://element-plus.org/zh-CN/component/time-picker',
  },
  {
    label: '时间范围',
    tag: 'el-time-picker',
    tagIcon: 'time-range',
    defaultValue: null,
    span: 24,
    labelWidth: null,
    style: { width: '100%' },
    disabled: false,
    clearable: true,
    required: true,
    'is-range': true,
    'range-separator': '至',
    'start-placeholder': '开始时间',
    'end-placeholder': '结束时间',
    format: 'HH:mm:ss',
    'value-format': 'HH:mm:ss',
    regList: [],
    changeTag: true,
    document: 'https://element-plus.org/zh-CN/component/time-picker',
  },
  {
    label: '日期选择',
    tag: 'el-date-picker',
    tagIcon: 'date',
    placeholder: '请选择',
    defaultValue: null,
    type: 'date',
    span: 24,
    labelWidth: null,
    style: { width: '100%' },
    disabled: false,
    clearable: true,
    required: true,
    format: 'YYYY-MM-DD',
    'value-format': 'YYYY-MM-DD',
    readonly: false,
    regList: [],
    changeTag: true,
    document: 'https://element-plus.org/zh-CN/component/date-picker',
  },
  {
    label: '日期范围',
    tag: 'el-date-picker',
    tagIcon: 'date-range',
    defaultValue: null,
    span: 24,
    labelWidth: null,
    style: { width: '100%' },
    type: 'daterange',
    'range-separator': '至',
    'start-placeholder': '开始日期',
    'end-placeholder': '结束日期',
    disabled: false,
    clearable: true,
    required: true,
    format: 'YYYY-MM-DD',
    'value-format': 'YYYY-MM-DD',
    readonly: false,
    regList: [],
    changeTag: true,
    document: 'https://element-plus.org/zh-CN/component/date-picker',
  },
  {
    label: '评分',
    tag: 'el-rate',
    tagIcon: 'rate',
    defaultValue: 0,
    span: 24,
    labelWidth: null,
    style: {},
    max: 5,
    'allow-half': false,
    'show-text': false,
    'show-score': false,
    disabled: false,
    required: true,
    regList: [],
    changeTag: true,
    document: 'https://element-plus.org/zh-CN/component/rate',
  },
  {
    label: '颜色选择',
    tag: 'el-color-picker',
    tagIcon: 'color',
    defaultValue: null,
    labelWidth: null,
    'show-alpha': false,
    'color-format': '',
    disabled: false,
    required: true,
    size: 'default',
    regList: [],
    changeTag: true,
    document: 'https://element-plus.org/zh-CN/component/color-picker',
  },
  {
    label: '上传',
    tag: 'el-upload',
    tagIcon: 'upload',
    action: 'https://jsonplaceholder.typicode.com/posts/',
    defaultValue: null,
    labelWidth: null,
    disabled: false,
    required: true,
    accept: '',
    name: 'file',
    'auto-upload': true,
    showTip: false,
    buttonText: '点击上传',
    fileSize: 2,
    sizeUnit: 'MB',
    'list-type': 'text',
    multiple: false,
    regList: [],
    changeTag: true,
    document: 'https://element-plus.org/zh-CN/component/upload',
    tip: '只能上传不超过 2MB 的文件',
    style: { width: '100%' },
  },
] satisfies ElementConfig[]

export const layoutComponents: ElementConfig[] = [
  {
    layout: 'rowFormItem',
    tagIcon: 'row',
    type: 'default',
    justify: 'start',
    align: 'top',
    label: '行容器',
    layoutTree: true,
    children: [],
    document: 'https://element-plus.org/zh-CN/component/layout',
  },
  {
    layout: 'colFormItem',
    label: '按钮',
    changeTag: true,
    labelWidth: null,
    tag: 'el-button',
    tagIcon: 'button',
    span: 24,
    default: '主要按钮',
    type: 'primary',
    icon: 'Search',
    size: 'default',
    disabled: false,
    document: 'https://element-plus.org/zh-CN/component/button',
  },
] satisfies ElementConfig[]

// 组件rule的触发方式，无触发方式的组件不生成rule
export const trigger: Record<string, string> = {
  'el-input': 'blur',
  'el-input-number': 'blur',
  'el-select': 'change',
  'el-radio-group': 'change',
  'el-checkbox-group': 'change',
  'el-cascader': 'change',
  'el-time-picker': 'change',
  'el-date-picker': 'change',
  'el-rate': 'change',
}
