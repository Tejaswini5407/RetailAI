package com.retailai.dto.forecast;

public class ForecastAccuracyResponseDto {
    private int totalEvaluated;
    private Double wape;
    private Double mae;
    private Double rmse;
    private boolean sufficientData;
    private String message;

    public ForecastAccuracyResponseDto() {}

    public ForecastAccuracyResponseDto(int totalEvaluated, Double wape, Double mae, Double rmse, boolean sufficientData, String message) {
        this.totalEvaluated = totalEvaluated;
        this.wape = wape;
        this.mae = mae;
        this.rmse = rmse;
        this.sufficientData = sufficientData;
        this.message = message;
    }

    public int getTotalEvaluated() { return totalEvaluated; }
    public void setTotalEvaluated(int totalEvaluated) { this.totalEvaluated = totalEvaluated; }

    public Double getWape() { return wape; }
    public void setWape(Double wape) { this.wape = wape; }

    public Double getMae() { return mae; }
    public void setMae(Double mae) { this.mae = mae; }

    public Double getRmse() { return rmse; }
    public void setRmse(Double rmse) { this.rmse = rmse; }

    public boolean isSufficientData() { return sufficientData; }
    public void setSufficientData(boolean sufficientData) { this.sufficientData = sufficientData; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
