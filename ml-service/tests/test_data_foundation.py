import os
import sys
import pandas as pd

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from app.data.validator import validate_sales_schema
from app.preprocessing.subset_generator import generate_development_subset

def test_dev_subset_file_exists():
    subset_path = os.path.abspath(
        os.path.join(os.path.dirname(__file__), "..", "..", "dataset", "dev_subset_sales.csv")
    )
    assert os.path.exists(subset_path), f"Development subset file missing at {subset_path}"
    df = pd.read_csv(subset_path)
    assert len(df) == 100
    assert df["store_id"].nunique() == 2
    assert df["item_id"].nunique() == 50

def test_sales_schema_validation():
    mock_data = {
        "id": ["item1_store1_validation"],
        "item_id": ["item1"],
        "dept_id": ["dept1"],
        "cat_id": ["cat1"],
        "store_id": ["store1"],
        "state_id": ["state1"],
        "d_1": [5]
    }
    df = pd.DataFrame(mock_data)
    report = validate_sales_schema(df)
    assert report["valid_schema"] is True
    assert report["unique_series"] is True
    assert report["total_series"] == 1
