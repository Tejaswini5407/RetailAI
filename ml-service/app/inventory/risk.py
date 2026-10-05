from typing import Tuple

def evaluate_stockout_risk(
    current_inventory: float,
    total_forecast_demand: float,
    reorder_point: float
) -> str:
    """
    Classify stockout risk level (HIGH, MEDIUM, LOW).
    
    Rules:
    - HIGH: Current inventory is less than total near-term forecast demand.
    - MEDIUM: Current inventory covers near-term demand, but is less than the reorder point.
    - LOW: Current inventory meets or exceeds the reorder point threshold.
    """
    if current_inventory < total_forecast_demand:
        return "HIGH"
    elif current_inventory < reorder_point:
        return "MEDIUM"
    else:
        return "LOW"

def evaluate_inventory_status(
    current_inventory: float,
    total_forecast_demand: float,
    reorder_point: float,
    target_inventory: float
) -> str:
    """
    Classify overall operational inventory status (CRITICAL, REORDER, WATCH, HEALTHY).
    
    Rules:
    - CRITICAL: Current stock is below total near-term forecast demand (Stockout Risk = HIGH).
    - REORDER: Current stock is below reorder point (Needs replenishment order).
    - WATCH: Current stock meets reorder point, but buffer is tight (< 1.2 * target_inventory).
    - HEALTHY: Current stock is comfortably stocked (>= 1.2 * target_inventory).
    """
    if current_inventory < total_forecast_demand:
        return "CRITICAL"
    elif current_inventory < reorder_point:
        return "REORDER"
    elif current_inventory < (target_inventory * 1.2):
        return "WATCH"
    else:
        return "HEALTHY"
