import os
import sys
import logging
from typing import Dict, Any, List, Optional
import joblib

logger = logging.getLogger(__name__)

# Default model path relative to ml-service directory root
DEFAULT_MODEL_PATH = os.path.join(
    os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))),
    "models",
    "best_model_random_forest.joblib"
)


class ModelNotFoundError(Exception):
    """Custom exception raised when the trained model artifact cannot be located on disk."""
    pass


class ModelLoadError(Exception):
    """Custom exception raised when loading or unpickling the model artifact fails."""
    pass


class ModelContainer:
    """Encapsulates loaded ML model object and associated feature schema metadata."""

    def __init__(
        self,
        model_object: Any,
        model_name: str,
        feature_names: List[str],
        metrics: Optional[Dict[str, float]] = None,
        split_info: Optional[Dict[str, Any]] = None
    ):
        self.model_object = model_object
        self.model_name = model_name
        self.feature_names = feature_names
        self.metrics = metrics or {}
        self.split_info = split_info or {}


_LOADED_MODEL_CONTAINER: Optional[ModelContainer] = None


def load_trained_model(model_path: Optional[str] = None, force_reload: bool = False) -> ModelContainer:
    """
    Load trained Random Forest model artifact from disk cleanly.

    Args:
        model_path: Optional explicit file path to joblib model artifact.
        force_reload: If True, bypass cached in-memory container.

    Returns:
        ModelContainer holding model object and metadata.

    Raises:
        ModelNotFoundError: If artifact file does not exist.
        ModelLoadError: If loading or unpickling fails.
    """
    global _LOADED_MODEL_CONTAINER

    if _LOADED_MODEL_CONTAINER is not None and not force_reload:
        return _LOADED_MODEL_CONTAINER

    target_path = model_path or os.getenv("MODEL_PATH", DEFAULT_MODEL_PATH)
    target_path = os.path.abspath(target_path)

    if not os.path.exists(target_path):
        error_msg = f"Model artifact file not found at path: '{target_path}'"
        logger.error(error_msg)
        raise ModelNotFoundError(error_msg)

    try:
        # Ensure ml-service root directory is in sys.path so pickle can resolve app.models.random_forest
        ml_service_dir = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
        if ml_service_dir not in sys.path:
            sys.path.insert(0, ml_service_dir)

        artifact = joblib.load(target_path)

        if isinstance(artifact, dict):
            model_obj = artifact.get("model_object")
            model_name = artifact.get("model_name", "Random Forest")
            feature_names = artifact.get("feature_names", [])
            metrics = artifact.get("metrics", {})
            split_info = artifact.get("split_info", {})
        else:
            model_obj = artifact
            model_name = getattr(model_obj, "model_name", "Random Forest")
            feature_names = getattr(model_obj, "feature_names", [])
            metrics = {}
            split_info = {}

        if model_obj is None:
            raise ModelLoadError("Loaded joblib artifact does not contain a valid model object.")

        container = ModelContainer(
            model_object=model_obj,
            model_name=model_name,
            feature_names=feature_names,
            metrics=metrics,
            split_info=split_info
        )

        _LOADED_MODEL_CONTAINER = container
        logger.info(f"Successfully loaded model '{model_name}' with {len(feature_names)} features from '{target_path}'")
        return container

    except ModelNotFoundError:
        raise
    except Exception as e:
        error_msg = f"Failed to load model artifact from '{target_path}': {str(e)}"
        logger.error(error_msg)
        raise ModelLoadError(error_msg) from e


def get_loaded_model() -> Optional[ModelContainer]:
    """Return currently cached model container or None if not loaded yet."""
    return _LOADED_MODEL_CONTAINER
