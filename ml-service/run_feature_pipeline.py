import os
import sys
import pandas as pd

sys.path.insert(0, os.path.dirname(__file__))

from app.data.loader import (
    load_calendar_data,
    load_sell_prices_data
)
from app.preprocessing.transformer import (
    transform_wide_to_long,
    merge_sell_prices
)
from app.preprocessing.feature_engineering import build_feature_pipeline
from app.preprocessing.leakage_validator import run_full_leakage_audit

def main():
    print("=== RetailAI Phase 3: Feature Engineering & Data Pipeline ===")
    
    dataset_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "dataset"))
    docs_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "docs"))
    
    dev_subset_path = os.path.join(dataset_dir, "dev_subset_sales.csv")
    if not os.path.exists(dev_subset_path):
        raise FileNotFoundError(f"Development subset not found at {dev_subset_path}. Run run_analysis.py first.")
        
    print("\n1. Ingesting development subset, calendar, and price data...")
    sales_df = pd.read_csv(dev_subset_path)
    calendar_df = load_calendar_data(dataset_dir)
    prices_df = load_sell_prices_data(dataset_dir)
    
    print(f"   - Input Sales Subset shape: {sales_df.shape} (100 series)")
    
    print("\n2. Transforming wide format to long format (store_id, item_id, date)...")
    long_df = transform_wide_to_long(sales_df, calendar_df)
    print(f"   - Long-format shape before price merge: {long_df.shape}")
    
    print("\n3. Merging price information...")
    long_df = merge_sell_prices(long_df, prices_df)
    print(f"   - Long-format shape after price merge: {long_df.shape}")
    
    print("\n4. Executing feature engineering pipeline (Date, Event, Price, Lags, Rolling)...")
    featured_df = build_feature_pipeline(long_df)
    print(f"   - Full featured dataset shape: {featured_df.shape}")
    
    print("\n5. Running automated data-leakage and grain integrity audit...")
    audit_report = run_full_leakage_audit(featured_df)
    print(f"   - Grain Uniqueness Valid: {audit_report['grain_uniqueness_valid']}")
    print(f"   - Lag Correctness Valid: {audit_report['lag_correctness']['all_lags_valid']}")
    print(f"   - Rolling Correctness Valid: {audit_report['rolling_correctness']['rolling_valid']}")
    print(f"   - Target Leakage Check (Current Day Excluded): {not audit_report['rolling_correctness']['leaking_current_day_target']}")
    print(f"   - Group Isolation Valid: {audit_report['group_isolation']['group_isolation_valid']}")
    print(f"   - ALL LEAKAGE TESTS PASSED: {audit_report['all_tests_passed']}")
    
    if not audit_report['all_tests_passed']:
        raise ValueError("Data leakage audit failed! Pipeline execution halted.")
        
    print("\n6. Handling missing values caused by initial 28-day lag window...")
    initial_row_count = len(featured_df)
    ml_ready_df = featured_df.dropna(subset=["lag_28", "rolling_mean_28"]).copy()
    rows_removed = initial_row_count - len(ml_ready_df)
    print(f"   - Initial Rows: {initial_row_count:,}")
    print(f"   - Rows Removed (28 days x 100 series): {rows_removed:,}")
    print(f"   - Final Clean Rows: {len(ml_ready_df):,}")
    
    # Save ML-ready dataset
    output_path = os.path.join(dataset_dir, "dev_features.csv")
    ml_ready_df.to_csv(output_path, index=False)
    print(f"   - ML-Ready Dataset saved to: {output_path}")
    print(f"   - Final Feature Set Columns ({len(ml_ready_df.columns)}): {list(ml_ready_df.columns)}")
    
    # 7. Generate Documentation: docs/feature-engineering.md
    print("\n7. Generating feature engineering documentation at docs/feature-engineering.md...")
    doc_content = f"""# RetailAI — Feature Engineering & ML Pipeline Documentation

## 1. ML Observation Grain
* **Observation Grain:** `(store_id, item_id, date)` — Each row represents one daily observation for a specific SKU in a specific store.
* **Target Variable:** `units_sold` (Integer daily sales volume $\\ge 0$).

## 2. Input Datasets & Transformations
* **Input Dataset:** `dataset/dev_subset_sales.csv` (100 series: 2 stores `CA_1` & `TX_1` $\\times$ 50 top items).
* **Wide-to-Long Transformation:** `pd.melt()` unpivoted `d_1184`..`d_1913` into {initial_row_count:,} tidy daily observations.
* **Calendar & Price Integration:** Merged on `d` / `date` and `(store_id, item_id, wm_yr_wk)` with 0 record duplication.

## 3. Engineered Feature Dictionary ({len(ml_ready_df.columns)} Columns)

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
* **Initial Observation Count:** {initial_row_count:,} rows.
* **Missing Value Strategy:** The initial 28 days of each series (`d_1184` to `d_1211`) contain `NaN` for `lag_28` and `rolling_mean_28` due to insufficient history before `d_1184`.
* **Rows Removed:** {rows_removed:,} rows (28 days $\\times$ 100 series).
* **Final ML-Ready Rows:** **{len(ml_ready_df):,} rows** $\\times$ **{len(ml_ready_df.columns)} columns**.
* **Output Path:** `dataset/dev_features.csv` (~11.5 MB).
"""

    os.makedirs(docs_dir, exist_ok=True)
    doc_path = os.path.join(docs_dir, "feature-engineering.md")
    with open(doc_path, "w", encoding="utf-8") as f:
        f.write(doc_content)
        
    print(f"   - Feature engineering documentation saved to: {doc_path}")
    print("\n=== Phase 3 Feature Pipeline Execution Complete ===")

if __name__ == "__main__":
    main()
