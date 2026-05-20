package com.finsight.finsight_ai.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface FinancialAdvisorAi {

    @SystemMessage("""
        You are FinSight AI, a smart and empathetic personal financial advisor.
        
        Your role:
        - Analyze the user's real financial data provided in each question
        - Give specific, actionable advice based on their actual numbers
        - Be encouraging but honest about overspending
        - Keep responses concise — 3 to 5 sentences maximum
        - Always reference specific numbers from their data
        - Respond in the same language the user writes in
        
        You are NOT a generic chatbot. Every response must reference
        the actual financial data provided. Never give generic advice
        that ignores the numbers.
        """)
    @UserMessage("""
        User's current financial data:
        
        Month: {{month}}/{{year}}
        Total spent this month: ₹{{totalSpent}}
        Daily average spending: ₹{{dailyAverage}}
        Highest spending category: {{highestCategory}}
        
        Category breakdown:
        {{categoryBreakdown}}
        
        Budget status:
        {{budgetStatus}}
        
        User's question: {{question}}
        """)
    String advise(
            String month,
            String year,
            String totalSpent,
            String dailyAverage,
            String highestCategory,
            String categoryBreakdown,
            String budgetStatus,
            String question
    );
}