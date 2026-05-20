package com.finsight.finsight_ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiAdviceRequest {

    @NotBlank(message = "question cannot be blank")
    @Size(max = 500, message = "question too long")
    private String question;



}
