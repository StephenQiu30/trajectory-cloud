package com.trajectory.cloud.user.service.impl;

import com.trajectory.cloud.common.common.ErrorCode;
import com.trajectory.cloud.common.common.ThrowUtils;
import com.trajectory.cloud.common.utils.RegexUtils;
import com.trajectory.cloud.user.mail.model.dto.EmailCodeRequest;
import com.trajectory.cloud.user.mail.model.vo.EmailCodeVO;
import com.trajectory.cloud.user.mail.service.EmailCodeService;
import com.trajectory.cloud.user.service.UserEmailService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 用户邮箱服务实现
 *
 * @author StephenQiu30
 */
@Service
@Slf4j
public class UserEmailServiceImpl implements UserEmailService {

    @Resource
    private EmailCodeService emailCodeService;

    /**
     * 发送邮箱验证码
     *
     * @param email    邮箱
     * @param clientIp 客户端 IP
     * @return 验证码有效期（秒）
     */
    @Override
    public Integer sendEmailCode(String email, String clientIp) {
        String normalizedEmail = StringUtils.trimToEmpty(email);
        ThrowUtils.throwIf(StringUtils.isBlank(normalizedEmail), ErrorCode.PARAMS_ERROR, "邮箱地址不能为空");
        ThrowUtils.throwIf(!RegexUtils.checkEmail(normalizedEmail), ErrorCode.PARAMS_ERROR, "用户邮箱格式有误");
        ThrowUtils.throwIf(StringUtils.isBlank(clientIp), ErrorCode.PARAMS_ERROR, "客户端IP不能为空");

        EmailCodeRequest emailCodeRequest = new EmailCodeRequest();
        emailCodeRequest.setEmail(normalizedEmail);
        emailCodeRequest.setClientIp(clientIp);
        EmailCodeVO emailCodeVO = emailCodeService.sendEmailCode(emailCodeRequest);
        ThrowUtils.throwIf(emailCodeVO == null || emailCodeVO.getExpireTime() == null, ErrorCode.OPERATION_ERROR,
                "发送验证码失败");
        return emailCodeVO.getExpireTime();
    }

    /**
     * 验证邮箱验证码
     *
     * @param email 邮箱
     * @param code  验证码
     * @return {@link boolean}
     */
    @Override
    public boolean verifyEmailCode(String email, String code) {
        String normalizedEmail = StringUtils.trimToEmpty(email);
        ThrowUtils.throwIf(StringUtils.isBlank(normalizedEmail), ErrorCode.PARAMS_ERROR, "邮箱地址不能为空");
        ThrowUtils.throwIf(!RegexUtils.checkEmail(normalizedEmail), ErrorCode.PARAMS_ERROR, "用户邮箱格式有误");
        ThrowUtils.throwIf(StringUtils.isBlank(code), ErrorCode.PARAMS_ERROR, "验证码不能为空");

        EmailCodeRequest emailCodeRequest = new EmailCodeRequest();
        emailCodeRequest.setEmail(normalizedEmail);
        emailCodeRequest.setCode(code);
        return emailCodeService.verifyEmailCode(emailCodeRequest);
    }

    /**
     * 删除邮箱验证码
     *
     * @param email 邮箱
     * @return {@link boolean}
     */
    @Override
    public boolean deleteEmailCode(String email) {
        String normalizedEmail = StringUtils.trimToEmpty(email);
        ThrowUtils.throwIf(StringUtils.isBlank(normalizedEmail), ErrorCode.PARAMS_ERROR, "邮箱地址不能为空");
        ThrowUtils.throwIf(!RegexUtils.checkEmail(normalizedEmail), ErrorCode.PARAMS_ERROR, "用户邮箱格式有误");
        emailCodeService.deleteEmailCode(normalizedEmail);
        return true;
    }
}
