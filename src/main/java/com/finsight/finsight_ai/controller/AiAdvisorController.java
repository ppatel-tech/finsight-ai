package com.finsight.finsight_ai.controller;

import com.finsight.finsight_ai.dto.request.AiAdviceRequest;
import com.finsight.finsight_ai.dto.response.AiAdviceResponse;
import com.finsight.finsight_ai.service.AiAdvisorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiAdvisorController {

    private final AiAdvisorService aiAdvisorService;

    @PostMapping("/advice")
    public ResponseEntity<AiAdviceResponse> getAdvice(
            @Valid @RequestBody AiAdviceRequest request) {
        return ResponseEntity.ok(aiAdvisorService.getAdvice(request));
    }
}