package com.trajectory.cloud.ai.tool;

import com.trajectory.cloud.api.search.client.SearchFeignClient;
import com.trajectory.cloud.api.search.model.SearchRequest;
import com.trajectory.cloud.api.search.model.SearchVO;
import com.trajectory.cloud.api.search.model.enums.SearchTypeEnum;
import com.trajectory.cloud.common.common.BaseResponse;
import dev.langchain4j.agent.tool.Tool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * 平台搜索工具 (RAG)
 * <p>
 * 提供给 AI 代理使用的工具类，用于检索平台内的帖子和相关内容，增强 AI 的回答准确性。
 * </p>
 *
 * @author StephenQiu30
 */
@Component
@Slf4j
public class AiSearchTool {

    @Resource
    private SearchFeignClient searchFeignClient;

    /**
     * 检索平台中的帖子和内容背景
     * <p>
     * 当用户询问关于平台内的热门内容、特定问题或需要事实性背景时，AI 会自动调用此工具。
     * </p>
     *
     * @param query 搜索关键词或自然语言查询
     * @return 格式化的搜索结果摘要，供 AI 参考
     */
    @Tool("搜索平台内的帖子、内容、技术文章或问题的相关背景事实")
    public String searchPlatformContent(String query) {
        log.info("[PlatformSearchTool] 收到 AI 搜索请求: query={}", query);
        try {
            SearchRequest searchRequest = new SearchRequest();
            searchRequest.setSearchText(query);
            searchRequest.setType(SearchTypeEnum.POST.getValue()); // 侧重搜索帖子
            searchRequest.setPageSize(3); // 只取前 3 条精华

            BaseResponse<SearchVO<Object>> response = searchFeignClient.doSearchAll(searchRequest);
            if (response == null || response.getData() == null || response.getData().getDataList() == null) {
                return "未找到相关内容。";
            }

            SearchVO<Object> searchVO = response.getData();
            if (searchVO.getDataList().isEmpty()) {
                return "未找到关于 \"" + query + "\" 的相关内容。";
            }

            // 将搜索结果格式化为字符串返回给 AI
            return searchVO.getDataList().stream()
                    .map(Object::toString)
                    .collect(Collectors.joining("\n---\n", "以下是为您找到的平台相关内容：\n", ""));
        } catch (Exception e) {
            log.error("[PlatformSearchTool] 搜索失败: {}", e.getMessage());
            return "搜索功能暂时不可用。";
        }
    }
}
