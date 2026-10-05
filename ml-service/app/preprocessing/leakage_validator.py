from typing import Dict, Any
import numpy as np
import pandas as pd

def validate_observation_grain_uniqueness(df: pd.DataFrame) -> bool:
    """Verify that (store_id, item_id, date) key is 100% unique."""
    return not df.duplicated(subset=["store_id", "item_id", "date"]).any()

def validate_lag_correctness(df: pd.DataFrame) -> Dict[str, Any]:
    """
    Verify Test A: Lags match exact historical target values for the same product and store.
    lag_1(t) == units_sold(t-1)
    lag_7(t) == units_sold(t-7)
    """
    # Pick a random clean series
    sample_store = df["store_id"].iloc[0]
    sample_item = df["item_id"].iloc[0]
    
    series_df = (
        df[(df["store_id"] == sample_store) & (df["item_id"] == sample_item)]
        .sort_values(by="date")
        .reset_index(drop=True)
    )
    
    # Check for row index 30 (where all lags are populated)
    idx = 30
    current_date = series_df.loc[idx, "date"]
    
    lag_1_val = series_df.loc[idx, "lag_1"]
    actual_prev_1_val = series_df.loc[idx - 1, "units_sold"]
    
    lag_7_val = series_df.loc[idx, "lag_7"]
    actual_prev_7_val = series_df.loc[idx - 7, "units_sold"]
    
    lag_28_val = series_df.loc[idx, "lag_28"]
    actual_prev_28_val = series_df.loc[idx - 28, "units_sold"]
    
    lag_1_correct = (lag_1_val == actual_prev_1_val)
    lag_7_correct = (lag_7_val == actual_prev_7_val)
    lag_28_correct = (lag_28_val == actual_prev_28_val)
    
    return {
        "sample_series": f"{sample_store}_{sample_item}",
        "evaluation_date": str(current_date),
        "lag_1_valid": bool(lag_1_correct),
        "lag_7_valid": bool(lag_7_correct),
        "lag_28_valid": bool(lag_28_correct),
        "all_lags_valid": bool(lag_1_correct and lag_7_correct and lag_28_correct)
    }

def validate_rolling_correctness(df: pd.DataFrame) -> Dict[str, Any]:
    """
    Verify Test B: Rolling mean uses ONLY previous days (t-7 to t-1).
    Current day's target units_sold(t) MUST NOT be included.
    """
    sample_store = df["store_id"].iloc[0]
    sample_item = df["item_id"].iloc[0]
    
    series_df = (
        df[(df["store_id"] == sample_store) & (df["item_id"] == sample_item)]
        .sort_values(by="date")
        .reset_index(drop=True)
    )
    
    idx = 35
    current_target = series_df.loc[idx, "units_sold"]
    reported_rolling_mean_7 = series_df.loc[idx, "rolling_mean_7"]
    
    # Calculate actual mean of previous 7 days (idx-7 to idx-1)
    prev_7_sales = series_df.loc[idx-7 : idx-1, "units_sold"]
    expected_rolling_mean_7 = round(float(prev_7_sales.mean()), 4)
    
    # Calculate INCORRECT mean (including current day idx)
    incorrect_sales_including_today = series_df.loc[idx-6 : idx, "units_sold"]
    incorrect_mean_including_today = round(float(incorrect_sales_including_today.mean()), 4)
    
    is_correct = np.isclose(reported_rolling_mean_7, expected_rolling_mean_7, atol=1e-3)
    is_leaking_today = (
        np.isclose(reported_rolling_mean_7, incorrect_mean_including_today, atol=1e-3)
        and not np.isclose(expected_rolling_mean_7, incorrect_mean_including_today, atol=1e-3)
    )
    
    return {
        "sample_series": f"{sample_store}_{sample_item}",
        "reported_rolling_mean_7": float(reported_rolling_mean_7),
        "expected_rolling_mean_7": float(expected_rolling_mean_7),
        "rolling_valid": bool(is_correct),
        "leaking_current_day_target": bool(is_leaking_today)
    }

def validate_group_isolation(df: pd.DataFrame) -> Dict[str, Any]:
    """
    Verify Test C: Lag/rolling features for store A/item A are completely isolated
    from store B/item B.
    """
    stores = df["store_id"].unique()
    items = df["item_id"].unique()
    
    if len(stores) < 2 or len(items) < 1:
        return {"group_isolation_valid": True, "note": "Single group dataset"}
        
    s1, s2 = stores[0], stores[1]
    item = items[0]
    
    df_s1 = df[(df["store_id"] == s1) & (df["item_id"] == item)].sort_values("date").reset_index(drop=True)
    df_s2 = df[(df["store_id"] == s2) & (df["item_id"] == item)].sort_values("date").reset_index(drop=True)
    
    # Verify s1 lag_1 matches s1 sales, not s2 sales
    lag_1_s1_idx10 = df_s1.loc[10, "lag_1"]
    sales_s1_idx9 = df_s1.loc[9, "units_sold"]
    sales_s2_idx9 = df_s2.loc[9, "units_sold"]
    
    isolated = (lag_1_s1_idx10 == sales_s1_idx9)
    contaminated = (sales_s1_idx9 != sales_s2_idx9) and (lag_1_s1_idx10 == sales_s2_idx9)
    
    return {
        "group_isolation_valid": bool(isolated and not contaminated),
        "store_1": s1,
        "store_2": s2,
        "item": item
    }

def run_full_leakage_audit(df: pd.DataFrame) -> Dict[str, Any]:
    """Run comprehensive automated data leakage test suite."""
    grain_unique = validate_observation_grain_uniqueness(df)
    lag_audit = validate_lag_correctness(df)
    rolling_audit = validate_rolling_correctness(df)
    isolation_audit = validate_group_isolation(df)
    
    all_passed = (
        grain_unique and
        lag_audit["all_lags_valid"] and
        rolling_audit["rolling_valid"] and
        not rolling_audit["leaking_current_day_target"] and
        isolation_audit["group_isolation_valid"]
    )
    
    return {
        "all_tests_passed": all_passed,
        "grain_uniqueness_valid": grain_unique,
        "lag_correctness": lag_audit,
        "rolling_correctness": rolling_audit,
        "group_isolation": isolation_audit
    }
