# RetailAI Spring Boot Business APIs & Tenant Security (Phase 9)

## 1. Overview & Architecture
Phase 9 establishes the core business REST API layer connecting Spring Security, JWT authentication, and `TenantContext` to the underlying PostgreSQL persistence layer, Phase 5 Inventory Engine, and Phase 6 FastAPI ML Inference Service.

```
JWT Token -> JwtAuthenticationFilter -> TenantContext -> Controller -> Service -> Repository -> PostgreSQL
                                                                 │
                                                                 ├──> MLInferenceClient (FastAPI ML Service)
                                                                 └──> Phase 5 Inventory Assessment Engine
```

---

## 2. Authorization Matrix

| Endpoint | Method | ADMIN | STORE_MANAGER | INVENTORY_MANAGER |
|---|---|---|---|---|
| `/api/v1/me` | `GET` | ✅ | ✅ | ✅ |
| `/api/v1/users/**` | `POST, GET, PUT, DELETE` | ✅ | ❌ | ❌ |
| `/api/v1/stores` | `POST, PUT` | ✅ | ✅ | ❌ |
| `/api/v1/stores/**` | `GET` | ✅ | ✅ | ✅ |
| `/api/v1/stores/{id}` | `DELETE` | ✅ | ❌ | ❌ |
| `/api/v1/products` | `POST, PUT` | ✅ | ❌ | ✅ |
| `/api/v1/products/**` | `GET` | ✅ | ✅ | ✅ |
| `/api/v1/products/{id}` | `DELETE` | ✅ | ❌ | ❌ |
| `/api/v1/sales` | `POST` | ✅ | ✅ | ❌ |
| `/api/v1/sales/**` | `GET` | ✅ | ✅ | ✅ |
| `/api/v1/inventory` | `POST, PUT` | ✅ | ❌ | ✅ |
| `/api/v1/inventory/**` | `GET` | ✅ | ✅ | ✅ |
| `/api/v1/inventory/assess` | `POST` | ✅ | ✅ | ✅ |
| `/api/v1/forecasts` | `POST` | ✅ | ✅ | ✅ |
| `/api/v1/inventory/forecast-assessment` | `POST` | ✅ | ✅ | ✅ |

---

## 3. Core Business Endpoints & Request/Response Contracts

### A. Current User Context
- **`GET /api/v1/me`**
  - **Response (`200 OK`):**
    ```json
    {
      "id": 1,
      "email": "admin@retailcorp.com",
      "name": "Jane Founder",
      "organizationId": 10,
      "organizationCode": "RETAIL-CORP",
      "role": "ADMIN"
    }
    ```

### B. User / Team Management (`/api/v1/users`)
- **`POST /api/v1/users`** (ADMIN Only)
  - **Request:**
    ```json
    {
      "name": "John Manager",
      "email": "john@retailcorp.com",
      "password": "SecurePassword123!",
      "role": "STORE_MANAGER"
    }
    ```
- **`GET /api/v1/users?page=0&size=10`**

### C. Store Management (`/api/v1/stores`)
- **`POST /api/v1/stores`**
  - **Request:**
    ```json
    {
      "storeCode": "CA_1",
      "name": "California Flagship Store",
      "state": "CA",
      "city": "Los Angeles"
    }
    ```

### D. Product Catalog (`/api/v1/products`)
- **`POST /api/v1/products`**
  - **Request:**
    ```json
    {
      "sku": "FOODS_1_001",
      "name": "Organic Whole Milk 1L",
      "category": "Dairy",
      "department": "Foods"
    }
    ```

### E. Sales Recording & Queries (`/api/v1/sales`)
- **`POST /api/v1/sales`**
  - **Request:**
    ```json
    {
      "storeId": 5,
      "productId": 12,
      "saleDate": "2026-04-25",
      "unitsSold": 24,
      "unitPrice": 3.99
    }
    ```
- **`GET /api/v1/sales?storeId=5&productId=12&startDate=2026-04-01&endDate=2026-04-30`**

### F. Inventory & Phase 5 Inventory Intelligence (`/api/v1/inventory`)
- **`POST /api/v1/inventory`** (Create Position)
  - **Request:** `{"storeId": 5, "productId": 12, "quantityOnHand": 15, "quantityOnOrder": 20}`
- **`POST /api/v1/inventory/assess`** (Phase 5 Inventory Assessment Engine)
  - **Request:**
    ```json
    {
      "storeId": 5,
      "productId": 12,
      "forecastDemand": 210.0,
      "forecastHorizonDays": 14,
      "leadTimeDays": 7,
      "safetyStock": 25,
      "moq": 50
    }
    ```
  - **Response (`200 OK`):**
    ```json
    {
      "storeId": 5,
      "storeCode": "CA_1",
      "productId": 12,
      "sku": "FOODS_1_001",
      "totalForecastDemand": 210.0,
      "averageDailyDemand": 15.0,
      "forecastHorizonDays": 14,
      "leadTimeDays": 7,
      "leadTimeDemand": 105.0,
      "safetyStock": 25,
      "reorderPoint": 130,
      "currentInventory": 15,
      "existingOnOrder": 20,
      "stockoutRisk": "HIGH",
      "inventoryStatus": "BELOW_REORDER_POINT",
      "recommendedOrderQuantity": 200
    }
    ```

### G. ML Forecast & Orchestration (`/api/v1/forecasts`)
- **`POST /api/v1/forecasts`**
  - Sends request to FastAPI ML Service (`http://localhost:8000/api/v1/forecast`) and returns `predictedDemand` and model metadata.
- **`POST /api/v1/inventory/forecast-assessment`**
  - Combines ML demand forecast inference + Phase 5 inventory order recommendation into a single response.

---

## 4. Tenant Isolation & Security Rules
1. **Authoritative Tenant:** `organizationId` is strictly resolved via `TenantContext.getTenantId()` derived from the authenticated JWT token.
2. **Cross-Tenant Prevention:** Attempting to read, update, or reference another tenant's store, product, sale, or inventory position returns `404 Not Found` to prevent information disclosure.
3. **Database Constraints:** PostgreSQL enforced composite unique constraints (`organization_id`, `store_code`), (`organization_id`, `sku`), (`organization_id`, `store_id`, `product_id`, `sale_date`).
