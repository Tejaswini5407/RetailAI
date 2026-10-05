import pytest
import numpy as np

from app.inventory.models import InventoryInput, InventoryAssessment
from app.inventory.demand import aggregate_forecast_demand
from app.inventory.safety_stock import (
    calculate_safety_stock,
    calculate_lead_time_demand,
    calculate_reorder_point
)
from app.inventory.risk import evaluate_stockout_risk, evaluate_inventory_status
from app.inventory.engine import InventoryOptimizationEngine

@pytest.fixture
def engine():
    return InventoryOptimizationEngine()

def test_forecast_demand_aggregation():
    demand_list = [10.0, 12.0, 15.0, 11.0, 13.0, 14.0, 16.0]
    total, avg_daily, horizon = aggregate_forecast_demand(demand_list, horizon_days=7)
    assert total == 91.0
    assert avg_daily == 13.0
    assert horizon == 7
    
    total_scalar, avg_scalar, horizon_scalar = aggregate_forecast_demand(540.0, horizon_days=7)
    assert total_scalar == 540.0
    assert np.isclose(avg_scalar, 540.0 / 7, atol=1e-3)

def test_safety_stock_and_reorder_point():
    ss_override = calculate_safety_stock(average_daily_demand=10.0, lead_time_days=5, safety_stock_override=100.0)
    assert ss_override == 100.0
    
    ss_std = calculate_safety_stock(average_daily_demand=10.0, lead_time_days=4, service_level_z=1.645, demand_std=2.0)
    assert np.isclose(ss_std, 6.58, atol=1e-3)
    
    ltd = calculate_lead_time_demand(average_daily_demand=20.0, lead_time_days=5)
    assert ltd == 100.0
    
    rop = calculate_reorder_point(lead_time_demand=100.0, safety_stock=30.0)
    assert rop == 130.0

def test_order_recommendation_scenarios(engine):
    # Scenario A: Low Stock -> Order 520
    inp_a = InventoryInput(
        store_id="CA_1",
        item_id="item_a",
        current_inventory=120,
        forecast_demand=540,
        forecast_horizon_days=7,
        lead_time_days=5,
        safety_stock_override=100,
        existing_on_order=0
    )
    res_a = engine.assess_inventory(inp_a)
    assert res_a.target_inventory == 640.0
    assert res_a.raw_recommended_order == 520.0
    assert res_a.recommended_order_quantity == 520.0
    assert res_a.stockout_risk_level == "HIGH"
    assert res_a.inventory_status == "CRITICAL"

    # Scenario B: High Stock -> Order 0
    inp_b = InventoryInput(
        store_id="CA_1",
        item_id="item_b",
        current_inventory=800,
        forecast_demand=540,
        forecast_horizon_days=7,
        lead_time_days=5,
        safety_stock_override=100
    )
    res_b = engine.assess_inventory(inp_b)
    assert res_b.recommended_order_quantity == 0.0
    assert res_b.stockout_risk_level == "LOW"
    assert res_b.inventory_status == "HEALTHY"

def test_three_required_horizon_scenarios(engine):
    # Scenario 1: horizon=7, lead_time=7
    inp_1 = InventoryInput("S1", "I1", current_inventory=120, forecast_demand=540, forecast_horizon_days=7, lead_time_days=7, safety_stock_override=100)
    res_1 = engine.assess_inventory(inp_1)
    assert res_1.total_forecast_demand == 540.0
    assert np.isclose(res_1.average_daily_demand, 77.1429, atol=1e-3)
    assert np.isclose(res_1.lead_time_demand, 540.0, atol=1e-2)
    assert np.isclose(res_1.reorder_point, 640.0, atol=1e-2)
    assert res_1.stockout_risk_level == "HIGH"
    assert res_1.recommended_order_quantity == 520.0

    # Scenario 2: horizon=30, lead_time=7
    inp_2 = InventoryInput("S1", "I1", current_inventory=120, forecast_demand=540, forecast_horizon_days=30, lead_time_days=7, safety_stock_override=100)
    res_2 = engine.assess_inventory(inp_2)
    assert res_2.total_forecast_demand == 540.0
    assert res_2.average_daily_demand == 18.0
    assert res_2.lead_time_demand == 126.0
    assert res_2.reorder_point == 226.0
    assert res_2.stockout_risk_level == "HIGH"
    assert res_2.recommended_order_quantity == 520.0

    # Scenario 3: horizon=7, lead_time=30
    inp_3 = InventoryInput("S1", "I1", current_inventory=120, forecast_demand=540, forecast_horizon_days=7, lead_time_days=30, safety_stock_override=100)
    res_3 = engine.assess_inventory(inp_3)
    assert res_3.total_forecast_demand == 540.0
    assert np.isclose(res_3.average_daily_demand, 77.1429, atol=1e-3)
    assert np.isclose(res_3.lead_time_demand, 2314.2857, atol=1e-2)
    assert np.isclose(res_3.reorder_point, 2414.2857, atol=1e-2)
    assert res_3.stockout_risk_level == "HIGH"
    assert res_3.recommended_order_quantity == 520.0

