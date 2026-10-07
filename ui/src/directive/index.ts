import type { App } from 'vue'
import hasRole from './permission/hasRole.ts'
import hasPermi from './permission/hasPermi.ts'
import copyText from './common/copyText.ts'

export default function directive(app: App){
  app.directive('hasRole', hasRole)
  app.directive('hasPermi', hasPermi)
  app.directive('copyText', copyText)
}
