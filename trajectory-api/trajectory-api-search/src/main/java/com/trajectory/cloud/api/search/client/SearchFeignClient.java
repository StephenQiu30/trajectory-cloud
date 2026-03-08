package com.trajectory.cloud.api.search.client;

import com.trajectory.cloud.api.search.model.SearchRequest;
import com.trajectory.cloud.api.search.model.SearchVO;
import com.trajectory.cloud.api.search.model.entity.UserEsDTO;
import com.trajectory.cloud.common.common.BaseResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 搜索服务 Feign 客户端
 *
 * @author stephen
 */
@FeignClient(name = "trajectory-search-service", path = "/api/search", contextId = "searchFeignClient")
public interface SearchFeignClient {

    /**
     * 聚合搜索查询
     *
     * @param searchRequest 搜索请求
     * @return 搜索结果
     */
    @PostMapping("/all")
    BaseResponse<SearchVO<Object>> doSearchAll(@RequestBody SearchRequest searchRequest);

    /**
     * 批量同步用户到 ES
     *
     * @param userEsDTOList 用户列表
     * @return 是否成功
     */
    @PostMapping("/user/batch/upsert")
    BaseResponse<Boolean> batchUpsertUser(@RequestBody List<UserEsDTO> userEsDTOList);

}
