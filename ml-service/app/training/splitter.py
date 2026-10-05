from typing import Tuple, Dict, Any
import pandas as pd

def temporal_train_val_split(
    df: pd.DataFrame,
    val_days: int = 28
) -> Tuple[pd.DataFrame, pd.DataFrame, Dict[str, Any]]:
    """
    Split time-series DataFrame into chronological training and validation sets.
    
    Validation Window: Last `val_days` continuous dates (default: 28 days ~ 4-week forecast horizon).
    Ensures max(train_date) < min(val_date) with zero temporal overlap.
    """
    sorted_df = df.sort_values(by="date").reset_index(drop=True)
    
    unique_dates = sorted_df["date"].drop_duplicates().sort_values()
    if len(unique_dates) <= val_days:
        raise ValueError(f"Insufficient distinct dates ({len(unique_dates)}) for {val_days}-day validation split.")
        
    val_cutoff_date = unique_dates.iloc[-val_days]
    
    train_df = sorted_df[sorted_df["date"] < val_cutoff_date].copy()
    val_df = sorted_df[sorted_df["date"] >= val_cutoff_date].copy()
    
    train_max_date = train_df["date"].max()
    val_min_date = val_df["date"].min()
    
    if train_max_date >= val_min_date:
        raise ValueError(
            f"Temporal split failure: max(train)={train_max_date} >= min(val)={val_min_date}"
        )
        
    split_info = {
        "val_days": val_days,
        "train_start": train_df["date"].min().strftime("%Y-%m-%d"),
        "train_end": train_max_date.strftime("%Y-%m-%d"),
        "val_start": val_min_date.strftime("%Y-%m-%d"),
        "val_end": val_df["date"].max().strftime("%Y-%m-%d"),
        "train_rows": len(train_df),
        "val_rows": len(val_df),
        "train_series_count": train_df["id"].nunique(),
        "val_series_count": val_df["id"].nunique()
    }
    
    return train_df, val_df, split_info
