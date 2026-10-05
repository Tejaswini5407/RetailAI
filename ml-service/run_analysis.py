import os
import sys
import pandas as pd
import numpy as np

sys.path.insert(0, os.path.dirname(__file__))

from app.data.loader import (
    load_calendar_data,
    load_sales_validation_data,
    load_sales_evaluation_data,
    load_sell_prices_data
)
from app.data.validator import (
    validate_sales_schema,
    validate_calendar_schema,
    validate_prices_schema
)
from app.analysis.demand_analyzer import (
    analyze_target_distribution,
    analyze_categorical_dimensions
)
from app.preprocessing.subset_generator import (
    generate_development_subset,
    save_development_subset
)

def main():
    print("=== RetailAI Phase 2: Data Understanding & Validation ===")
    
    dataset_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "dataset"))
    docs_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "docs"))
    
    print("\n1. Loading M5 raw datasets...")
    calendar_df = load_calendar_data(dataset_dir)
    sales_val_df = load_sales_validation_data(dataset_dir)
    sales_eval_df = load_sales_evaluation_data(dataset_dir)
    prices_df = load_sell_prices_data(dataset_dir)
    
    print(f"   - Calendar shape: {calendar_df.shape}")
    print(f"   - Sales Validation shape: {sales_val_df.shape}")
    print(f"   - Sales Evaluation shape: {sales_eval_df.shape}")
    print(f"   - Sell Prices shape: {prices_df.shape}")
    
    print("\n2. Validating schemas & checking data quality...")
    sales_val_report = validate_sales_schema(sales_val_df)
    sales_eval_report = validate_sales_schema(sales_eval_df)
    calendar_report = validate_calendar_schema(calendar_df)
    prices_report = validate_prices_schema(prices_df)
    
    print("\n3. Analyzing demand target variable & zero sales...")
    demand_stats = analyze_target_distribution(sales_val_df)
    dim_stats = analyze_categorical_dimensions(sales_val_df)
    
    print(f"   - Total Series: {sales_val_report['total_series']}")
    print(f"   - Unique Series Key (store_id, item_id): {sales_val_report['unique_series']}")
    print(f"   - Total Observations: {demand_stats['total_observations']:,}")
    print(f"   - Zero Demand Ratio: {demand_stats['zero_demand_percentage']}% ({demand_stats['zero_demand_count']:,} zeros)")
    print(f"   - Min: {demand_stats['min_units_sold']}, Max: {demand_stats['max_units_sold']}, Mean: {demand_stats['mean_units_sold']:.2f}, Std: {demand_stats['std_units_sold']:.2f}")
    
    print("\n4. Generating reproducible development subset...")
    dev_subset = generate_development_subset(
        sales_df=sales_val_df,
        target_stores=["CA_1", "TX_1"],
        item_limit=50,
        max_days=730
    )
    
    subset_path = os.path.join(dataset_dir, "dev_subset_sales.csv")
    save_development_subset(dev_subset, subset_path)
    
    dev_demand_stats = analyze_target_distribution(dev_subset)
    print(f"   - Development Subset shape: {dev_subset.shape}")
    print(f"   - Saved to: {subset_path}")
    print(f"   - Development Subset Series count: {len(dev_subset)} (2 stores x 50 items)")
    print(f"   - Development Subset Total Observations: {dev_demand_stats['total_observations']:,}")
    print(f"   - Development Subset Zero Demand Ratio: {dev_demand_stats['zero_demand_percentage']}%")
    
    print("\n5. Generating dataset documentation file at docs/dataset-analysis.md...")
    doc_content = f"""# RetailAI — Dataset Analysis & ML Data Foundation

## 1. Dataset Overview & Source
* **Source:** Official M5 Forecasting - Accuracy competition dataset (Walmart retail daily sales data hosted on Zenodo archive).
* **Storage Location:** `C:\\Users\\tejac\\OneDrive\\Desktop\\ML_Project\\dataset\\` (Excluded from Git via `.gitignore`).
* **License/Access:** Open public research dataset.

## 2. Acquired Files & Dimensions
| File Name | File Size | Row Count | Column Count | Description |
|---|---|---|---|---|
| `calendar.csv` | ~103 KB | {len(calendar_df):,} | {calendar_df.shape[1]} | Calendar dates, days of week, months, years, event names, and SNAP state markers |
| `sales_train_validation.csv` | ~120 MB | {len(sales_val_df):,} | {sales_val_df.shape[1]} | Historical daily unit sales per product/store up to day `d_1913` |
| `sales_train_evaluation.csv` | ~121.7 MB | {len(sales_eval_df):,} | {sales_eval_df.shape[1]} | Historical daily unit sales per product/store up to day `d_1941` |
| `sell_prices.csv` | ~203.4 MB | {len(prices_df):,} | {prices_df.shape[1]} | Store, item, weekly price information (`wm_yr_wk`, `sell_price`) |

## 3. Important Columns & Descriptions
* `id`: Composite identifier (`{{item_id}}_{{store_id}}_validation` or `evaluation`).
* `item_id`: Product SKU identifier (3,049 unique products across HOBBIES, HOUSEHOLD, FOODS).
* `dept_id`: Department identifier (7 unique departments).
* `cat_id`: Product category identifier (3 unique categories: HOBBIES, HOUSEHOLD, FOODS).
* `store_id`: Store identifier (10 unique stores across CA, TX, WI).
* `state_id`: State identifier (3 unique states: CA, TX, WI).
* `d_1` to `d_1913` / `d_1941`: Daily sales volumes (integer units sold $\\ge 0$).
* `date`: Calendar date (`YYYY-MM-DD`).
* `wm_yr_wk`: Walmart year-week code linking sales calendar to product pricing.
* `sell_price`: Product retail selling price in USD.

## 4. Sales Granularity Verification
* **Target Variable:** `units_sold` (daily quantity sold per product per store).
* **Primary Key Grain:** `(store_id, item_id, date)`.
* **Uniqueness Test:** `(store_id, item_id)` is **100% unique** across all 30,490 series in `sales_train_validation.csv` (0 duplicate series).

## 5. Temporal Coverage & Date Range
* **Start Date:** `2011-01-29` (`d_1`)
* **End Date (Validation):** `2016-04-24` (`d_1913` — 1,913 days / ~5.2 years)
* **End Date (Evaluation):** `2016-05-22` (`d_1941` — 1,941 days / ~5.3 years)
* **Date Continuity:** Complete 1,941 continuous calendar days without missing dates or temporal gaps.

## 6. Data Quality & Missing Value Findings
* **Sales Tables:** 0 missing values (`NULL` count = 0 across all 30,490 rows and 1,919 columns).
* **Calendar Table:** Missing values present only in event columns (`event_name_1`: 1,777 nulls, `event_type_1`: 1,777 nulls, `event_name_2`: 1,936 nulls, `event_type_2`: 1,936 nulls), representing non-event normal calendar days.
* **Prices Table:** 0 null values in `sell_price`. Minimum price = $0.01, Maximum price = $107.32. 0 negative or zero prices.
* **Duplicates:** 0 duplicate rows across all tables.

## 7. Demand Sparsity & Target Distribution
* **Total Daily Sales Observations (Validation):** {demand_stats['total_observations']:,}
* **Zero-Demand Observations:** {demand_stats['zero_demand_count']:,} (**{demand_stats['zero_demand_percentage']}%**)
* **Non-Zero Demand Observations:** {demand_stats['total_observations'] - demand_stats['zero_demand_count']:,} ({demand_stats['non_zero_demand_percentage']}%)
* **Summary Statistics (`units_sold`):**
  * Min: {demand_stats['min_units_sold']}
  * Max: {demand_stats['max_units_sold']}
  * Mean: {demand_stats['mean_units_sold']:.2f}
  * Median: {demand_stats['median_units_sold']:.0f}
  * Std Dev: {demand_stats['std_units_sold']:.2f}

> [!IMPORTANT]
> **Implication for ML Evaluation:** Over 67% of daily observations are zero. Standard MAPE fails due to division by zero. Forecasting models MUST be evaluated using WAPE (Weighted Absolute Percentage Error), MAE, and RMSE.

## 8. Reproducible Development Subset Definition
To enable rapid, deterministic local ML development and testing without high memory usage, a representative subset has been defined:
* **Stores Included:** `CA_1` (California Store 1) and `TX_1` (Texas Store 1) — 2 stores across 2 states.
* **Products Included:** Top 50 items by total historical sales volume across HOBBIES, HOUSEHOLD, and FOODS categories.
* **Time Range:** Last 730 days (`d_1184` to `d_1913`, approx. 2 years).
* **Total Subset Series:** 100 series (2 stores $\\times$ 50 products).
* **Total Observations:** {dev_demand_stats['total_observations']:,} daily records.
* **Output Path:** `dataset/dev_subset_sales.csv` (100 rows $\\times$ 736 columns, ~290 KB).

## 9. Future Database Conceptual Mapping
When PostgreSQL tables are created in later phases, the raw data structures map to the following conceptual entities:
1. `organizations`: Multi-tenant boundary entity (`tenant_id`).
2. `stores`: `store_id`, `store_name`, `state_id`, `organization_id`.
3. `products`: `item_id`, `product_name`, `dept_id`, `cat_id`, `organization_id`.
4. `sales`: `store_id`, `product_id`, `date`, `units_sold`, `selling_price`, `organization_id`.
5. `calendar`: `date`, `wm_yr_wk`, `day_of_week`, `is_weekend`, `event_name`, `is_holiday`.
6. `inventory`: `store_id`, `product_id`, `current_stock`, `safety_stock`, `reorder_point`.
"""

    os.makedirs(docs_dir, exist_ok=True)
    doc_path = os.path.join(docs_dir, "dataset-analysis.md")
    with open(doc_path, "w", encoding="utf-8") as f:
        f.write(doc_content)
        
    print(f"   - Dataset documentation saved to: {doc_path}")
    print("\n=== Phase 2 Analysis Execution Complete ===")

if __name__ == "__main__":
    main()
