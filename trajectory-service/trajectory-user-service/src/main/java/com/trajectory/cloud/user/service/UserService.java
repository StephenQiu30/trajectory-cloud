package com.trajectory.cloud.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.trajectory.cloud.api.user.model.dto.UserEmailLoginRequest;
import com.trajectory.cloud.api.user.model.dto.UserQueryRequest;
import com.trajectory.cloud.api.user.model.vo.LoginUserVO;
import com.trajectory.cloud.api.user.model.vo.UserVO;
import com.trajectory.cloud.user.model.entity.User;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 用户服务
 * <p>
 * 负责用户账户的完整生命周期管理，包括认证、授权、个人信息管理等核心功能。
 * 核心功能包括：
 * 1. <b>多方式登录</b>：支持邮箱验证码登录、GitHub OAuth 登录、微信登录等
 * 2. <b>权限管理</b>：基于角色的访问控制（RBAC），支持管理员权限校验
 * 3. <b>数据脱敏</b>：提供安全的用户信息脱敏视图，保护敏感数据
 * 4. <b>用户管理</b>：支持用户创建、更新、删除、查询等基础CRUD操作
 * </p>
 *
 * @author StephenQiu30
 */
public interface UserService extends IService<User> {

    /**
     * 校验用户数据
     * <p>
     * 对用户实体进行业务规则校验，包括必填字段、数据格式、唯一性等检查。
     * </p>
     *
     * @param user 用户实体
     * @param add  是否为新增操作（新增时校验账号唯一性，更新时校验ID）
     */
    void validUser(User user, boolean add);

    /**
     * 获取当前登录用户
     * <p>
     * 从HTTP请求中解析用户身份信息并返回完整的用户实体。
     * 如果用户未登录，抛出未登录异常。
     * </p>
     *
     * @param request HTTP请求对象
     * @return 当前登录的用户实体
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 获取当前登录用户（允许未登录）
     * <p>
     * 从HTTP请求中解析用户身份信息，如果用户未登录则返回null。
     * 适用于需要可选登录的场景。
     * </p>
     *
     * @param request HTTP请求对象
     * @return 当前登录的用户实体，未登录则返回null
     */
    User getLoginUserPermitNull(HttpServletRequest request);

    /**
     * 判断当前用户是否为管理员
     * <p>
     * 根据HTTP请求中的用户身份信息，判断是否具备管理员权限。
     * </p>
     *
     * @param request HTTP请求对象
     * @return true表示是管理员，false表示不是管理员
     */
    boolean isAdmin(HttpServletRequest request);

    /**
     * 判断指定用户是否为管理员
     * <p>
     * 根据用户实体的角色信息，判断是否具备管理员权限。
     * </p>
     *
     * @param user 用户实体
     * @return true表示是管理员，false表示不是管理员
     */
    boolean isAdmin(User user);

    /**
     * 用户注销
     * <p>
     * 清除当前用户的登录状态，使其退出系统。
     * </p>
     *
     * @param request HTTP请求对象
     * @return 注销是否成功
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取脱敏的登录用户信息
     * <p>
     * 将用户实体转换为登录视图对象，包含必要的认证信息和脱敏后的用户数据。
     * </p>
     *
     * @param user 用户实体
     * @return 脱敏后的登录用户视图对象
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获取脱敏的用户信息
     * <p>
     * 将用户实体转换为视图对象，隐藏敏感信息如邮箱、手机号等。
     * 根据当前登录用户的权限决定脱敏程度。
     * </p>
     *
     * @param user    用户实体
     * @param request HTTP请求对象（用于获取当前用户权限）
     * @return 脱敏后的用户视图对象
     */
    UserVO getUserVO(User user, HttpServletRequest request);

    /**
     * 批量获取脱敏的用户信息
     * <p>
     * 将用户实体列表转换为视图对象列表，统一进行数据脱敏处理。
     * </p>
     *
     * @param userList 用户实体列表
     * @param request  HTTP请求对象（用于获取当前用户权限）
     * @return 脱敏后的用户视图对象列表
     */
    List<UserVO> getUserVO(List<User> userList, HttpServletRequest request);

    /**
     * 分页获取用户视图对象
     * <p>
     * 将用户实体分页对象转换为视图对象分页，包含分页元数据和脱敏后的数据。
     * </p>
     *
     * @param userPage 用户实体分页对象
     * @param request  HTTP请求对象（用于获取当前用户权限）
     * @return 脱敏后的用户视图对象分页
     */
    Page<UserVO> getUserVOPage(Page<User> userPage, HttpServletRequest request);

    /**
     * GitHub OAuth 登录
     * <p>
     * 使用GitHub授权码进行登录，自动处理用户注册和登录流程。
     * 如果是首次登录，自动创建用户账号并绑定GitHub信息。
     * </p>
     *
     * @param code    GitHub授权码
     * @param state   状态码（用于防止CSRF攻击）
     * @param request HTTP请求对象
     * @return 登录成功的用户视图对象（包含认证Token）
     */
    LoginUserVO userLoginByGitHub(String code, String state, HttpServletRequest request);

    /**
     * 获取GitHub OAuth授权URL
     * <p>
     * 生成包含state参数的GitHub授权URL，用于跳转到GitHub授权页面。
     * state参数会在授权回调时返回，用于验证请求的合法性。
     * </p>
     *
     * @return GitHub授权URL
     */
    String getGitHubAuthorizeUrl();

    /**
     * 邮箱验证码登录
     * <p>
     * 使用邮箱和验证码进行登录，验证码通过邮件发送。
     * 如果邮箱未注册，自动创建用户账号。
     * </p>
     *
     * @param userEmailLoginRequest 邮箱登录请求（包含邮箱和验证码）
     * @param request               HTTP请求对象
     * @return 登录成功的用户视图对象（包含认证Token）
     */
    LoginUserVO userLoginByEmail(UserEmailLoginRequest userEmailLoginRequest, HttpServletRequest request);

    /**
     * 根据查询请求构建查询条件
     * <p>
     * 将用户查询请求对象转换为MyBatis Plus的LambdaQueryWrapper，
     * 支持多条件组合查询，如用户名、邮箱、角色等。
     * </p>
     *
     * @param userQueryRequest 用户查询请求对象
     * @return MyBatis Plus查询条件封装对象
     */
    LambdaQueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

}
