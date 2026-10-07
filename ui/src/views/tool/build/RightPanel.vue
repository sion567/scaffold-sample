<template>
  <div class="right-board">
    <el-tabs v-model="currentTab" stretch class="center-tabs">
      <el-tab-pane label="组件属性" name="field" />
      <el-tab-pane label="表单属性" name="form" />
    </el-tabs>
    <div class="field-box">
      <a class="document-link" target="_blank" :href="documentLink" title="查看组件文档">
        <el-icon>
          <Link />
        </el-icon>
      </a>
      <el-scrollbar class="right-scrollbar">
        <!-- 组件属性 -->
        <el-form
v-show="currentTab === 'field' && showField" size="default" label-width="90px" label-position="top"
          style="">
          <el-form-item v-if="activeData.changeTag" label="组件类型">
            <el-select :model-value="activeData.tagIcon" @update:model-value="(v: unknown) => emit('update-field', 'tagIcon', v)" placeholder="请选择组件类型" :style="{ width: '100%' }" @change="tagChange">
              <el-option-group v-for="group in tagList" :key="group.label" :label="group.label">
                <el-option v-for="item in group.options" :key="item.label" :label="item.label" :value="item.tagIcon">
                  <svg-icon class="node-icon" :icon-class="item.tagIcon" style="margin-right: 10px;" />
                  <span> {{ item.label }}</span>
                </el-option>
              </el-option-group>
            </el-select>
          </el-form-item>
          <el-form-item v-if="activeData.vModel !== undefined" label="字段名">
            <el-input :model-value="activeData.vModel" @update:model-value="(v: unknown) => emit('update-field', 'vModel', v)" placeholder="请输入字段名（v-model）" />
          </el-form-item>
          <el-form-item v-if="activeData.componentName !== undefined" label="组件名">
            {{ activeData.componentName }}
          </el-form-item>
          <el-form-item v-if="activeData.label !== undefined" label="标题">
            <el-input :model-value="activeData.label" @update:model-value="(v: unknown) => emit('update-field', 'label', v)" placeholder="请输入标题" />
          </el-form-item>
          <el-form-item v-if="activeData.placeholder !== undefined" label="占位提示">
            <el-input :model-value="activeData.placeholder" @update:model-value="(v: unknown) => emit('update-field', 'placeholder', v)" placeholder="请输入占位提示" />
          </el-form-item>
          <el-form-item v-if="activeData['start-placeholder'] !== undefined" label="开始占位">
            <el-input :model-value="activeData['start-placeholder']" @update:model-value="(v: unknown) => emit('update-field', 'start-placeholder', v)" placeholder="请输入占位提示" />
          </el-form-item>
          <el-form-item v-if="activeData['end-placeholder'] !== undefined" label="结束占位">
            <el-input :model-value="activeData['end-placeholder']" @update:model-value="(v: unknown) => emit('update-field', 'end-placeholder', v)" placeholder="请输入占位提示" />
          </el-form-item>
          <el-form-item v-if="activeData.span !== undefined" label="表单栅格">
            <el-slider :model-value="activeData.span" @update:model-value="(v: unknown) => emit('update-field', 'span', v)" :max="24" :min="1" :marks="{ 12: '' }" @change="spanChange" />
          </el-form-item>
          <el-form-item v-if="activeData.layout === 'rowFormItem'" label="栅格间隔">
            <el-input-number :model-value="activeData.gutter" @update:model-value="(v: unknown) => emit('update-field', 'gutter', v)" :min="0" placeholder="栅格间隔" />
          </el-form-item>

          <el-form-item v-if="activeData.justify !== undefined" label="水平排列">
            <el-select :model-value="activeData.justify" @update:model-value="(v: unknown) => emit('update-field', 'justify', v)" placeholder="请选择水平排列" :style="{ width: '100%' }">
              <el-option v-for="(item, index) in justifyOptions" :key="index" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="activeData.align !== undefined" label="垂直排列">
            <el-radio-group :model-value="activeData.align" @update:model-value="(v: unknown) => emit('update-field', 'align', v)">
              <el-radio-button label="top" />
              <el-radio-button label="middle" />
              <el-radio-button label="bottom" />
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="activeData.labelWidth !== undefined" label="标签宽度">
            <el-input :model-value="activeData.labelWidth" @update:model-value="(v: unknown) => emit('update-field', 'labelWidth', Number(v))" type="number" placeholder="请输入标签宽度" />
          </el-form-item>
          <el-form-item v-if="activeData.style && activeData.style.width !== undefined" label="组件宽度">
            <el-input :model-value="activeData.style.width" @update:model-value="(v: unknown) => emit('update-field', 'style.width', v)" placeholder="请输入组件宽度" clearable />
          </el-form-item>
          <el-form-item v-if="activeData.vModel !== undefined" label="默认值">
            <el-input
:value="setDefaultValue(activeData.defaultValue)" placeholder="请输入默认值"
              @input="onDefaultValueInput" />
          </el-form-item>
          <el-form-item v-if="activeData.tag === 'el-checkbox-group'" label="至少应选">
            <el-input-number
