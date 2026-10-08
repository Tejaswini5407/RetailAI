package com.retailai.service;

import com.retailai.dto.forecast.ForecastResponseDto;
import com.retailai.exception.MLServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class MLInferenceClient {

    private static final Logger logger = LoggerFactory.getLogger(MLInferenceClient.class);

    private final RestTemplate restTemplate;
    private final String mlServiceUrl;

    public MLInferenceClient(
        RestTemplate restTemplate,
        @Value("${ml.service.url:http://localhost:8000}") String mlServiceUrl
    ) {
        this.restTemplate = restTemplate;
        this.mlServiceUrl = mlServiceUrl;
    }

    public ForecastResponseDto predictDemand(Long storeId, String storeCode, Long productId, String sku, LocalDate date, Map<String, Double> features) {
        String url = mlServiceUrl + "/api/v1/forecast";

        Map<String, Object> featureMap = new HashMap<>();
        if (features != null) {
            featureMap.putAll(features);
        }

        Map<String, Object> item = new HashMap<>();
        item.put("item_id", sku);
        item.put("store_id", storeCode);
        item.put("date", date != null ? date.toString() : LocalDate.now().toString());
        item.put("features", featureMap);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("items", List.of(item));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        logger.info("Calling ML Service at URL: {} for store: {}, sku: {}", url, storeCode, sku);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                String modelName = (String) body.getOrDefault("model_name", "Random Forest");
                String modelVersion = (String) body.get("model_version");

                List<Map<String, Object>> predictions = (List<Map<String, Object>>) body.get("predictions");
                if (predictions != null && !predictions.isEmpty()) {
                    Map<String, Object> first = predictions.get(0);
                    Number predDemand = (Number) first.getOrDefault("predicted_demand", 0.0);
                    Number roundDemand = (Number) first.getOrDefault("rounded_demand", 0);

                    return new ForecastResponseDto(
                        storeId,
                        storeCode,
                        productId,
                        sku,
                        date,
                        predDemand.doubleValue(),
                        roundDemand.intValue(),
                        modelName,
                        modelVersion
                    );
                }
            }
        } catch (Exception e) {
            logger.error("ML Service call failed ({}). Returning 503 Service Unavailable.", e.getMessage());
            throw new MLServiceUnavailableException("Forecast service is temporarily unavailable. Please try again later.");
        }

        throw new MLServiceUnavailableException("Forecast service is temporarily unavailable. Please try again later.");
    }
}
