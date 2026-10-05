import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI

from app.api.v1.forecast import router as forecast_router
from app.inference.model_loader import load_trained_model, get_loaded_model

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s - %(message)s"
)
logger = logging.getLogger("retailai-ml-service")


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Lifespan context manager for loading model artifact on startup."""
    logger.info("Initializing RetailAI ML Serving application...")
    try:
        container = load_trained_model()
        logger.info(f"Loaded ML model '{container.model_name}' successfully on startup.")
    except Exception as e:
        logger.warning(f"Could not preload model artifact on startup: {str(e)}")
    yield
    logger.info("Shutting down RetailAI ML Serving application...")


app = FastAPI(
    title="RetailAI ML Service",
    description="Production ML Model Serving & Forecast API for Retail Demand Forecasting",
    version="1.0.0",
    lifespan=lifespan
)

# Include API Routers
app.include_router(forecast_router)


@app.get("/health", tags=["System"])
def health_check():
    """
    Health check endpoint returning service and model operational status.
    """
    model_container = get_loaded_model()
    model_loaded = model_container is not None
    
    return {
        "status": "UP",
        "service": "retailai-ml-service",
        "model_loaded": model_loaded,
        "model_name": model_container.model_name if model_loaded else None,
        "feature_count": len(model_container.feature_names) if model_loaded else 0
    }
