import urllib.request
import urllib.error
import json
import socket
import time
import random

SPRING_BOOT_URL = "http://localhost:8080"
FASTAPI_URL = "http://localhost:8000"
REACT_URL = "http://localhost:3000"

def make_request(method, path, data=None, token=None, base_url=SPRING_BOOT_URL):
    url = f"{base_url}{path}"
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"

    body = json.dumps(data).encode("utf-8") if data else None
    req = urllib.request.Request(url, data=body, headers=headers, method=method)

    try:
        with urllib.request.urlopen(req) as resp:
            status = resp.status
            content = resp.read().decode("utf-8")
            json_data = json.loads(content) if content else None
            return status, json_data
    except urllib.error.HTTPError as e:
        content = e.read().decode("utf-8")
        json_data = json.loads(content) if content else None
        return e.code, json_data
    except Exception as e:
        return 500, {"error": str(e)}

def check_tcp_port(host, port):
    try:
        with socket.create_connection((host, port), timeout=3):
            return True
    except Exception:
        return False

def run_phase11_e2e():
    print("==================================================")
    print(" RETAILAI — PHASE 11 SYSTEM E2E VALIDATION SUITE ")
    print("==================================================")

    # 1. Services Health Check
    pg_ok = check_tcp_port("localhost", 5432)
    sb_status, sb_res = make_request("GET", "/api/health")
    fa_status, fa_res = make_request("GET", "/health", base_url=FASTAPI_URL)
    react_ok = check_tcp_port("localhost", 3000)

    print(f"[1.1] PostgreSQL (:5432): {'UP' if pg_ok else 'DOWN'}")
    print(f"[1.2] Spring Boot (:8080): {sb_res.get('status') if sb_status == 200 else 'DOWN'} (HTTP {sb_status})")
    print(f"[1.3] FastAPI ML (:8000): {fa_res.get('status') if fa_status == 200 else 'DOWN'} (HTTP {fa_status})")
    print(f"[1.4] React Frontend (:3000): {'UP' if react_ok else 'DOWN'}")

    assert pg_ok, "PostgreSQL not running"
    assert sb_status == 200 and sb_res.get("status") == "UP", "Spring Boot health check failed"
    assert fa_status == 200 and fa_res.get("status") in ["UP", "healthy"], "FastAPI health check failed"
    assert react_ok, "React frontend server not running"

    rand_tag = random.randint(10000, 99999)

    # 2. User Journey — Org A Registration & Auth
    org_a_code = f"DEMO-ORG-{rand_tag}"
    org_a_payload = {
        "organizationName": "RetailAI Demo Corp",
        "organizationCode": org_a_code,
        "name": "Demo Admin User",
        "email": f"admin_{rand_tag}@retailai-demo.com",
        "password": "Password123!",
        "role": "ADMIN"
    }
    status, res = make_request("POST", "/api/v1/auth/register", org_a_payload)
    print(f"[2.1] Register Demo Org A ({org_a_code}): Status={status}")
    assert status == 201
    token_admin_a = res.get("accessToken") or res.get("token")
    assert token_admin_a

    # Verify /api/v1/me
    status, me_res = make_request("GET", "/api/v1/me", token=token_admin_a)
    print(f"[2.2] Authenticated GET /api/v1/me: Status={status}, Org={me_res.get('organizationCode')}, Role={me_res.get('role')}")
    assert status == 200 and me_res.get("organizationCode") == org_a_code

    # 3. Create Store, Product, Sales, Inventory
    store_payload = {"storeCode": "STORE-001", "name": "Main Demo Store", "city": "Austin", "state": "TX"}
    status, store_res = make_request("POST", "/api/v1/stores", store_payload, token=token_admin_a)
    print(f"[3.1] Create Store (STORE-001): Status={status}, ID={store_res.get('id') if store_res else None}")
    assert status == 201
    store_id = store_res["id"]

    prod_payload = {"sku": "SKU-001", "name": "Organic Espresso Beans", "category": "Coffee", "department": "Grocery"}
    status, prod_res = make_request("POST", "/api/v1/products", prod_payload, token=token_admin_a)
    print(f"[3.2] Create Product (SKU-001): Status={status}, ID={prod_res.get('id') if prod_res else None}")
    assert status == 201
    prod_id = prod_res["id"]

    # Record Sales for distinct dates to respect unique grain constraint
    dates = ["2026-10-01", "2026-10-02", "2026-10-03"]
    for i, qty in enumerate([25, 50, 30]):
        sale_payload = {"storeId": store_id, "productId": prod_id, "unitsSold": qty, "unitPrice": 4.50, "saleDate": dates[i]}
        status, sale_res = make_request("POST", "/api/v1/sales", sale_payload, token=token_admin_a)
        assert status == 201, f"Expected 201 for sale, got {status} ({sale_res})"
    print(f"[3.3] Record Sales Transactions: 3 records saved successfully (Status 201)")

    # Create Inventory (Low stock on hand = 15)
    inv_payload = {"storeId": store_id, "productId": prod_id, "quantityOnHand": 15, "quantityOnOrder": 0}
    status, inv_res = make_request("POST", "/api/v1/inventory", inv_payload, token=token_admin_a)
    print(f"[3.4] Create Inventory Record: Status={status}, ID={inv_res.get('id') if inv_res else None}, QuantityOnHand=15")
    assert status == 201

    # 4. Forecast Pipeline (React -> Spring Boot -> FastAPI -> Random Forest ML Model -> Spring Boot -> React)
    fc_req = {"storeId": store_id, "productId": prod_id, "date": "2026-10-15"}
    status, fc_res = make_request("POST", "/api/v1/forecasts", fc_req, token=token_admin_a)
    print(f"[4.1] Forecast Pipeline Execution: Status={status}")
    print(f"      - Predicted Demand: {fc_res.get('predictedDemand')}")
    print(f"      - ML Model Name: {fc_res.get('modelName')}")
    print(f"      - Model Version: {fc_res.get('modelVersion')}")
    assert status == 200
    assert fc_res.get("modelName") == "Random Forest"
    assert fc_res.get("modelVersion") is None  # Preserves null, zero fake version string

    # 5. Inventory Intelligence Engine (Phase 5 Deterministic Logic)
    assess_req = {
        "storeId": store_id,
        "productId": prod_id,
        "forecastDemand": 140.0,
        "forecastHorizonDays": 7,
        "leadTimeDays": 7,
        "safetyStock": 10,
        "existingOnOrder": 0,
        "moq": 1
    }
    status, assess_res = make_request("POST", "/api/v1/inventory/assess", assess_req, token=token_admin_a)
    print(f"[5.1] Inventory Assessment (/assess): Status={status}")
    print(f"      - Total Forecast Demand: {assess_res.get('totalForecastDemand')}")
    print(f"      - Avg Daily Demand: {assess_res.get('averageDailyDemand')}")
    print(f"      - Lead-Time Demand: {assess_res.get('leadTimeDemand')}")
    print(f"      - Safety Stock: {assess_res.get('safetyStock')}")
    print(f"      - Reorder Point: {assess_res.get('reorderPoint')}")
    print(f"      - Stockout Risk: {assess_res.get('stockoutRisk')}")
    print(f"      - Recommended Order: {assess_res.get('recommendedOrderQuantity')}")
    print(f"      - Operational Inventory Status: {assess_res.get('inventoryStatus')}")
    assert status == 200
    assert assess_res.get("inventoryStatus") in ["CRITICAL", "REORDER", "WATCH", "HEALTHY", "OUT_OF_STOCK", "BELOW_REORDER_POINT", "ADEQUATE"]

    # Forecast + Inventory Assessment Integrated Chain
    comb_req = {
        "storeId": store_id,
        "productId": prod_id,
        "date": "2026-10-15",
        "forecastHorizonDays": 7,
        "leadTimeDays": 7,
        "safetyStock": 10,
        "existingOnOrder": 0,
        "moq": 1
    }
    status, comb_res = make_request("POST", "/api/v1/inventory/forecast-assessment", comb_req, token=token_admin_a)
    print(f"[5.2] Integrated Forecast + Inventory Assessment: Status={status}")
    print(f"      - ML Forecast Demand: {comb_res.get('forecast', {}).get('predictedDemand')}")
    print(f"      - Inventory Status: {comb_res.get('assessment', {}).get('inventoryStatus')}")
    print(f"      - Recommended Order: {comb_res.get('assessment', {}).get('recommendedOrderQuantity')}")
    assert status == 200

    # 6. Tenant Isolation Verification (Org A vs Org B)
    org_b_code = f"ISOLATION-ORG-{rand_tag}"
    status, res_b = make_request("POST", "/api/v1/auth/register", {
        "organizationName": "Isolation Test Org",
        "organizationCode": org_b_code,
        "name": "Org B Admin",
        "email": f"admin_{rand_tag}@orgb.com",
        "password": "Password123!",
        "role": "ADMIN"
    })
    assert status == 201
    token_admin_b = res_b.get("accessToken") or res_b.get("token")

    status, list_b = make_request("GET", "/api/v1/stores", token=token_admin_b)
    print(f"[6.1] Tenant Isolation List Stores (Org B): Status={status}, TotalElements={list_b.get('totalElements')}")
    assert status == 200 and list_b.get("totalElements") == 0

    status, direct_fetch = make_request("GET", f"/api/v1/stores/{store_id}", token=token_admin_b)
    print(f"[6.2] Cross-Tenant ID Direct Access Attempt: Status={direct_fetch.get('status') if isinstance(direct_fetch, dict) else status} (Expected 404)")
    assert status == 404

    # 7. RBAC Verification
    # Create Store Manager User in Org A
    status, mgr_res = make_request("POST", "/api/v1/users", {
        "name": "Demo Store Manager",
        "email": f"manager_{rand_tag}@retailai-demo.com",
        "password": "Password123!",
        "role": "STORE_MANAGER"
    }, token=token_admin_a)
    assert status == 201

    # Login Store Manager
    status, login_mgr = make_request("POST", "/api/v1/auth/login", {
        "organizationCode": org_a_code,
        "email": f"manager_{rand_tag}@retailai-demo.com",
        "password": "Password123!"
    })
    token_mgr = login_mgr.get("accessToken") or login_mgr.get("token")

    # Store Manager attempts to access Admin User Management -> Expected 403
    status, rbac_res = make_request("GET", "/api/v1/users", token=token_mgr)
    print(f"[7.1] STORE_MANAGER access /api/v1/users: Status={status} (Expected 403 Forbidden)")
    assert status == 403

    # 8. Failure Testing & Error Recovery
    status, _ = make_request("GET", "/api/v1/me")  # No token
    print(f"[8.1] Failure Test - Unauthenticated Request: Status={status} (Expected 401)")
    assert status == 401

    status, _ = make_request("POST", "/api/v1/auth/register", org_a_payload)  # Duplicate Org
    print(f"[8.2] Failure Test - Duplicate Organization Registration: Status={status} (Expected 409)")
    assert status == 409

    print("==================================================")
    print(" ALL PHASE 11 E2E INTEGRATION VERIFICATIONS PASSED ")
    print("==================================================")

if __name__ == "__main__":
    run_phase11_e2e()
