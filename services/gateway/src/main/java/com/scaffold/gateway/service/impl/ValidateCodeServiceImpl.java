package com.scaffold.gateway.service.impl;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.Resource;
import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.FastByteArrayOutputStream;
import com.google.code.kaptcha.Producer;
import com.scaffold.common.core.constant.CacheConstants;
import com.scaffold.common.core.constant.Constants;
import com.scaffold.common.core.exception.CaptchaException;
import com.scaffold.common.core.utils.sign.Base64;
import com.scaffold.common.core.utils.uuid.IdUtils;
import com.scaffold.common.core.web.domain.AjaxResult;
import com.scaffold.common.redis.service.RedisService;
import com.scaffold.gateway.config.properties.CaptchaProperties;
import com.scaffold.gateway.service.ValidateCodeService;

/**
 * 验证码实现处理
 *
 * @author scaffold
 */
@Service
public class ValidateCodeServiceImpl implements ValidateCodeService
{
    private final Producer captchaProducer;
    private final Producer captchaProducerMath;
    private final RedisService redisService;
    private final CaptchaProperties captchaProperties;

    public ValidateCodeServiceImpl(@Qualifier("captchaProducer") Producer captchaProducer,
                                   @Qualifier("captchaProducerMath") Producer captchaProducerMath,
                                   RedisService redisService,
                                   CaptchaProperties captchaProperties)
    {
        this.captchaProducer = captchaProducer;
        this.captchaProducerMath = captchaProducerMath;
        this.redisService = redisService;
        this.captchaProperties = captchaProperties;
    }

    /**
     * 生成验证码
     */
    @Override
    public AjaxResult createCaptcha() throws IOException, CaptchaException
    {
        AjaxResult ajax = AjaxResult.success();
        boolean captchaEnabled = captchaProperties.getEnabled();
        ajax.put("captchaEnabled", captchaEnabled);
        if (!captchaEnabled)
        {
            return ajax;
        }

        // 保存验证码信息
        String uuid = IdUtils.simpleUUID();
        String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + uuid;

        String capStr = null, code = null;
        BufferedImage image = null;

        String captchaType = captchaProperties.getType();
        // 生成验证码
        if ("math".equals(captchaType))
        {
            String capText = captchaProducerMath.createText();
            capStr = capText.substring(0, capText.lastIndexOf("@"));
            code = capText.substring(capText.lastIndexOf("@") + 1);
            image = captchaProducerMath.createImage(capStr);
        }
        else if ("char".equals(captchaType))
        {
            capStr = code = captchaProducer.createText();
            image = captchaProducer.createImage(capStr);
        }

        redisService.setCacheObject(verifyKey, code, Constants.CAPTCHA_EXPIRATION, TimeUnit.MINUTES);
        // 转换流信息写出
        FastByteArrayOutputStream os = new FastByteArrayOutputStream();
        try
        {
            ImageIO.write(image, "jpg", os);
        }
        catch (IOException e)
        {
            return AjaxResult.error(e.getMessage());
        }

        ajax.put("uuid", uuid);
        ajax.put("img", Base64.encode(os.toByteArray()));
        return ajax;
    }
}
