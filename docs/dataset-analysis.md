# RetailAI — Dataset Analysis & ML Data Foundation

## 1. Dataset Overview & Source
* **Source:** Official M5 Forecasting - Accuracy competition dataset (Walmart retail daily sales data hosted on Zenodo archive).
* **Storage Location:** `C:\Users\tejac\OneDrive\Desktop\ML_Project\dataset\` (Excluded from Git via `.gitignore`).
* **License/Access:** Open public research dataset.

## 2. Acquired Files & Dimensions
| File Name | File Size | Row Count | Column Count | Description |
|---|---|---|---|---|
| `calendar.csv` | ~103 KB | 1,969 | 14 | Calendar dates, days of week, months, years, event names, and SNAP state markers |
| `sales_train_validation.csv` | ~120 MB | 30,490 | 1919 | Historical daily unit sales per product/store up to day `d_1913` |
| `sales_train_evaluation.csv` | ~121.7 MB | 30,490 | 1947 | Historical daily unit sales per product/store up to day `d_1941` |
| `sell_prices.csv` | ~203.4 MB | 6,841,121 | 4 | Store, item, weekly price information (`wm_yr_wk`, `sell_price`) |

## 3. Important Columns & Descriptions
* `id`: Composite identifier (`{item_id}_{store_id}_validation` or `evaluation`).
* `item_id`: Product SKU identifier (3,049 unique products across HOBBIES, HOUSEHOLD, FOODS).
* `dept_id`: Department identifier (7 unique departments).
* `cat_id`: Product category identifier (3 unique categories: HOBBIES, HOUSEHOLD, FOODS).
* `store_id`: Store identifier (10 unique stores across CA, TX, WI).
* `state_id`: State identifier (3 unique states: CA, TX, WI).
* `d_1` to `d_1913` / `d_1941`: Daily sales volumes (integer units sold $\ge 0$).
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
* **Total Daily Sales Observations (Validation):** 58,327,370
* **Zero-Demand Observations:** 39,777,094 (**68.2%**)
* **Non-Zero Demand Observations:** 18,550,276 (31.8%)
* **Summary Statistics (`units_sold`):**
  * Min: 0
  * Max: 763
  * Mean: 1.13
  * Median: 0
  * Std Dev: 3.87

> [!IMPORTANT]
> **Implication for ML Evaluation:** Over 67% of daily observations are zero. Standard MAPE fails due to division by zero. Forecasting models MUST be evaluated using WAPE (Weighted Absolute Percentage Error), MAE, and RMSE.

## 8. Reproducible Development Subset Definition
To enable rapid, deterministic local ML development and testing without high memory usage, a representative subset has been defined:
* **Stores Included:** `CA_1` (California Store 1) and `TX_1` (Texas Store 1) — 2 stores across 2 states.
* **Products Included:** Top 50 items by total historical sales volume across HOBBIES, HOUSEHOLD, and FOODS categories.
* **Time Range:** Last 730 days (`d_1184` to `d_1913`, approx. 2 years).
* **Total Subset Series:** 100 series (2 stores $\times$ 50 products).
* **Total Observations:** 73,000 daily records.
* **Output Path:** `dataset/dev_subset_sales.csv` (100 rows $\times$ 736 columns, ~290 KB).

## 9. Future Database Conceptual Mapping
When PostgreSQL tables are created in later phases, the raw data structures map to the following conceptual entities:
1. `organizations`: Multi-tenant boundary entity (`tenant_id`).
2. `stores`: `store_id`, `store_name`, `state_id`, `organization_id`.
3. `products`: `item_id`, `product_name`, `dept_id`, `cat_id`, `organization_id`.
4. `sales`: `store_id`, `product_id`, `date`, `units_sold`, `selling_price`, `organization_id`.
5. `calendar`: `date`, `wm_yr_wk`, `day_of_week`, `is_weekend`, `event_name`, `is_holiday`.
6. `inventory`: `store_id`, `product_id`, `current_stock`, `safety_stock`, `reorder_point`.
