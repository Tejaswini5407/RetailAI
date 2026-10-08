# RetailAI — Phase 10 React Frontend Architecture

## Overview

The RetailAI Frontend is a multi-tenant, role-aware React application built with **Vite**, **React Router v6**, **Axios**, and **Recharts**. It provides an executive and operational web dashboard for managing stores, products, sales transactions, stock levels, inventory intelligence order recommendations, machine-learning demand forecasting, and multi-tenant user access control.

---

## 1. Architecture & Technology Stack

- **Framework:** React 18 + Vite
- **Routing:** React Router DOM v6
- **HTTP Client:** Axios with dynamic JWT interceptors (`frontend/src/api/axios.js`)
- **Visualizations:** Recharts (`ResponsiveContainer`, `BarChart`, `PieChart`)
- **State Management:** React Context API (`AuthContext`)
- **Styling:** Modular SaaS CSS (`frontend/src/index.css`)

---

## 2. Security & Multi-Tenant Context

All network communication connects strictly to the **Spring Boot API Gateway / Business Layer** (`http://localhost:8080`).

### Authentication Flow:
1. Users authenticate via `/api/v1/auth/login` or register via `/api/v1/auth/register`.
2. Upon login, the returned JWT is stored in `localStorage` (`retailai_token`).
3. Every outgoing HTTP request automatically includes the `Authorization: Bearer <token>` header via Axios request interceptor.
4. Active user details are validated against `GET /api/v1/me`.
5. On `401 Unauthorized` response, token is invalidated and user is securely redirected to `/login`.

### Role-Based Access Control (RBAC):
- **`ADMIN`**: Full access to all business features + **User Management** (`/users`).
- **`STORE_MANAGER`**: Access to Dashboard, Stores, Sales, Inventory, Intelligence, and Forecasts.
- **`INVENTORY_MANAGER`**: Access to Dashboard, Products, Inventory, Intelligence, and Forecasts.

---

## 3. Directory Structure

```
frontend/src/
├── api/
│   ├── axios.js           # Centralized Axios client & JWT interceptors
│   ├── authApi.js         # Login, Register, GET /api/v1/me
│   ├── storeApi.js        # Store CRUD endpoints (/api/v1/stores)
│   ├── productApi.js      # Product CRUD endpoints (/api/v1/products)
│   ├── salesApi.js        # Sales recording & filtering (/api/v1/sales)
│   ├── inventoryApi.js    # Stock CRUD, /assess, and /forecast-assessment
│   ├── forecastApi.js     # ML Forecast endpoint (/api/v1/forecasts)
│   └── userApi.js         # User management endpoints (/api/v1/users)
├── components/
│   ├── common/
│   │   ├── Badge.jsx            # Status & Risk level color badges
│   │   ├── ErrorMessage.jsx     # Alert banner component
│   │   ├── LoadingSpinner.jsx   # Async loading indicator
│   │   └── Pagination.jsx       # Page controls for Spring Boot Pageable
│   └── layout/
│       ├── Header.jsx           # Top navbar with tenant & user context
│       ├── Sidebar.jsx          # Role-aware navigation sidebar
│       └── Layout.jsx           # Main application shell
├── context/
│   └── AuthContext.jsx    # React context for auth state & JWT token persistence
├── pages/
│   ├── Login.jsx          # User login page
│   ├── Register.jsx       # Public organization registration page
│   ├── Dashboard.jsx      # Metric cards, inventory health & demand charts
│   ├── Stores.jsx         # Physical store management
│   ├── Products.jsx       # SKU & product catalog management
│   ├── Sales.jsx          # Point-of-sale transaction recording & filtering
│   ├── Inventory.jsx      # Stock levels, reorder thresholds & status
│   ├── Assessment.jsx     # Inventory Intelligence & Order Rec engine UI
│   ├── Forecasts.jsx      # ML Demand Forecasting UI
│   ├── Users.jsx          # Team user management (ADMIN only)
│   └── Profile.jsx        # Current session & tenant context details
├── routes/
│   └── ProtectedRoute.jsx # Route guard with role-based access control
├── App.jsx                # Application routes configuration
├── index.css              # Main SaaS styling sheet
└── main.jsx               # Application entrypoint
```

---

## 4. Phase 5 Parity & Domain Validation

The React frontend strictly preserves all validated Phase 5 & 9 domain rules:
- **Operational Inventory Statuses:** `CRITICAL`, `REORDER`, `WATCH`, `HEALTHY`.
- **Stockout Risk Levels:** `HIGH`, `MEDIUM`, `LOW`.
- **Model Version Handling:** Preserves `null` when the ML microservice returns `null` (never fabricates fake versions).
- **Zero Mock Data:** All visual components consume real backend API responses.