:value="activeData.min" :min="0" placeholder="至少应选"
              @input="(v: number | undefined) => emit('update-field', 'min', v ? v : undefined)" />
          </el-form-item>
          <el-form-item v-if="activeData.tag === 'el-checkbox-group'" label="最多可选">
            <el-input-number
:value="activeData.max" :min="0" placeholder="最多可选"
              @input="(v: number | undefined) => emit('update-field', 'max', v ? v : undefined)" />
          </el-form-item>
          <el-form-item v-if="activeData.prepend !== undefined" label="前缀">
            <el-input :model-value="activeData.prepend" @update:model-value="(v: unknown) => emit('update-field', 'prepend', v)" placeholder="请输入前缀" />
          </el-form-item>
          <el-form-item v-if="activeData.append !== undefined" label="后缀">
            <el-input :model-value="activeData.append" @update:model-value="(v: unknown) => emit('update-field', 'append', v)" placeholder="请输入后缀" />
          </el-form-item>
          <el-form-item v-if="activeData['prefix-icon'] !== undefined" label="前图标">
            <el-input :model-value="activeData['prefix-icon']" @update:model-value="(v: unknown) => emit('update-field', 'prefix-icon', v)" placeholder="请输入前图标名称">
              <template #append>
                <el-button icon="Pointer" @click="openIconsDialog('prefix-icon')">
                  选择
                </el-button>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item v-if="activeData['suffix-icon'] !== undefined" label="后图标">
            <el-input :model-value="activeData['suffix-icon']" @update:model-value="(v: unknown) => emit('update-field', 'suffix-icon', v)" placeholder="请输入后图标名称">
              <template #append>
                <el-button icon="Pointer" @click="openIconsDialog('suffix-icon')">
                  选择
                </el-button>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item v-if="activeData.tag === 'el-cascader'" label="选项分隔符">
            <el-input :model-value="activeData.separator" @update:model-value="(v: unknown) => emit('update-field', 'separator', v)" placeholder="请输入选项分隔符" />
          </el-form-item>
          <el-form-item v-if="activeData.autosize !== undefined" label="最小行数">
            <el-input-number :model-value="activeData.autosize.minRows" @update:model-value="(v: unknown) => emit('update-field', 'autosize.minRows', v)" :min="1" placeholder="最小行数" />
          </el-form-item>
          <el-form-item v-if="activeData.autosize !== undefined" label="最大行数">
            <el-input-number :model-value="activeData.autosize.maxRows" @update:model-value="(v: unknown) => emit('update-field', 'autosize.maxRows', v)" :min="1" placeholder="最大行数" />
          </el-form-item>
          <el-form-item v-if="activeData.min !== undefined" label="最小值">
            <el-input-number :model-value="activeData.min" @update:model-value="(v: unknown) => emit('update-field', 'min', v)" placeholder="最小值" />
          </el-form-item>
          <el-form-item v-if="activeData.max !== undefined" label="最大值">
            <el-input-number :model-value="activeData.max" @update:model-value="(v: unknown) => emit('update-field', 'max', v)" placeholder="最大值" />
          </el-form-item>
          <el-form-item v-if="activeData.step !== undefined" label="步长">
            <el-input-number :model-value="activeData.step" @update:model-value="(v: unknown) => emit('update-field', 'step', v)" placeholder="步数" />
          </el-form-item>
          <el-form-item v-if="activeData.tag === 'el-input-number'" label="精度">
            <el-input-number :model-value="activeData.precision" @update:model-value="(v: unknown) => emit('update-field', 'precision', v)" :min="0" placeholder="精度" />
          </el-form-item>
          <el-form-item v-if="activeData.tag === 'el-input-number'" label="按钮位置">
            <el-radio-group :model-value="activeData['controls-position']" @update:model-value="(v: unknown) => emit('update-field', 'controls-position', v)">
              <el-radio-button label="">
                默认
              </el-radio-button>
              <el-radio-button label="right">
                右侧
              </el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="activeData.maxlength !== undefined" label="最多输入">
            <el-input :model-value="activeData.maxlength" @update:model-value="(v: unknown) => emit('update-field', 'maxlength', v)" placeholder="请输入字符长度">
              <template #append>
                个字符
              </template>
            </el-input>
          </el-form-item>
          <el-form-item v-if="activeData['active-text'] !== undefined" label="开启提示">
            <el-input :model-value="activeData['active-text']" @update:model-value="(v: unknown) => emit('update-field', 'active-text', v)" placeholder="请输入开启提示" />
          </el-form-item>
          <el-form-item v-if="activeData['inactive-text'] !== undefined" label="关闭提示">
            <el-input :model-value="activeData['inactive-text']" @update:model-value="(v: unknown) => emit('update-field', 'inactive-text', v)" placeholder="请输入关闭提示" />
          </el-form-item>
          <el-form-item v-if="activeData['active-value'] !== undefined" label="开启值">
            <el-input
