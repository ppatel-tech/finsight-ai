package com.finsight.finsight_ai.config;

import com.finsight.finsight_ai.ai.FinancialAdvisorAi;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Value("${application.ai.gemini.api-key}")
    private String apiKey;

    @Value("${application.ai.gemini.model}")
    private String model;

    @Value("${application.ai.gemini.temperature}")
    private Double temperature;

    @Bean
    public GoogleAiGeminiChatModel geminiChatModel() {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(model)
                .temperature(temperature)
                .build();
    }

    @Bean
    public FinancialAdvisorAi financialAdvisorAi(
            GoogleAiGeminiChatModel chatModel) {
        return AiServices.builder(FinancialAdvisorAi.class)
                .chatLanguageModel(chatModel)
                .build();
    }
}