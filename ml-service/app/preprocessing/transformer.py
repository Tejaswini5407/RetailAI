from typing import Dict, Any, Tuple
import pandas as pd

REQUIRED_ID_COLS = ["id", "item_id", "dept_id", "cat_id", "store_id", "state_id"]

def transform_wide_to_long(
    sales_df: pd.DataFrame,
    calendar_df: pd.DataFrame
) -> pd.DataFrame:
    """
    Transform wide-format M5 sales data (d_1, d_2, ...) into a long-format DataFrame
    and merge calendar date attributes.
    
    Observation Grain: (store_id, item_id, date)
    """
    day_cols = [c for c in sales_df.columns if c.startswith("d_")]
    id_cols = [c for c in sales_df.columns if c in REQUIRED_ID_COLS]
    
    # Melt wide day columns to long format
    long_df = pd.melt(
        sales_df,
        id_vars=id_cols,
        value_vars=day_cols,
        var_name="d",
        value_name="units_sold"
    )
    
    # Merge calendar metadata
    calendar_cols = [
        "d", "date", "wm_yr_wk", "weekday", "wday", "month", "year",
        "event_name_1", "event_type_1", "snap_CA", "snap_TX", "snap_WI"
    ]
    calendar_subset = calendar_df[calendar_cols].copy()
    
    merged_df = pd.merge(long_df, calendar_subset, on="d", how="inner")
    merged_df["date"] = pd.to_datetime(merged_df["date"])
    
    # Sort chronologically per store & product
    merged_df = merged_df.sort_values(by=["store_id", "item_id", "date"]).reset_index(drop=True)
    
    # Verify observation grain uniqueness
    duplicate_mask = merged_df.duplicated(subset=["store_id", "item_id", "date"])
    if duplicate_mask.any():
        dup_count = int(duplicate_mask.sum())
        raise ValueError(f"Transformation created {dup_count} duplicate (store_id, item_id, date) records.")
        
    return merged_df

def merge_sell_prices(
    long_df: pd.DataFrame,
    prices_df: pd.DataFrame
) -> pd.DataFrame:
    """
    Merge weekly item pricing into the long-format DataFrame using (store_id, item_id, wm_yr_wk).
    Validates row count integrity and handles pre-launch missing prices via group-level fill.
    """
    initial_row_count = len(long_df)
    
    price_subset = prices_df[["store_id", "item_id", "wm_yr_wk", "sell_price"]].drop_duplicates()
    
    merged_df = pd.merge(
        long_df,
        price_subset,
        on=["store_id", "item_id", "wm_yr_wk"],
        how="left"
    )
    
    if len(merged_df) != initial_row_count:
        raise ValueError(
            f"Price merge altered row count: expected {initial_row_count}, got {len(merged_df)}."
        )
        
    # Handle missing sell prices for weeks prior to official product launch
    if merged_df["sell_price"].isnull().any():
        merged_df["sell_price"] = (
            merged_df.groupby(["store_id", "item_id"])["sell_price"]
            .transform(lambda g: g.ffill().bfill())
        )
        
    return merged_df
