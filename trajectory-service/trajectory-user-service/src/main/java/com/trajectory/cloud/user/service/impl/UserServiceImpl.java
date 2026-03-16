package com.trajectory.cloud.user.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.trajectory.cloud.api.user.model.dto.UserEmailLoginRequest;
import com.trajectory.cloud.api.user.model.dto.UserQueryRequest;
import com.trajectory.cloud.api.user.model.enums.EmailVerifiedEnum;
import com.trajectory.cloud.api.user.model.enums.UserRoleEnum;
import com.trajectory.cloud.api.user.model.vo.GitHubUserVO;
import com.trajectory.cloud.api.user.model.vo.LoginUserVO;
import com.trajectory.cloud.api.user.model.vo.UserVO;
import com.trajectory.cloud.common.auth.utils.SecurityUtils;
import com.trajectory.cloud.common.cache.model.TimeModel;
import com.trajectory.cloud.common.cache.utils.lock.LockUtils;
import com.trajectory.cloud.common.common.ErrorCode;
import com.trajectory.cloud.common.common.ThrowUtils;
import com.trajectory.cloud.common.constants.CommonConstant;
import com.trajectory.cloud.common.constants.UserConstant;
import com.trajectory.cloud.common.exception.BusinessException;
import com.trajectory.cloud.common.mysql.utils.SqlUtils;
import com.trajectory.cloud.common.rabbitmq.utils.MqSender;
import com.trajectory.cloud.common.utils.IpUtils;
import com.trajectory.cloud.common.utils.RegexUtils;
import com.trajectory.cloud.user.convert.UserConvert;
import com.trajectory.cloud.user.mapper.UserMapper;
import com.trajectory.cloud.user.model.entity.User;
import com.trajectory.cloud.user.service.GitHubService;
import com.trajectory.cloud.user.service.UserEmailService;
import com.trajectory.cloud.user.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 用户服务实现
 *
 * @author StephenQiu30
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    @Resource
    private GitHubService gitHubService;

    @Resource
    private UserEmailService userEmailService;

    @Resource
    private MqSender mqSender;

    @Resource
    private LockUtils lockUtils;

    /**
     * 校验用户信息合法性
     * <p>
     * 校验规则如下：
     * 1. 必填项校验: 新增时 userName, userEmail 不能为空
     * 2. 昵称长度: 2 - 30 个字符
     * 3. 邮箱校验: 符合 Regex 常规邮箱格式，且在数据库中唯一 (排除自身)
     * 4. 手机号校验: 符合 Regex 中国大陆手机号格式
     * 5. 简介展示: 不超过 500 字
     *
     * @param user 用户实体
     * @param add  是否为新增操作
     */
    @Override
    public void validUser(User user, boolean add) {
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userName = user.getUserName();
        String userEmail = user.getUserEmail();
        String userPhone = user.getUserPhone();
        String userProfile = user.getUserProfile();

        // 1. 必填项校验 (仅在新增时)
        if (add) {
            ThrowUtils.throwIf(StringUtils.isBlank(userName), ErrorCode.PARAMS_ERROR, "用户名称不能为空");
            ThrowUtils.throwIf(StringUtils.isBlank(userEmail), ErrorCode.PARAMS_ERROR, "用户邮箱不能为空");
        }
        
        // 2. 昵称长度校验
        if (StringUtils.isNotBlank(userName)) {
            ThrowUtils.throwIf(userName.length() < 2 || userName.length() > 30, ErrorCode.PARAMS_ERROR, "用户昵称过短或过长");
        }
        
        // 3. 邮箱格式及唯一性校验
        if (StringUtils.isNotBlank(userEmail)) {
            ThrowUtils.throwIf(!RegexUtils.checkEmail(userEmail), ErrorCode.PARAMS_ERROR, "用户邮箱格式有误");
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getUserEmail, userEmail);
            queryWrapper.ne(user.getId() != null, User::getId, user.getId());
            long count = this.count(queryWrapper);
            ThrowUtils.throwIf(count > 0, ErrorCode.PARAMS_ERROR, "该邮箱已被占用");
        }
        
        // 4. 手机号格式校验
        if (StringUtils.isNotBlank(userPhone)) {
            ThrowUtils.throwIf(!RegexUtils.checkPhone(userPhone), ErrorCode.PARAMS_ERROR, "用户手机号格式有误");
        }
        
        // 5. 简介长度校验
        if (StringUtils.isNotBlank(userProfile)) {
            ThrowUtils.throwIf(userProfile.length() > 500, ErrorCode.PARAMS_ERROR, "用户简介过长");
        }
    }

    /**
     * 获取当前登录用户信息 (从 Security Context 中提取并查库)
     *
     * @param request HTTP 请求
     * @return 当前登录的用户实体
     * @throws BusinessException 若未登录则抛出异常
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        Long userId = SecurityUtils.getLoginUserId();
        User currentUser = this.getById(userId);
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    /**
     * 获取当前登录用户（允许未登录）
     *
     * @param request request
     * @return {@link User}
     */
    @Override
    public User getLoginUserPermitNull(HttpServletRequest request) {
        Long userId = SecurityUtils.getLoginUserIdPermitNull();
        if (userId == null) {
            return null;
        }
        return this.getById(userId);
    }

    /**
     * 是否为管理员
     *
     * @param request request
     * @return boolean 是否为管理员
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        return SecurityUtils.isAdmin();
    }

    @Override
    public boolean isAdmin(User user) {
        return UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }

    /**
     * 用户注销
     *
     * @param request request
     * @return boolean 是否退出成功
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        StpUtil.checkLogin();
        StpUtil.logout();
        return true;
    }

    /**
     * 获取登录用户视图类
     *
     * @param user user
     * @return {@link LoginUserVO}
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);
        loginUserVO.setToken(StpUtil.getTokenInfo().getTokenValue());
        return loginUserVO;
    }

    /**
     * 获取用户 VO 封装类
     *
     * @param user    user
     * @param request request
     * @return {@link UserVO}
     */
    @Override
    public UserVO getUserVO(User user, HttpServletRequest request) {
        return UserConvert.objToVo(user);
    }

    /**
     * 获取用户 VO 视图类列表
     *
     * @param userList 用户列表
     * @param request  HTTP 请求
     * @return 用户视图类列表
     */
    @Override
    public List<UserVO> getUserVO(List<User> userList, HttpServletRequest request) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(user -> getUserVO(user, request)).collect(Collectors.toList());
    }

    /**
     * 分页获取用户视图类
     *
     * @param userPage 用户分页数据
     * @param request  HTTP 请求
     * @return 用户视图类分页对象
     */
    @Override
    public Page<UserVO> getUserVOPage(Page<User> userPage, HttpServletRequest request) {
        List<User> userList = userPage.getRecords();
        Page<UserVO> userVOPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        if (CollUtil.isEmpty(userList)) {
            return userVOPage;
        }
        List<UserVO> userVOList = userList.stream().map(UserConvert::objToVo).collect(Collectors.toList());
        userVOPage.setRecords(userVOList);

        return userVOPage;
    }

    /**
     * 获取查询封装类
     *
     * @param userQueryRequest userQueryRequest
     * @return {@link LambdaQueryWrapper<User>}
     */
    @Override
    public LambdaQueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        Long notId = userQueryRequest.getNotId();
        String userName = userQueryRequest.getUserName();
        String userRole = userQueryRequest.getUserRole();
        String userEmail = userQueryRequest.getUserEmail();
        String userPhone = userQueryRequest.getUserPhone();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        String searchText = userQueryRequest.getSearchText();

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                .eq(id != null, User::getId, id)
                .ne(ObjectUtils.isNotEmpty(notId), User::getId, notId)
                .eq(StringUtils.isNotBlank(userRole), User::getUserRole, userRole)
                .like(StringUtils.isNotBlank(userName), User::getUserName, userName)
                .like(StringUtils.isNotBlank(userEmail), User::getUserEmail, userEmail)
                .like(StringUtils.isNotBlank(userPhone), User::getUserPhone, userPhone);

        if (StringUtils.isNotBlank(searchText)) {
            queryWrapper.and(qw -> qw
                    .like(User::getUserName, searchText)
                    .or()
                    .like(User::getUserProfile, searchText));
        }

        if (SqlUtils.validSortField(sortField)) {
            boolean isAsc = CommonConstant.SORT_ORDER_ASC.equalsIgnoreCase(sortOrder);
            switch (sortField) {
                case "createTime" -> queryWrapper.orderBy(true, isAsc, User::getCreateTime);
                case "updateTime" -> queryWrapper.orderBy(true, isAsc, User::getUpdateTime);
                case "userName" -> queryWrapper.orderBy(true, isAsc, User::getUserName);
                default -> {
                }
            }
        }

        return queryWrapper;
    }

    /**
     * 通过 GitHub 授权码进行登录或注册
     * <p>
     * 1. 验证 state 防 CSRF
     * 2. 换取 Token 并获取 GitHub 用户信息
     * 3. 根据 GitHub ID 查找用户，若不存在则根据 Email 关联或创建新用户
     * 4. 使用分布式锁保证注册过程原子性
     *
     * @param code    GitHub 授权码
     * @param state   安全校验状态
     * @param request HTTP 请求
     * @return 登录用户视图对象
     */
    @Override
    public LoginUserVO userLoginByGitHub(String code, String state, HttpServletRequest request) {
        // 安全校验
        gitHubService.validateAndConsumeState(state);
        ThrowUtils.throwIf(StringUtils.isBlank(code), ErrorCode.PARAMS_ERROR, "授权码不能为空");

        // 获取用户信息
        String accessToken = gitHubService.getAccessToken(code);
        ThrowUtils.throwIf(StringUtils.isBlank(accessToken), ErrorCode.OPERATION_ERROR, "获取 GitHub Access Token 失败");

        GitHubUserVO gitHubUserVO = gitHubService.getUserInfo(accessToken);
        if (gitHubUserVO == null || StringUtils.isBlank(gitHubUserVO.getId())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取 GitHub 用户信息失败");
        }

        String githubId = gitHubUserVO.getId();
        User user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getGithubId, githubId));

        // 如果用户不存在且 GitHub 返回了 Email，尝试通过 Email 找找存量账号进行关联
        if (user == null && StringUtils.isNotBlank(gitHubUserVO.getEmail())) {
            user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getUserEmail, gitHubUserVO.getEmail()));
            if (user != null) {
                user.setGithubId(githubId);
                user.setGithubLogin(gitHubUserVO.getLogin());
                user.setGithubUrl(gitHubUserVO.getHtmlUrl());
                // 如果用户没有头像，顺便同步下 GitHub 的头像
                if (StringUtils.isNotBlank(gitHubUserVO.getAvatarUrl()) && StringUtils.isBlank(user.getUserAvatar())) {
                    user.setUserAvatar(gitHubUserVO.getAvatarUrl());
                }
                boolean result = this.updateById(user);
                ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "关联 GitHub 账号失败");
            }
        }

        // 使用分布式锁处理注册，防止同一账号突发多次请求导致重复创建
        String lockKey = "user:register:github:" + githubId;
        return lockUtils.lockEvent(lockKey, new TimeModel(5L, TimeUnit.SECONDS), () -> {
            User lockedUser = this.getOne(new LambdaQueryWrapper<User>().eq(User::getGithubId, githubId));
            if (lockedUser == null) {
                // 执行注册逻辑
                lockedUser = new User();
                lockedUser.setGithubId(githubId);
                lockedUser
                        .setUserName(gitHubUserVO.getName() != null ? gitHubUserVO.getName() : gitHubUserVO.getLogin());
                lockedUser.setUserAvatar(gitHubUserVO.getAvatarUrl());
                lockedUser.setGithubLogin(gitHubUserVO.getLogin());
                lockedUser.setGithubUrl(gitHubUserVO.getHtmlUrl());
                if (StringUtils.isNotBlank(gitHubUserVO.getEmail())) {
                    lockedUser.setUserEmail(gitHubUserVO.getEmail());
                    lockedUser.setEmailVerified(EmailVerifiedEnum.VERIFIED.getValue());
                }
                lockedUser.setUserRole(UserRoleEnum.USER.getValue());
                boolean result = this.save(lockedUser);
                ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "GitHub 注册失败");
            }
            User finalUser = lockedUser;

            // 更新最后登录状态
            finalUser.setLastLoginTime(new Date());
            finalUser.setLastLoginIp(IpUtils.getClientIp(request));
            this.updateById(finalUser);

            // 执行登录状态维护
            StpUtil.login(finalUser.getId());

            LoginUserVO loginUserVO = getLoginUserVO(finalUser);
            UserVO userVO = UserConvert.objToVo(finalUser);
            // 缓存用户状态在 Session 中 (可选，依据框架配置)
            StpUtil.getSession().set(UserConstant.USER_LOGIN_STATE, userVO);
            return loginUserVO;
        }, () -> {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "登录人数过多，请稍后再试");
        });
    }

    /**
     * 邮箱验证码登录
     * <p>
     * 1. 验证邮箱格式及验证码准确性
     * 2. 检查用户是否存在，若不存在则自动执行注册逻辑 (静默注册)
     * 3. 更新最后登录信息并签发 Token
     *
     * @param userEmailLoginRequest 邮箱登录请求参数
     * @param request               HTTP 请求
     * @return 登录用户视图对象
     */
    @Override
    public LoginUserVO userLoginByEmail(UserEmailLoginRequest userEmailLoginRequest, HttpServletRequest request) {
        if (userEmailLoginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String email = StringUtils.trimToEmpty(userEmailLoginRequest.getEmail());
        String code = userEmailLoginRequest.getCode();
        if (StringUtils.isAnyBlank(email, code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ThrowUtils.throwIf(!RegexUtils.checkEmail(email), ErrorCode.PARAMS_ERROR, "用户邮箱格式有误");

        // 校验验证码
        boolean verifyResult = userEmailService.verifyEmailCode(email, code);
        ThrowUtils.throwIf(!verifyResult, ErrorCode.PARAMS_ERROR, "验证码错误或已过期");

        // 分布式锁保障静默注册幂等性
        String lockKey = "user:register:email:" + email;
        return lockUtils.lockEvent(lockKey, new TimeModel(5L, TimeUnit.SECONDS), () -> {
            User user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getUserEmail, email));

            if (user == null) {
                // 自动注册逻辑
                user = new User();
                user.setUserEmail(email);
                user.setEmailVerified(EmailVerifiedEnum.VERIFIED.getValue());
                String userName = email.split("@")[0]; // 默认取邮箱前缀作为初始昵称
                user.setUserName(userName);
                user.setUserRole(UserRoleEnum.USER.getValue());
                boolean result = this.save(user);
                ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "用户注册失败");
            } else {
                // 补全已验证标记
                if (user.getEmailVerified() == null
                        || user.getEmailVerified().equals(EmailVerifiedEnum.UNVERIFIED.getValue())) {
                    user.setEmailVerified(EmailVerifiedEnum.VERIFIED.getValue());
                    this.updateById(user);
                }
            }

            // 更新登录轨迹
            user.setLastLoginTime(new Date());
            user.setLastLoginIp(IpUtils.getClientIp(request));
            this.updateById(user);

            // 保持状态
            StpUtil.login(user.getId());

            // 消费掉验证码，防止二次使用
            userEmailService.deleteEmailCode(email);

            LoginUserVO loginUserVO = getLoginUserVO(user);
            UserVO userVO = UserConvert.objToVo(user);
            StpUtil.getSession().set(UserConstant.USER_LOGIN_STATE, userVO);
            return loginUserVO;
        }, () -> {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "登录人数过多，请稍后再试");
        });
    }

    @Override
    public String getGitHubAuthorizeUrl() {
        return gitHubService.buildAuthorizeUrl();
    }

}
