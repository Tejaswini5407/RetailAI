from typing import List, Tuple, Union
import numpy as np

def aggregate_forecast_demand(
    forecast_demand: Union[float, List[float], np.ndarray],
    horizon_days: int = 7
) -> Tuple[float, float, int]:
    """
    Aggregate forecast demand into total forecast demand and average daily demand.
    
    Returns:
        (total_forecast_demand, average_daily_demand, actual_horizon_days)
    """
    if isinstance(forecast_demand, (list, np.ndarray)):
        arr = np.asarray(forecast_demand, dtype=float)
        total_demand = float(np.sum(arr))
        actual_horizon = len(arr) if len(arr) > 0 else horizon_days
    else:
        total_demand = float(forecast_demand)
        actual_horizon = horizon_days
        
    avg_daily_demand = total_demand / actual_horizon if actual_horizon > 0 else 0.0
    return round(total_demand, 4), round(avg_daily_demand, 4), actual_horizon
