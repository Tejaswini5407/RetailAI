# RetailAI — Inventory Optimization Engine Documentation (Phase 5)

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
