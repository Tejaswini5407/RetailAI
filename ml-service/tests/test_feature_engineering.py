import os
import sys
import pandas as pd
import pytest

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from app.preprocessing.transformer import transform_wide_to_long, merge_sell_prices
from app.preprocessing.feature_engineering import (
    create_date_features,
    create_event_features,
    create_price_features,
    create_lag_features,
    create_rolling_features
)

def test_wide_to_long_transform():
    sales_mock = pd.DataFrame({
        "id": ["item1_store1_validation"],
        "item_id": ["item1"],
        "dept_id": ["dept1"],
        "cat_id": ["cat1"],
        "store_id": ["store1"],
        "state_id": ["CA"],
        "d_1": [10],
        "d_2": [15]
    })
    calendar_mock = pd.DataFrame({
        "d": ["d_1", "d_2"],
        "date": ["2016-01-01", "2016-01-02"],
        "wm_yr_wk": [11601, 11601],
        "weekday": ["Friday", "Saturday"],
        "wday": [7, 1],
        "month": [1, 1],
        "year": [2016, 2016],
        "event_name_1": [None, "NewYear"],
        "event_type_1": [None, "National"],
        "snap_CA": [0, 1],
        "snap_TX": [0, 0],
        "snap_WI": [0, 0]
    })
    
    long_df = transform_wide_to_long(sales_mock, calendar_mock)
    assert len(long_df) == 2
    assert "units_sold" in long_df.columns
    assert long_df.loc[0, "units_sold"] == 10
    assert long_df.loc[1, "units_sold"] == 15

def test_price_merge():
    long_mock = pd.DataFrame({
        "store_id": ["store1", "store1"],
        "item_id": ["item1", "item1"],
        "wm_yr_wk": [11601, 11602],
        "d": ["d_1", "d_2"],
        "units_sold": [5, 10]
    })
    prices_mock = pd.DataFrame({
        "store_id": ["store1", "store1"],
        "item_id": ["item1", "item1"],
        "wm_yr_wk": [11601, 11602],
        "sell_price": [2.50, 2.75]
    })
    
    merged = merge_sell_prices(long_mock, prices_mock)
    assert len(merged) == 2
    assert "sell_price" in merged.columns
    assert merged.loc[0, "sell_price"] == 2.50
    assert merged.loc[1, "sell_price"] == 2.75

def test_date_and_event_features():
    df = pd.DataFrame({
        "date": pd.to_datetime(["2016-01-01", "2016-01-02"]),
        "event_name_1": [None, "NewYear"],
        "state_id": ["CA", "CA"],
        "snap_CA": [0, 1],
        "snap_TX": [0, 0],
        "snap_WI": [0, 0]
    })
    df = create_date_features(df)
    df = create_event_features(df)
    
    assert "day_of_week" in df.columns
    assert "is_weekend" in df.columns
    assert df.loc[0, "is_weekend"] == 0
    assert df.loc[1, "is_weekend"] == 1
    assert df.loc[0, "is_event_day"] == 0
    assert df.loc[1, "is_event_day"] == 1

def test_state_specific_snap_logic():
    """Verify that is_snap_day correctly evaluates store state (CA -> snap_CA, TX -> snap_TX, WI -> snap_WI)."""
    df = pd.DataFrame({
        "state_id": ["CA", "TX", "WI", "TX"],
        "event_name_1": [None, None, None, None],
        "snap_CA": [1, 1, 0, 0],  # Note: snap_CA is 1 on row 1, but row 1 state is TX
        "snap_TX": [0, 1, 0, 0],
        "snap_WI": [0, 0, 1, 0]
    })
    
    res = create_event_features(df)
    
    # Row 0: State CA, snap_CA=1 -> is_snap_day must be 1
    assert res.loc[0, "is_snap_day"] == 1
    
    # Row 1: State TX, snap_TX=1 (even though snap_CA=1) -> is_snap_day must be 1 (derived from snap_TX)
    assert res.loc[1, "is_snap_day"] == 1
    
    # Row 2: State WI, snap_WI=1 -> is_snap_day must be 1
    assert res.loc[2, "is_snap_day"] == 1
    
    # Row 3: State TX, snap_TX=0 (even if snap_CA was 1) -> is_snap_day must be 0
    assert res.loc[3, "is_snap_day"] == 0
