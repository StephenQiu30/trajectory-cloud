package com.trajectory.cloud.user.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.trajectory.cloud.api.user.model.dto.*;
import com.trajectory.cloud.api.user.model.vo.LoginUserVO;
import com.trajectory.cloud.api.user.model.vo.UserVO;
import com.trajectory.cloud.common.common.*;
import com.trajectory.cloud.common.constants.UserConstant;
import com.trajectory.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.trajectory.cloud.common.rabbitmq.model.EsSyncMessage;
import com.trajectory.cloud.common.rabbitmq.utils.MqSender;
import com.trajectory.cloud.common.utils.IpUtils;
import com.trajectory.cloud.user.convert.UserConvert;
import com.trajectory.cloud.user.model.entity.User;
import com.trajectory.cloud.user.model.vo.AvatarUploadVO;
import com.trajectory.cloud.user.service.UserEmailService;
import com.trajectory.cloud.user.service.UserService;
import com.trajectory.cloud.user.storage.properties.FileStorageProperties;
import com.trajectory.cloud.user.storage.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.io.FileUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户接口
 *
 * @author StephenQiu30
 */
@RestController
@RequestMapping("/user")
@Slf4j
@Tag(name = "UserController", description = "用户管理")
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private UserEmailService userEmailService;

    @Resource
    private MqSender mqSender;

    @Resource
    private ObjectProvider<FileStorageService> fileStorageServiceProvider;

    @Resource
    private FileStorageProperties fileStorageProperties;

    /**
     * GitHub 登录
     * <p>
     * 通过 GitHub 授权码进行登录或注册。
     *
     * @param gitHubLoginRequest GitHub 登录请求参数
     * @param request            HTTP 请求
     * @return 登录成功的用户信息
     */
    @PostMapping("/login/github")
    @Operation(summary = "GitHub 登录", description = "通过 GitHub 授权码进行登录或注册")
    public BaseResponse<LoginUserVO> userLoginByGitHub(@RequestBody GitHubLoginRequest gitHubLoginRequest,
                                                       HttpServletRequest request) {
        ThrowUtils.throwIf(gitHubLoginRequest == null || StringUtils.isBlank(gitHubLoginRequest.getCode())
                || StringUtils.isBlank(gitHubLoginRequest.getState()), ErrorCode.PARAMS_ERROR);
        String code = gitHubLoginRequest.getCode();
        String state = gitHubLoginRequest.getState();
        LoginUserVO loginUserVO = userService.userLoginByGitHub(code, state, request);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 用户邮箱登录
     *
     * @param userEmailLoginRequest 邮箱登录请求参数
     * @param request               HTTP 请求
     * @return 登录成功的用户信息
     */
    @PostMapping("/login/email")
    @Operation(summary = "用户邮箱登录", description = "使用邮箱 and 验证码进行登录")
    public BaseResponse<LoginUserVO> userLoginByEmail(
            @Validated @RequestBody UserEmailLoginRequest userEmailLoginRequest,
            HttpServletRequest request) {
        LoginUserVO loginUserVO = userService.userLoginByEmail(userEmailLoginRequest, request);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 发送邮箱验证码
     * <p>
     * 向指定邮箱发送登录或注册所需的验证码。
     *
     * @param request     发送邮箱验证码请求
     * @param httpRequest HTTP 请求
     * @return 验证码过期时间（秒）
     */
    @PostMapping("/login/email/code")
    @Operation(summary = "发送邮箱验证码", description = "向指定邮箱发送登录或注册所需的验证码")
    public BaseResponse<Integer> sendEmailLoginCode(@Validated @RequestBody UserEmailCodeSendRequest request,
                                                    HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        String clientIp = IpUtils.getClientIp(httpRequest);
        Integer expireSeconds = userEmailService.sendEmailCode(request.getEmail(), clientIp);
        return ResultUtils.success(expireSeconds);
    }

    /**
     * 获取 GitHub 授权 URL
     * <p>
     * 获取跳转到 GitHub 授权页面的 URL。
     *
     * @return GitHub 授权 URL
     */
    @GetMapping("/login/github")
    @Operation(summary = "获取 GitHub 授权 URL", description = "获取跳转到 GitHub 授权页面的 URL")
    public BaseResponse<String> getGitHubAuthorizeUrl() {
        String authorizeUrl = userService.getGitHubAuthorizeUrl();
        return ResultUtils.success(authorizeUrl);
    }

    @PostMapping(value = "/avatar/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传用户头像", description = "上传头像图片，返回访问 URL")
    public BaseResponse<AvatarUploadVO> uploadAvatar(@RequestPart("file") MultipartFile file,
                                                     HttpServletRequest request) {
        ThrowUtils.throwIf(file == null || file.isEmpty(), ErrorCode.PARAMS_ERROR, "文件不能为空");
        ThrowUtils.throwIf(!StpUtil.isLogin(), ErrorCode.NOT_LOGIN_ERROR);
        long fileSize = file.getSize();
        long FIVE_MB = 5 * 1024L * 1024L;
        ThrowUtils.throwIf(fileSize > FIVE_MB, ErrorCode.PARAMS_ERROR, "用户头像文件大小不能超过 5M");
        String originalFilename = file.getOriginalFilename();
        ThrowUtils.throwIf(originalFilename == null, ErrorCode.PARAMS_ERROR, "文件名不能为空");
        String fileSuffix = FileUtil.getSuffix(originalFilename).toLowerCase();
        ThrowUtils.throwIf(!Arrays.asList("jpeg", "jpg", "svg", "png", "webp").contains(fileSuffix),
                ErrorCode.PARAMS_ERROR, "用户头像仅支持 jpeg、jpg、svg、png、webp 格式");
        FileStorageService fileStorageService = fileStorageServiceProvider.getIfAvailable();
        ThrowUtils.throwIf(fileStorageService == null, ErrorCode.SYSTEM_ERROR, "未配置对象存储");
        Long userId = StpUtil.getLoginIdAsLong();
        String path = String.format("/%s/user_avatar/%s", fileStorageProperties.getPathPrefix(), userId);
        String fileUrl = fileStorageService.upload(file, path);
        return ResultUtils.success(AvatarUploadVO.builder()
                .url(fileUrl)
                .fileName(originalFilename)
                .build());
    }

    /**
     * GitHub 登录回调
     * <p>
     * GitHub 授权后的重定向处理接口
     *
     * @param request     GitHub 回调请求参数
     * @param httpRequest HTTP 请求
     * @return 登录成功的用户信息
     */
    @GetMapping("/login/github/callback")
    @Operation(summary = "GitHub 登录回调", description = "GitHub 授权后的重定向处理接口")
    public BaseResponse<LoginUserVO> gitHubLoginCallback(@ModelAttribute GitHubCallbackRequest request,
                                                         HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(StringUtils.isAnyBlank(request.getCode(), request.getState()), ErrorCode.PARAMS_ERROR);
        LoginUserVO loginUserVO = userService.userLoginByGitHub(request.getCode(), request.getState(), httpRequest);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 用户注销
     * <p>
     * 退出当前登录状态。
     *
     * @param request HTTP 请求
     * @return 是否成功退出
     */
    @PostMapping("/logout")
    @Operation(summary = "用户注销", description = "退出当前登录状态")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 获取当前登录用户
     * <p>
     * 获取系统当前登录的用户信息。
     *
     * @param request HTTP 请求
     * @return 当前登录的用户信息
     */
    @GetMapping("/get/login")
    @Operation(summary = "获取当前登录用户", description = "获取系统当前登录的用户信息")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(user));
    }

    // endregion

    /**
     * 创建用户 (仅管理员)
     *
     * @param userAddRequest 用户创建请求参数
     * @param request        HTTP 请求
     * @return 新创建用户的 ID
     */
    @PostMapping("/add")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @Operation(summary = "创建用户", description = "由管理员手动创建新用户")
    public BaseResponse<Long> addUser(@Validated @RequestBody UserAddRequest userAddRequest,
                                      HttpServletRequest request) {
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        User user = UserConvert.addRequestToObj(userAddRequest);
        // 数据校验
        userService.validUser(user, true);
        // 写入数据库
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 返回新写入的数据 id
        long newTagId = user.getId();

        // 发送 ES 同步消息（失败不影响用户创建）
        try {
            UserVO userVO = UserConvert.objToVo(user);
            EsSyncMessage message = new EsSyncMessage("user", "upsert", newTagId,
                    cn.hutool.json.JSONUtil.toJsonStr(userVO), System.currentTimeMillis());
            mqSender.send(MqBizTypeEnum.ES_SYNC_SINGLE, newTagId + "", message);
            log.info("用户创建 ES 同步消息已发送, userId: {}", newTagId);
        } catch (Exception e) {
            log.error("【ES同步失败】用户创建 ES 同步消息发送失败, userId: {}", newTagId, e);
        }

        return ResultUtils.success(newTagId);
    }

    /**
     * 删除用户
     * <p>
     * 仅用户本人或管理员有权执行删除操作
     *
     * @param deleteRequest 删除请求参数 (包含用户 ID)
     * @param request       HTTP 请求
     * @return 是否删除成功
     */
    @PostMapping("/delete")
    @Operation(summary = "删除用户", description = "删除指定 ID 的用户，需要本人或管理员权限")
    public BaseResponse<Boolean> deleteUser(@Validated @RequestBody DeleteRequest deleteRequest,
                                            HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        User oldUser = userService.getById(id);
        ThrowUtils.throwIf(oldUser == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        ThrowUtils.throwIf(!oldUser.getId().equals(user.getId()) && !userService.isAdmin(request),
                ErrorCode.NO_AUTH_ERROR);
        // 操作数据库
        boolean result = userService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        // 发送 ES 同步消息（失败不影响用户删除）
        try {
            // 注：删除操作需要在目标端进行判断，所以直接发送包含 ID 的空对象或是特殊标记，此处为了简单且保留一致性，建议在重构模型时改进
            // 目前依然发送，利用 MqSender 但需要 Consumer 处理
            EsSyncMessage message = new EsSyncMessage("user", "delete", id, null, System.currentTimeMillis());
            mqSender.send(MqBizTypeEnum.ES_SYNC_SINGLE, id + "", message);
            log.info("用户删除 ES 同步消息已发送, userId: {}", id);
        } catch (Exception e) {
            log.error("【ES同步失败】用户删除 ES 同步消息发送失败, userId: {}", id, e);
        }

        return ResultUtils.success(true);
    }

    /**
     * 更新用户 (管理员更新)
     *
     * @param userUpdateRequest 用户更新请求参数
     * @param request           HTTP 请求
     * @return 是否更新成功
     */
    @PostMapping("/update")
    @Operation(summary = "更新用户", description = "管理员更新指定用户信息")
    public BaseResponse<Boolean> updateUser(@Validated @RequestBody UserUpdateRequest userUpdateRequest,
                                            HttpServletRequest request) {
        ThrowUtils.throwIf(userUpdateRequest == null || userUpdateRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        User user = UserConvert.updateRequestToObj(userUpdateRequest);
        userService.validUser(user, false);
        long id = userUpdateRequest.getId();
        User oldUser = userService.getById(id);
        ThrowUtils.throwIf(oldUser == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        try {
            User updatedUser = userService.getById(id);
            UserVO userVO = UserConvert.objToVo(updatedUser);
            EsSyncMessage message = new EsSyncMessage("user", "upsert", id, cn.hutool.json.JSONUtil.toJsonStr(userVO),
                    System.currentTimeMillis());
            mqSender.send(MqBizTypeEnum.ES_SYNC_SINGLE, id + "", message);
            log.info("用户更新 ES 同步消息已发送, userId: {}", id);
        } catch (Exception e) {
            log.error("【ES同步失败】用户更新 ES 同步消息发送失败, userId: {}", id, e);
        }

        return ResultUtils.success(true);
    }

    /**
     * 是否管理员（Feign 调用）
     *
     * @param request request
     * @return 是否管理员
     */
    @GetMapping("/is/admin")
    @Operation(summary = "是否管理员", description = "返回当前登录用户是否为管理员")
    public BaseResponse<Boolean> isAdmin(HttpServletRequest request) {
        try {
            return ResultUtils.success(userService.isAdmin(request));
        } catch (Exception e) {
            log.warn("获取管理员标记失败，按非管理员处理", e);
            return ResultUtils.success(false);
        }
    }

    /**
     * 根据 ID 获取用户 (仅管理员)
     *
     * @param id      用户 ID
     * @param request HTTP 请求
     * @return 用户详细信息
     */
    @GetMapping("/get")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @Operation(summary = "根据 ID 获取用户", description = "管理员根据用户 ID 获取用户详细脱敏前信息")
    public BaseResponse<User> getUserById(@RequestParam("id") long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 根据 ID 获取用户视图对象
     *
     * @param id      用户 ID
     * @param request HTTP 请求
     * @return 用户脱敏后的视图信息
     */
    @GetMapping("/get/vo")
    @Operation(summary = "根据 ID 获取用户信息 (VO)", description = "获取指定用户脱敏后的视图对象")
    public BaseResponse<UserVO> getUserVOById(@RequestParam("id") long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(userService.getUserVO(user, request));
    }

    /**
     * 批量根据 ID 获取用户视图对象 (内部 Feign 调用)
     *
     * @param ids 用户 ID 列表
     * @return 用户视图对象列表
     */
    @GetMapping("/get/vo/batch")
    @Operation(summary = "批量获取用户信息 (VO)", description = "批量获取指定用户 ID 列表的脱敏视图信息，主要用于微服务内部调用")
    public BaseResponse<List<UserVO>> getUserVOByIds(@RequestParam("ids") List<Long> ids) {
        ThrowUtils.throwIf(ids == null || ids.isEmpty(), ErrorCode.PARAMS_ERROR);
        List<User> userList = userService.listByIds(ids);
        // 批量接口主要用于内部 Feign 调用（如 ES 同步），不依赖 HttpServletRequest 上下文
        List<UserVO> userVOList = userList.stream()
                .map(UserConvert::objToVo)
                .collect(Collectors.toList());
        return ResultUtils.success(userVOList);
    }

    /**
     * 分页获取用户列表 (仅管理员)
     *
     * @param userQueryRequest 分页查询请求参数
     * @param request          HTTP 请求
     * @return 分页后的用户列表
     */
    @PostMapping("/list/page")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @Operation(summary = "分页获取用户列表", description = "管理员分页获取系统所有用户原始数据")
    public BaseResponse<Page<User>> listUserByPage(@RequestBody UserQueryRequest userQueryRequest,
                                                   HttpServletRequest request) {
        long current = userQueryRequest.getCurrent();
        long size = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(new Page<>(current, size),
                userService.getQueryWrapper(userQueryRequest));
        return ResultUtils.success(userPage);
    }

    /**
     * 分页获取用户视图对象列表
     *
     * @param userQueryRequest 用户分页查询请求参数
     * @param request          HTTP 请求
     * @return 分页后的用户视图信息列表
     */
    @PostMapping("/list/page/vo")
    @Operation(summary = "分页获取用户列表 (VO)", description = "分页获取系统用户脱敏后的视图对象列表")
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest,
                                                       HttpServletRequest request) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = userQueryRequest.getCurrent();
        long size = userQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<User> userPage = userService.page(new Page<>(current, size),
                userService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(current, size, userPage.getTotal());
        List<UserVO> userVOList = userService.getUserVO(userPage.getRecords(), request);
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }

    /**
     * 编辑当前登录用户信息
     *
     * @param userEditRequest 用户编辑请求参数
     * @param request         HTTP 请求
     * @return 是否修改成功
     */
    @PostMapping("/edit")
    @Operation(summary = "编辑个人信息", description = "用户修改并保存自己的个人基本资料")
    public BaseResponse<Boolean> editUser(@Validated @RequestBody UserEditRequest userEditRequest,
                                          HttpServletRequest request) {
        ThrowUtils.throwIf(userEditRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        User user = UserConvert.editRequestToObj(userEditRequest);
        user.setId(loginUser.getId());
        userService.validUser(user, false);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        try {
            User updatedUser = userService.getById(user.getId());
            UserVO userVO = UserConvert.objToVo(updatedUser);
            EsSyncMessage message = new EsSyncMessage("user", "upsert", user.getId(),
                    cn.hutool.json.JSONUtil.toJsonStr(userVO), System.currentTimeMillis());
            mqSender.send(MqBizTypeEnum.ES_SYNC_SINGLE, user.getId() + "", message);
            log.info("用户更新个人信息 ES 同步消息已发送, userId: {}", user.getId());
        } catch (Exception e) {
            log.error("【ES同步失败】用户更新个人信息 ES 同步消息发送失败, userId: {}", user.getId(), e);
        }

        return ResultUtils.success(true);
    }

}
