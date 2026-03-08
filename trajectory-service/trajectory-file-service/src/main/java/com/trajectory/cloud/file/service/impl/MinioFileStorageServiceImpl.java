package com.trajectory.cloud.file.service.impl;

import com.trajectory.cloud.file.manager.MinioManager;
import com.trajectory.cloud.file.service.FileStorageService;
import org.springframework.web.multipart.MultipartFile;

/**
 * MinIO 文件存储实现
 *
 * @author StephenQiu30
 */
public class MinioFileStorageServiceImpl implements FileStorageService {

    private final MinioManager minioManager;

    public MinioFileStorageServiceImpl(MinioManager minioManager) {
        this.minioManager = minioManager;
    }

    @Override
    public String upload(MultipartFile multipartFile, String path) {
        return minioManager.uploadToMinio(multipartFile, path);
    }

    @Override
    public void delete(String url) {
        minioManager.deleteByUrl(url);
    }
}
