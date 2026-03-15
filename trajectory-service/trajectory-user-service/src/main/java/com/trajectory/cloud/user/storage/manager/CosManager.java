package com.trajectory.cloud.user.storage.manager;

import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.IdUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.trajectory.cloud.common.common.ErrorCode;
import com.trajectory.cloud.common.common.ThrowUtils;
import com.trajectory.cloud.common.exception.BusinessException;
import com.trajectory.cloud.user.storage.properties.CosProperties;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Component
@Slf4j
@ConditionalOnProperty(prefix = "file.storage", name = "type", havingValue = "cos")
public class CosManager {

    @Resource
    private CosProperties cosProperties;
    @Resource
    private COSClient cosClient;

    public String uploadToCos(MultipartFile file, String path) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件不能为空");
        }
        String originalName = file.getOriginalFilename();
        String suffix = FileNameUtil.getSuffix(originalName);
        long fileSize = file.getSize();
        String fileName = IdUtil.simpleUUID() + "." + suffix;
        String filePath = (path.endsWith("/") ? path : path + "/") + fileName;
        if (filePath.startsWith("/")) {
            filePath = filePath.substring(1);
        }
        try (InputStream inputStream = file.getInputStream()) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(fileSize);
            PutObjectRequest putRequest = new PutObjectRequest(
                    cosProperties.getBucket(),
                    filePath,
                    inputStream,
                    metadata);
            cosClient.putObject(putRequest);
            log.info("文件上传成功: {}", filePath);
            return String.format("https://%s.cos.%s.myqcloud.com/%s",
                    cosProperties.getBucket(),
                    cosProperties.getRegion(),
                    filePath);
        } catch (IOException | CosClientException e) {
            log.error("文件上传失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "上传失败: " + e.getMessage());
        }
    }
}