:value="setDefaultValue(activeData['active-value'])" placeholder="请输入开启值"
              @input="onSwitchValueInput($event, 'active-value')" />
          </el-form-item>
          <el-form-item v-if="activeData['inactive-value'] !== undefined" label="关闭值">
            <el-input
:value="setDefaultValue(activeData['inactive-value'])" placeholder="请输入关闭值"
              @input="onSwitchValueInput($event, 'inactive-value')" />
          </el-form-item>
          <el-form-item v-if="activeData.type !== undefined && 'el-date-picker' === activeData.tag" label="时间类型">
            <el-select
:model-value="activeData.type" @update:model-value="(v: unknown) => emit('update-field', 'type', v)" placeholder="请选择时间类型" :style="{ width: '100%' }"
              @change="dateTypeChange">
              <el-option v-for="(item, index) in dateOptions" :key="index" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="activeData.name !== undefined" label="文件字段名">
            <el-input :model-value="activeData.name" @update:model-value="(v: unknown) => emit('update-field', 'name', v)" placeholder="请输入上传文件字段名" />
          </el-form-item>
          <el-form-item v-if="activeData.accept !== undefined" label="文件类型">
            <el-select :model-value="activeData.accept" @update:model-value="(v: unknown) => emit('update-field', 'accept', v)" placeholder="请选择文件类型" :style="{ width: '100%' }" clearable>
              <el-option label="图片" value="image/*" />
              <el-option label="视频" value="video/*" />
              <el-option label="音频" value="audio/*" />
              <el-option label="excel" value=".xls,.xlsx" />
              <el-option label="word" value=".doc,.docx" />
              <el-option label="pdf" value=".pdf" />
              <el-option label="txt" value=".txt" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="activeData.fileSize !== undefined" label="文件大小">
            <el-input :model-value="activeData.fileSize" @update:model-value="(v: unknown) => emit('update-field', 'fileSize', Number(v))" placeholder="请输入文件大小">
              <template #append>
<el-select  :model-value="activeData.sizeUnit" @update:model-value="(v: unknown) => emit('update-field', 'sizeUnit', v)" :style="{ width: '66px' }">
                <el-option label="KB" value="KB" />
                <el-option label="MB" value="MB" />
                <el-option label="GB" value="GB" />
              </el-select>
</template>
            </el-input>
          </el-form-item>
          <el-form-item v-if="activeData.action !== undefined" label="上传地址">
            <el-input :model-value="activeData.action" @update:model-value="(v: unknown) => emit('update-field', 'action', v)" placeholder="请输入上传地址" clearable />
          </el-form-item>
          <el-form-item v-if="activeData['list-type'] !== undefined" label="列表类型">
            <el-radio-group :model-value="activeData['list-type']" @update:model-value="(v: unknown) => emit('update-field', 'list-type', v)" size="small">
              <el-radio-button label="text">
                text
              </el-radio-button>
              <el-radio-button label="picture">
                picture
              </el-radio-button>
              <el-radio-button label="picture-card">
                picture-card
              </el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item
v-if="activeData.buttonText !== undefined" v-show="'picture-card' !== activeData['list-type']"
            label="按钮文字">
            <el-input :model-value="activeData.buttonText" @update:model-value="(v: unknown) => emit('update-field', 'buttonText', v)" placeholder="请输入按钮文字" />
          </el-form-item>
          <el-form-item v-if="activeData['range-separator'] !== undefined" label="分隔符">
            <el-input :model-value="activeData['range-separator']" @update:model-value="(v: unknown) => emit('update-field', 'range-separator', v)" placeholder="请输入分隔符" />
          </el-form-item>
          <el-form-item v-if="activeData['picker-options'] !== undefined" label="时间段">
            <el-input :model-value="activeData['picker-options']?.selectableRange" @update:model-value="(v: unknown) => emit('update-field', 'picker-options.selectableRange', v)" placeholder="请输入时间段" />
          </el-form-item>
          <el-form-item v-if="activeData.format !== undefined" label="时间格式">
            <el-input :value="activeData.format" placeholder="请输入时间格式" @input="setTimeValue($event)" />
          </el-form-item>
          <template v-if="['el-checkbox-group', 'el-radio-group', 'el-select'].indexOf(activeData.tag ?? '') > -1">
            <el-divider>选项</el-divider>
            <draggable
:list="activeData.options" :animation="340" group="selectItem" handle=".option-drag"
              item-key="label">
              <template #item="{ element, index }">
                <div :key="index" class="select-item">
                  <div class="select-line-icon option-drag">
                    <i class="el-icon-s-operation" />
                  </div>
                  <el-input v-model="element.label" placeholder="选项名" size="small" />
                  <el-input
placeholder="选项值" size="small" :value="element.value"
                    @input="setOptionValue(element, $event)" />
                  <div class="close-btn select-line-icon" @click="emit('update-field', 'options', activeData.options?.filter((_, i) => i !== index))">
                    <el-icon>
                      <Remove />
                    </el-icon>
                  </div>
                </div>
              </template>
            </draggable>
            <div>
              <el-button
