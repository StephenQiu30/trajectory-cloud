package com.trajectory.cloud.search.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.trajectory.cloud.api.search.model.SearchRequest;
import com.trajectory.cloud.api.search.model.SearchVO;
import com.trajectory.cloud.api.search.model.entity.UserEsDTO;
import com.trajectory.cloud.api.user.model.dto.UserQueryRequest;
import com.trajectory.cloud.common.common.BaseResponse;
import com.trajectory.cloud.common.common.ErrorCode;
import com.trajectory.cloud.common.common.ResultUtils;
import com.trajectory.cloud.common.common.ThrowUtils;
import com.trajectory.cloud.common.log.annotation.OperationLog;
import com.trajectory.cloud.search.manager.SearchFacade;
import com.trajectory.cloud.search.service.UserEsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 搜索接口
 *
 * @author stephen
 */
@RestController
@RequestMapping("/search")
@Slf4j
@Tag(name = "SearchController", description = "搜索服务")
public class SearchController {

    @Resource
    private SearchFacade searchFacade;

    @Resource
    private UserEsService userEsService;

    /**
     * 分页搜索用户（从 ES 查询）
     *
     * @param userQueryRequest 查询请求
     * @param request          HTTP 请求
     * @return 分页结果
     */
    @Operation(summary = "分页搜索用户（从 ES 查询）")
    @PostMapping("/user/page")
    @OperationLog(module = "搜索服务", action = "搜索用户")
    public BaseResponse<Page<?>> searchUserByPage(@RequestBody UserQueryRequest userQueryRequest,
            HttpServletRequest request) {
        long size = userQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<UserEsDTO> userPage = (Page<UserEsDTO>) userEsService.searchFromEs(userQueryRequest);
        return ResultUtils.success(userPage);
    }

    /**
     * 聚合搜索查询
     *
     * @param searchRequest 搜索请求
     * @param request       HTTP 请求
     * @return 搜索结果
     */
    @Operation(summary = "聚合搜索查询")
    @PostMapping("/all")
    @OperationLog(module = "搜索服务", action = "聚合搜索")
    public BaseResponse<SearchVO<Object>> doSearchAll(@RequestBody SearchRequest searchRequest,
            HttpServletRequest request) {
        return ResultUtils.success(searchFacade.searchAll(searchRequest, request));
    }

    /**
     * 批量同步用户到 ES
     *
     * @param userEsDTOList 用户列表
     * @return 是否成功
     */
    @Operation(summary = "批量同步用户到 ES")
    @PostMapping("/user/batch/upsert")
    @OperationLog(module = "搜索服务", action = "批量同步用户")
    public BaseResponse<Boolean> batchUpsertUser(@RequestBody List<UserEsDTO> userEsDTOList) {
        return ResultUtils.success(userEsService.batchUpsert(userEsDTOList));
    }

}
