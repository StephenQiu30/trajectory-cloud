package com.trajectory.cloud.ai.config;

import dev.langchain4j.model.dashscope.QwenChatModel;
import dev.langchain4j.model.dashscope.QwenStreamingChatModel;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 模型配置类
 *
 * @author StephenQiu30
 */
@Configuration
public class AiConfig {

    @Resource
    private DashScopeProperties dashScopeProperties;

    /**
     * 通义千问同步模型客户端
     *
     * @return {@link QwenChatModel}
     */
    @Bean
    public QwenChatModel qwenChatModel() {
        return QwenChatModel.builder()
                .apiKey(dashScopeProperties.getApiKey())
                .modelName(dashScopeProperties.getModelName())
                .build();
    }

    /**
     * 通义千问流式模型客户端
     *
     * @return {@link QwenStreamingChatModel}
     */
    @Bean
    public QwenStreamingChatModel qwenStreamingChatModel() {
        return QwenStreamingChatModel.builder()
                .apiKey(dashScopeProperties.getApiKey())
                .modelName(dashScopeProperties.getModelName())
                .build();
    }

}
