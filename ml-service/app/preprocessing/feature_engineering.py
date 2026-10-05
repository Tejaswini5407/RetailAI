from typing import List
import numpy as np
import pandas as pd

DEFAULT_LAGS = [1, 7, 14, 28]
DEFAULT_WINDOWS = [7, 14, 28]

def create_date_features(df: pd.DataFrame) -> pd.DataFrame:
    """Generate calendar date-based features from datetime column."""
    df["day_of_week"] = df["date"].dt.dayofweek.astype(int)
    df["day_of_month"] = df["date"].dt.day.astype(int)
    df["week_of_year"] = df["date"].dt.isocalendar().week.astype(int)
    df["month"] = df["date"].dt.month.astype(int)
    df["quarter"] = df["date"].dt.quarter.astype(int)
    df["year"] = df["date"].dt.year.astype(int)
    df["is_weekend"] = (df["day_of_week"] >= 5).astype(int)
    return df

def create_event_features(df: pd.DataFrame) -> pd.DataFrame:
    """Generate event indicators and store-specific SNAP features."""
    df["is_event_day"] = df["event_name_1"].notnull().astype(int)
    
    # Map state-specific SNAP marker based on store state
    snap_conditions = [
        df["state_id"] == "CA",
        df["state_id"] == "TX",
        df["state_id"] == "WI"
    ]
    snap_choices = [
        df["snap_CA"],
        df["snap_TX"],
        df["snap_WI"]
    ]
    df["is_snap_day"] = np.select(snap_conditions, snap_choices, default=0).astype(int)
    return df

def create_price_features(df: pd.DataFrame) -> pd.DataFrame:
    """Generate price trend and price momentum features per store & product."""
    grouped_price = df.groupby(["store_id", "item_id"])["sell_price"]
    
    # Weekly percentage change in selling price
    df["price_change_pct"] = grouped_price.pct_change().fillna(0.0).round(4)
    
    # Price ratio relative to 4-week (28-day) moving average price
    rolling_4w_price = grouped_price.transform(lambda g: g.rolling(28, min_periods=1).mean())
    df["price_rel_4w_avg"] = (df["sell_price"] / rolling_4w_price).fillna(1.0).round(4)
    return df

def create_lag_features(
    df: pd.DataFrame,
    lags: List[int] = DEFAULT_LAGS
) -> pd.DataFrame:
    """
    Generate historical demand lag features per (store_id, item_id) group.
    
    lag_k on day t represents sales at day (t - k).
    """
    grouped_sales = df.groupby(["store_id", "item_id"])["units_sold"]
    
    for lag in lags:
        df[f"lag_{lag}"] = grouped_sales.shift(lag)
        
    return df

def create_rolling_features(
    df: pd.DataFrame,
    windows: List[int] = DEFAULT_WINDOWS
) -> pd.DataFrame:
    """
    Generate historical rolling mean and standard deviation features.
    
    LEAKAGE PREVENTION RULE:
    Target 'units_sold' on day t is SHIFTED BY 1 DAY before rolling calculations.
    Therefore, rolling_mean_w on day t is mean(units_sold[t-w : t-1]).
    """
    # Shift sales by 1 day so today's target is excluded from rolling statistics
    shifted_sales = df.groupby(["store_id", "item_id"])["units_sold"].shift(1)
    grouped_shifted = shifted_sales.groupby([df["store_id"], df["item_id"]])
    
    for window in windows:
        df[f"rolling_mean_{window}"] = (
            grouped_shifted.transform(lambda g: g.rolling(window).mean()).round(4)
        )
        df[f"rolling_std_{window}"] = (
            grouped_shifted.transform(lambda g: g.rolling(window).std()).fillna(0.0).round(4)
        )
        
    return df

def build_feature_pipeline(
    long_df: pd.DataFrame,
    lags: List[int] = DEFAULT_LAGS,
    windows: List[int] = DEFAULT_WINDOWS
) -> pd.DataFrame:
    """Execute complete feature engineering pipeline chronologically."""
    df = long_df.copy()
    
    # Ensure chronological order
    df = df.sort_values(by=["store_id", "item_id", "date"]).reset_index(drop=True)
    
    df = create_date_features(df)
    df = create_event_features(df)
    df = create_price_features(df)
    df = create_lag_features(df, lags=lags)
    df = create_rolling_features(df, windows=windows)
    
    return df
