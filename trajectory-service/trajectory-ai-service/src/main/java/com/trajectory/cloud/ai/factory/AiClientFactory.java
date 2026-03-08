package com.trajectory.cloud.ai.factory;

import com.trajectory.cloud.ai.config.DashScopeProperties;
import com.trajectory.cloud.api.ai.model.dto.AiChatRequest;
import com.trajectory.cloud.api.ai.model.enums.AiModelTypeEnum;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.dashscope.QwenChatModel;
import dev.langchain4j.model.dashscope.QwenStreamingChatModel;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * AI 客户端工厂类
 * <p>
 * 负责根据对话请求中的模型类型（如 DashScope, Ollama）和后台配置属性，
 * 动态构建并提供对应的 LangChain4j 对话模型实例。
 * </p>
 *
 * @author StephenQiu30
 */
@Component
public class AiClientFactory {

    @Resource
    private DashScopeProperties dashScopeProperties;

    /**
     * 获取对话模型
     *
     * @param request 对话请求
     * @return 对话模型
     */
    public ChatLanguageModel getChatModel(AiChatRequest request) {
        String modelType = request.getModelType();
        AiModelTypeEnum typeEnum = AiModelTypeEnum.getEnumByValue(modelType);
        if (typeEnum == null) {
            typeEnum = AiModelTypeEnum.DASHSCOPE;
        }

        return QwenChatModel.builder()
                .apiKey(dashScopeProperties.getApiKey())
                .modelName(dashScopeProperties.getModelName())
                .temperature(dashScopeProperties.getTemperature().floatValue())
                .topP(dashScopeProperties.getTopP())
                .maxTokens(dashScopeProperties.getMaxTokens())
                .build();
    }

    /**
     * 获取流式对话模型
     *
     * @param request 对话请求
     * @return 流式对话模型
     */
    public StreamingChatLanguageModel getStreamingChatModel(AiChatRequest request) {
        String modelType = request.getModelType();
        AiModelTypeEnum typeEnum = AiModelTypeEnum.getEnumByValue(modelType);
        if (typeEnum == null) {
            typeEnum = AiModelTypeEnum.DASHSCOPE;
        }

        return QwenStreamingChatModel.builder()
                .apiKey(dashScopeProperties.getApiKey())
                .modelName(dashScopeProperties.getModelName())
                .temperature(dashScopeProperties.getTemperature().floatValue())
                .topP(dashScopeProperties.getTopP())
                .maxTokens(dashScopeProperties.getMaxTokens())
                .build();
    }
}
