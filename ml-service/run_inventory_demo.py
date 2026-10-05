import os
import sys

sys.path.insert(0, os.path.dirname(__file__))

from app.inventory.models import InventoryInput
from app.inventory.engine import InventoryOptimizationEngine

def main():
    print("=== RetailAI Phase 5: Business Intelligence & Inventory Optimization Engine ===")
    
    engine = InventoryOptimizationEngine()
    
    # Scenario A: High Demand / Low Stock -> Stockout Risk HIGH
    input_a = InventoryInput(
        store_id="CA_1",
        item_id="HOBBIES_1_001",
        current_inventory=120,
        forecast_demand=540,
        forecast_horizon_days=7,
        lead_time_days=5,
        safety_stock_override=100,
        existing_on_order=0
    )
    res_a = engine.assess_inventory(input_a)
    print("\nScenario A (Low Stock vs High Demand):")
    print(f"   - Current Stock: {res_a.current_inventory}, Forecast Demand: {res_a.total_forecast_demand}, Safety Stock: {res_a.safety_stock}")
    print(f"   - Target Inventory: {res_a.target_inventory}, Reorder Point: {res_a.reorder_point}")
    print(f"   - Recommended Order: {res_a.recommended_order_quantity} (Raw: {res_a.raw_recommended_order})")
    print(f"   - Stockout Risk: {res_a.stockout_risk_level}, Status: {res_a.inventory_status}")

    # Scenario B: High Stock -> No Order Needed
    input_b = InventoryInput(
        store_id="CA_1",
        item_id="HOBBIES_1_002",
        current_inventory=800,
        forecast_demand=540,
        forecast_horizon_days=7,
        lead_time_days=5,
        safety_stock_override=100
    )
    res_b = engine.assess_inventory(input_b)
    print("\nScenario B (Sufficient Stock):")
    print(f"   - Current Stock: {res_b.current_inventory}, Forecast Demand: {res_b.total_forecast_demand}")
    print(f"   - Recommended Order: {res_b.recommended_order_quantity}")
    print(f"   - Stockout Risk: {res_b.stockout_risk_level}, Status: {res_b.inventory_status}")

    # Scenario C: Minimum Order Quantity (MOQ) Constraint
    input_c = InventoryInput(
        store_id="TX_1",
        item_id="FOODS_1_005",
        current_inventory=100,
        forecast_demand=120,
        forecast_horizon_days=7,
        lead_time_days=5,
        safety_stock_override=17,
        minimum_order_quantity=50
    )
    res_c = engine.assess_inventory(input_c)
    print("\nScenario C (MOQ Applied):")
    print(f"   - Target Inventory: {res_c.target_inventory}, Current Stock: {res_c.current_inventory}")
    print(f"   - Raw Needed Order: {res_c.raw_recommended_order}, MOQ: {res_c.minimum_order_quantity}")
    print(f"   - Final Recommended Order Quantity: {res_c.recommended_order_quantity}")
    
    # 5. Generate Documentation: docs/inventory-optimization.md
    docs_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "docs"))
    os.makedirs(docs_dir, exist_ok=True)
    doc_path = os.path.join(docs_dir, "inventory-optimization.md")
    
    doc_content = r"""# RetailAI — Inventory Optimization Engine Documentation (Phase 5)

## 1. Business Objective
The Inventory Optimization Engine acts as the **Business Intelligence Layer** of RetailAI. It converts machine-learning demand forecasts into actionable operational inventory decisions:
* **Stockout Risk Classification:** Identifies whether a store faces immediate or upcoming stockout risk.
* **Replenishment Order Recommendations:** Calculates exact order quantities accounting for safety stock, lead time, existing purchase orders, and supplier minimum order quantities (MOQ).

> [!NOTE]
> **Architectural Separation:** The inventory engine is completely decoupled from model fitting code (Random Forest, XGBoost, etc.). It consumes forecast demand quantities ($\hat{D}_{N}$), allowing ML models to be upgraded without modifying business logic.

## 2. Core Business Logic & Formulas

### A. Demand Aggregation
Given a forecast demand array for horizon $N$ days:
$$\text{Total Forecast Demand } (\hat{D}_N) = \sum_{t=1}^N \hat{y}_t$$
$$\text{Average Daily Demand } (\bar{d}) = \frac{\hat{D}_N}{N}$$

### B. Safety Stock
1. **Standard Formula (with Demand Variability $\sigma$):**
   $$\text{Safety Stock} = z \times \sigma_d \times \sqrt{\text{Lead Time Days}}$$
   where $z = 1.645$ for a 95% service level.
2. **Simplified Fallback Formula (CV = 30% default retail variation):**
   $$\text{Safety Stock} = z \times (0.30 \times \bar{d}) \times \sqrt{\text{Lead Time Days}}$$
3. **Manual Override:** Allows inventory managers to override calculated safety stock.

### C. Reorder Point & Target Inventory
$$\text{Lead Time Demand} = \bar{d} \times \text{Lead Time Days}$$
$$\text{Reorder Point} = \text{Lead Time Demand} + \text{Safety Stock}$$
$$\text{Target Inventory} = \hat{D}_N + \text{Safety Stock}$$

### D. Order Recommendation & MOQ Logic
$$\text{Raw Recommended Order} = \max(0, \text{Target Inventory} - \text{Current Stock} - \text{Existing On-Order})$$
$$\text{Recommended Order Quantity} = \begin{cases} 
0 & \text{if Raw Order} = 0 \\
\max(\text{Raw Order}, \text{MOQ}) & \text{if Raw Order} > 0 \text{ and MOQ} > 0 \\
\text{Raw Order} & \text{otherwise}
\end{cases}$$

### E. Stockout Risk Classification Rules
* **`HIGH`:** $\text{Current Stock} < \hat{D}_N$ (Inventory is insufficient to cover near-term forecast demand).
* **`MEDIUM`:** $\text{Current Stock} \ge \hat{D}_N$ AND $\text{Current Stock} < \text{Reorder Point}$ (Inventory covers near-term demand but falls below safety threshold).
* **`LOW`:** $\text{Current Stock} \ge \text{Reorder Point}$ (Inventory is comfortably above reorder threshold).

### F. Operational Inventory Status Rules
* **`CRITICAL`:** Current stock $< \hat{D}_N$ (High stockout risk).
* **`REORDER`:** Current stock $<$ Reorder Point (Replenishment order required).
* **`WATCH`:** Current stock $\ge$ Reorder Point, but buffer is tight ($< 1.2 \times \text{Target Inventory}$).
* **`HEALTHY`:** Current stock $\ge 1.2 \times \text{Target Inventory}$ (Adequately stocked).

## 3. Example Calculations

| Parameter | Scenario A (Low Stock) | Scenario B (Sufficient Stock) | Scenario C (MOQ Applied) |
|---|---|---|---|
| **Current Stock** | 120 | 800 | 100 |
| **7-Day Forecast Demand** | 540 | 540 | 120 |
| **Safety Stock** | 100 | 100 | 17 |
| **Target Inventory** | 640 | 640 | 137 |
| **Existing On-Order** | 0 | 0 | 0 |
| **MOQ** | 0 | 0 | 50 |
| **Raw Recommended Order** | 520 | 0 | 37 |
| **Final Recommended Order** | **520** | **0** | **50** |
| **Stockout Risk Level** | **HIGH** | **LOW** | **MEDIUM** |
| **Inventory Status** | **CRITICAL** | **HEALTHY** | **REORDER** |

## 4. Phase 5 Disclaimer & Scope
Phase 5 establishes the deterministic business logic engine. It is NOT yet integrated into PostgreSQL database tables, REST APIs, or React UI components (which are reserved for Phase 6+).
"""

    with open(doc_path, "w", encoding="utf-8") as f:
        f.write(doc_content)
        
    print(f"\nDocumentation saved to: {doc_path}")
    print("\n=== Phase 5 Inventory Engine Execution Complete ===")

if __name__ == "__main__":
    main()
