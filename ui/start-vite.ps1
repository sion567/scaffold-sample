$ui = 'E:\workspace\scaffold' + [char]0x200C + '-sample\ui'
Start-Process -FilePath 'D:\tools\fnm\node-versions\v22.22.2\installation\node.exe' `
  -ArgumentList 'node_modules/vite/bin/vite.js' `
  -WorkingDirectory $ui `
  -RedirectStandardOutput 'E:\tmp\log-ui.txt' `
  -RedirectStandardError 'E:\tmp\log-ui-err.txt' `
  -WindowStyle Hidden
Write-Output 'vite launched'
