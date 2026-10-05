import os
import pandas as pd

DEFAULT_DATASET_DIR = os.path.abspath(
    os.path.join(os.path.dirname(__file__), "..", "..", "..", "dataset")
)

def load_calendar_data(dataset_dir: str = DEFAULT_DATASET_DIR) -> pd.DataFrame:
    """Load calendar.csv containing dates, day-of-week, events, and snap markers."""
    file_path = os.path.join(dataset_dir, "calendar.csv")
    if not os.path.exists(file_path):
        raise FileNotFoundError(f"Calendar file not found at: {file_path}")
    return pd.read_csv(file_path)

def load_sales_validation_data(dataset_dir: str = DEFAULT_DATASET_DIR) -> pd.DataFrame:
    """Load sales_train_validation.csv containing historical daily sales up to d_1913."""
    file_path = os.path.join(dataset_dir, "sales_train_validation.csv")
    if not os.path.exists(file_path):
        raise FileNotFoundError(f"Sales validation file not found at: {file_path}")
    return pd.read_csv(file_path)

def load_sales_evaluation_data(dataset_dir: str = DEFAULT_DATASET_DIR) -> pd.DataFrame:
    """Load sales_train_evaluation.csv containing historical daily sales up to d_1941."""
    file_path = os.path.join(dataset_dir, "sales_train_evaluation.csv")
    if not os.path.exists(file_path):
        raise FileNotFoundError(f"Sales evaluation file not found at: {file_path}")
    return pd.read_csv(file_path)

def load_sell_prices_data(dataset_dir: str = DEFAULT_DATASET_DIR) -> pd.DataFrame:
    """Load sell_prices.csv containing product pricing per store and week."""
    file_path = os.path.join(dataset_dir, "sell_prices.csv")
    if not os.path.exists(file_path):
        raise FileNotFoundError(f"Sell prices file not found at: {file_path}")
    return pd.read_csv(file_path)
