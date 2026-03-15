package com.trajectory.cloud.mail.service;

import com.trajectory.cloud.api.mail.model.dto.MailSendCodeRequest;
import com.trajectory.cloud.api.mail.model.dto.MailSendRequest;
import com.trajectory.cloud.common.rabbitmq.model.EmailMessage;

/**
 * 邮件服务接口
 *
 * @author StephenQiu30
 */
public interface MailService {

    /**
     * 发送简单文本邮件（同步）
     *
     * @param mailSendRequest 邮件发送请求
     */
    void sendSimpleMailSync(MailSendRequest mailSendRequest);

    /**
     * 发送HTML邮件（同步）
     *
     * @param mailSendRequest 邮件发送请求
     */
    void sendHtmlMailSync(MailSendRequest mailSendRequest);

    /**
     * 发送邮件（同步）
     *
     * @param emailMessage 邮件消息
     */
    void sendMailSync(EmailMessage emailMessage);

    /**
     * 发送邮件（异步，通过MQ）
     *
     * @param emailMessage 邮件消息
     */
    void sendMailAsync(EmailMessage emailMessage);

    /**
     * 发送验证码邮件 (通常包含特定的 HTML 模板)
     *
     * @param mailSendCodeRequest 发送验证码邮件请求 (包含邮箱、业务类型等)
     */
    void sendVerificationCode(MailSendCodeRequest mailSendCodeRequest);
}
