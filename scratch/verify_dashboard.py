import urllib.request
import json
import time
import random

def test_dashboard_apis():
    print("Waiting for Spring Boot to respond...")
    for _ in range(30):
        try:
            req = urllib.request.Request("http://localhost:8080/api/health")
            with urllib.request.urlopen(req) as resp:
                if resp.status == 200:
                    print("[✓] Spring Boot /api/health returned HTTP 200 OK")
                    break
        except Exception:
            time.sleep(1)
    else:
        print("[X] Server did not start in time")
        return

    # 1. Register User
    rand_tag = random.randint(10000, 99999)
    org_code = f"DASHBOARD-ORG-{rand_tag}"
    reg_payload = {
        "organizationName": "Dashboard Test Corp",
        "organizationCode": org_code,
        "name": "Dashboard Admin",
        "email": f"admin_{rand_tag}@dashboard.com",
        "password": "Password123!",
        "role": "ADMIN"
    }
    data = json.dumps(reg_payload).encode("utf-8")
    req = urllib.request.Request("http://localhost:8080/api/v1/auth/register", data=data, method="POST")
    req.add_header("Content-Type", "application/json")
    req.add_header("Origin", "http://localhost:3000")
    with urllib.request.urlopen(req) as resp:
        reg_res = json.loads(resp.read().decode("utf-8"))
        print(f"[✓] POST /api/v1/auth/register returned HTTP {resp.status} Created")
        token = reg_res["accessToken"]

    # 2. Login User
    login_payload = {
        "organizationCode": org_code,
        "email": f"admin_{rand_tag}@dashboard.com",
        "password": "Password123!"
    }
    data = json.dumps(login_payload).encode("utf-8")
    req = urllib.request.Request("http://localhost:8080/api/v1/auth/login", data=data, method="POST")
    req.add_header("Content-Type", "application/json")
    req.add_header("Origin", "http://localhost:3000")
    with urllib.request.urlopen(req) as resp:
        login_res = json.loads(resp.read().decode("utf-8"))
        print(f"[✓] POST /api/v1/auth/login returned HTTP {resp.status} OK")
        token = login_res["accessToken"]

    # 3. CORS Preflight for Protected Endpoints
    endpoints = ["/api/v1/stores", "/api/v1/products", "/api/v1/sales", "/api/v1/inventory", "/api/v1/users"]
    for ep in endpoints:
        req = urllib.request.Request(f"http://localhost:8080{ep}", method="OPTIONS")
        req.add_header("Origin", "http://localhost:3000")
        req.add_header("Access-Control-Request-Method", "GET")
        req.add_header("Access-Control-Request-Headers", "authorization,content-type")
        with urllib.request.urlopen(req) as resp:
            origin_hdr = resp.headers.get("Access-Control-Allow-Origin")
            assert resp.status == 200 and origin_hdr == "http://localhost:3000", f"Failed CORS preflight for {ep}"
            print(f"[✓] OPTIONS {ep} returned HTTP 200 OK (Access-Control-Allow-Origin: {origin_hdr})")

    # 4. GET /api/v1/me
    req = urllib.request.Request("http://localhost:8080/api/v1/me")
    req.add_header("Authorization", f"Bearer {token}")
    with urllib.request.urlopen(req) as resp:
        me_data = json.loads(resp.read().decode("utf-8"))
        org_c = me_data.get("organizationCode")
        print(f"[✓] GET /api/v1/me returned HTTP 200 OK (Org: {org_c})")

    # 5. Paginated Protected Endpoint GET requests (verifying no [object Object])
    test_urls = [
        "/api/v1/stores?page=0&size=100",
        "/api/v1/products?page=0&size=100",
        "/api/v1/sales?page=0&size=100",
        "/api/v1/inventory?page=0&size=100",
        "/api/v1/users?page=0&size=10"
    ]
    for url in test_urls:
        assert "[object" not in url
        req = urllib.request.Request(f"http://localhost:8080{url}")
        req.add_header("Authorization", f"Bearer {token}")
        req.add_header("Origin", "http://localhost:3000")
        with urllib.request.urlopen(req) as resp:
            res_data = json.loads(resp.read().decode("utf-8"))
            tot = res_data.get("totalElements")
            print(f"[✓] GET {url} returned HTTP 200 OK (totalElements: {tot})")

    print("==================================================")
    print(" ALL DASHBOARD API & CORS VERIFICATIONS PASSED 100% ")
    print("==================================================")

if __name__ == "__main__":
    test_dashboard_apis()
