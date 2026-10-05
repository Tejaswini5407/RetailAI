from fastapi import FastAPI

app = FastAPI(
    title="RetailAI ML Service",
    description="Intelligent Demand Forecasting Service",
    version="0.0.1"
)

@app.get("/health")
def health_check():
    return {
        "status": "UP",
        "service": "retailai-ml-service"
    }
