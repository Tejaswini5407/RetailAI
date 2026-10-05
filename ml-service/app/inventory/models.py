from dataclasses import dataclass
from typing import List, Optional, Union
import numpy as np

@dataclass
class InventoryInput:
    """Input parameters for inventory assessment and replenishment recommendation."""
    store_id: str
    item_id: str
    current_inventory: float
    forecast_demand: Union[float, List[float], np.ndarray]
    forecast_horizon_days: int = 7
    lead_time_days: int = 7
    safety_stock_override: Optional[float] = None
    existing_on_order: float = 0.0
    minimum_order_quantity: float = 0.0
    service_level_z: float = 1.645  # 95% service level factor
    demand_std: Optional[float] = None

    def validate(self):
        """Validate input constraints and raise clear ValueErrors on bad business inputs."""
        if self.current_inventory < 0:
            raise ValueError(f"current_inventory cannot be negative: {self.current_inventory}")
            
        if self.lead_time_days < 0:
            raise ValueError(f"lead_time_days cannot be negative: {self.lead_time_days}")
            
        if self.existing_on_order < 0:
            raise ValueError(f"existing_on_order cannot be negative: {self.existing_on_order}")
            
        if self.minimum_order_quantity < 0:
            raise ValueError(f"minimum_order_quantity cannot be negative: {self.minimum_order_quantity}")
            
        if self.service_level_z <= 0:
            raise ValueError(f"service_level_z must be positive: {self.service_level_z}")
            
        if self.forecast_horizon_days <= 0:
            raise ValueError(f"forecast_horizon_days must be positive: {self.forecast_horizon_days}")
            
        if isinstance(self.forecast_demand, (list, np.ndarray)):
            arr = np.asarray(self.forecast_demand, dtype=float)
            if (arr < 0).any():
                raise ValueError("forecast_demand contains negative values.")
        elif isinstance(self.forecast_demand, (int, float)):
            if self.forecast_demand < 0:
                raise ValueError(f"forecast_demand cannot be negative: {self.forecast_demand}")
        else:
            raise TypeError("forecast_demand must be float, list, or numpy array.")

@dataclass
class InventoryAssessment:
    """Result payload from the Inventory Optimization Engine."""
    store_id: str
    item_id: str
    current_inventory: float
    total_forecast_demand: float
    forecast_horizon_days: int
    average_daily_demand: float
    lead_time_days: int
    safety_stock: float
    lead_time_demand: float
    reorder_point: float
    target_inventory: float
    existing_on_order: float
    raw_recommended_order: float
    recommended_order_quantity: float
    minimum_order_quantity: float
    stockout_risk_level: str  # HIGH, MEDIUM, LOW
    inventory_status: str     # CRITICAL, REORDER, WATCH, HEALTHY
