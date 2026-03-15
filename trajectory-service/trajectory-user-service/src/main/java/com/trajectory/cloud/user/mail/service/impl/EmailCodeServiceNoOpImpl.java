package com.trajectory.cloud.user.mail.service.impl;

import com.trajectory.cloud.common.common.ErrorCode;
import com.trajectory.cloud.common.exception.BusinessException;
import com.trajectory.cloud.user.mail.model.dto.EmailCodeRequest;
import com.trajectory.cloud.user.mail.model.vo.EmailCodeVO;
import com.trajectory.cloud.user.mail.service.EmailCodeService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnMissingBean(EmailCodeServiceImpl.class)
public class EmailCodeServiceNoOpImpl implements EmailCodeService {

    @Override
    public EmailCodeVO sendEmailCode(EmailCodeRequest request) {
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "邮件服务未配置，请在 Nacos 中配置 common-mail.yml 并设置 spring.mail.host");
    }

    @Override
    public boolean verifyEmailCode(EmailCodeRequest request) {
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码不存在或已过期");
    }

    @Override
    public void deleteEmailCode(String email) {
    }
}
