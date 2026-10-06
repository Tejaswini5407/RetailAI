import os
import sys
import numpy as np
import pandas as pd
import pytest
from fastapi.testclient import TestClient

# Ensure ml-service root is in sys.path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from app.main import app
from app.inference.model_loader import load_trained_model, ModelContainer, ModelNotFoundError
from app.inference.predictor import DemandPredictor
from app.schemas.forecast import ForecastRequest, ForecastItem, ForecastResponse, SinglePrediction


client = TestClient(app)

SAMPLE_FEATURE_PAYLOAD = {
    "sell_price": 2.50,
    "price_change_pct": 0.0,
    "price_rel_4w_avg": 1.0,
    "day_of_week": 0,
    "day_of_month": 25,
    "week_of_year": 17,
    "month": 4,
    "quarter": 2,
    "year": 2016,
    "is_weekend": 0,
    "is_event_day": 0,
    "is_snap_day": 0,
    "snap_CA": 0,
    "snap_TX": 0,
    "snap_WI": 0,
    "lag_1": 3.0,
    "lag_7": 2.0,
    "lag_14": 4.0,
    "lag_28": 1.0,
    "rolling_mean_7": 3.14,
    "rolling_std_7": 0.85,
    "rolling_mean_14": 3.00,
    "rolling_std_14": 0.90,
    "rolling_mean_28": 2.80,
    "rolling_std_28": 0.75,
    "item_code": 1
}


def test_model_loader_success():
    """Test loading the actual Random Forest model artifact."""
    container = load_trained_model(force_reload=True)
    assert container is not None
    assert container.model_name == "Random Forest"
    assert len(container.feature_names) == 38
    assert container.model_object is not None


def test_model_loader_file_not_found():
    """Test that loading a non-existent path raises ModelNotFoundError."""
    with pytest.raises(ModelNotFoundError):
        load_trained_model(model_path="invalid/path/non_existent_model.joblib", force_reload=True)


def test_predictor_single_item():
    """Test predictor with a single item payload."""
    container = load_trained_model()
    predictor = DemandPredictor(model_container=container)

    request = ForecastRequest(
        items=[
            ForecastItem(
                item_id="FOODS_1_001",
                store_id="CA_1",
                date="2016-04-25",
                features=SAMPLE_FEATURE_PAYLOAD
            )
        ]
    )

    response = predictor.predict(request)

    assert isinstance(response, ForecastResponse)
    assert response.model_name == "Random Forest"
    assert response.model_version is None
    assert response.status == "success"
    assert response.count == 1
    assert len(response.predictions) == 1

    pred = response.predictions[0]
    assert pred.item_id == "FOODS_1_001"
    assert pred.store_id == "CA_1"
    assert pred.date == "2016-04-25"
    assert pred.predicted_demand >= 0.0
    assert pred.rounded_demand == int(np.round(pred.predicted_demand))


def test_predictor_non_negative_clipping(monkeypatch):
    """Test that negative predictions produced by a dummy model are clipped at 0.0."""
    class DummyNegativeModel:
        def predict(self, X):
            return np.array([-5.2, -0.1, 3.4])

    container = ModelContainer(
        model_object=DummyNegativeModel(),
        model_name="Dummy Negative Model",
        feature_names=SAMPLE_FEATURE_PAYLOAD.keys()
    )

    predictor = DemandPredictor(model_container=container)
    request = ForecastRequest(
        items=[
            ForecastItem(item_id=f"ITEM_{i}", store_id="CA_1", features=SAMPLE_FEATURE_PAYLOAD)
            for i in range(3)
        ]
    )

    response = predictor.predict(request)
    assert response.predictions[0].predicted_demand == 0.0
    assert response.predictions[0].rounded_demand == 0
    assert response.predictions[1].predicted_demand == 0.0
    assert response.predictions[1].rounded_demand == 0
    assert response.predictions[2].predicted_demand == 3.4
    assert response.predictions[2].rounded_demand == 3


def test_health_check_endpoint():
    """Test GET /health API endpoint."""
    res = client.get("/health")
    assert res.status_code == 200
    data = res.json()
    assert data["status"] == "UP"
    assert data["service"] == "retailai-ml-service"
    assert data["model_loaded"] is True
    assert data["model_name"] == "Random Forest"
    assert data["feature_count"] == 38


def test_forecast_api_endpoint():
    """Test POST /api/v1/forecast HTTP API endpoint."""
    payload = {
        "items": [
            {
                "item_id": "FOODS_1_001",
                "store_id": "CA_1",
                "date": "2016-04-25",
                "features": SAMPLE_FEATURE_PAYLOAD
            },
            {
                "item_id": "HOBBIES_1_002",
                "store_id": "TX_1",
                "date": "2016-04-25",
                "features": SAMPLE_FEATURE_PAYLOAD
            }
        ]
    }

    res = client.post("/api/v1/forecast", json=payload)
    assert res.status_code == 200
    data = res.json()

    assert data["status"] == "success"
    assert data["model_name"] == "Random Forest"
    assert data["model_version"] is None
    assert data["count"] == 2
    assert len(data["predictions"]) == 2

    assert data["predictions"][0]["item_id"] == "FOODS_1_001"
    assert data["predictions"][0]["predicted_demand"] >= 0.0
    assert data["predictions"][1]["item_id"] == "HOBBIES_1_002"
    assert data["predictions"][1]["predicted_demand"] >= 0.0


def test_forecast_api_invalid_payload():
    """Test POST /api/v1/forecast with missing required fields returns 422."""
    payload = {"items": [{"item_id": "FOODS_1_001"}]}  # missing store_id and features
    res = client.post("/api/v1/forecast", json=payload)
    assert res.status_code == 422
