from fastapi import APIRouter, Query
from app.schemas.kg import GraphResponse
from app.service.kg_service import kg_service

router = APIRouter(prefix="/kg", tags=["知识图谱"])


@router.get("/graph/{book_id}", response_model=GraphResponse, summary="查询图书关联图谱")
def get_book_graph(
    book_id: int,
    depth: int = Query(default=2, ge=1, le=3, description="关联深度，最多3跳")
):
    """获取指定图书的知识图谱节点和关系，用于前端可视化"""
    return kg_service.get_book_graph(book_id, depth)
