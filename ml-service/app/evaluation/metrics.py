from typing import Dict
import numpy as np

def calculate_wape(actual: np.ndarray, prediction: np.ndarray) -> float:
    """
    Calculate Weighted Absolute Percentage Error (WAPE).
    WAPE = sum(|actual - prediction|) / sum(|actual|)
    
    Handles zero denominator safely.
    """
    actual_arr = np.asarray(actual, dtype=float)
    pred_arr = np.asarray(prediction, dtype=float)
    
    total_actual = np.sum(np.abs(actual_arr))
    if total_actual == 0:
        return 0.0
        
    wape = np.sum(np.abs(actual_arr - pred_arr)) / total_actual
    return float(wape)

def calculate_mae(actual: np.ndarray, prediction: np.ndarray) -> float:
    """Calculate Mean Absolute Error (MAE)."""
    actual_arr = np.asarray(actual, dtype=float)
    pred_arr = np.asarray(prediction, dtype=float)
    mae = np.mean(np.abs(actual_arr - pred_arr))
    return float(mae)

def calculate_rmse(actual: np.ndarray, prediction: np.ndarray) -> float:
    """Calculate Root Mean Squared Error (RMSE)."""
    actual_arr = np.asarray(actual, dtype=float)
    pred_arr = np.asarray(prediction, dtype=float)
    rmse = np.sqrt(np.mean((actual_arr - pred_arr) ** 2))
    return float(rmse)

def evaluate_predictions(actual: np.ndarray, prediction: np.ndarray) -> Dict[str, float]:
    """Calculate WAPE, MAE, and RMSE for a set of actual and predicted values."""
    wape = calculate_wape(actual, prediction)
    mae = calculate_mae(actual, prediction)
    rmse = calculate_rmse(actual, prediction)
    
    return {
        "WAPE": round(wape, 4),
        "WAPE_pct": round(wape * 100, 2),
        "MAE": round(mae, 4),
        "RMSE": round(rmse, 4)
    }
