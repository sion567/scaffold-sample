package com.scaffold.file.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传接口
 * 
 * @author ct
 */
public interface ISysFileService
{
    /**
     * 文件上传接口
     *
     * @param file 上传的文件
     * @return 访问地址
     * @throws Exception
     */
    public String uploadFile(MultipartFile file) throws Exception;

    /**
     * 文件上传接口（字节数组）
     *
     * @param bytes 文件字节数组
     * @param filename 文件名
     * @return 访问地址
     * @throws Exception
     */
    public String uploadFile(byte[] bytes, String filename) throws Exception;

    /**
     * 文件删除接口
     *
     * @param fileUrl 文件访问URL
     * @throws Exception
     */
    public void deleteFile(String fileUrl) throws Exception;
}
