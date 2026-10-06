from typing import Dict, List, Optional
from pydantic import BaseModel, Field


class ForecastItem(BaseModel):
    """Schema for a single product/store demand forecast request item."""
    item_id: str = Field(..., description="Product identifier (e.g. FOODS_1_001)", json_schema_extra={"example": "FOODS_1_001"})
    store_id: str = Field(..., description="Store identifier (e.g. CA_1)", json_schema_extra={"example": "CA_1"})
    date: Optional[str] = Field(None, description="Target forecast date (YYYY-MM-DD)", json_schema_extra={"example": "2016-04-25"})
    features: Dict[str, float] = Field(
        ...,
        description="Feature dictionary containing numerical lag, rolling, calendar, and price features."
    )


class ForecastRequest(BaseModel):
    """Container schema for demand forecast requests (single or batch)."""
    items: List[ForecastItem] = Field(..., description="List of items to generate demand forecasts for.")


class SinglePrediction(BaseModel):
    """Schema for a single item demand forecast result."""
    item_id: str
    store_id: str
    date: Optional[str] = None
    predicted_demand: float = Field(..., description="Continuous non-negative demand forecast (clipped at 0.0)")
    rounded_demand: int = Field(..., description="Discrete integer unit demand recommendation")


class ForecastResponse(BaseModel):
    """Schema for demand forecast API response."""
    status: str = "success"
    model_name: str = "Random Forest"
    model_version: Optional[str] = Field(None, description="Model version identifier if persisted in metadata")
    predictions: List[SinglePrediction]
    count: int
