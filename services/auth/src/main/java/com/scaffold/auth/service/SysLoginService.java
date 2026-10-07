package com.scaffold.auth.service;

import com.scaffold.system.api.proto.BoolResponse;
import com.scaffold.system.api.proto.GetUserInfoRequest;
import com.scaffold.system.api.proto.GetUserInfoResponse;
import com.scaffold.system.api.proto.RecordUserLoginRequest;
import com.scaffold.system.api.proto.RegisterUserInfoRequest;
import com.scaffold.system.api.proto.RemoteUserService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;
import com.scaffold.auth.config.CaptchaProperties;
import com.scaffold.common.core.constant.CacheConstants;
import com.scaffold.common.core.constant.Constants;
import com.scaffold.common.core.constant.SecurityConstants;
import com.scaffold.common.core.constant.UserConstants;
import com.scaffold.common.core.domain.R;
import com.scaffold.common.core.enums.UserStatus;
import com.scaffold.common.core.exception.ServiceException;
import com.scaffold.common.core.text.Convert;
import com.scaffold.common.core.utils.DateUtils;
import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.common.core.utils.ip.IpUtils;
import com.scaffold.common.redis.service.RedisService;
import com.scaffold.common.security.utils.SecurityUtils;
import com.scaffold.system.api.convert.LoginUserConvert;
import com.scaffold.system.api.convert.SysUserConvert;
import com.scaffold.system.api.domain.SysUser;
import com.scaffold.system.api.model.LoginUser;

/**
 * 登录校验方法
 * 
 * @author scaffold
 */
@Component
public class SysLoginService
{
    @DubboReference(check = false)
    private RemoteUserService remoteUserService;
    private final SysPasswordService passwordService;
    private final SysRecordLogService recordLogService;
    private final RedisService redisService;
    private final TwoFactorService twoFactorService;
    private final CaptchaProperties captchaProperties;

    public SysLoginService(SysPasswordService passwordService,
                           SysRecordLogService recordLogService, RedisService redisService, TwoFactorService twoFactorService,
                           CaptchaProperties captchaProperties)
    {
        this.passwordService = passwordService;
        this.recordLogService = recordLogService;
        this.redisService = redisService;
        this.twoFactorService = twoFactorService;
        this.captchaProperties = captchaProperties;
    }

    /**
     * 登录（双因子可选）
     */
    public LoginUser login(String username, String password)
    {
        return login(username, password, null, null, null);
    }

