package com.trajectory.cloud.file.service.impl;

import com.trajectory.cloud.file.manager.OssManager;
import com.trajectory.cloud.file.service.FileStorageService;
import org.springframework.web.multipart.MultipartFile;

/**
 * 阿里云 OSS 文件存储实现
 *
 * @author StephenQiu30
 */
public class OssFileStorageServiceImpl implements FileStorageService {

    private final OssManager ossManager;

    public OssFileStorageServiceImpl(OssManager ossManager) {
        this.ossManager = ossManager;
    }

    @Override
    public String upload(MultipartFile multipartFile, String path) {
        return ossManager.uploadToOss(multipartFile, path);
    }

    @Override
    public void delete(String url) {
        ossManager.deleteByUrl(url);
    }
}
