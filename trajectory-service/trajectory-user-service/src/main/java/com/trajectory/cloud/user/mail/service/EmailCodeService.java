package com.trajectory.cloud.user.mail.service;

import com.trajectory.cloud.user.mail.model.dto.EmailCodeRequest;
import com.trajectory.cloud.user.mail.model.vo.EmailCodeVO;

public interface EmailCodeService {

    EmailCodeVO sendEmailCode(EmailCodeRequest request);

    boolean verifyEmailCode(EmailCodeRequest request);

    void deleteEmailCode(String email);
}
