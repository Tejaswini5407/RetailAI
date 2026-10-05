import os
import sys
import numpy as np
import pandas as pd
import pytest

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from app.preprocessing.feature_engineering import create_lag_features, create_rolling_features
from app.preprocessing.leakage_validator import run_full_leakage_audit

def test_lag_and_rolling_leakage_prevention():
    # Build deterministic synthetic sequence: sales = 10, 20, 30, 40, 50, 60, 70, 80, 90, 100...
    dates = pd.date_range(start="2020-01-01", periods=10, freq="D")
    sales_values = [10, 20, 30, 40, 50, 60, 70, 80, 90, 100]
    
    df = pd.DataFrame({
        "store_id": ["store1"] * 10,
        "item_id": ["item1"] * 10,
        "date": dates,
        "units_sold": sales_values
    })
    
    df = create_lag_features(df, lags=[1, 7])
    df = create_rolling_features(df, windows=[3])
    
    # Test A: Lag Correctness
    # On day 3 (sales=40), lag_1 must equal previous day's sales (30)
    assert df.loc[3, "lag_1"] == 30
    assert df.loc[7, "lag_7"] == 10
    
    # Test B: Rolling Correctness (Target Leakage Check)
    # On day 3 (index 3, sales=40):
    # Previous 3 days sales are index 0 (10), index 1 (20), index 2 (30).
    # Expected rolling_mean_3 = (10 + 20 + 30) / 3 = 20.0
    # Current day sales (40) MUST NOT be included!
    assert df.loc[3, "rolling_mean_3"] == 20.0
    
    # If today's target (40) were leaked, the mean would be (20 + 30 + 40) / 3 = 30.0!
    assert df.loc[3, "rolling_mean_3"] != 30.0

def test_group_isolation():
    dates = pd.date_range(start="2020-01-01", periods=5, freq="D")
    df = pd.DataFrame({
        "store_id": ["store1"] * 5 + ["store2"] * 5,
        "item_id": ["item1"] * 10,
        "date": list(dates) + list(dates),
        "units_sold": [10, 20, 30, 40, 50] + [1000, 2000, 3000, 4000, 5000]
    })
    
    df = create_lag_features(df, lags=[1])
    
    # Store 1 day 1 lag_1 must be Store 1 day 0 (10), NOT NaN or Store 2 value
    df_s1 = df[df["store_id"] == "store1"].reset_index(drop=True)
    df_s2 = df[df["store_id"] == "store2"].reset_index(drop=True)
    
    assert df_s1.loc[1, "lag_1"] == 10
    assert df_s2.loc[1, "lag_1"] == 1000
    assert np.isnan(df_s2.loc[0, "lag_1"])
