package com.trajectory.cloud.user.storage.service.impl;

import com.trajectory.cloud.user.storage.manager.CosManager;
import com.trajectory.cloud.user.storage.service.FileStorageService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Primary
@ConditionalOnProperty(prefix = "file.storage", name = "type", havingValue = "cos")
public class CosFileStorageServiceImpl implements FileStorageService {

    private final CosManager cosManager;

    public CosFileStorageServiceImpl(CosManager cosManager) {
        this.cosManager = cosManager;
    }

    @Override
    public String upload(MultipartFile multipartFile, String path) {
        return cosManager.uploadToCos(multipartFile, path);
    }
}
