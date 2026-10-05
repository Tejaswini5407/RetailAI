import os
import sys
import numpy as np
import pandas as pd
import pytest

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from app.evaluation.metrics import calculate_wape, calculate_mae, calculate_rmse, evaluate_predictions
from app.features.feature_selector import prepare_model_features
from app.models.baseline import SeasonalNaiveBaseline
from app.models.random_forest import RandomForestForecaster
from app.models.xgboost_model import XGBoostForecaster
from app.training.splitter import temporal_train_val_split

def test_metrics_correctness():
    actual = np.array([10.0, 20.0, 30.0, 0.0])
    pred = np.array([12.0, 18.0, 33.0, 0.0])
    
    wape = calculate_wape(actual, pred)
    mae = calculate_mae(actual, pred)
    rmse = calculate_rmse(actual, pred)
    
    assert np.isclose(wape, 7.0 / 60.0, atol=1e-4)
    assert np.isclose(mae, 1.75, atol=1e-4)
    assert np.isclose(rmse, np.sqrt(4.25), atol=1e-4)

def test_zero_denominator_wape():
    actual = np.array([0.0, 0.0, 0.0])
    pred = np.array([0.0, 0.0, 0.0])
    wape = calculate_wape(actual, pred)
    assert wape == 0.0

def test_temporal_train_val_split():
    dates = pd.date_range("2020-01-01", periods=100, freq="D")
    df = pd.DataFrame({
        "id": ["item1_store1"] * 100,
        "store_id": ["store1"] * 100,
        "item_id": ["item1"] * 100,
        "date": dates,
        "units_sold": np.random.randint(0, 10, 100)
    })
    
    train_df, val_df, info = temporal_train_val_split(df, val_days=20)
    
    assert len(val_df["date"].unique()) == 20
    assert len(train_df["date"].unique()) == 80
    assert train_df["date"].max() < val_df["date"].min()

def test_feature_preparation_excludes_target():
    df = pd.DataFrame({
        "id": ["s1_i1"],
        "store_id": ["s1"],
        "state_id": ["CA"],
        "cat_id": ["c1"],
        "dept_id": ["d1"],
        "item_id": ["i1"],
        "date": pd.to_datetime(["2020-01-01"]),
        "d": ["d_1"],
        "units_sold": [5],
        "sell_price": [2.5],
        "day_of_week": [0],
        "lag_1": [3],
        "rolling_mean_7": [4.0]
    })
    
    X, y, feature_names = prepare_model_features(df)
    
    assert "units_sold" not in feature_names
    assert "date" not in feature_names
    assert "id" not in feature_names
    assert y.iloc[0] == 5

def test_prediction_shapes_and_equality():
    dates = pd.date_range("2020-01-01", periods=50, freq="D")
    df = pd.DataFrame({
        "id": ["s1_i1"] * 50,
        "store_id": ["s1"] * 50,
        "state_id": ["CA"] * 50,
        "cat_id": ["c1"] * 50,
        "dept_id": ["d1"] * 50,
        "item_id": ["i1"] * 50,
        "date": dates,
        "d": [f"d_{i+1}" for i in range(50)],
        "units_sold": np.random.randint(0, 10, 50),
        "sell_price": [2.5] * 50,
        "day_of_week": [d.dayofweek for d in dates],
        "day_of_month": [d.day for d in dates],
        "week_of_year": [d.isocalendar().week for d in dates],
        "month": [d.month for d in dates],
        "quarter": [d.quarter for d in dates],
        "year": [d.year for d in dates],
        "is_weekend": [int(d.dayofweek >= 5) for d in dates],
        "is_event_day": [0] * 50,
        "is_snap_day": [0] * 50,
        "snap_CA": [0] * 50,
        "snap_TX": [0] * 50,
        "snap_WI": [0] * 50,
        "price_change_pct": [0.0] * 50,
        "price_rel_4w_avg": [1.0] * 50,
        "lag_1": np.random.randint(0, 10, 50),
        "lag_7": np.random.randint(0, 10, 50),
        "lag_14": np.random.randint(0, 10, 50),
        "lag_28": np.random.randint(0, 10, 50),
        "rolling_mean_7": np.random.rand(50) * 5,
        "rolling_std_7": np.random.rand(50),
        "rolling_mean_14": np.random.rand(50) * 5,
        "rolling_std_14": np.random.rand(50),
        "rolling_mean_28": np.random.rand(50) * 5,
        "rolling_std_28": np.random.rand(50)
    })
    
    train_df, val_df, _ = temporal_train_val_split(df, val_days=10)
    X_train, y_train, _ = prepare_model_features(train_df)
    X_val, y_val, _ = prepare_model_features(val_df)
    
    baseline = SeasonalNaiveBaseline(lag_col="lag_7")
    pred_base = baseline.predict(val_df)
    
    rf = RandomForestForecaster(n_estimators=10, max_depth=4, random_state=42)
    rf.fit(X_train, y_train)
    pred_rf = rf.predict(X_val)
    
    xgb = XGBoostForecaster(n_estimators=10, max_depth=3, random_state=42)
    xgb.fit(X_train, y_train)
    pred_xgb = xgb.predict(X_val)
    
    # Assert exact prediction shape equality across all models and validation target
    assert len(y_val) == len(pred_rf) == len(pred_xgb) == len(pred_base) == 10
    assert not np.isnan(pred_rf).any()
    assert not np.isnan(pred_xgb).any()
    assert not np.isnan(pred_base).any()
    assert (pred_rf >= 0).all()
    assert (pred_xgb >= 0).all()
