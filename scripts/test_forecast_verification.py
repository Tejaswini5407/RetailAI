import urllib.request
import json
import sys
import time

BASE_URL = "http://localhost:8080/api/v1"

def request(method, path, body=None, token=None):
    url = BASE_URL + path
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    
    data = json.dumps(body).encode('utf-8') if body else None
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    
    try:
        with urllib.request.urlopen(req) as resp:
            status = resp.status
            res_body = resp.read().decode('utf-8')
            return status, json.loads(res_body) if res_body else {}
    except urllib.error.HTTPError as e:
        res_body = e.read().decode('utf-8')
        return e.code, json.loads(res_body) if res_body else {}
    except Exception as e:
        print(f"Error executing {method} {url}: {e}")
        sys.exit(1)

def main():
    print("=== LIVE FORECASTING & ACCURACY CENTER VERIFICATION ===")

    ts = int(time.time())
    org_code = f"ORG-FC-{ts}"
    email = f"fc-{ts}@test.com"
    password = "Password123!"

    # 1. Register & Login
    print(f"\n1. Registering test organization: {org_code}")
    reg_payload = {
        "organizationCode": org_code,
        "organizationName": "Forecast Test Org",
        "name": "Forecast User",
        "email": email,
        "password": password,
        "role": "ADMIN"
    }
    status, res = request("POST", "/auth/register", reg_payload)
    assert status == 201, f"Failed registration: {res}"
    token = res["accessToken"]
    print("[OK] Registration succeeded.")

    # 2. Get Stores and Products
    status, stores_res = request("GET", "/stores?page=0&size=10", token=token)
    stores = stores_res.get("content", [])
    status, products_res = request("GET", "/products?page=0&size=10", token=token)
    products = products_res.get("content", [])

    print(f"[OK] Fetched {len(stores)} stores and {len(products)} products.")

    if not stores or not products:
        print("Creating sample store and product for forecast testing...")
        # Create store
        status, s_res = request("POST", "/stores", {"storeCode": "STORE-001", "name": "Main Supercenter", "state": "CA", "city": "Los Angeles"}, token=token)
        store_1_id = s_res["id"]
        status, s_res_2 = request("POST", "/stores", {"storeCode": "STORE-002", "name": "Second Store", "state": "TX", "city": "Dallas"}, token=token)
        store_2_id = s_res_2["id"]

        # Create products
        status, p_res = request("POST", "/products", {"sku": "SKU-001", "name": "Fresh Milk 1L", "category": "FOODS", "department": "FOODS_1"}, token=token)
        product_1_id = p_res["id"]
        status, p_res_2 = request("POST", "/products", {"sku": "SKU-002", "name": "Board Game", "category": "HOBBIES", "department": "HOBBIES_1"}, token=token)
        product_2_id = p_res_2["id"]
    else:
        store_1_id = stores[0]["id"]
        store_2_id = stores[1]["id"] if len(stores) > 1 else stores[0]["id"]
        product_1_id = products[0]["id"]
        product_2_id = products[1]["id"] if len(products) > 1 else products[0]["id"]

    # 3. Generate Forecast for Store 1 / Product 1
    print("\n3. Generating ML Forecast for Store 1 / Product 1...")
    fc_req_1 = {
        "storeId": store_1_id,
        "productId": product_1_id,
        "date": "2026-10-15",
        "horizonDays": 7
    }
    status, fc_1 = request("POST", "/forecasts", fc_req_1, token=token)
    print(f"Status: {status}")
    print(f"Store 1 / Product 1 Forecast Result: {json.dumps(fc_1, indent=2)}")
    assert status == 200, f"Forecast 1 failed: {fc_1}"
    assert fc_1["modelName"] == "Random Forest"
    assert fc_1["modelVersion"] == "unversioned-artifact"
    assert fc_1["predictedDemand"] >= 0.0
    print("[OK] Forecast 1 completed successfully.")

    # 4. Generate Forecast for Store 2 / Product 2
    print("\n4. Generating ML Forecast for Store 2 / Product 2...")
    fc_req_2 = {
        "storeId": store_2_id,
        "productId": product_2_id,
        "date": "2026-10-15",
        "horizonDays": 14
    }
    status, fc_2 = request("POST", "/forecasts", fc_req_2, token=token)
    print(f"Status: {status}")
    print(f"Store 2 / Product 2 Forecast Result: {json.dumps(fc_2, indent=2)}")
    assert status == 200, f"Forecast 2 failed: {fc_2}"
    assert fc_2["modelName"] == "Random Forest"
    assert fc_2["modelVersion"] == "unversioned-artifact"
    print("[OK] Forecast 2 completed successfully.")

    # 5. Verify Forecast History Endpoint
    print("\n5. Verifying /forecasts/history log...")
    status, history = request("GET", "/forecasts/history", token=token)
    assert status == 200, f"History failed: {history}"
    assert len(history) >= 2, f"Expected history records, got {len(history)}"
    for h in history:
        assert h["modelName"] == "Random Forest", "Missing modelName in history"
        assert h["modelVersion"] == "unversioned-artifact", "Missing modelVersion in history"
        assert h["forecastRunId"] is not None, "Missing forecastRunId in history"
    print(f"[OK] Persisted history contains {len(history)} run records with model name and version.")

    # 6. Verify Forecast Explanation Endpoint
    print("\n6. Verifying /forecasts/explanation endpoint...")
    status, exp = request("GET", f"/forecasts/explanation?storeId={store_1_id}&productId={product_1_id}", token=token)
    assert status == 200, f"Explanation failed: {exp}"
    assert len(exp.get("factors", [])) > 0, "Missing factors in explanation"
    print("[OK] Explanation factors retrieved successfully.")

    # 7. Verify Accuracy Tracking Endpoint
    print("\n7. Verifying /forecasts/accuracy endpoint...")
    status, acc = request("GET", "/forecasts/accuracy", token=token)
    assert status == 200, f"Accuracy failed: {acc}"
    print(f"Accuracy Response: {json.dumps(acc, indent=2)}")
    assert acc["sufficientData"] is False, "Expected sufficientData=false when actuals have not elapsed"
    assert "recorded for the forecast period" in acc["message"]
    print("[OK] Accuracy endpoint correctly reports insufficient data when target period actuals are unavailable.")

    print("\nALL VERIFICATION CHECKS PASSED CLEANLY!")

if __name__ == "__main__":
    main()
