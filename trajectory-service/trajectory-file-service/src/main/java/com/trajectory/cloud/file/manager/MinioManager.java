package com.trajectory.cloud.file.manager;

import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.IdUtil;
import com.trajectory.cloud.common.common.ErrorCode;
import com.trajectory.cloud.common.common.ThrowUtils;
import com.trajectory.cloud.common.exception.BusinessException;
import com.trajectory.cloud.file.config.properties.MinioProperties;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * MinIO 对象存储管理器
 * 提供文件上传和删除功能
 *
 * @author StephenQiu30
 */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "file.storage", name = "type", havingValue = "minio")
public class MinioManager {

    @Resource
    private MinioProperties minioProperties;

    @Resource
    private MinioClient minioClient;

    /**
     * 上传文件到 MinIO
     *
     * @param file 待上传的文件
     * @param path 上传的路径（目录），如 "images"、"documents"
     * @return 文件在 MinIO 的完整访问 URL
     * @throws BusinessException 文件为空或上传失败时抛出
     */
    public String uploadToMinio(MultipartFile file, String path) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件不能为空");
        }

        // 获取文件的原始名称和后缀
        String originalName = file.getOriginalFilename();
        String suffix = FileNameUtil.getSuffix(originalName);
        long fileSize = file.getSize();

        // 生成唯一文件名（UUID + 后缀）
        String fileName = IdUtil.simpleUUID() + "." + suffix;
        // 拼接路径，确保没有重复的斜杠
        String filePath = (path.endsWith("/") ? path : path + "/") + fileName;
        // 移除开头的斜杠（MinIO 对象名通常不推荐以 / 开头）
        if (filePath.startsWith("/")) {
            filePath = filePath.substring(1);
        }

        try (InputStream inputStream = file.getInputStream()) {
            // 构建上传请求
            PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(filePath)
                    .stream(inputStream, fileSize, -1)
                    .contentType(file.getContentType())
                    .build();

            // 执行上传
            minioClient.putObject(putObjectArgs);

            log.info("文件上传成功: {}", filePath);

            // 动态生成完整的访问 URL
            return String.format("%s/%s/%s",
                    minioProperties.getEndpoint().replaceAll("/$", ""),
                    minioProperties.getBucket(),
                    filePath);
        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "上传失败: " + e.getMessage());
        }
    }

    /**
     * 通过文件的 URL 从 MinIO 中删除文件
     *
     * @param url 文件 URL
     */
    public void deleteByUrl(String url) {
        ThrowUtils.throwIf(StringUtils.isEmpty(url), ErrorCode.PARAMS_ERROR, "被删除地址为空");

        // 提取 Key：从 URL 中提取对象名
        // URL 格式通常为 http://endpoint/bucket/path/to/file
        String bucket = minioProperties.getBucket();
        String bucketPrefix = "/" + bucket + "/";

        int bucketIndex = url.indexOf(bucketPrefix);
        if (bucketIndex == -1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "URL不属于当前MinIO存储桶");
        }

        String key = url.substring(bucketIndex + bucketPrefix.length());
        try {
            RemoveObjectArgs removeObjectArgs = RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .build();
            minioClient.removeObject(removeObjectArgs);
            log.info("文件删除成功: {}", key);
        } catch (Exception e) {
            log.error("文件删除失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除失败: " + e.getMessage());
        }
    }

}
