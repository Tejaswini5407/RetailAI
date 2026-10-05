# RetailAI — Model Development & Baseline Benchmark (Phase 4)

## 1. Problem Formulation & Target
* **Task:** Product-level daily demand forecasting per store (Supervised Regression).
* **Target Variable:** `units_sold` (Integer daily sales volume $\ge 0$). Un-transformed raw units.
* **Evaluation Horizon:** 28 days (4-week demand forecast window).

## 2. Chronological Train / Validation Split
* **Strategy:** Strict temporal (chronological) validation split to eliminate forward data leakage.
* **Training Period:** `2014-05-24` to `2016-03-27` (67,400 observations across 100 series / 674 days).
* **Validation Period:** `2016-03-28` to `2016-04-24` (2,800 observations across 100 series / 28 days).
* **Temporal Integrity Check:** `max(train_date) = 2016-03-27` < `min(val_date) = 2016-03-28` (**PASSED**).

## 3. Predictive Feature Matrix (38 Features)
* **Date Features:** `day_of_week`, `day_of_month`, `week_of_year`, `month`, `quarter`, `year`, `is_weekend`, `is_event_day`, `is_snap_day`, `snap_CA`, `snap_TX`, `snap_WI`.
* **Price Features:** `sell_price`, `price_change_pct`, `price_rel_4w_avg`.
* **Lag Features:** `lag_1`, `lag_7`, `lag_14`, `lag_28`.
* **Rolling Statistics:** `rolling_mean_7`, `rolling_std_7`, `rolling_mean_14`, `rolling_std_14`, `rolling_mean_28`, `rolling_std_28`.
* **Categorical Encoded Features:** One-hot encoded `store_id`, `state_id`, `cat_id`, `dept_id`, and ordinal `item_code`.

## 4. Evaluation Metrics
1. **WAPE (Weighted Absolute Percentage Error):**
   $$\text{WAPE} = \frac{\sum |y_i - \hat{y}_i|}{\sum |y_i|}$$
   Primary metric for sparse retail demand with high zero counts.
2. **MAE (Mean Absolute Error):** Average absolute deviation in units.
3. **RMSE (Root Mean Squared Error):** Penalizes large forecasting errors.

## 5. Model Comparison Results

| Model | WAPE (%) | MAE | RMSE | Description |
|---|---|---|---|---|
| **Seasonal Naive (`lag_7`)** | 44.41% | 6.2325 | 9.8505 | Heuristic baseline predicting sales from same weekday 1 week prior |
| **Random Forest** | 32.49% | 4.5598 | 6.9628 | 100 Trees, Max Depth 12 |
| **XGBoost** | **32.78%** | **4.6004** | **6.9618** | Gradient Boosted Trees (100 estimators, max_depth=6, lr=0.05) |

## 6. Best Model & Selection Rationale
* **Winner:** **Random Forest**
* **Performance Gain:** Random Forest achieved a **WAPE of 32.49%** (outperforming the Seasonal Naive baseline of 44.41%, representing a **11.92% percentage point reduction in forecast error**).
* **Model Artifact:** Persisted to `ml-service/models/best_model_random_forest.joblib` (Git-ignored).

## 7. Known Limitations & Next Steps
* Model was trained on 100 benchmark series over 2 years.
* Hyperparameter tuning was kept lightweight for baseline comparison.
* Future phases will incorporate feature importance analysis, model API endpoints in FastAPI, and Spring Boot service orchestration.
