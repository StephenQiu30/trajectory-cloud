package com.trajectory.cloud.file.manager;

import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.IdUtil;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.trajectory.cloud.common.common.ErrorCode;
import com.trajectory.cloud.common.common.ThrowUtils;
import com.trajectory.cloud.common.exception.BusinessException;
import com.trajectory.cloud.file.config.properties.OssProperties;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * 阿里云 OSS 对象存储管理器
 *
 * @author StephenQiu30
 */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "file.storage", name = "type", havingValue = "oss")
public class OssManager {

    @Resource
    private OssProperties ossProperties;

    @Resource
    private OSS ossClient;

    /**
     * 上传文件到阿里云 OSS
     *
     * @param file 待上传的文件
     * @param path 上传的路径
     * @return 文件在 OSS 的完整访问 URL
     */
    public String uploadToOss(MultipartFile file, String path) {
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
            metadata.setContentType(file.getContentType());

            PutObjectRequest putRequest = new PutObjectRequest(
                    ossProperties.getBucket(),
                    filePath,
                    inputStream,
                    metadata);

            ossClient.putObject(putRequest);

            log.info("文件上传成功: {}", filePath);

            // 动态生成 URL
            String endpoint = ossProperties.getEndpoint().replace("https://", "").replace("http://", "");
            return String.format("https://%s.%s/%s",
                    ossProperties.getBucket(),
                    endpoint,
                    filePath);
        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "上传失败: " + e.getMessage());
        }
    }

    /**
     * 删除文件
     *
     * @param url 文件 URL
     */
    public void deleteByUrl(String url) {
        ThrowUtils.throwIf(StringUtils.isEmpty(url), ErrorCode.PARAMS_ERROR, "被删除地址为空");

        String bucket = ossProperties.getBucket();
        String endpoint = ossProperties.getEndpoint().replace("https://", "").replace("http://", "");
        String host = String.format("%s.%s/", bucket, endpoint);

        int hostIndex = url.indexOf(host);
        if (hostIndex == -1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "URL不属于当前OSS存储桶");
        }

        String key = url.substring(hostIndex + host.length());
        try {
            ossClient.deleteObject(bucket, key);
            log.info("文件删除成功: {}", key);
        } catch (Exception e) {
            log.error("文件删除失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除失败: " + e.getMessage());
        }
    }
}
