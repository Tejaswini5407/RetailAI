from typing import Dict, Any
import pandas as pd

REQUIRED_SALES_COLUMNS = ["id", "item_id", "dept_id", "cat_id", "store_id", "state_id"]
REQUIRED_CALENDAR_COLUMNS = ["date", "wm_yr_wk", "weekday", "wday", "month", "year", "d"]
REQUIRED_PRICES_COLUMNS = ["store_id", "item_id", "wm_yr_wk", "sell_price"]

def validate_sales_schema(sales_df: pd.DataFrame) -> Dict[str, Any]:
    """Validate schema, missing columns, and duplicates for sales data."""
    missing_cols = [col for col in REQUIRED_SALES_COLUMNS if col not in sales_df.columns]
    
    is_series_unique = not sales_df.duplicated(subset=["store_id", "item_id"]).any()
    duplicate_count = sales_df.duplicated(subset=["store_id", "item_id"]).sum()
    
    day_columns = [col for col in sales_df.columns if col.startswith("d_")]
    
    return {
        "valid_schema": len(missing_cols) == 0,
        "missing_required_columns": missing_cols,
        "total_series": len(sales_df),
        "unique_series": is_series_unique,
        "duplicate_series_count": int(duplicate_count),
        "day_columns_count": len(day_columns),
        "day_columns_range": f"{day_columns[0]} to {day_columns[-1]}" if day_columns else "None"
    }

def validate_calendar_schema(calendar_df: pd.DataFrame) -> Dict[str, Any]:
    """Validate schema, missing values, and date continuity for calendar data."""
    missing_cols = [col for col in REQUIRED_CALENDAR_COLUMNS if col not in calendar_df.columns]
    
    dates = pd.to_datetime(calendar_df["date"])
    min_date = dates.min().strftime("%Y-%m-%d")
    max_date = dates.max().strftime("%Y-%m-%d")
    expected_days = (dates.max() - dates.min()).days + 1
    actual_days = len(calendar_df)
    
    return {
        "valid_schema": len(missing_cols) == 0,
        "missing_required_columns": missing_cols,
        "total_days": actual_days,
        "date_range": (min_date, max_date),
        "has_date_gaps": expected_days != actual_days,
        "missing_values_per_column": {k: int(v) for k, v in calendar_df.isnull().sum().to_dict().items()}
    }

def validate_prices_schema(prices_df: pd.DataFrame) -> Dict[str, Any]:
    """Validate prices dataset schema, nulls, and value ranges."""
    missing_cols = [col for col in REQUIRED_PRICES_COLUMNS if col not in prices_df.columns]
    
    negative_prices = int((prices_df["sell_price"] < 0).sum()) if "sell_price" in prices_df.columns else 0
    zero_prices = int((prices_df["sell_price"] == 0).sum()) if "sell_price" in prices_df.columns else 0
    
    return {
        "valid_schema": len(missing_cols) == 0,
        "missing_required_columns": missing_cols,
        "total_records": len(prices_df),
        "negative_prices_count": negative_prices,
        "zero_prices_count": zero_prices,
        "min_price": float(prices_df["sell_price"].min()) if "sell_price" in prices_df.columns else None,
        "max_price": float(prices_df["sell_price"].max()) if "sell_price" in prices_df.columns else None,
        "null_counts": {k: int(v) for k, v in prices_df.isnull().sum().to_dict().items()}
    }