icon="CirclePlus" style="margin-left: 8px; margin-top: 10px;" text bg type="primary"
                @click="addSelectItem">
                添加选项
              </el-button>
            </div>
            <el-divider />
          </template>

          <template v-if="['el-cascader'].indexOf(activeData.tag ?? '') > -1">
            <el-divider>选项</el-divider>
            <el-form-item label="数据类型">
              <el-radio-group :model-value="activeData.dataType" @update:model-value="(v: unknown) => emit('update-field', 'dataType', v)" size="small">
                <el-radio-button label="dynamic">
                  动态数据
                </el-radio-button>
                <el-radio-button label="static">
                  静态数据
                </el-radio-button>
              </el-radio-group>
            </el-form-item>

            <template v-if="activeData.dataType === 'dynamic'">
              <el-form-item label="标签键名">
                <el-input :model-value="activeData.labelKey" @update:model-value="(v: unknown) => emit('update-field', 'labelKey', v)" placeholder="请输入标签键名" />
              </el-form-item>
              <el-form-item label="值键名">
                <el-input :model-value="activeData.valueKey" @update:model-value="(v: unknown) => emit('update-field', 'valueKey', v)" placeholder="请输入值键名" />
              </el-form-item>
              <el-form-item label="子级键名">
                <el-input :model-value="activeData.childrenKey" @update:model-value="(v: unknown) => emit('update-field', 'childrenKey', v)" placeholder="请输入子级键名" />
              </el-form-item>
            </template>

            <el-tree
v-if="activeData.dataType === 'static'" draggable :data="activeData.options" node-key="id"
              :expand-on-click-node="false" :render-content="renderContent" />
            <div v-if="activeData.dataType === 'static'">
              <el-button
icon="CirclePlus" style="margin-left: 0; margin-top: 10px;" type="primary" text bg
                @click="addTreeItem">
                添加父级
              </el-button>
            </div>
            <el-divider />
          </template>

          <el-form-item v-if="activeData.optionType !== undefined" label="选项样式">
            <el-radio-group :model-value="activeData.optionType" @update:model-value="(v: unknown) => emit('update-field', 'optionType', v)">
              <el-radio-button label="default">
                默认
              </el-radio-button>
              <el-radio-button label="button">
                按钮
              </el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="activeData['active-color'] !== undefined" label="开启颜色">
            <el-color-picker :model-value="activeData['active-color']" @update:model-value="(v: unknown) => emit('update-field', 'active-color', v)" />
          </el-form-item>
          <el-form-item v-if="activeData['inactive-color'] !== undefined" label="关闭颜色">
            <el-color-picker :model-value="activeData['inactive-color']" @update:model-value="(v: unknown) => emit('update-field', 'inactive-color', v)" />
          </el-form-item>

          <el-form-item v-if="activeData['allow-half'] !== undefined" label="允许半选">
            <el-switch :model-value="activeData['allow-half']" @update:model-value="(v: unknown) => emit('update-field', 'allow-half', v)" />
          </el-form-item>
          <el-form-item v-if="activeData['show-text'] !== undefined" label="辅助文字">
            <el-switch :model-value="activeData['show-text']" @update:model-value="(v: unknown) => emit('update-field', 'show-text', v)" @change="rateTextChange" />
          </el-form-item>
          <el-form-item v-if="activeData['show-score'] !== undefined" label="显示分数">
            <el-switch :model-value="activeData['show-score']" @update:model-value="(v: unknown) => emit('update-field', 'show-score', v)" @change="rateScoreChange" />
          </el-form-item>
          <el-form-item v-if="activeData['show-stops'] !== undefined" label="显示间断点">
            <el-switch :model-value="activeData['show-stops']" @update:model-value="(v: unknown) => emit('update-field', 'show-stops', v)" />
          </el-form-item>
          <el-form-item v-if="activeData.range !== undefined" label="范围选择">
            <el-switch :model-value="activeData.range" @update:model-value="(v: unknown) => emit('update-field', 'range', v)" @change="rangeChange" />
          </el-form-item>
          <el-form-item v-if="activeData.border !== undefined && activeData.optionType === 'default'" label="是否带边框">
            <el-switch :model-value="activeData.border" @update:model-value="(v: unknown) => emit('update-field', 'border', v)" />
          </el-form-item>
          <el-form-item v-if="activeData.tag === 'el-color-picker'" label="颜色格式">
            <el-select
:model-value="activeData['color-format']" @update:model-value="(v: unknown) => emit('update-field', 'color-format', v)" placeholder="请选择颜色格式" :style="{ width: '100%' }"
              @change="colorFormatChange">
              <el-option
v-for="(item, index) in colorFormatOptions" :key="index" :label="item.label"
                :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item
