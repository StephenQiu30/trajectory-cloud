package com.trajectory.cloud.api.ai.client;

import com.trajectory.cloud.api.ai.model.vo.AiModelVO;
import com.trajectory.cloud.common.common.BaseResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * AI 服务 Feign 客户端
 *
 * @author StephenQiu30
 */
@FeignClient(name = "trajectory-ai-service", path = "/api/ai", contextId = "aiFeignClient")
public interface AiFeignClient {

    /**
     * 获取支持的模型列表
     *
     * @return 模型列表
     */
    @GetMapping("/models")
    BaseResponse<List<AiModelVO>> listModels();

}
