package com.trajectory.cloud.user.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.trajectory.cloud.api.user.model.vo.GitHubUserVO;
import com.trajectory.cloud.common.cache.constants.KeyPrefixConstants;
import com.trajectory.cloud.common.cache.utils.CacheUtils;
import com.trajectory.cloud.common.common.ErrorCode;
import com.trajectory.cloud.common.common.ThrowUtils;
import com.trajectory.cloud.common.exception.BusinessException;
import com.trajectory.cloud.user.config.GitHubProperties;
import com.trajectory.cloud.user.service.GitHubService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * GitHub 服务实现
 *
 * @author StephenQiu30
 */
@Service
@Slf4j
public class GitHubServiceImpl implements GitHubService {

    /**
     * GitHub OAuth state 过期时间（秒）
     */
    private static final long STATE_EXPIRE_SECONDS = 10 * 60L;

    @Resource
    private CacheUtils cacheUtils;

    @Resource
    private GitHubProperties gitHubProperties;

    @Override
    public String getAccessToken(String code) {
        if (StringUtils.isBlank(code)) {
            return null;
        }
        String url = "https://github.com/login/oauth/access_token";
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("client_id", gitHubProperties.getClientId());
        paramMap.put("client_secret", gitHubProperties.getClientSecret());
        paramMap.put("code", code);
        if (StringUtils.isNotBlank(gitHubProperties.getRedirectUri())) {
            paramMap.put("redirect_uri", gitHubProperties.getRedirectUri());
        }
        try {
            HttpResponse response = HttpRequest.post(url)
                    .form(paramMap)
                    .header("Accept", "application/json")
                    .header("User-Agent", "trajectory-cloud")
                    .timeout(5000)
                    .execute();
            int status = response.getStatus();
            String body = response.body();
            if (status != 200) {
                log.warn("GitHub access_token 请求失败，status={}, body={}", status, body);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "GitHub 返回异常: " + (StringUtils.isNotBlank(body) ? body : "status=" + status));
            }
            if (StringUtils.isBlank(body)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取 GitHub Access Token 失败: 响应为空");
            }
            var json = JSONUtil.parseObj(body);
            String accessToken = json.getStr("access_token");
            if (StringUtils.isNotBlank(accessToken)) {
                return accessToken;
            }
            String error = json.getStr("error");
            String errorDesc = json.getStr("error_description");
            log.warn("GitHub access_token 响应无 token，error={}, error_description={}, body={}", error, errorDesc, body);
            String msg = StringUtils.isNotBlank(errorDesc) ? errorDesc : (StringUtils.isNotBlank(error) ? error : "未知错误");
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取 GitHub Access Token 失败: " + msg);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("GitHub access_token 请求异常", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取 GitHub Access Token 失败: " + e.getMessage());
        }
    }

    @Override
    public GitHubUserVO getUserInfo(String accessToken) {
        if (StringUtils.isBlank(accessToken)) {
            return null;
        }
        String url = "https://api.github.com/user";
        try {
            HttpResponse response = HttpRequest.get(url)
                    .header("Authorization", "token " + accessToken)
                    .header("Accept", "application/json")
                    .header("User-Agent", "trajectory-cloud")
                    .timeout(5000)
                    .execute();
            if (response.getStatus() != 200) {
                log.warn("GitHub user 信息请求失败，status={}, body={}", response.getStatus(), response.body());
                return null;
            }
            String body = response.body();
            if (StringUtils.isBlank(body)) {
                return null;
            }
            return JSONUtil.toBean(body, GitHubUserVO.class);
        } catch (Exception e) {
            log.error("GitHub user 信息请求异常", e);
            return null;
        }
    }

    @Override
    public String buildAuthorizeUrl() {
        String clientId = gitHubProperties.getClientId();
        ThrowUtils.throwIf(StringUtils.isBlank(clientId), ErrorCode.OPERATION_ERROR, "GitHub ClientId 未配置");
        String redirectUri = gitHubProperties.getRedirectUri();

        String state = UUID.randomUUID().toString().replace("-", "");
        String stateKey = KeyPrefixConstants.GITHUB_OAUTH_STATE + state;
        cacheUtils.putString(stateKey, "1", STATE_EXPIRE_SECONDS);

        StringBuilder urlBuilder = new StringBuilder("https://github.com/login/oauth/authorize");
        urlBuilder.append("?client_id=").append(clientId);
        if (StringUtils.isNotBlank(redirectUri)) {
            urlBuilder.append("&redirect_uri=").append(URLEncoder.encode(redirectUri, StandardCharsets.UTF_8));
        }
        urlBuilder.append("&state=").append(state);
        return urlBuilder.toString();
    }

    @Override
    public void validateAndConsumeState(String state) {
        ThrowUtils.throwIf(StringUtils.isBlank(state), ErrorCode.PARAMS_ERROR, "state 不能为空");
        String stateKey = KeyPrefixConstants.GITHUB_OAUTH_STATE + state;
        boolean exists = cacheUtils.exists(stateKey);
        ThrowUtils.throwIf(!exists, ErrorCode.PARAMS_ERROR, "state 已失效");
        cacheUtils.remove(stateKey);
    }
}
