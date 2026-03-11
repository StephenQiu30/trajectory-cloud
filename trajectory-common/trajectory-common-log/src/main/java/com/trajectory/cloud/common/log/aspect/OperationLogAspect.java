package com.trajectory.cloud.common.log.aspect;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.trajectory.cloud.common.log.annotation.OperationLog;
import com.trajectory.cloud.common.log.model.OperationLogContext;
import com.trajectory.cloud.common.log.service.OperationLogRecorder;
import com.trajectory.cloud.common.utils.IpUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;

/**
 * 操作日志AOP切面（通用版）
 * 自动拦截带有@OperationLog注解的方法并记录日志
 * <p>
 * 各服务需提供OperationLogRecorder实现类来处理具体的日志记录逻辑
 *
 * @author StephenQiu30
 */
@Aspect
@Component
@Slf4j
public class OperationLogAspect {

    @Lazy
    @Resource
    private OperationLogRecorder operationLogRecorder;

    /**
     * 定义切点：拦截所有带有@OperationLog注解的方法
     */
    @Pointcut("@annotation(operationLog)")
    public void operationLogPointcut(OperationLog operationLog) {
    }

    /**
     * 环绕通知：在方法执行前后记录日志
     */
    @Around("operationLogPointcut(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        if (operationLogRecorder == null) {
            log.warn("OperationLogRecorder 未配置，跳过操作日志记录");
            return joinPoint.proceed();
        }

        // 获取 HttpServletRequest
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

        // 准备日志上下文对象
        OperationLogContext context = new OperationLogContext();
        context.setModule(operationLog.module());
        context.setAction(operationLog.action());

        // 设置 HTTP信息
        if (request != null) {
            context.setMethod(request.getMethod());
            context.setPath(request.getRequestURI());
            context.setHttpRequest(request);

            // 尝试获取操作人信息并设置到上下文中
            String userIdStr = request.getHeader("userId");
            String userName = request.getHeader("userName");
            if (StrUtil.isNotBlank(userIdStr)) {
                context.setOperatorId(Convert.toLong(userIdStr));
            }
            context.setOperatorName(userName);

            // 记录请求参数
            if (operationLog.recordParams()) {
                Object[] args = joinPoint.getArgs();
                // 过滤掉 HttpServletRequest 等非业务参数
                StringBuilder params = new StringBuilder();
                for (Object arg : args) {
                    if (arg == null) {
                        continue;
                    }
                    // 排除不需要序列化的参数
                    if (arg instanceof HttpServletRequest || arg instanceof HttpServletResponse
                            || arg instanceof BindingResult) {
                        continue;
                    }

                    if (!params.isEmpty()) {
                        params.append(", ");
                    }

                    // 特殊处理 MultipartFile
                    if (arg instanceof MultipartFile file) {
                        params.append(
                                String.format("File(name=%s, size=%d)", file.getOriginalFilename(), file.getSize()));
                    } else {
                        try {
                            params.append(JSONUtil.toJsonStr(arg));
                        } catch (Exception e) {
                            params.append(arg);
                        }
                    }
                }
                context.setRequestParams(params.toString());
            }

            // 提前提取 IP 地址，避免在异步线程中访问已回收的 request 对象
            context.setClientIp(IpUtils.getClientIp(request));
        }

        // 执行目标方法
        Object result = null;
        boolean success = false;
        String errorMessage = null;

        try {
            result = joinPoint.proceed();
            success = true;

            // 记录响应结果（如果需要）
            if (operationLog.recordResult() && result != null) {
                try {
                    context.setResponseResult(JSONUtil.toJsonStr(result));
                } catch (Exception e) {
                    context.setResponseResult(result.toString());
                }
            }

            return result;
        } catch (Throwable throwable) {
            success = false;
            errorMessage = throwable.getMessage();
            throw throwable;
        } finally {
            // 异步记录日志
            context.setSuccess(success);
            context.setErrorMessage(errorMessage);

            try {
                operationLogRecorder.recordOperationLogAsync(context);
            } catch (Exception e) {
                log.error("记录操作日志失败", e);
            }
        }
    }
}
