package com.trajectory.cloud.common.constants;

/**
 * 安全相关常量
 *
 * @author stephen
 */
public interface SecurityConstant {

    /**
     * 认证请求头名称，与 Nacos 中 sa-token.token-name 保持一致。
     * 前端需设置：Authorization: Bearer &lt;token&gt;，否则网关会返回 401。
     */
    String AUTHORIZATION_HEADER = "Authorization";

    /**
     * 请求来源
     */
    String FROM_SOURCE = "from-source";

    /**
     * 内部调用
     */
    String INNER = "inner";

    /**
     * 用户 ID 请求头
     */
    String USER_ID_HEADER = "userId";

    /**
     * 用户姓名请求头
     */
    String USER_NAME_HEADER = "userName";

}
