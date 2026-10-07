package com.retailai.dto.inventory;

import com.retailai.dto.forecast.ForecastResponseDto;

public class ForecastInventoryAssessmentResponseDto {
    private ForecastResponseDto forecast;
    private InventoryAssessmentResponseDto assessment;

    public ForecastInventoryAssessmentResponseDto() {}

    public ForecastInventoryAssessmentResponseDto(ForecastResponseDto forecast, InventoryAssessmentResponseDto assessment) {
        this.forecast = forecast;
        this.assessment = assessment;
    }

    public ForecastResponseDto getForecast() { return forecast; }
    public void setForecast(ForecastResponseDto forecast) { this.forecast = forecast; }

    public InventoryAssessmentResponseDto getAssessment() { return assessment; }
    public void setAssessment(InventoryAssessmentResponseDto assessment) { this.assessment = assessment; }
}
