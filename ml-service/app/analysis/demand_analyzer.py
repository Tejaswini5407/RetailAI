from typing import Dict, Any
import numpy as np
import pandas as pd

def analyze_target_distribution(sales_df: pd.DataFrame) -> Dict[str, Any]:
    """Analyze the distribution of units_sold across the wide-format sales dataset."""
    day_columns = [col for col in sales_df.columns if col.startswith("d_")]
    sales_values = sales_df[day_columns].values.flatten()
    
    total_observations = len(sales_values)
    zero_sales_count = int(np.sum(sales_values == 0))
    zero_sales_percentage = float(zero_sales_count / total_observations * 100)
    
    return {
        "total_observations": int(total_observations),
        "min_units_sold": int(np.min(sales_values)),
        "max_units_sold": int(np.max(sales_values)),
        "mean_units_sold": float(np.mean(sales_values)),
        "median_units_sold": float(np.median(sales_values)),
        "std_units_sold": float(np.std(sales_values)),
        "zero_demand_count": zero_sales_count,
        "zero_demand_percentage": round(zero_sales_percentage, 2),
        "non_zero_demand_percentage": round(100 - zero_sales_percentage, 2)
    }

def analyze_categorical_dimensions(sales_df: pd.DataFrame) -> Dict[str, Any]:
    """Analyze unique counts and dimensions of stores, products, categories, departments, and states."""
    return {
        "unique_stores": int(sales_df["store_id"].nunique()),
        "unique_products": int(sales_df["item_id"].nunique()),
        "unique_categories": int(sales_df["cat_id"].nunique()),
        "unique_departments": int(sales_df["dept_id"].nunique()),
        "unique_states": int(sales_df["state_id"].nunique()),
        "stores_list": sorted(sales_df["store_id"].unique().tolist()),
        "categories_list": sorted(sales_df["cat_id"].unique().tolist()),
        "departments_list": sorted(sales_df["dept_id"].unique().tolist()),
        "states_list": sorted(sales_df["state_id"].unique().tolist())
    }
