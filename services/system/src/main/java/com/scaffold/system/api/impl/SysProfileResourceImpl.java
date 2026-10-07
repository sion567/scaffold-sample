package com.scaffold.system.api.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Base64;

import com.scaffold.file.api.proto.DeleteFileRequest;
import com.scaffold.file.api.proto.RemoteFileService;
import com.scaffold.file.api.proto.UploadRequest;
import com.scaffold.file.api.proto.UploadResponse;
import com.google.protobuf.ByteString;
import org.apache.dubbo.config.annotation.DubboReference;
import com.scaffold.common.core.domain.R;
import com.scaffold.common.core.text.Convert;
import com.scaffold.common.core.utils.DateUtils;
import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.common.core.utils.file.FileTypeUtils;
import com.scaffold.common.core.utils.file.MimeTypeUtils;
import com.scaffold.common.core.web.controller.BaseController;
import com.scaffold.common.core.web.domain.AjaxResult;
import com.scaffold.common.log.annotation.Log;
import com.scaffold.common.log.enums.BusinessType;
import com.scaffold.common.security.service.TokenService;
import com.scaffold.common.security.utils.SecurityUtils;
import com.scaffold.system.api.SysProfileResource;
import com.scaffold.system.api.domain.SysUser;
import com.scaffold.system.api.model.LoginUser;
import com.scaffold.system.service.ISysConfigService;
import com.scaffold.system.service.ISysPasswordPolicyService;
import com.scaffold.system.service.ISysUserPasswordHistoryService;
import com.scaffold.system.service.ISysUserService;

/**
 * 个人信息管理实现（Triple REST）
 *
 * @author scaffold
 */
@org.apache.dubbo.config.annotation.DubboService(protocol = "tri")
public class SysProfileResourceImpl extends BaseController implements SysProfileResource
{
    private final ISysUserService userService;
    private final ISysConfigService configService;
    private final ISysPasswordPolicyService passwordPolicyService;
    private final ISysUserPasswordHistoryService passwordHistoryService;
    private final TokenService tokenService;
    // 文件上传/删除为非幂等操作：禁止 failover 自动重试造成重复处理（P0-2）
    @DubboReference(check = false, retries = 0)
    private RemoteFileService remoteFileService;

    public SysProfileResourceImpl(ISysUserService userService, ISysConfigService configService,
                                  ISysPasswordPolicyService passwordPolicyService,
                                  ISysUserPasswordHistoryService passwordHistoryService,
                                  TokenService tokenService)
    {
        this.userService = userService;
        this.configService = configService;
        this.passwordPolicyService = passwordPolicyService;
        this.passwordHistoryService = passwordHistoryService;
        this.tokenService = tokenService;
    }

    @Override
    public AjaxResult profile()
    {
        String username = SecurityUtils.getUsername();
        SysUser user = userService.selectUserByUserName(username);
        AjaxResult ajax = AjaxResult.success(user);
        ajax.put("roleGroup", userService.selectUserRoleGroup(username));
        ajax.put("postGroup", userService.selectUserPostGroup(username));
        return ajax;
    }

    @Log(title = "个人信息", businessType = BusinessType.UPDATE)
    @Override
    public AjaxResult updateProfile(SysUser user)
    {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (StringUtils.isNull(loginUser))
        {
            return error("登录状态已过期，请重新登录");
        }
        SysUser currentUser = loginUser.getSysUser();
        currentUser.setNickName(user.getNickName());
        currentUser.setEmail(user.getEmail());
        currentUser.setPhonenumber(user.getPhonenumber());
        currentUser.setSex(user.getSex());
        if (StringUtils.isNotEmpty(user.getPhonenumber()) && !userService.checkPhoneUnique(currentUser))
        {
            return error("修改用户'" + loginUser.getUsername() + "'失败，手机号码已存在");
        }
        if (StringUtils.isNotEmpty(user.getEmail()) && !userService.checkEmailUnique(currentUser))
        {
            return error("修改用户'" + loginUser.getUsername() + "'失败，邮箱账号已存在");
        }
        if (userService.updateUserProfile(currentUser))
        {
            tokenService.setLoginUser(loginUser);
            return success();
        }
        return error("修改个人信息异常，请联系管理员");
    }

