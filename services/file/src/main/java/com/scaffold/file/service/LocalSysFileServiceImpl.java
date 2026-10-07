package com.scaffold.file.service;

import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.scaffold.common.core.utils.DateUtils;
import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.common.core.utils.file.FileUtils;
import com.scaffold.common.core.utils.uuid.Seq;
import com.scaffold.file.utils.FileUploadUtils;

/**
 * 本地文件存储
 *
 * @author ct
 */
@Primary
@Service
public class LocalSysFileServiceImpl implements ISysFileService
{
    /**
     * 资源映射路径 前缀
     */
    @Value("${file.prefix}")
    public String localFilePrefix;

    /**
     * 域名或本机访问地址
     */
    @Value("${file.domain}")
    public String domain;

    /**
     * 上传文件存储在本地的根路径
     */
    @Value("${file.path}")
    private String localFilePath;

    /**
     * 本地文件上传接口
     *
     * @param file 上传的文件
     * @return 访问地址
     * @throws Exception
     */
    @Override
    public String uploadFile(MultipartFile file) throws Exception
    {
        String name = FileUploadUtils.upload(localFilePath, file);
        String url = domain + localFilePrefix + name;
        return url;
    }

    /**
     * 本地文件上传接口（字节数组）
     *
     * @param bytes 上传的文件字节
     * @param filename 文件名
     * @return 访问地址
     * @throws Exception
     */
    @Override
    public String uploadFile(byte[] bytes, String filename) throws Exception
    {
        String baseName = FilenameUtils.getBaseName(filename);
        String extension = FilenameUtils.getExtension(filename);
        String name = StringUtils.format("{}/{}_{}.{}", DateUtils.datePath(), baseName, Seq.getId(Seq.uploadSeqType), extension);
        Path targetPath = FileUploadUtils.getAbsoluteFile(localFilePath, name).toPath();
        Files.write(targetPath, bytes);
        String url = domain + localFilePrefix + "/" + name;
        return url;
    }

    /**
     * 本地文件删除接口
     *
     * @param fileUrl 文件访问URL
     * @throws Exception
     */
    @Override
    public void deleteFile(String fileUrl) throws Exception
    {
        String localFile = StringUtils.substringAfter(fileUrl, localFilePrefix);
        FileUtils.deleteFile(localFilePath + localFile);
    }
}
