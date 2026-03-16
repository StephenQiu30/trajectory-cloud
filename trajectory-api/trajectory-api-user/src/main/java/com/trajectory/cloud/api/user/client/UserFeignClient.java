package com.trajectory.cloud.api.user.client;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.trajectory.cloud.api.user.model.dto.UserQueryRequest;
import com.trajectory.cloud.api.user.model.vo.LoginUserVO;
import com.trajectory.cloud.api.user.model.vo.UserVO;
import com.trajectory.cloud.common.common.BaseResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 用户服务 Feign 客户端
 *
 * @author StephenQiu30
 */
@FeignClient(name = "trajectory-user-service", path = "/api/user", contextId = "userFeignClient")
public interface UserFeignClient {

    /**
     * 根据 ID 获取用户脱敏视图对象 (VO)
     * <p>
     * 内部逻辑：根据 ID 查库并进行敏感信息脱敏处理。
     *
     * @param id 用户唯一标识 ID
     * @return 统一响应封装的 UserVO
     */
    @GetMapping("/get/vo")
    BaseResponse<UserVO> getUserVOById(@RequestParam("id") Long id);

    /**
     * 批量获取用户脱敏视图对象 (VO)
     * <p>
     * 常用于聚合查询或消息队列同步任务。
     *
     * @param ids 用户 ID 数组/列表
     * @return 统一响应封装的 UserVO 集合
     */
    @GetMapping("/get/vo/batch")
    BaseResponse<List<UserVO>> getUserVOByIds(@RequestParam("ids") List<Long> ids);

    /**
     * 获取当前系统登录用户的视图信息
     * <p>
     * 依赖 Feign 请求头透传 Token。
     *
     * @return 登录用户详细视图对象
     */
    @GetMapping("/get/login")
    BaseResponse<LoginUserVO> getLoginUser();

    /**
     * 校验当前通过网关及 Feign 链路的用户是否具备管理员权限
     *
     * @return 是否为管理员标识
     */
    @GetMapping("/is/admin")
    BaseResponse<Boolean> isAdmin();

    /**
     * 分页查询用户视图列表
     * <p>
     * 通常用于搜索、同步等大批量数据流转场景。
     *
     * @param userQueryRequest 包含分页及过滤条件的查询对象
     * @return 分页包装的 UserVO 列表
     */
    @PostMapping("/list/page/vo")
    BaseResponse<Page<UserVO>> listUserByPage(
            @RequestBody UserQueryRequest userQueryRequest);
}
