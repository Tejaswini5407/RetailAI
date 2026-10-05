import math
from typing import Optional

def calculate_safety_stock(
    average_daily_demand: float,
    lead_time_days: int,
    service_level_z: float = 1.645,
    demand_std: Optional[float] = None,
    safety_stock_override: Optional[float] = None
) -> float:
    """
    Calculate safety stock buffer.
    
    Formulas:
    1. Override: Return safety_stock_override if explicitly set.
    2. Standard Formula (when demand_std is present):
       Safety Stock = z * demand_std * sqrt(lead_time_days)
    3. Simplified Fallback Formula (when demand_std is absent):
       Assumes a standard retail daily demand coefficient of variation (CV = 0.3):
       demand_std_est = 0.3 * average_daily_demand
       Safety Stock = z * (0.3 * average_daily_demand) * sqrt(lead_time_days)
    """
    if safety_stock_override is not None:
        if safety_stock_override < 0:
            raise ValueError(f"safety_stock_override cannot be negative: {safety_stock_override}")
        return float(safety_stock_override)
        
    if lead_time_days == 0 or average_daily_demand == 0:
        return 0.0
        
    if demand_std is not None:
        if demand_std < 0:
            raise ValueError(f"demand_std cannot be negative: {demand_std}")
        sigma = demand_std
    else:
        # Fallback estimation using standard retail CV = 30%
        sigma = 0.3 * average_daily_demand
        
    raw_safety_stock = service_level_z * sigma * math.sqrt(lead_time_days)
    return round(float(raw_safety_stock), 4)

def calculate_lead_time_demand(
    average_daily_demand: float,
    lead_time_days: int
) -> float:
    """Calculate expected demand during supplier lead time."""
    return round(float(average_daily_demand * lead_time_days), 4)

def calculate_reorder_point(
    lead_time_demand: float,
    safety_stock: float
) -> float:
    """
    Calculate Reorder Point threshold.
    Reorder Point = Lead Time Demand + Safety Stock
    """
    return round(float(lead_time_demand + safety_stock), 4)
