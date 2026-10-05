# RetailAI — Feature Engineering & ML Pipeline Documentation

## 1. ML Observation Grain
* **Observation Grain:** `(store_id, item_id, date)` — Each row represents one daily observation for a specific SKU in a specific store.
* **Target Variable:** `units_sold` (Integer daily sales volume $\ge 0$).

## 2. Input Datasets & Transformations
* **Input Dataset:** `dataset/dev_subset_sales.csv` (100 series: 2 stores `CA_1` & `TX_1` $\times$ 50 top items).
* **Wide-to-Long Transformation:** `pd.melt()` unpivoted `d_1184`..`d_1913` into 73,000 tidy daily observations.
* **Calendar & Price Integration:** Merged on `d` / `date` and `(store_id, item_id, wm_yr_wk)` with 0 record duplication.

## 3. Engineered Feature Dictionary (39 Columns)

### Identifiers & Target
* `store_id`, `item_id`, `date`, `d`: Primary keys.
* `units_sold`: Target variable ($y_t$).

### Date Features
* `day_of_week`: Day of week (0=Monday to 6=Sunday).
* `day_of_month`: Day of month (1 to 31).
* `week_of_year`: ISO week number (1 to 53).
* `month`: Calendar month (1 to 12).
* `quarter`: Financial quarter (1 to 4).
* `year`: Calendar year.
* `is_weekend`: Binary flag (1 if Saturday/Sunday, else 0).

### Event & Context Features
* `is_event_day`: Binary indicator (1 if cultural/sporting/national event on date, else 0).
* `is_snap_day`: Binary indicator for state SNAP assistance distribution days (`snap_CA` for CA stores, `snap_TX` for TX stores).

### Price Features
* `sell_price`: Current item selling price in USD.
* `price_change_pct`: Percentage change in selling price relative to previous week.
* `price_rel_4w_avg`: Price ratio relative to rolling 4-week mean price ($>1.0$ = price premium, $<1.0$ = discount).

### Historical Lag Features
* `lag_1`: Demand at $t-1$.
* `lag_7`: Demand at $t-7$ (same day previous week).
* `lag_14`: Demand at $t-14$ (2 weeks prior).
* `lag_28`: Demand at $t-28$ (4 weeks prior / full monthly cycle).

### Historical Rolling Window Features (Target Shifted by 1 Day)
* `rolling_mean_7`: Mean demand over previous 7 days ($t-7$ to $t-1$).
* `rolling_std_7`: Standard deviation of demand over previous 7 days.
* `rolling_mean_14`: Mean demand over previous 14 days ($t-14$ to $t-1$).
* `rolling_std_14`: Standard deviation of demand over previous 14 days.
* `rolling_mean_28`: Mean demand over previous 28 days ($t-28$ to $t-1$).
* `rolling_std_28`: Standard deviation of demand over previous 28 days.

## 4. Leakage Prevention & Audit Results
* **Target Shift Rule:** Target `units_sold` is shifted by 1 day prior to computing rolling windows (`df.groupby().shift(1).rolling(w)`). This guarantees that current day target $y_t$ is NEVER included in rolling statistics on day $t$.
* **Group Isolation:** All lags and rolling statistics are calculated strictly within isolated `(store_id, item_id)` groups.
* **Automated Audit Results:**
  * Grain Uniqueness: **PASSED** (0 duplicates)
  * Lag Correctness: **PASSED** (`lag_1`, `lag_7`, `lag_28` match exact historical target values)
  * Rolling Correctness: **PASSED** (`rolling_mean_7` excludes current target $y_t$)
  * Group Isolation: **PASSED** (0 cross-series contamination)

## 5. Missing Value Handling & Dataset Size
* **Initial Observation Count:** 73,000 rows.
* **Missing Value Strategy:** The initial 28 days of each series (`d_1184` to `d_1211`) contain `NaN` for `lag_28` and `rolling_mean_28` due to insufficient history before `d_1184`.
* **Rows Removed:** 2,800 rows (28 days $\times$ 100 series).
* **Final ML-Ready Rows:** **70,200 rows** $\times$ **39 columns**.
* **Output Path:** `dataset/dev_features.csv` (~11.5 MB).
