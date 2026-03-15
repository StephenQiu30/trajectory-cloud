package com.trajectory.cloud.user.storage.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    String upload(MultipartFile multipartFile, String path);
}
