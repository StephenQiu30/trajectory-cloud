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
     * <p>
     * 校验邮箱合法性，并调用底层 EmailCodeService 执行发送逻辑。
     *
     * @param email    目标邮箱地址
     * @param clientIp 客户端 IP，用于频率限制校验
     * @return 验证码有效期 (秒)，用于前端倒计时显示
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
     * <p>
     * 在登录或关键操作前，核对用户输入的验证码是否正确及是否过期。
     *
     * @param email 待验证邮箱地址
     * @param code  用户填写的验证码
     * @return 验证通过返回 true，否则返回 false
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
     * 从缓存中清除指定邮箱的验证码
     * <p>
     * 通常在业务流程结束后执行，防止验证码被非法复用。
     *
     * @param email 目标邮箱地址
     * @return 是否清除成功 (目前逻辑固定返回 true)
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
