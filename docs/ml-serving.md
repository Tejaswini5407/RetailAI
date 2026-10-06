# RetailAI — Phase 6: ML Model Serving & Forecast API Architecture

## 1. Overview

Phase 6 implements a production-grade, asynchronous FastAPI inference service for demand forecasting in RetailAI. The service loads the trained Phase 4 Random Forest model artifact (`best_model_random_forest.joblib`), validates and aligns incoming feature payloads against the exact 38-feature schema expected by the model, enforces strict non-negative demand clipping (`max(0.0, pred)`), and exposes a RESTful API.

---

## 2. Component Architecture

```
                                  +------------------------------------+
                                  |         Client Request             |
                                  +------------------------------------+
                                                    |
                                                    v
                                  +------------------------------------+
                                  |     FastAPI /api/v1/forecast       |
                                  +------------------------------------+
                                                    |
                                                    v
                                  +------------------------------------+
                                  |    Pydantic Request Validation     |
                                  |         (ForecastRequest)          |
                                  +------------------------------------+
                                                    |
                                                    v
                                  +------------------------------------+
                                  |          DemandPredictor           |
                                  |  - Feature alignment (38 columns)  |
                                  |  - One-hot categorical derivation  |
                                  +------------------------------------+
                                                    |
                                                    v
                                  +------------------------------------+
                                  |       ModelContainer / RF          |
                                  |  - best_model_random_forest.joblib |
                                  +------------------------------------+
                                                    |
                                                    v
                                  +------------------------------------+
                                  |       Non-Negative Clipping        |
                                  |      pred = max(0.0, raw_pred)     |
                                  +------------------------------------+
                                                    |
                                                    v
                                  +------------------------------------+
                                  |          ForecastResponse          |
                                  +------------------------------------+
```

---

## 3. Directory Layout

```
ml-service/
├── app/
│   ├── api/
│   │   └── v1/
│   │       ├── __init__.py
│   │       └── forecast.py            # POST /api/v1/forecast endpoint
│   ├── inference/
│   │   ├── __init__.py
│   │   ├── model_loader.py        # Clean joblib model loading & caching
│   │   └── predictor.py           # Feature alignment & inference logic
│   ├── schemas/
│   │   ├── __init__.py
│   │   └── forecast.py            # Pydantic request/response schemas
│   └── main.py                    # FastAPI application & GET /health
├── models/
│   └── best_model_random_forest.joblib  # Trained Phase 4 RF artifact
└── tests/
    └── test_model_serving.py      # Unit & end-to-end integration tests
```

---

## 4. Model Loading & Caching (`app/inference/model_loader.py`)

- **Artifact Path:** `ml-service/models/best_model_random_forest.joblib`.
- **Preloading:** Initialized during FastAPI startup via `lifespan` context manager.
- **Caching:** In-memory `ModelContainer` singleton prevents reloading disk file on every HTTP request.
- **Error Handling:**
  - Missing file: Raises `ModelNotFoundError` (returns HTTP `503 Service Unavailable`).
  - Unpickling error: Raises `ModelLoadError` (returns HTTP `500 Internal Server Error`).

---

## 5. Feature Schema & Alignment (38 Features)

The predictor constructs a pandas DataFrame with columns strictly matching the trained Random Forest schema:

1. `sell_price`
2. `price_change_pct`
3. `price_rel_4w_avg`
4. `day_of_week`
5. `day_of_month`
6. `week_of_year`
7. `month`
8. `quarter`
9. `year`
10. `is_weekend`
11. `is_event_day`
12. `is_snap_day`
13. `snap_CA`
14. `snap_TX`
15. `snap_WI`
16. `lag_1`
17. `lag_7`
18. `lag_14`
19. `lag_28`
20. `rolling_mean_7`
21. `rolling_std_7`
22. `rolling_mean_14`
23. `rolling_std_14`
24. `rolling_mean_28`
25. `rolling_std_28`
26. `item_code`
27. `store_id_CA_1`
28. `store_id_TX_1`
29. `state_id_CA`
30. `state_id_TX`
31. `cat_id_FOODS`
32. `cat_id_HOBBIES`
33. `cat_id_HOUSEHOLD`
34. `dept_id_FOODS_1`
35. `dept_id_FOODS_2`
36. `dept_id_FOODS_3`
37. `dept_id_HOBBIES_1`
38. `dept_id_HOUSEHOLD_1`

---

## 6. Non-Negative Demand Guarantee

Retail demand cannot be negative. The predictor explicitly clips raw model predictions:

$$\hat{y}_{\text{clipped}} = \max(0.0, \hat{y}_{\text{raw}})$$

In addition, an integer unit demand recommendation is provided:

$$\hat{y}_{\text{rounded}} = \text{round}(\hat{y}_{\text{clipped}})$$

---

## 7. API Specification

### 7.1 `GET /health`
- **Description:** Operational check for API service and model load status.
- **Response (200 OK):**
```json
{
  "status": "UP",
  "service": "retailai-ml-service",
  "model_loaded": true,
  "model_name": "Random Forest",
  "feature_count": 38
}
```

### 7.2 `POST /api/v1/forecast`
- **Description:** Generates daily demand forecasts for single or batch items.
- **Sample Request Payload:**
```json
{
  "items": [
    {
      "item_id": "FOODS_1_001",
      "store_id": "CA_1",
      "date": "2016-04-25",
      "features": {
        "sell_price": 2.50,
        "price_change_pct": 0.0,
        "price_rel_4w_avg": 1.0,
        "day_of_week": 0,
        "day_of_month": 25,
        "week_of_year": 17,
        "month": 4,
        "quarter": 2,
        "year": 2016,
        "is_weekend": 0,
        "is_event_day": 0,
        "is_snap_day": 0,
        "snap_CA": 0,
        "snap_TX": 0,
        "snap_WI": 0,
        "lag_1": 3.0,
        "lag_7": 2.0,
        "lag_14": 4.0,
        "lag_28": 1.0,
        "rolling_mean_7": 3.14,
        "rolling_std_7": 0.85,
        "rolling_mean_14": 3.00,
        "rolling_std_14": 0.90,
        "rolling_mean_28": 2.80,
        "rolling_std_28": 0.75,
        "item_code": 1
      }
    }
  ]
}
```

- **Sample Response Payload (200 OK):**
```json
{
  "status": "success",
  "model_name": "Random Forest",
  "model_version": null,
  "predictions": [
    {
      "item_id": "FOODS_1_001",
      "store_id": "CA_1",
      "date": "2016-04-25",
      "predicted_demand": 2.9545590423735866,
      "rounded_demand": 3
    }
  ],
  "count": 1
}
```

> **Note on Model Versioning:** `model_name` is `"Random Forest"`. `model_version` returns `null` because Phase 4 model artifacts do not persist explicit semantic version tags in joblib metadata. Future model management iterations can extend artifact metadata without breaking backward compatibility.

---

## 8. Verification & Test Execution

Run the comprehensive pytest suite:

```bash
cd ml-service
pytest tests/ -v
```

All 29 tests pass successfully.
