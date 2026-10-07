package com.scaffold.file.dubbo;

import com.scaffold.file.api.convert.SysFileConvert;
import com.scaffold.file.api.proto.*;
import org.apache.dubbo.config.annotation.DubboService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.scaffold.common.core.domain.R;
import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.common.core.utils.file.FileUtils;
import com.scaffold.file.api.domain.SysFile;
import com.scaffold.file.service.ISysFileService;

import java.util.concurrent.CompletableFuture;

/**
 * 文件服务内部 Dubbo 实现（IDL/protobuf，Triple 协议）
 *
 * @author ct
 */
@DubboService
public class RemoteFileProtoServiceImpl implements RemoteFileService
{
    private static final Logger log = LoggerFactory.getLogger(RemoteFileProtoServiceImpl.class);

    private final ISysFileService sysFileService;

    public RemoteFileProtoServiceImpl(ISysFileService sysFileService)
    {
        this.sysFileService = sysFileService;
    }

    @Override
    public UploadResponse upload(UploadRequest request)
    {
        try
        {
            String url = sysFileService.uploadFile(request.getFile().toByteArray(), request.getName());
            SysFile sysFile = new SysFile();
            sysFile.setName(FileUtils.getName(url));
            sysFile.setUrl(url);
            return UploadResponse.newBuilder()
                    .setCode(R.SUCCESS)
                    .setData(SysFileConvert.toProto(sysFile))
                    .build();
        }
        catch (Exception e)
        {
            log.error("上传文件失败", e);
            return UploadResponse.newBuilder()
                    .setCode(R.FAIL).setMsg(e.getMessage()).build();
        }
    }

    @Override
    public CompletableFuture<UploadResponse> uploadAsync(UploadRequest request) {
        return CompletableFuture.completedFuture(upload(request));
    }

    @Override
    public BoolResponse delete(DeleteFileRequest request)
    {
        try
        {
            String fileUrl = request.getFileUrl();
            if (!FileUtils.validateFilePath(fileUrl))
            {
                throw new Exception(StringUtils.format("资源文件({})非法，不允许删除。 ", fileUrl));
            }
            sysFileService.deleteFile(fileUrl);
            return BoolResponse.newBuilder().setCode(R.SUCCESS).build();
        }
        catch (Exception e)
        {
            log.error("删除文件失败", e);
            return BoolResponse.newBuilder()
                    .setCode(R.FAIL).setMsg(e.getMessage()).build();
        }
    }

    @Override
    public CompletableFuture<BoolResponse> deleteAsync(DeleteFileRequest request) {
        return CompletableFuture.completedFuture(delete(request));
    }
}
