package com.trajectory.cloud.user.service;

import com.trajectory.cloud.api.user.model.vo.GitHubUserVO;

/**
 * GitHub 服务
 *
 * @author StephenQiu30
 */
public interface GitHubService {

    /**
     * 根据授权码向 GitHub 换取访问令牌 (access_token)
     *
     * @param code GitHub 授权回调提供的授权码
     * @return 成功返回 access_token，失败返回 null 或抛出异常
     */
    String getAccessToken(String code);

    /**
     * 使用访问令牌获取 GitHub 用户的详细公开信息
     *
     * @param accessToken 有效的 GitHub 访问令牌
     * @return {@link GitHubUserVO} 用户信息视图对象
     */
    GitHubUserVO getUserInfo(String accessToken);

    /**
     * 构建 GitHub OAuth 授权地址 (包含防伪造攻击的 state 参数)
     *
     * @return 完整的 GitHub 授权跳转 URL
     */
    String buildAuthorizeUrl();

    /**
     * 校验并消费 OAuth2 回调中的 state 参数，防止 CSRF 攻击
     *
     * @param state 回调返回的 state 值
     * @throws com.trajectory.cloud.common.exception.BusinessException 如果 state
     *                                                                 不合法或已过期，则抛出异常
     */
    void validateAndConsumeState(String state);
}
