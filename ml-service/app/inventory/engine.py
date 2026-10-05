from .models import InventoryInput, InventoryAssessment
from .demand import aggregate_forecast_demand
from .safety_stock import (
    calculate_safety_stock,
    calculate_lead_time_demand,
    calculate_reorder_point
)
from .risk import evaluate_stockout_risk, evaluate_inventory_status

class InventoryOptimizationEngine:
    """
    Business Intelligence & Inventory Optimization Engine.
    
    Converts demand forecasts into actionable inventory decisions:
    - Total & Average Daily Forecast Demand
    - Safety Stock & Reorder Point
    - Stockout Risk Classification (HIGH, MEDIUM, LOW)
    - Order Recommendation (accounting for existing on-order and MOQ)
    - Operational Inventory Status (CRITICAL, REORDER, WATCH, HEALTHY)
    
    Note: Completely independent of ML model fitting code. Consumes forecast demand values.
    """
    
    def assess_inventory(self, inp: InventoryInput) -> InventoryAssessment:
        """Run complete inventory assessment pipeline on input parameters."""
        # 1. Validate Business Inputs
        inp.validate()
        
        # 2. Demand Aggregation
        total_demand, avg_daily_demand, horizon_days = aggregate_forecast_demand(
            inp.forecast_demand,
            inp.forecast_horizon_days
        )
        
        # 3. Safety Stock & Lead Time Demand
        safety_stock = calculate_safety_stock(
            average_daily_demand=avg_daily_demand,
            lead_time_days=inp.lead_time_days,
            service_level_z=inp.service_level_z,
            demand_std=inp.demand_std,
            safety_stock_override=inp.safety_stock_override
        )
        
        lead_time_demand = calculate_lead_time_demand(
            average_daily_demand=avg_daily_demand,
            lead_time_days=inp.lead_time_days
        )
        
        # 4. Reorder Point & Target Inventory
        reorder_point = calculate_reorder_point(lead_time_demand, safety_stock)
        target_inventory = round(float(total_demand + safety_stock), 4)
        
        # 5. Order Recommendation Calculation
        net_needed = target_inventory - inp.current_inventory - inp.existing_on_order
        raw_recommended_order = max(0.0, net_needed)
        
        # Apply MOQ logic
        if raw_recommended_order > 0.0 and inp.minimum_order_quantity > 0.0:
            recommended_order = max(raw_recommended_order, inp.minimum_order_quantity)
        else:
            recommended_order = raw_recommended_order
            
        raw_recommended_order = round(float(raw_recommended_order), 4)
        recommended_order = round(float(recommended_order), 4)
        
        # 6. Risk & Status Classification (Uses lead_time_demand for horizon-leadtime consistency)
        risk_level = evaluate_stockout_risk(
            current_inventory=inp.current_inventory,
            lead_time_demand=lead_time_demand,
            reorder_point=reorder_point
        )
        
        inv_status = evaluate_inventory_status(
            current_inventory=inp.current_inventory,
            lead_time_demand=lead_time_demand,
            reorder_point=reorder_point,
            target_inventory=target_inventory
        )
        
        return InventoryAssessment(
            store_id=inp.store_id,
            item_id=inp.item_id,
            current_inventory=inp.current_inventory,
            total_forecast_demand=total_demand,
            forecast_horizon_days=horizon_days,
            average_daily_demand=avg_daily_demand,
            lead_time_days=inp.lead_time_days,
            safety_stock=safety_stock,
            lead_time_demand=lead_time_demand,
            reorder_point=reorder_point,
            target_inventory=target_inventory,
            existing_on_order=inp.existing_on_order,
            raw_recommended_order=raw_recommended_order,
            recommended_order_quantity=recommended_order,
            minimum_order_quantity=inp.minimum_order_quantity,
            stockout_risk_level=risk_level,
            inventory_status=inv_status
        )