v-if="activeData.size !== undefined &&
            (activeData.optionType === 'button' ||
              activeData.border ||
              activeData.tag === 'el-color-picker')" label="选项尺寸">
            <el-radio-group :model-value="activeData.size" @update:model-value="(v: unknown) => emit('update-field', 'size', v)">
              <el-radio-button label="large">
                较大
              </el-radio-button>
              <el-radio-button label="default">
                默认
              </el-radio-button>
              <el-radio-button label="small">
                较小
              </el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="activeData['show-word-limit'] !== undefined" label="输入统计">
            <el-switch :model-value="activeData['show-word-limit']" @update:model-value="(v: unknown) => emit('update-field', 'show-word-limit', v)" />
          </el-form-item>
          <el-form-item v-if="activeData.tag === 'el-input-number'" label="严格步数">
            <el-switch :model-value="activeData['step-strictly']" @update:model-value="(v: unknown) => emit('update-field', 'step-strictly', v)" />
          </el-form-item>
          <el-form-item v-if="activeData.tag === 'el-cascader'" label="是否多选">
            <el-switch :model-value="activeData.props?.props?.multiple" @update:model-value="(v: unknown) => emit('update-field', 'props.props.multiple', v)" />
          </el-form-item>
          <el-form-item v-if="activeData.tag === 'el-cascader'" label="展示全路径">
            <el-switch :model-value="activeData['show-all-levels']" @update:model-value="(v: unknown) => emit('update-field', 'show-all-levels', v)" />
          </el-form-item>
          <el-form-item v-if="activeData.tag === 'el-cascader'" label="可否筛选">
            <el-switch :model-value="activeData.filterable" @update:model-value="(v: unknown) => emit('update-field', 'filterable', v)" />
          </el-form-item>
          <el-form-item v-if="activeData.clearable !== undefined" label="能否清空">
            <el-switch :model-value="activeData.clearable" @update:model-value="(v: unknown) => emit('update-field', 'clearable', v)" />
          </el-form-item>
          <el-form-item v-if="activeData.showTip !== undefined" label="显示提示">
            <el-switch :model-value="activeData.showTip" @update:model-value="(v: unknown) => emit('update-field', 'showTip', v)" />
          </el-form-item>
          <el-form-item v-if="activeData.multiple !== undefined" label="多选文件">
            <el-switch :model-value="activeData.multiple" @update:model-value="(v: unknown) => emit('update-field', 'multiple', v)" />
          </el-form-item>
          <el-form-item v-if="activeData['auto-upload'] !== undefined" label="自动上传">
            <el-switch :model-value="activeData['auto-upload']" @update:model-value="(v: unknown) => emit('update-field', 'auto-upload', v)" />
          </el-form-item>
          <el-form-item v-if="activeData.readonly !== undefined" label="是否只读">
            <el-switch :model-value="activeData.readonly" @update:model-value="(v: unknown) => emit('update-field', 'readonly', v)" />
          </el-form-item>
          <el-form-item v-if="activeData.disabled !== undefined" label="是否禁用">
            <el-switch :model-value="activeData.disabled" @update:model-value="(v: unknown) => emit('update-field', 'disabled', v)" />
          </el-form-item>
          <el-form-item v-if="activeData.tag === 'el-select'" label="是否可搜索">
            <el-switch :model-value="activeData.filterable" @update:model-value="(v: unknown) => emit('update-field', 'filterable', v)" />
          </el-form-item>
          <el-form-item v-if="activeData.tag === 'el-select'" label="是否多选">
            <el-switch :model-value="activeData.multiple" @update:model-value="(v: unknown) => emit('update-field', 'multiple', v)" @change="multipleChange" />
          </el-form-item>
          <el-form-item v-if="activeData.required !== undefined" label="是否必填">
            <el-switch :model-value="activeData.required" @update:model-value="(v: unknown) => emit('update-field', 'required', v)" />
          </el-form-item>

          <template v-if="activeData.layoutTree">
            <el-divider>布局结构树</el-divider>
            <el-tree :data="[activeData]" :props="layoutTreeProps" node-key="renderKey" default-expand-all draggable>
              <template #default="{ node: treeNode, data: treeNodeData }">
                <span class="node-label">
                  <svg-icon class="node-icon" :icon-class="treeNodeData.tagIcon" style="margin-right: 5px;" />
                  {{ treeNode.label }}
                </span>
              </template>
            </el-tree>
          </template>

          <template v-if="activeData.layout === 'colFormItem'">
            <el-divider>正则校验</el-divider>
            <div v-for="(item, index) in activeData.regList" :key="index" class="reg-item">
              <span class="close-btn" @click="emit('update-field', 'regList', activeData.regList?.filter((_, i) => i !== index))">
                <el-icon>
                  <Close />
                </el-icon>
              </span>
              <el-form-item label="表达式">
                <el-input v-model="item.pattern" placeholder="请输入正则" />
              </el-form-item>
              <el-form-item label="错误提示" style="margin-bottom:0">
                <el-input v-model="item.message" placeholder="请输入错误提示" />
              </el-form-item>
            </div>
            <div>
              <el-button
