import urllib.request
import json
import sys

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
    print("=== LIVE E2E AUTHENTICATION & ORGANIZATION REGISTRATION AUDIT ===")
    
    # Generate unique test code based on timestamp if needed or clean check
    import time
    timestamp = int(time.time())
    org_code = f"ORG-AUDIT-{timestamp}"
    email = f"audit-{timestamp}@test.com"
    password = "SecurePassword123!"

    # 1. Register fresh organization
    print(f"\n1. Registering new organization: {org_code}")
    reg_payload = {
        "organizationCode": org_code,
        "organizationName": "Audit Retail Corp",
        "name": "Audit Admin",
        "email": email,
        "password": password,
        "role": "STORE_MANAGER" # Client requests STORE_MANAGER, server MUST force ADMIN
    }
    status, res = request("POST", "/auth/register", reg_payload)
    print(f"Status: {status}")
    assert status == 201, f"Expected 201, got {status}: {res}"
    token = res.get("accessToken")
    user_info = res.get("user", {})
    assert token is not None, "Missing accessToken"
    assert user_info.get("role") == "ADMIN", f"Expected role ADMIN, got {user_info.get('role')}"
    assert user_info.get("organizationCode") == org_code, "Mismatched org code"
    print("[OK] Registration succeeded with 201 Created and forced ADMIN role.")

    # 2. Login with correct credentials
    print(f"\n2. Login with correct org code ({org_code}), email, password")
    login_payload = {
        "organizationCode": org_code,
        "email": email,
        "password": password
    }
    status, res = request("POST", "/auth/login", login_payload)
    print(f"Status: {status}")
    assert status == 200, f"Expected 200, got {status}: {res}"
    token = res.get("accessToken")
    print("[OK] Login succeeded with 200 OK.")

    # 3. Case-insensitive login check (lower case org code & upper case email)
    print("\n3. Login with case-insensitive org code and email")
    mixed_login_payload = {
        "organizationCode": org_code.lower(),
        "email": email.upper(),
        "password": password
    }
    status, res = request("POST", "/auth/login", mixed_login_payload)
    print(f"Status: {status}")
    assert status == 200, f"Expected 200, got {status}: {res}"
    print("[OK] Case-insensitive login succeeded with 200 OK.")

    # 4. Login with wrong password
    print("\n4. Login with wrong password")
    wrong_pwd_payload = {
        "organizationCode": org_code,
        "email": email,
        "password": "WrongPassword999!"
    }
    status, res = request("POST", "/auth/login", wrong_pwd_payload)
    print(f"Status: {status}")
    assert status == 401, f"Expected 401, got {status}: {res}"
    print("[OK] Wrong password rejected with 401 Unauthorized.")

    # 5. Login with mismatched organization code
    print("\n5. Login with mismatched organization code")
    mismatched_org_payload = {
        "organizationCode": "ORG-WRONG-CODE",
        "email": email,
        "password": password
    }
    status, res = request("POST", "/auth/login", mismatched_org_payload)
    print(f"Status: {status}")
    assert status == 401, f"Expected 401, got {status}: {res}"
    print("[OK] Mismatched organization code rejected with 401 Unauthorized.")

    # 6. Duplicate organization registration (same case & mixed case)
    print("\n6. Registering duplicate organization code (mixed case)")
    dup_org_payload = {
        "organizationCode": org_code.lower(),
        "organizationName": "Impostor Corp",
        "name": "Attacker",
        "email": f"attacker-{timestamp}@test.com",
        "password": "Password123!",
        "role": "ADMIN"
    }
    status, res = request("POST", "/auth/register", dup_org_payload)
    print(f"Status: {status}")
    assert status == 409, f"Expected 409, got {status}: {res}"
    print(f"[OK] Duplicate organization code registration rejected with 409 Conflict: {res.get('message')}")

    # 7. Verify authenticated endpoint /security/me
    print("\n7. Verifying /security/me with JWT")
    status, res = request("GET", "/security/me", token=token)
    print(f"Status: {status}")
    assert status == 200, f"Expected 200, got {status}: {res}"
    assert res.get("email") == email.lower(), "Email mismatch in /security/me"
    assert res.get("organizationCode") == org_code, "Org code mismatch in /security/me"
    print("[OK] JWT authentication & TenantContext verified successfully.")

    print("\nALL LIVE E2E HTTP VERIFICATION CHECKS PASSED PERFECTLY!")

if __name__ == "__main__":
    main()
