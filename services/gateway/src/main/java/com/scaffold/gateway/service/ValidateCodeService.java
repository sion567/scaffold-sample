package com.scaffold.gateway.service;

import java.io.IOException;
import com.scaffold.common.core.exception.CaptchaException;
import com.scaffold.common.core.web.domain.AjaxResult;

/**
 * 验证码处理
 *
 * @author scaffold
 */
public interface ValidateCodeService
{
    /**
     * 生成验证码（校验由 auth 服务在国密信封解密后执行，见 SysLoginService#validateCaptcha）
     */
    public AjaxResult createCaptcha() throws IOException, CaptchaException;
}