    @Log(title = "个人信息", businessType = BusinessType.UPDATE)
    @Override
    public AjaxResult updatePwd(String oldPassword, String newPassword)
    {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (StringUtils.isNull(loginUser))
        {
            return error("登录状态已过期，请重新登录");
        }
        Long userId = loginUser.getUserid();
        String password = loginUser.getSysUser().getPassword();
        if (!SecurityUtils.matchesPassword(oldPassword, password))
        {
            return error("修改密码失败，旧密码错误");
        }
        if (SecurityUtils.matchesPassword(newPassword, password))
        {
            return error("新密码不能与旧密码相同");
        }
        List<String> policyMessages = passwordPolicyService.validate(newPassword, loginUser.getUsername());
        if (!policyMessages.isEmpty())
        {
            return error(String.join("；", policyMessages));
        }
        int historyCount = Convert.toInt(configService.selectConfigByKey("sys.account.password.history"), 5);
        if (passwordHistoryService.matchesRecentHistory(newPassword, userId, historyCount))
        {
            return error("新密码不能与最近 " + historyCount + " 次使用过的密码相同");
        }
        newPassword = SecurityUtils.encryptPassword(newPassword);
        if (userService.resetUserPwdWithHistory(userId, newPassword, historyCount) > 0)
        {
            loginUser.getSysUser().setPwdUpdateDate(DateUtils.getNowDate());
            loginUser.getSysUser().setPassword(newPassword);
            tokenService.setLoginUser(loginUser);
            return success();
        }
        return error("修改密码异常，请联系管理员");
    }

    @Log(title = "用户头像", businessType = BusinessType.UPDATE)
    @Override
    public AjaxResult avatar(String bytesBase64, String filename)
    {
        if (StringUtils.isNotEmpty(bytesBase64))
        {
            LoginUser loginUser = SecurityUtils.getLoginUser();
            if (StringUtils.isNull(loginUser))
            {
                return error("登录状态已过期，请重新登录");
            }
            String extension = FileTypeUtils.getExtension(filename);
            if (!StringUtils.equalsAnyIgnoreCase(extension, MimeTypeUtils.IMAGE_EXTENSION))
            {
                return error("文件格式不正确，请上传" + Arrays.toString(MimeTypeUtils.IMAGE_EXTENSION) + "格式");
            }
            UploadResponse fileResult;
            try
            {
                byte[] bytes = Base64.getDecoder().decode(bytesBase64);
                fileResult = remoteFileService.upload(UploadRequest.newBuilder()
                        .setFile(ByteString.copyFrom(bytes))
                        .setName(filename)
                        .build());
            }
            catch (Exception e)
            {
                fileResult = null;
            }
            if (StringUtils.isNull(fileResult) || R.FAIL == fileResult.getCode() || !fileResult.hasData())
            {
                return error("文件服务异常，请联系管理员");
            }
            String url = fileResult.getData().getUrl();
            if (userService.updateUserAvatar(loginUser.getUserid(), url))
            {
                String oldAvatarUrl = loginUser.getSysUser().getAvatar();
                if (StringUtils.isNotEmpty(oldAvatarUrl))
                {
                    remoteFileService.delete(DeleteFileRequest.newBuilder()
                            .setFileUrl(oldAvatarUrl)
                            .build());
                }
                AjaxResult ajax = AjaxResult.success();
                ajax.put("imgUrl", url);
                loginUser.getSysUser().setAvatar(url);
                tokenService.setLoginUser(loginUser);
                return ajax;
            }
        }
        return error("上传图片异常，请联系管理员");
    }
}