icon="CirclePlus" style="margin-left: 0; margin-top: 10px;" type="primary" text bg
                @click="addReg">
                添加规则
              </el-button>
            </div>
          </template>
        </el-form>
        <!-- 表单属性 -->
        <el-form v-show="currentTab === 'form'" label-width="90px" label-position="top">
          <el-form-item label="表单名">
            <el-input :model-value="formConf.formRef" @update:model-value="(v: unknown) => emit('update-conf', 'formRef', v)" placeholder="请输入表单名（ref）" />
          </el-form-item>
          <el-form-item label="表单模型">
            <el-input :model-value="formConf.formModel" @update:model-value="(v: unknown) => emit('update-conf', 'formModel', v)" placeholder="请输入数据模型" />
          </el-form-item>
          <el-form-item label="校验模型">
            <el-input :model-value="formConf.formRules" @update:model-value="(v: unknown) => emit('update-conf', 'formRules', v)" placeholder="请输入校验模型" />
          </el-form-item>
          <el-form-item label="表单尺寸">
            <el-radio-group :model-value="formConf.size" @update:model-value="(v: unknown) => emit('update-conf', 'size', v)">
              <el-radio-button label="large" value="较大" />
              <el-radio-button label="default" value="默认" />
              <el-radio-button label="small" value="较小" />
            </el-radio-group>
          </el-form-item>
          <el-form-item label="标签对齐">
            <el-radio-group :model-value="formConf.labelPosition" @update:model-value="(v: unknown) => emit('update-conf', 'labelPosition', v)">
              <el-radio-button label="left" value="左对齐" />
              <el-radio-button label="right" value="右对齐" />
              <el-radio-button label="top" value="顶部对齐" />
            </el-radio-group>
          </el-form-item>
          <el-form-item label="标签宽度">
            <el-input-number :model-value="formConf.labelWidth" @update:model-value="(v: unknown) => emit('update-conf', 'labelWidth', v)" placeholder="标签宽度" />
          </el-form-item>
          <el-form-item label="栅格间隔">
            <el-input-number :model-value="formConf.gutter" @update:model-value="(v: unknown) => emit('update-conf', 'gutter', v)" :min="0" placeholder="栅格间隔" />
          </el-form-item>
          <el-form-item label="禁用表单">
            <el-switch :model-value="formConf.disabled" @update:model-value="(v: unknown) => emit('update-conf', 'disabled', v)" />
          </el-form-item>
          <el-form-item label="表单按钮">
            <el-switch :model-value="formConf.formBtns" @update:model-value="(v: unknown) => emit('update-conf', 'formBtns', v)" />
          </el-form-item>
          <el-form-item label="显示未选中组件边框">
            <el-switch :model-value="formConf.unFocusedComponentBorder" @update:model-value="(v: unknown) => emit('update-conf', 'unFocusedComponentBorder', v)" />
          </el-form-item>
        </el-form>
      </el-scrollbar>
    </div>
    <icons-dialog v-model="iconsVisible" @select="setIcon" />
    <treeNode-dialog v-model="dialogVisible" @commit="addNode" />

  </div>
</template>

<script setup lang="ts">
import draggable from "vuedraggable/dist/vuedraggable.common"
import { isNumberStr } from '@/utils/index'
import IconsDialog from './IconsDialog.vue'
import TreeNodeDialog from './TreeNodeDialog.vue'
import { inputComponents, selectComponents } from '@/utils/generator/config'
import type { ComponentPublicInstance } from 'vue'
import type { VNode } from 'vue'
import type { ElementConfig, FormConf } from '@/utils/generator/config'

interface Props {
  /** 是否显示字段面板 */
  showField?: boolean
  /** 当前选中的组件配置 */
  activeData: ElementConfig
  /** 表单全局配置 */
  formConf: FormConf
}

const props = withDefaults(defineProps<Props>(), { showField: false })

const { proxy } = getCurrentInstance() as { proxy: ComponentPublicInstance }

/** 通用下拉选项 */
interface LabelValueOption {
  label: string
  value: string
}

/** 选项树节点（activeData.options 与新增选项的统一形状） */
type TreeNodeOption = {
  id?: number
  label?: string | number
  value?: string | number
  disabled?: boolean
  componentName?: string
  vModel?: string
  children?: TreeNodeOption[]
}

const dateTimeFormat: Record<string, string> = {
  date: 'YYYY-MM-DD',
  week: 'YYYY 第 ww 周',
  month: 'YYYY-MM',
  year: 'YYYY',
  datetime: 'YYYY-MM-DD HH:mm:ss',
  daterange: 'YYYY-MM-DD',
  monthrange: 'YYYY-MM',
  datetimerange: 'YYYY-MM-DD HH:mm:ss'
}

