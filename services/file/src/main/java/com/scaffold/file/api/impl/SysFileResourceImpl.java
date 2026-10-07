package com.scaffold.file.api.impl;

import java.util.Base64;
import org.apache.dubbo.config.annotation.DubboService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.scaffold.common.core.domain.R;
import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.common.core.utils.file.FileUtils;
import com.scaffold.file.api.SysFileResource;
import com.scaffold.file.api.domain.SysFile;
import com.scaffold.file.service.ISysFileService;

/**
 * 文件信息服务实现（Triple REST）
 *
 * @author ct
 */
@DubboService
public class SysFileResourceImpl implements SysFileResource
{
    private static final Logger log = LoggerFactory.getLogger(SysFileResourceImpl.class);

    private final ISysFileService sysFileService;

    public SysFileResourceImpl(ISysFileService sysFileService)
    {
        this.sysFileService = sysFileService;
    }

    @Override
    public R<SysFile> upload(String bytesBase64, String filename)
    {
        try
        {
            byte[] bytes = Base64.getDecoder().decode(bytesBase64);
            String url = sysFileService.uploadFile(bytes, filename);
            SysFile sysFile = new SysFile();
            sysFile.setName(FileUtils.getName(url));
            sysFile.setUrl(url);
            return R.ok(sysFile);
        }
        catch (Exception e)
        {
            log.error("上传文件失败", e);
            return R.fail(e.getMessage());
        }
    }

    @Override
    public R<Boolean> delete(String fileUrl)
    {
        try
        {
            if (!FileUtils.validateFilePath(fileUrl))
            {
                throw new Exception(StringUtils.format("资源文件({})非法，不允许删除。 ", fileUrl));
            }
            sysFileService.deleteFile(fileUrl);
            return R.ok();
        }
        catch (Exception e)
        {
            log.error("删除文件失败", e);
            return R.fail(e.getMessage());
        }
    }
}