def test_existing_on_order_deduction(engine):
    inp = InventoryInput(
        store_id="CA_1",
        item_id="item_c",
        current_inventory=120,
        forecast_demand=540,
        forecast_horizon_days=7,
        lead_time_days=5,
        safety_stock_override=100,
        existing_on_order=200
    )
    res = engine.assess_inventory(inp)
    assert res.recommended_order_quantity == 320.0

def test_moq_behavior(engine):
    inp = InventoryInput(
        store_id="TX_1",
        item_id="item_moq",
        current_inventory=100,
        forecast_demand=120,
        forecast_horizon_days=7,
        lead_time_days=5,
        safety_stock_override=17,
        minimum_order_quantity=50
    )
    res = engine.assess_inventory(inp)
    assert res.raw_recommended_order == 37.0
    assert res.recommended_order_quantity == 50.0

def test_risk_and_status_classifications():
    assert evaluate_stockout_risk(current_inventory=50, lead_time_demand=100, reorder_point=120) == "HIGH"
    assert evaluate_inventory_status(current_inventory=50, lead_time_demand=100, reorder_point=120, target_inventory=150) == "CRITICAL"
    
    assert evaluate_stockout_risk(current_inventory=110, lead_time_demand=100, reorder_point=120) == "MEDIUM"
    assert evaluate_inventory_status(current_inventory=110, lead_time_demand=100, reorder_point=120, target_inventory=150) == "REORDER"
    
    assert evaluate_stockout_risk(current_inventory=130, lead_time_demand=100, reorder_point=120) == "LOW"
    assert evaluate_inventory_status(current_inventory=130, lead_time_demand=100, reorder_point=120, target_inventory=150) == "WATCH"

    assert evaluate_inventory_status(current_inventory=200, lead_time_demand=100, reorder_point=120, target_inventory=150) == "HEALTHY"

def test_invalid_inputs_raise_errors(engine):
    with pytest.raises(ValueError, match="current_inventory cannot be negative"):
        InventoryInput("CA_1", "item1", current_inventory=-10, forecast_demand=100).validate()

    with pytest.raises(ValueError, match="lead_time_days cannot be negative"):
        InventoryInput("CA_1", "item1", current_inventory=10, forecast_demand=100, lead_time_days=-5).validate()

    with pytest.raises(ValueError, match="minimum_order_quantity cannot be negative"):
        InventoryInput("CA_1", "item1", current_inventory=10, forecast_demand=100, minimum_order_quantity=-10).validate()

    with pytest.raises(ValueError, match="forecast_demand cannot be negative"):
        InventoryInput("CA_1", "item1", current_inventory=10, forecast_demand=-50).validate()

def test_zero_demand_handling(engine):
    inp = InventoryInput(
        store_id="CA_1",
        item_id="item_zero",
        current_inventory=10,
        forecast_demand=0.0,
        forecast_horizon_days=7,
        lead_time_days=5
    )
    res = engine.assess_inventory(inp)
    assert res.total_forecast_demand == 0.0
    assert res.average_daily_demand == 0.0
    assert res.recommended_order_quantity == 0.0
    assert res.stockout_risk_level == "LOW"