const data = reactive({
  currentTab: 'field',
  currentNode: null as TreeNodeOption[] | null,
  dialogVisible: false,
  iconsVisible: false,
  currentIconModel: null as string | null,
  dateTypeOptions: [
    {
      label: '日(date)',
      value: 'date'
    },
    {
      label: '周(week)',
      value: 'week'
    },
    {
      label: '月(month)',
      value: 'month'
    },
    {
      label: '年(year)',
      value: 'year'
    },
    {
      label: '日期时间(datetime)',
      value: 'datetime'
    }
  ] as LabelValueOption[],
  dateRangeTypeOptions: [
    {
      label: '日期范围(daterange)',
      value: 'daterange'
    },
    {
      label: '月范围(monthrange)',
      value: 'monthrange'
    },
    {
      label: '日期时间范围(datetimerange)',
      value: 'datetimerange'
    }
  ] as LabelValueOption[],
  colorFormatOptions: [
    {
      label: 'hex',
      value: 'hex'
    },
    {
      label: 'rgb',
      value: 'rgb'
    },
    {
      label: 'rgba',
      value: 'rgba'
    },
    {
      label: 'hsv',
      value: 'hsv'
    },
    {
      label: 'hsl',
      value: 'hsl'
    }
  ] as LabelValueOption[],
  justifyOptions: [
    {
      label: 'start',
      value: 'start'
    },
    {
      label: 'end',
      value: 'end'
    },
    {
      label: 'center',
      value: 'center'
    },
    {
      label: 'space-around',
      value: 'space-around'
    },
    {
      label: 'space-between',
      value: 'space-between'
    }
  ] as LabelValueOption[],
  layoutTreeProps: {
    label(data: TreeNodeOption, node: unknown): string {
      void node
      return data.componentName || `${data.label}: ${data.vModel}`
    }
  }
})

const { currentTab, currentNode, dialogVisible, iconsVisible, currentIconModel, dateTypeOptions, dateRangeTypeOptions, colorFormatOptions, justifyOptions, layoutTreeProps } = toRefs(data)

const documentLink = computed(() => props.activeData.document || 'https://element-plus.org/zh-CN/guide/installation')

const dateOptions = computed<LabelValueOption[]>(() => {
  if (props.activeData.type !== undefined && props.activeData.tag === 'el-date-picker') {
    if (props.activeData['start-placeholder'] === undefined) {
      return dateTypeOptions.value
    }
    return dateRangeTypeOptions.value
  }
  return []
})

/** 组件面板分组 */
interface TagGroup {
  label: string
  options: ElementConfig[]
}

const tagList = ref<TagGroup[]>([
  {
    label: '输入型组件',
    options: inputComponents
  },
  {
    label: '选择型组件',
    options: selectComponents
  }
])

const emit = defineEmits<{
  (e: 'tag-change', tag: ElementConfig): void
  (e: 'update-field', key: string, value: unknown): void
  (e: 'update-conf', key: string, value: unknown): void
}>()

function addReg() {
  emit('update-field', 'regList', [...(props.activeData.regList ?? []), {
    pattern: '',
    message: ''
  }])
}
function addSelectItem() {
  emit('update-field', 'options', [...(props.activeData.options ?? []), {
    label: '',
    value: ''
  }])
}

function addTreeItem() {
  // 保留原实现：自增挂在实例上的 idGlobal（历史遗留，RightPanel 内无实际消费者）
  const host = proxy as ComponentPublicInstance & { idGlobal?: number }
  host.idGlobal = (host.idGlobal ?? 0) + 1
  dialogVisible.value = true
  currentNode.value = props.activeData.options ?? []
}

/** el-tree render-content 上下文（结构对齐 element-plus RenderContentContext，避免依赖其内部类型） */
interface TreeRenderContext {
  node: { label?: string; parent?: { data: TreeNodeOption } }
  data: TreeNodeOption
}

function renderContent(h: typeof import('vue')['h'], { node, data }: TreeRenderContext): VNode {
  return h('div', {
    class: "custom-tree-node"
  }, [
    h('span', node.label),
    h('span', {
      class: "node-operation"
    }, [
      h(resolveComponent('el-link'), {
        type: "primary",
        icon: "Plus",
        underline: false,
        onClick: () => {
          append(data)

        }
      }),
      h(resolveComponent('el-link'), {
        type: "danger",
        icon: "Delete",
        underline: false,
        style: "margin-left: 5px;",
        onClick: () => {
          remove(node, data)
        }
      })
    ])
  ])
}
function append(data: TreeNodeOption) {
  if (!data.children) {
    data.children = []
  }
  dialogVisible.value = true
  currentNode.value = data.children
}
function remove(node: TreeRenderContext['node'], data: TreeNodeOption) {
  const parent = node.parent
  if (!parent) return
  const children = (parent.data.children || parent.data) as TreeNodeOption[]
  const index = children.findIndex(d => d.id === data.id)
  children.splice(index, 1)
}
function addNode(data: TreeNodeOption) {
  currentNode.value?.push(data)
}

