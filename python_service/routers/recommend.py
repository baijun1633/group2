from fastapi import APIRouter, Query
from pydantic import BaseModel
from typing import Optional
from services.recommend_service import (
    get_home_recommend, get_hot_books, get_new_books,
    recommend_by_itemcf, recommend_by_kg,
)
from config import WEIGHT_KG, WEIGHT_ITEMCF, WEIGHT_HOT, WEIGHT_NEW

router = APIRouter(prefix="/api/v1/recommend", tags=["推荐服务"])


class RecommendRequest(BaseModel):
    userId: Optional[int] = None
    limit: int = 10


@router.post("/home")
def home_recommend(req: RecommendRequest):
    """首页混合推荐"""
    items = get_home_recommend(
        req.userId, req.limit,
        WEIGHT_KG, WEIGHT_ITEMCF, WEIGHT_HOT, WEIGHT_NEW,
    )
    return {"code": 0, "message": "success", "data": items}


@router.get("/hot")
def hot_books(days: int = Query(90, ge=1), limit: int = Query(10, ge=1, le=50)):
    """热门图书"""
    return {"code": 0, "message": "success", "data": get_hot_books(limit)}


@router.get("/new")
def new_books(months: int = Query(6, ge=1), limit: int = Query(10, ge=1, le=50)):
    """新书推荐"""
    return {"code": 0, "message": "success", "data": get_new_books(months, limit)}


@router.get("/itemcf/{user_id}")
def itemcf_recommend(user_id: int, limit: int = Query(10, ge=1, le=50)):
    """ItemCF 协同过滤推荐"""
    return {"code": 0, "message": "success", "data": recommend_by_itemcf(user_id, limit)}


@router.get("/kg/{user_id}")
def kg_recommend(user_id: int, limit: int = Query(10, ge=1, le=50)):
    """知识图谱推荐"""
    return {"code": 0, "message": "success", "data": recommend_by_kg(user_id, limit)}
