package com.trajectory.cloud.file.service.impl;

import com.trajectory.cloud.api.log.client.LogFeignClient;
import com.trajectory.cloud.api.log.model.dto.operation.OperationLogAddRequest;
import com.trajectory.cloud.common.log.model.OperationLogContext;
import com.trajectory.cloud.common.log.service.OperationLogRecorder;
import com.trajectory.cloud.common.utils.IpUtils;
import com.trajectory.cloud.file.service.FileStorageService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 文件服务操作日志记录器实现
 *
 * @author StephenQiu30
 */
@Service
@Slf4j
public class FileOperationLogRecorderImpl implements OperationLogRecorder {

    @Lazy
    @Resource
    private LogFeignClient logFeignClient;

    @Lazy
    @Resource
    private FileStorageService fileStorageService;

    /**
     * 异步记录操作日志
     *
     * @param context 操作日志上下文
     */
    @Async
    @Override
    public void recordOperationLogAsync(OperationLogContext context) {
        try {
            // 构建操作日志创建请求
            OperationLogAddRequest request = new OperationLogAddRequest();
            request.setModule(context.getModule());
            request.setAction(context.getAction());
            request.setMethod(context.getMethod());
            request.setPath(context.getPath());
            request.setRequestParams(context.getRequestParams());
            // 转换 Boolean 为 Integer：true ->1, false -> 0
            request.setSuccess(Boolean.TRUE.equals(context.getSuccess()) ? 1 : 0);
            request.setErrorMessage(context.getErrorMessage());

            // 获取操作人信息
            HttpServletRequest httpRequest = context.getHttpRequest();
            if (httpRequest != null) {
                // 从上下文中获取操作人ID和名称（由AOP设置）
                if (context.getOperatorId() != null) {
                    request.setOperatorId(context.getOperatorId());
                }
                if (context.getOperatorName() != null) {
                    request.setOperatorName(context.getOperatorName());
                }

                // 获取IP地址
                String clientIp = IpUtils.getClientIp(httpRequest);
                request.setClientIp(clientIp);
            }

            // 调用日志服务记录操作日志
            logFeignClient.addOperationLog(request);
            log.debug("操作日志记录成功: module={}, action={}, success={}",
                    context.getModule(), context.getAction(), context.getSuccess());
        } catch (Exception e) {
            log.error("记录操作日志失败", e);
        }
    }
}
