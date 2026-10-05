from typing import List, Tuple
import pandas as pd

EXCLUDED_COLUMNS = [
    "id", "date", "d", "units_sold", "event_name_1", "event_type_1", "weekday", "wday"
]

NUMERICAL_FEATURES = [
    "sell_price", "price_change_pct", "price_rel_4w_avg",
    "day_of_week", "day_of_month", "week_of_year", "month", "quarter", "year",
    "is_weekend", "is_event_day", "is_snap_day",
    "snap_CA", "snap_TX", "snap_WI",
    "lag_1", "lag_7", "lag_14", "lag_28",
    "rolling_mean_7", "rolling_std_7",
    "rolling_mean_14", "rolling_std_14",
    "rolling_mean_28", "rolling_std_28"
]

CATEGORICAL_FEATURES = ["store_id", "state_id", "cat_id", "dept_id", "item_id"]

def prepare_model_features(
    df: pd.DataFrame
) -> Tuple[pd.DataFrame, pd.Series, List[str]]:
    """
    Select predictive features and apply one-hot / categorical encoding for tree models.
    
    Returns:
        (X, y, feature_names)
    """
    target = df["units_sold"].copy()
    
    # One-hot encode low-cardinality categoricals (store_id, state_id, cat_id, dept_id)
    low_card_cats = ["store_id", "state_id", "cat_id", "dept_id"]
    encoded_df = pd.get_dummies(df[low_card_cats], prefix=low_card_cats, drop_first=False, dtype=int)
    
    # Categorical code encoding for item_id (50 items) to preserve item identity without explosion
    item_codes = df["item_id"].astype("category").cat.codes.rename("item_code")
    
    # Combine numerical features, one-hot encoded features, and item codes
    num_df = df[[col for col in NUMERICAL_FEATURES if col in df.columns]].copy()
    
    X = pd.concat([num_df, item_codes, encoded_df], axis=1)
    
    feature_names = list(X.columns)
    return X, target, feature_names
