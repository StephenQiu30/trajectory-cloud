package com.trajectory.cloud.user.mail.service;

import com.trajectory.cloud.user.mail.model.dto.MailSendCodeRequest;

public interface MailSendService {

    void sendVerificationCode(MailSendCodeRequest request);
}
