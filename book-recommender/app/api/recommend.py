from fastapi import APIRouter, Query
from typing import List
from app.service.recommend_service import recommend_service
from app.schemas.recommend import RecommendResponse

router = APIRouter(prefix="/recommend", tags=["推荐服务"])


@router.get("/kg", response_model=RecommendResponse, summary="知识图谱推荐")
def kg_recommend(
    preferred_tags: List[str] = Query(..., description="用户偏好标签列表"),
    top3_categories: List[str] = Query(..., description="用户高频Top3分类"),
    top5_categories: List[str] = Query(..., description="用户高频Top5分类"),
    preferred_authors: List[str] = Query(..., description="用户偏好作者列表"),
    liked_book_ids: List[int] = Query(..., description="用户已读/喜欢的图书ID"),
    limit: int = Query(default=10, ge=1, le=50, description="返回条数")
):
    result = recommend_service.kg_recommend(
        preferred_tags, top3_categories, top5_categories,
        preferred_authors, liked_book_ids, limit
    )
    return {"code": 200, "data": result, "msg": "success"}
