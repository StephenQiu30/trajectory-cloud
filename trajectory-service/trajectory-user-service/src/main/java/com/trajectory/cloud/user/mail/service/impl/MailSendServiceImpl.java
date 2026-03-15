package com.trajectory.cloud.user.mail.service.impl;

import com.trajectory.cloud.common.common.ErrorCode;
import com.trajectory.cloud.common.common.ThrowUtils;
import com.trajectory.cloud.common.exception.BusinessException;
import com.trajectory.cloud.user.mail.model.dto.MailSendCodeRequest;
import com.trajectory.cloud.user.mail.properties.MailProperties;
import com.trajectory.cloud.user.mail.service.MailSendService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "spring.mail", name = "host")
public class MailSendServiceImpl implements MailSendService {

    @Resource
    private JavaMailSender javaMailSender;
    @Resource
    private MailProperties mailProperties;
    @Resource
    private TemplateEngine templateEngine;

    @Override
    public void sendVerificationCode(MailSendCodeRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        String to = request.getTo();
        String code = request.getCode();
        Integer minutes = request.getMinutes() != null ? request.getMinutes() : 5;
        ThrowUtils.throwIf(StringUtils.isAnyBlank(to, code), ErrorCode.PARAMS_ERROR, "收件人或验证码为空");
        Map<String, Object> variables = new HashMap<>();
        variables.put("code", code);
        variables.put("minutes", minutes);
        variables.put("action", "登录");
        String content = templateEngine.process("email/verification-code", new Context(null, variables));
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(mailProperties.getFrom(), mailProperties.getFromName());
            helper.setTo(to);
            helper.setSubject("【轨迹-基于AIGC的数据可视化平台】登录验证码");
            helper.setText(content, true);
            javaMailSender.send(mimeMessage);
            log.info("验证码邮件发送成功, to: {}", to);
        } catch (Exception e) {
            log.error("验证码邮件发送失败, to: {}", to, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "邮件发送失败：" + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }
}
