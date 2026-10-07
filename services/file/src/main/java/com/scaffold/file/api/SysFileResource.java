package com.scaffold.file.api;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.scaffold.common.core.domain.R;
import com.scaffold.file.api.domain.SysFile;

/**
 * 文件信息服务（Triple REST 对外接口）
 *
 * @author ct
 */
@RequestMapping("/file")
public interface SysFileResource
{
    /**
     * 文件上传
     *
     * @param bytesBase64 文件内容（Base64编码）
     * @param filename 文件名
     * @return 文件信息
     */
    @PostMapping("/upload")
    R<SysFile> upload(@RequestParam(value = "bytes") String bytesBase64, @RequestParam(value = "filename") String filename);

    /**
     * 文件删除
     *
     * @param fileUrl 文件访问URL
     * @return 操作结果
     */
    @DeleteMapping("/delete")
    R<Boolean> delete(@RequestParam(value = "fileUrl") String fileUrl);
}
