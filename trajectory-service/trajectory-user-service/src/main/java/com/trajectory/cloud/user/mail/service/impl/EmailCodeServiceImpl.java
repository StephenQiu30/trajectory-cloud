package com.trajectory.cloud.user.mail.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.trajectory.cloud.common.cache.model.TimeModel;
import com.trajectory.cloud.common.cache.utils.CacheUtils;
import com.trajectory.cloud.common.cache.utils.lock.LockUtils;
import com.trajectory.cloud.common.cache.utils.ratelimit.RateLimitUtils;
import com.trajectory.cloud.common.common.ErrorCode;
import com.trajectory.cloud.common.common.ThrowUtils;
import com.trajectory.cloud.common.exception.BusinessException;
import com.trajectory.cloud.common.utils.RegexUtils;
import com.trajectory.cloud.user.mail.model.dto.EmailCodeRequest;
import com.trajectory.cloud.user.mail.model.dto.MailSendCodeRequest;
import com.trajectory.cloud.user.mail.model.vo.EmailCodeVO;
import com.trajectory.cloud.user.mail.properties.EmailCodeProperties;
import com.trajectory.cloud.user.mail.service.EmailCodeService;
import com.trajectory.cloud.user.mail.service.MailSendService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "spring.mail", name = "host")
public class EmailCodeServiceImpl implements EmailCodeService {

    @Resource
    private EmailCodeProperties emailCodeProperties;
    @Resource
    private RateLimitUtils rateLimitUtils;
    @Resource
    private CacheUtils cacheUtils;
    @Resource
    private MailSendService mailSendService;
    @Resource
    private LockUtils lockUtils;

    private static final String LOGIN_CODE_EMAIL = "login:code:email:";
    private static final String LOGIN_LIMIT_EMAIL = "login:limit:email:";
    private static final String LOGIN_LIMIT_IP = "login:limit:ip:";

    @Override
    public EmailCodeVO sendEmailCode(EmailCodeRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        String email = request.getEmail();
        String clientIp = request.getClientIp();
        ThrowUtils.throwIf(email == null || email.isEmpty(), ErrorCode.PARAMS_ERROR, "收件邮箱不能为空");
        ThrowUtils.throwIf(!RegexUtils.checkEmail(email), ErrorCode.PARAMS_ERROR, "收件邮箱格式非法");
        ThrowUtils.throwIf(clientIp == null || clientIp.isEmpty(), ErrorCode.PARAMS_ERROR, "客户端 IP 不能为空");
        String lockKey = "mail:send:code:" + email;
        return lockUtils.lockEvent(lockKey, () -> {
            String emailLimitKey = LOGIN_LIMIT_EMAIL + email;
            try {
                rateLimitUtils.doRateLimitAndExpire(
                        emailLimitKey,
                        new TimeModel((long) emailCodeProperties.getSendLimit(), TimeUnit.SECONDS),
                        1L, 1L,
                        new TimeModel((long) emailCodeProperties.getSendLimit(), TimeUnit.SECONDS));
            } catch (BusinessException e) {
                throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, "验证码发送过于频繁，请稍后再试");
            }
            String ipLimitKey = LOGIN_LIMIT_IP + clientIp;
            try {
                rateLimitUtils.doRateLimitAndExpire(
                        ipLimitKey,
                        new TimeModel(1L, TimeUnit.HOURS),
                        (long) emailCodeProperties.getIpLimit(),
                        1L,
                        new TimeModel(1L, TimeUnit.HOURS));
            } catch (BusinessException e) {
                throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, "该 IP 请求验证码次数已达上限");
            }
            String code = RandomUtil.randomNumbers(emailCodeProperties.getLength());
            String codeKey = LOGIN_CODE_EMAIL + email;
            try {
                MailSendCodeRequest sendCodeRequest = MailSendCodeRequest.builder()
                        .to(email)
                        .code(code)
                        .minutes(emailCodeProperties.getExpireTime() / 60)
                        .async(false)
                        .build();
                mailSendService.sendVerificationCode(sendCodeRequest);
                log.info("[EmailCodeServiceImpl] 验证码邮件已发送, Email: {}", email);
            } catch (Exception e) {
                log.error("[EmailCodeServiceImpl] 邮件发送异常, Email: {}", email, e);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "邮件发送失败，请重试");
            }
            try {
                cacheUtils.putString(codeKey, code, emailCodeProperties.getExpireTime());
            } catch (Exception e) {
                log.error("[EmailCodeServiceImpl] Redis 写入失败, Email: {}", email, e);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "存储异常，请联系管理员");
            }
            return EmailCodeVO.builder()
                    .expireTime(emailCodeProperties.getExpireTime())
                    .build();
        }, () -> {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, "请求处理中，请稍后刷新");
        });
    }

    @Override
    public boolean verifyEmailCode(EmailCodeRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        String email = request.getEmail();
        String code = request.getCode();
        ThrowUtils.throwIf(email == null || email.isEmpty(), ErrorCode.PARAMS_ERROR, "校验邮箱不能为空");
        ThrowUtils.throwIf(!RegexUtils.checkEmail(email), ErrorCode.PARAMS_ERROR, "校验邮箱格式非法");
        ThrowUtils.throwIf(code == null || code.isEmpty(), ErrorCode.PARAMS_ERROR, "验证码不能为空");
        String codeKey = LOGIN_CODE_EMAIL + email;
        String storedCode = cacheUtils.getString(codeKey);
        if (storedCode == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码不存在或已过期");
        }
        if (!storedCode.equals(code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "输入的验证码有误");
        }
        return true;
    }

    @Override
    public void deleteEmailCode(String email) {
        ThrowUtils.throwIf(email == null || email.isEmpty(), ErrorCode.PARAMS_ERROR, "待清理邮箱不能为空");
        ThrowUtils.throwIf(!RegexUtils.checkEmail(email), ErrorCode.PARAMS_ERROR, "待清理邮箱格式非法");
        cacheUtils.remove(LOGIN_CODE_EMAIL + email);
    }
}
