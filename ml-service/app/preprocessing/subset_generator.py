from typing import List, Optional
import os
import pandas as pd

DEFAULT_TARGET_STORES = ["CA_1", "TX_1"]
DEFAULT_ITEM_LIMIT = 50

def generate_development_subset(
    sales_df: pd.DataFrame,
    target_stores: Optional[List[str]] = None,
    item_limit: int = DEFAULT_ITEM_LIMIT,
    max_days: Optional[int] = 730
) -> pd.DataFrame:
    """
    Generate a deterministic, reproducible development subset of the sales data.
    
    Selection Strategy:
    - Stores: Filtered to specific target stores (default: CA_1 and TX_1).
    - Items: Top `item_limit` products by total historical volume within selected stores.
    - Time Horizon: Last `max_days` daily observations (default: 730 days ~ 2 years) for fast iteration.
    """
    stores = target_stores or DEFAULT_TARGET_STORES
    filtered_df = sales_df[sales_df["store_id"].isin(stores)].copy()
    
    day_columns = [c for c in filtered_df.columns if c.startswith("d_")]
    if max_days and len(day_columns) > max_days:
        day_columns = day_columns[-max_days:]
        
    filtered_df["total_sales"] = filtered_df[day_columns].sum(axis=1)
    
    # Get top items deterministically sorted by item_id
    top_items = (
        filtered_df.groupby("item_id")["total_sales"]
        .sum()
        .nlargest(item_limit)
        .index.tolist()
    )
    
    subset_df = filtered_df[filtered_df["item_id"].isin(top_items)].copy()
    
    keep_columns = ["id", "item_id", "dept_id", "cat_id", "store_id", "state_id"] + day_columns
    subset_df = subset_df[keep_columns].sort_values(by=["store_id", "item_id"]).reset_index(drop=True)
    
    return subset_df

def save_development_subset(subset_df: pd.DataFrame, output_path: str) -> str:
    """Save the generated development subset to CSV."""
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    subset_df.to_csv(output_path, index=False)
    return output_path
