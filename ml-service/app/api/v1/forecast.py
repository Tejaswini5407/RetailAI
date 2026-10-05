import logging
from fastapi import APIRouter, HTTPException, status

from app.schemas.forecast import ForecastRequest, ForecastResponse
from app.inference.model_loader import load_trained_model, ModelNotFoundError, ModelLoadError
from app.inference.predictor import DemandPredictor

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1", tags=["Inference"])


@router.post(
    "/forecast",
    response_model=ForecastResponse,
    status_code=status.HTTP_200_OK,
    summary="Generate daily demand forecast",
    description="Accepts feature payload for product/store items and returns leakage-safe daily demand forecasts clipped at 0.0."
)
def generate_forecast(request: ForecastRequest) -> ForecastResponse:
    """
    POST /api/v1/forecast endpoint handler.
    """
    try:
        model_container = load_trained_model()
        predictor = DemandPredictor(model_container=model_container)
        return predictor.predict(request)
    except ModelNotFoundError as e:
        logger.error(f"Model not found: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=f"Forecast model service unavailable: {str(e)}"
        )
    except ModelLoadError as e:
        logger.error(f"Model load error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to load model artifact: {str(e)}"
        )
    except ValueError as e:
        logger.warning(f"Invalid request payload: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(e)
        )
    except Exception as e:
        logger.exception("Unexpected error during forecast generation")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"An unexpected error occurred during prediction: {str(e)}"
        )