function setOptionValue(item: { value?: string | number }, val: string) {
  item.value = isNumberStr(val) ? +val : val
}
function setDefaultValue(val: unknown): string | number | null | undefined {
  if (Array.isArray(val)) {
    return val.join(',')
  }
  if (val === 'string' || val === 'number') {
    return val
  }
  if (typeof val === 'boolean') {
    return `${val}`
  }
  return val as string | number | null | undefined
}

function onDefaultValueInput(str: string) {
  if (Array.isArray(props.activeData.defaultValue)) {
    // 数组
    emit('update-field', 'defaultValue', str.split(',').map((val: string) => (isNumberStr(val) ? +val : val)))
  } else if (['true', 'false'].indexOf(str) > -1) {
    // 布尔
    emit('update-field', 'defaultValue', JSON.parse(str))
  } else {
    // 字符串和数字
    emit('update-field', 'defaultValue', isNumberStr(str) ? +str : str)
  }
}

function onSwitchValueInput(val: string, name: string) {
  if (['true', 'false'].indexOf(val) > -1) {
    emit('update-field', name, JSON.parse(val))
  } else {
    emit('update-field', name, isNumberStr(val) ? +val : val)
  }
}

function setTimeValue(val: string, type?: string) {
  const valueFormat = type === 'week' ? dateTimeFormat.date : val
  emit('update-field', 'defaultValue', null)
  emit('update-field', 'value-format', valueFormat)
  emit('update-field', 'format', val)
}

function spanChange(val: number) {
  emit('update-conf', 'span', val)
}

function multipleChange(val: boolean) {
  emit('update-field', 'defaultValue', val ? [] : '')
}

function dateTypeChange(val: string) {
  setTimeValue(dateTimeFormat[val], val)
}

function rangeChange(val: boolean) {
  emit('update-field', 'defaultValue', val ? [props.activeData.min, props.activeData.max] : props.activeData.min)
}

function rateTextChange(val: boolean) {
  if (val) emit('update-field', 'show-score', false)
}

function rateScoreChange(val: boolean) {
  if (val) emit('update-field', 'show-text', false)
}

function colorFormatChange(val: string) {
  emit('update-field', 'defaultValue', null)
  emit('update-field', 'show-alpha', val.indexOf('a') > -1)
  emit('update-field', 'renderKey', +new Date()) // 更新renderKey,重新渲染该组件
}

function openIconsDialog(model: string) {
  iconsVisible.value = true
  currentIconModel.value = model
}

function setIcon(val: string) {
  if (currentIconModel.value) {
    emit('update-field', currentIconModel.value, val)
  }
}

function tagChange(tagIcon: string) {
  const target = inputComponents.find(item => item.tagIcon === tagIcon) ?? selectComponents.find(item => item.tagIcon === tagIcon)
  if (target) {
    emit('tag-change', target)
  }
}
</script>

<style lang="scss" scoped>
.right-board {
  width: 350px;
  position: absolute;
  right: 0;
  top: 0;
  padding-top: 3px;

  &:deep() {
    .el-tabs__header {
      margin: 0;
    }

    .el-input-group__append .el-button {
      display: inline-flex;
    }
  }

  .field-box {
    position: relative;
    height: calc(100vh - 50px - 40px - 42px);
    box-sizing: border-box;
    overflow: hidden;
  }

  .el-scrollbar {
    height: 100%;

    &:deep() {
      .el-scrollbar__view {
        padding: 30px 20px;
      }

    }
  }
}

.reg-item {
  padding: 12px 6px;
  background: var(--el-border-color-extra-light);
  position: relative;
  border-radius: 4px;

  .close-btn {
    position: absolute;
    right: -6px;
    top: -6px;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 16px;
    height: 16px;
    line-height: 16px;
    background: rgba(0, 0, 0, .2);
    border-radius: 50%;
    color: #fff;
    z-index: 1;
    cursor: pointer;
    font-size: 12px;
  }
}

.select-item {
  display: flex;
  border: 1px dashed #fff;
  box-sizing: border-box;

  & .close-btn {
    cursor: pointer;
    color: #f56c6c;
  }

  & .el-input+.el-input {
    margin-left: 4px;
  }
}

.select-item+.select-item {
  margin-top: 4px;
}

.select-item.sortable-chosen {
  border: 1px dashed #409eff;
}

.select-line-icon {
  line-height: 32px;
  font-size: 22px;
  padding: 0 4px;
  color: #777;
}

.option-drag {
  cursor: move;
}

.time-range {
  .el-date-editor {
    width: 227px;
  }

  :deep() {
    .el-icon-time {
      display: none;
    }
  }
}

.document-link {
  position: absolute;
  display: flex;
  width: 26px;
  height: 26px;
  top: 0;
  left: 0;
  cursor: pointer;
  background: #409eff;
  z-index: 1;
  border-radius: 0 0 6px 0;
  justify-content: center;
  align-items: center;
  color: #fff;
  font-size: 18px;
}

.node-label {
  font-size: 14px;
}

.node-icon {
  color: #bebfc3;
}

.custom-tree-node {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  padding-right: 8px;
}
</style>