    /**
     * 登录（双因子开启时校验 secondAuthCode，命中范围见 TwoFactorProperties；
     * 图形验证码开启时校验 code/uuid——请求经国密信封解密后到达此处）
     */
    public LoginUser login(String username, String password, String secondAuthCode, String code, String uuid)
    {
        // 图形验证码校验（在密码校验之前，防暴力破解）
        validateCaptcha(username, code, uuid);
        // 用户名或密码为空 错误
        if (StringUtils.isAnyBlank(username, password))
        {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "用户/密码必须填写");
            throw new ServiceException("用户/密码必须填写");
        }
        // 密码如果不在指定范围内 错误
        if (password.length() < UserConstants.PASSWORD_MIN_LENGTH
                || password.length() > UserConstants.PASSWORD_MAX_LENGTH)
        {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "用户密码不在指定范围");
            throw new ServiceException("用户密码不在指定范围");
        }
        // 用户名不在指定范围内 错误
        if (username.length() < UserConstants.USERNAME_MIN_LENGTH
                || username.length() > UserConstants.USERNAME_MAX_LENGTH)
        {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "用户名不在指定范围");
            throw new ServiceException("用户名不在指定范围");
        }
        // IP黑名单校验
        String blackStr = Convert.toStr(redisService.getCacheObject(CacheConstants.SYS_LOGIN_BLACKIPLIST));
        if (IpUtils.isMatchedIp(blackStr, IpUtils.getIpAddr()))
        {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "很遗憾，访问IP已被列入系统黑名单");
            throw new ServiceException("很遗憾，访问IP已被列入系统黑名单");
        }
        // 查询用户信息
        GetUserInfoResponse userResult = remoteUserService.getUserInfo(
                GetUserInfoRequest.newBuilder()
                        .setUsername(username)
                        .setSource(SecurityConstants.INNER)
                        .build());

        if (R.FAIL == userResult.getCode())
        {
            throw new ServiceException(userResult.getMsg());
        }

        LoginUser userInfo = LoginUserConvert.toJava(userResult.getData());
        SysUser user = userInfo.getSysUser();
        if (UserStatus.DELETED.getCode().equals(user.getDelFlag()))
        {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "对不起，您的账号已被删除");
            throw new ServiceException("对不起，您的账号：" + username + " 已被删除");
        }
        if (UserStatus.DISABLE.getCode().equals(user.getStatus()))
        {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "用户已停用，请联系管理员");
            throw new ServiceException("对不起，您的账号：" + username + " 已停用");
        }
        passwordService.validate(user, password);
        // 双因子认证（配置驱动的合规增强，短信/OTP）
        if (twoFactorService.isRequired(userInfo))
        {
            if (!twoFactorService.verifyOtp(username, secondAuthCode))
            {
                recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "双因子验证失败");
                throw new ServiceException("双因子验证失败，请先获取短信验证码");
            }
        }
        recordLogService.recordLogininfor(username, Constants.LOGIN_SUCCESS, "登录成功");
        recordLoginInfo(user.getUserId());
        return userInfo;
    }

    /**
     * 图形验证码校验（图片由网关 GET /captchaImage 生成并存 Redis，两服务共享；
     * scaffold.auth.captcha.enabled=false 时跳过校验）。验证码一次性使用，取出即删。
     */
    private void validateCaptcha(String username, String code, String uuid)
    {
        if (!captchaProperties.isEnabled())
        {
            return;
        }
        if (StringUtils.isEmpty(code))
        {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "验证码不能为空");
            throw new ServiceException("验证码不能为空");
        }
        String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + StringUtils.nvl(uuid, "");
        String captcha = redisService.getCacheObject(verifyKey);
        redisService.deleteObject(verifyKey);
        if (captcha == null)
        {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "验证码已失效");
            throw new ServiceException("验证码已失效");
        }
        if (!code.equalsIgnoreCase(captcha))
        {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "验证码错误");
            throw new ServiceException("验证码错误");
        }
    }

    /**
     * 记录登录信息
     *
     * @param userId 用户ID
     */
    public void recordLoginInfo(Long userId)
    {
        SysUser sysUser = new SysUser();
        sysUser.setUserId(userId);
        // 更新用户登录IP
        sysUser.setLoginIp(IpUtils.getIpAddr());
        // 更新用户登录时间
        sysUser.setLoginDate(DateUtils.getNowDate());
        remoteUserService.recordUserLogin(RecordUserLoginRequest.newBuilder()
                .setUser(SysUserConvert.toProto(sysUser))
                .setSource(SecurityConstants.INNER)
                .build());
    }

    /**
     * 退出
     */
    public void logout(String loginName)
    {
        recordLogService.recordLogininfor(loginName, Constants.LOGOUT, "退出成功");
    }

    /**
     * 解锁
     */
    public void unlock(String password)
    {
        String username = SecurityUtils.getUsername();
        // 或密码为空 错误
        if (StringUtils.isEmpty(password))
        {
            throw new ServiceException("密码不能为空");
        }
        // 查询用户信息
        GetUserInfoResponse userResult = remoteUserService.getUserInfo(
                GetUserInfoRequest.newBuilder()
                        .setUsername(username)
                        .setSource(SecurityConstants.INNER)
                        .build());

        if (R.FAIL == userResult.getCode())
        {
            throw new ServiceException(userResult.getMsg());
        }

        SysUser user = LoginUserConvert.toJava(userResult.getData()).getSysUser();
        if (!SecurityUtils.matchesPassword(password, user.getPassword()))
        {
            throw new ServiceException("密码错误，请重新输入");
        }
    }

    /**
     * 注册（图形验证码开启时校验 code/uuid）
     */
    public void register(String username, String password, String code, String uuid)
    {
        // 图形验证码校验
        validateCaptcha(username, code, uuid);
        // 用户名或密码为空 错误
        if (StringUtils.isAnyBlank(username, password))
        {
            throw new ServiceException("用户/密码必须填写");
        }
        if (username.length() < UserConstants.USERNAME_MIN_LENGTH
                || username.length() > UserConstants.USERNAME_MAX_LENGTH)
        {
            throw new ServiceException("账户长度必须在2到20个字符之间");
        }
        if (password.length() < UserConstants.PASSWORD_MIN_LENGTH
                || password.length() > UserConstants.PASSWORD_MAX_LENGTH)
        {
            throw new ServiceException("密码长度必须在5到20个字符之间");
        }

        // 注册用户信息
        SysUser sysUser = new SysUser();
        sysUser.setUserName(username);
        sysUser.setNickName(username);
        sysUser.setPwdUpdateDate(DateUtils.getNowDate());
        sysUser.setPassword(SecurityUtils.encryptPassword(password));
        BoolResponse registerResult = remoteUserService.registerUserInfo(
                RegisterUserInfoRequest.newBuilder()
                        .setUser(SysUserConvert.toProto(sysUser))
                        .setSource(SecurityConstants.INNER)
                        .build());

        if (R.FAIL == registerResult.getCode())
        {
            throw new ServiceException(registerResult.getMsg());
        }
        recordLogService.recordLogininfor(username, Constants.REGISTER, "注册成功");
    }
}
