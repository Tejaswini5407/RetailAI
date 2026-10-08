import urllib.request
import urllib.error
import json
import sys
import random

BASE_URL = "http://localhost:8080"

def make_request(method, path, data=None, token=None):
    url = f"{BASE_URL}{path}"
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

def run_verifications():
    print("=== STARTING LIVE END-TO-END VERIFICATION ===")
    rand_id = random.randint(1000, 9999)

    # 1. Register Org A
    org_a_code = f"ALPHA-VERIFY-{rand_id}"
    org_a_reg = {
        "organizationName": "Alpha Retail Corp",
        "organizationCode": org_a_code,
        "name": "Alpha Admin",
        "email": f"admin{rand_id}@alpha.com",
        "password": "Password123!",
        "role": "ADMIN"
    }
    status, res = make_request("POST", "/api/v1/auth/register", org_a_reg)
    token_a = (res or {}).get("accessToken") or (res or {}).get("token")
    print(f"[1] Register Org A ({org_a_code}): Status={status}, TokenReceived={bool(token_a)}")
    assert status == 201, f"Expected 201, got {status} ({res})"
    assert token_a, "Expected access token in response"

    # 2. Register Org B
    org_b_code = f"BETA-VERIFY-{rand_id}"
    org_b_reg = {
        "organizationName": "Beta Supermarkets",
        "organizationCode": org_b_code,
        "name": "Beta Admin",
        "email": f"admin{rand_id}@beta.com",
        "password": "Password123!",
        "role": "ADMIN"
    }
    status, res = make_request("POST", "/api/v1/auth/register", org_b_reg)
    token_b = (res or {}).get("accessToken") or (res or {}).get("token")
    print(f"[2] Register Org B ({org_b_code}): Status={status}, TokenReceived={bool(token_b)}")
    assert status == 201, f"Expected 201, got {status} ({res})"
    assert token_b, "Expected access token in response"

    # 3. Authenticated /api/v1/me
    status, res = make_request("GET", "/api/v1/me", token=token_a)
    print(f"[3] GET /api/v1/me Org A: Status={status}, OrgCode={res.get('organizationCode')}, Role={res.get('role')}")
    assert status == 200, f"Expected 200, got {status}"
    assert res.get("organizationCode") == org_a_code

    # 4. Org A Creates Store & Product matching exact DTOs
    store_payload = {
        "storeCode": f"STORE-A-{rand_id}",
        "name": "Alpha Flagship Store",
        "city": "New York",
        "state": "NY"
    }
    status, store_a = make_request("POST", "/api/v1/stores", store_payload, token=token_a)
    print(f"[4] Create Store Org A: Status={status}, StoreId={store_a.get('id') if store_a else None}")
    assert status == 201, f"Expected 201, got {status} ({store_a})"
    store_a_id = store_a["id"]

    prod_payload = {
        "sku": f"SKU-A-{rand_id}",
        "name": "Alpha Premium Milk",
        "category": "Dairy",
        "department": "Grocery"
    }
    status, prod_a = make_request("POST", "/api/v1/products", prod_payload, token=token_a)
    print(f"[5] Create Product Org A: Status={status}, ProductId={prod_a.get('id') if prod_a else None}")
    assert status == 201, f"Expected 201, got {status} ({prod_a})"
    prod_a_id = prod_a["id"]

    # Register Inventory for Store A + Product A
    inv_payload = {
        "storeId": store_a_id,
        "productId": prod_a_id,
        "quantityOnHand": 10,
        "quantityOnOrder": 0
    }
    status, inv_a = make_request("POST", "/api/v1/inventory", inv_payload, token=token_a)
    print(f"[6] Create Inventory Org A: Status={status}, InventoryId={inv_a.get('id') if inv_a else None}")
    assert status == 201, f"Expected 201, got {status} ({inv_a})"

    # 5. Create Store Manager in Org A & Test RBAC
    mgr_email = f"manager{rand_id}@alpha.com"
    status, mgr_user = make_request("POST", "/api/v1/users", {"name": "Alpha Manager", "email": mgr_email, "password": "Password123!", "role": "STORE_MANAGER"}, token=token_a)
    print(f"[7] Create STORE_MANAGER user Org A: Status={status}, UserId={mgr_user.get('id') if mgr_user else None}")
    assert status == 201

    # Login Store Manager
    status, res = make_request("POST", "/api/v1/auth/login", {"organizationCode": org_a_code, "email": mgr_email, "password": "Password123!"})
    assert status == 200
    token_mgr = res.get("accessToken") or res.get("token")

    # Store Manager attempts to list users -> Expected 403 Forbidden!
    status, res = make_request("GET", "/api/v1/users", token=token_mgr)
    print(f"[8] STORE_MANAGER access /api/v1/users (RBAC check): Status={status} (Expected 403)")
    assert status == 403, f"Expected 403 Forbidden, got {status}"

    # 6. Multi-Tenant Isolation Verification (Org B context)
    status, res = make_request("GET", "/api/v1/stores", token=token_b)
    print(f"[9] Org B GET /api/v1/stores: Status={status}, TotalElements={res.get('totalElements')}")
    assert status == 200
    assert res.get("totalElements") == 0, "Tenant isolation failure: Org B sees Org A's stores!"

    # Manipulating ID: Org B attempts to fetch Store A directly
    status, res = make_request("GET", f"/api/v1/stores/{store_a_id}", token=token_b)
    print(f"[10] Org B direct fetch Store A (ID manipulation check): Status={status} (Expected 404)")
    assert status == 404, f"Expected 404 Not Found for cross-tenant ID access, got {status}"

    # 7. Inventory Assessment Verification (/api/v1/inventory/assess)
    assess_req = {
        "storeId": store_a_id,
        "productId": prod_a_id,
        "forecastDemand": 140.0,
        "forecastHorizonDays": 7,
        "leadTimeDays": 7,
        "safetyStock": 10,
        "existingOnOrder": 0,
        "moq": 1
    }
    status, res = make_request("POST", "/api/v1/inventory/assess", assess_req, token=token_a)
    inv_status = res.get("inventoryStatus") or res.get("status")
    rec_qty = res.get("recommendedOrderQuantity") or res.get("recommendedOrderQty")
    print(f"[11] POST /api/v1/inventory/assess: Status={status}, InventoryStatus={inv_status}, StockoutRisk={res.get('stockoutRisk')}, RecOrderQty={rec_qty}")
    assert status == 200, f"Expected 200, got {status} ({res})"
    assert inv_status is not None

    # 8. ML Demand Forecast Verification (/api/v1/forecasts)
    fc_req = {
        "storeId": store_a_id,
        "productId": prod_a_id,
        "date": "2026-10-15"
    }
    status, res = make_request("POST", "/api/v1/forecasts", fc_req, token=token_a)
    print(f"[12] POST /api/v1/forecasts: Status={status}, PredictedDemand={res.get('predictedDemand') if res else None}, ModelName={res.get('modelName') if res else None}, ModelVersion={res.get('modelVersion') if res else None}")
    assert status == 200, f"Expected 200, got {status} ({res})"
    assert "predictedDemand" in res

    # 9. Forecast + Inventory Combined Assessment (/api/v1/inventory/forecast-assessment)
    comb_req = {
        "storeId": store_a_id,
        "productId": prod_a_id,
        "date": "2026-10-15",
        "forecastHorizonDays": 7,
        "leadTimeDays": 7,
        "safetyStock": 10,
        "existingOnOrder": 0,
        "moq": 1
    }
    status, res = make_request("POST", "/api/v1/inventory/forecast-assessment", comb_req, token=token_a)
    comb_inv_status = res.get("assessment", {}).get("inventoryStatus") or res.get("assessment", {}).get("status")
    print(f"[13] POST /api/v1/inventory/forecast-assessment: Status={status}, AssessmentStatus={comb_inv_status}, ForecastDemand={res.get('forecast', {}).get('predictedDemand') if res else None}")
    assert status == 200, f"Expected 200, got {status} ({res})"
    assert "forecast" in res and "assessment" in res

    # 10. Error Code Verification (401, 400, 409, 404)
    status, _ = make_request("GET", "/api/v1/me") # No token -> 401
    print(f"[14] Unauthenticated /api/v1/me status: {status} (Expected 401)")
    assert status == 401

    status, _ = make_request("POST", "/api/v1/auth/register", org_a_reg) # Duplicate org code -> 409
    print(f"[15] Duplicate org registration status: {status} (Expected 409)")
    assert status == 409

    print("=== ALL LIVE END-TO-END VERIFICATIONS PASSED SUCCESSFULLY ===")

if __name__ == "__main__":
    run_verifications()
