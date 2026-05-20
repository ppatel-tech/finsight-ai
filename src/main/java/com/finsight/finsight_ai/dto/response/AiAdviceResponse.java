package com.finsight.finsight_ai.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AiAdviceResponse {

    private String question;
    private String advice;
    private LocalDateTime timestamp;


}
