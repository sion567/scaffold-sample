package com.scaffold.file.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.alibaba.nacos.common.utils.IoUtils;
import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.file.config.MinioConfig;
import com.scaffold.file.utils.FileUploadUtils;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;

/**
 * Minio 文件存储
 *
 * @author ct
 */
@Service
public class MinioSysFileServiceImpl implements ISysFileService
{
    private final MinioConfig minioConfig;
    private final MinioClient client;

    public MinioSysFileServiceImpl(MinioConfig minioConfig, MinioClient client)
    {
        this.minioConfig = minioConfig;
        this.client = client;
    }

    /**
     * Minio文件上传接口
     *
     * @param file 上传的文件
     * @return 访问地址
     * @throws Exception
     */
    @Override
    public String uploadFile(MultipartFile file) throws Exception
    {
        InputStream inputStream = null;
        try
        {
            String fileName = FileUploadUtils.extractFilename(file);
            inputStream = file.getInputStream();
            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(fileName)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build();
            client.putObject(args);
            return minioConfig.getUrl() + "/" + minioConfig.getBucketName() + "/" + fileName;
        }
        catch (Exception e)
        {
            throw new RuntimeException("Minio Failed to upload file", e);
        }
        finally
        {
            IoUtils.closeQuietly(inputStream);
        }
    }

    /**
     * Minio文件上传接口（字节数组）
     *
     * @param bytes 上传的文件字节
     * @param filename 文件名
     * @return 访问地址
     * @throws Exception
     */
    @Override
    public String uploadFile(byte[] bytes, String filename) throws Exception
    {
        InputStream inputStream = null;
        try
        {
            String fileName = FileUploadUtils.extractFilename(filename);
            inputStream = new ByteArrayInputStream(bytes);
            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(fileName)
                    .stream(inputStream, bytes.length, -1)
                    .build();
            client.putObject(args);
            return minioConfig.getUrl() + "/" + minioConfig.getBucketName() + "/" + fileName;
        }
        catch (Exception e)
        {
            throw new RuntimeException("Minio Failed to upload file", e);
        }
        finally
        {
            IoUtils.closeQuietly(inputStream);
        }
    }

    /**
     * Minio文件删除接口
     *
     * @param fileUrl 文件访问URL
     * @throws Exception
     */
    @Override
    public void deleteFile(String fileUrl) throws Exception
    {
        try
        {
            String minioFile = StringUtils.substringAfter(fileUrl, minioConfig.getBucketName());
            client.removeObject(RemoveObjectArgs.builder().bucket(minioConfig.getBucketName()).object(minioFile).build());
        }
        catch (Exception e)
        {
            throw new RuntimeException("Minio Failed to delete file", e);
        }
    }
}
