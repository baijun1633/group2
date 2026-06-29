from fastapi import APIRouter
from services.tasks_service import compute_similarity_matrix, sync_kg_data

router = APIRouter(prefix="/api/v1/tasks", tags=["离线任务"])


@router.post("/compute-similarity")
def run_similarity():
    """手动触发相似度矩阵计算"""
    count = compute_similarity_matrix()
    return {
        "code": 0, "message": "success",
        "data": {"similarityPairs": count, "message": "相似度矩阵计算完成"},
    }


@router.post("/sync-kg")
def run_kg_sync():
    """手动触发知识图谱同步"""
    count = sync_kg_data()
    return {
        "code": 0, "message": "success",
        "data": {"recordsSynced": count, "message": "知识图谱数据同步完成"},
    }
