import logging
from typing import List, Dict, Any, Optional
import numpy as np
import pandas as pd

from app.schemas.forecast import ForecastRequest, ForecastResponse, SinglePrediction, ForecastItem
from app.inference.model_loader import ModelContainer, load_trained_model

logger = logging.getLogger(__name__)


class DemandPredictor:
    """Production-grade ML inference predictor for retail demand forecasting."""

    def __init__(self, model_container: Optional[ModelContainer] = None):
        """
        Initialize predictor with loaded model container.
        If container is not provided, loads model artifact automatically.
        """
        self.container = model_container or load_trained_model()
        self.model = self.container.model_object
        self.feature_names = self.container.feature_names
        self.model_name = self.container.model_name

    def _align_feature_row(self, item: ForecastItem) -> Dict[str, float]:
        """
        Extract, derive, and align input features into exact model schema.
        """
        raw_feats = dict(item.features)
        aligned: Dict[str, float] = {}

        # Identifiers & categories
        store_id = item.store_id or ""
        item_id = item.item_id or ""

        # Auto-derive categorical indicators if not explicitly in raw features
        categorical_defaults = {
            "store_id_CA_1": 1.0 if store_id == "CA_1" else 0.0,
            "store_id_TX_1": 1.0 if store_id == "TX_1" else 0.0,
            "state_id_CA": 1.0 if store_id.startswith("CA") or raw_feats.get("state_id") == "CA" else 0.0,
            "state_id_TX": 1.0 if store_id.startswith("TX") or raw_feats.get("state_id") == "TX" else 0.0,
            "cat_id_FOODS": 1.0 if "FOODS" in item_id or "FOODS" in str(raw_feats.get("cat_id")) else 0.0,
            "cat_id_HOBBIES": 1.0 if "HOBBIES" in item_id or "HOBBIES" in str(raw_feats.get("cat_id")) else 0.0,
            "cat_id_HOUSEHOLD": 1.0 if "HOUSEHOLD" in item_id or "HOUSEHOLD" in str(raw_feats.get("cat_id")) else 0.0,
            "dept_id_FOODS_1": 1.0 if "FOODS_1" in item_id or "FOODS_1" in str(raw_feats.get("dept_id")) else 0.0,
            "dept_id_FOODS_2": 1.0 if "FOODS_2" in item_id or "FOODS_2" in str(raw_feats.get("dept_id")) else 0.0,
            "dept_id_FOODS_3": 1.0 if "FOODS_3" in item_id or "FOODS_3" in str(raw_feats.get("dept_id")) else 0.0,
            "dept_id_HOBBIES_1": 1.0 if "HOBBIES_1" in item_id or "HOBBIES_1" in str(raw_feats.get("dept_id")) else 0.0,
            "dept_id_HOUSEHOLD_1": 1.0 if "HOUSEHOLD_1" in item_id or "HOUSEHOLD_1" in str(raw_feats.get("dept_id")) else 0.0,
        }

        for col in self.feature_names:
            if col in raw_feats:
                aligned[col] = float(raw_feats[col])
            elif col in categorical_defaults:
                aligned[col] = categorical_defaults[col]
            else:
                aligned[col] = 0.0

        return aligned

    def predict(self, request: ForecastRequest) -> ForecastResponse:
        """
        Generate leakage-safe demand predictions for items in request payload.

        Args:
            request: Validated ForecastRequest schema containing 1 or more items.

        Returns:
            ForecastResponse schema with non-negative predictions clipped at 0.0.
        """
        if not request.items:
            raise ValueError("Forecast request contains no items.")

        feature_rows = [self._align_feature_row(item) for item in request.items]
        X_df = pd.DataFrame(feature_rows, columns=self.feature_names)

        # Generate raw predictions from model
        if hasattr(self.model, "predict"):
            raw_predictions = self.model.predict(X_df)
        else:
            raise AttributeError(f"Loaded model object {type(self.model)} has no 'predict' method.")

        # Ensure raw_predictions is array-like
        raw_predictions = np.asarray(raw_predictions).flatten()

        predictions: List[SinglePrediction] = []
        for item, raw_pred in zip(request.items, raw_predictions):
            # Clip negative predictions at 0.0 to maintain non-negative demand semantics
            clipped_pred = float(np.maximum(0.0, raw_pred))
            rounded_pred = int(np.round(clipped_pred))

            predictions.append(
                SinglePrediction(
                    item_id=item.item_id,
                    store_id=item.store_id,
                    date=item.date,
                    predicted_demand=clipped_pred,
                    rounded_demand=rounded_pred
                )
            )

        return ForecastResponse(
            status="success",
            model_name=self.model_name,
            model_version=self.container.model_version,
            predictions=predictions,
            count=len(predictions)
        )
