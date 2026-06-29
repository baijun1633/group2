from fastapi import APIRouter, Query
from pydantic import BaseModel
from typing import Optional
from services.kg_service import query_cypher, get_graph_stats, get_subgraph

router = APIRouter(prefix="/api/v1/kg", tags=["知识图谱"])


class CypherRequest(BaseModel):
    cypher: str
    limit: int = 100


@router.post("/query")
def cypher_query(req: CypherRequest):
    """Cypher 查询"""
    try:
        data = query_cypher(req.cypher, req.limit)
        return {"code": 0, "message": "success", "data": data}
    except ValueError as e:
        return {"code": 4001, "message": str(e), "data": None}


@router.get("/stats")
def graph_stats():
    """图谱统计"""
    return {"code": 0, "message": "success", "data": get_graph_stats()}


@router.get("/subgraph")
def subgraph(bookId: Optional[int] = None, depth: int = Query(2, ge=1, le=4),
             limit: int = Query(50, ge=1, le=200)):
    """获取子图"""
    return {"code": 0, "message": "success", "data": get_subgraph(bookId, depth, limit)}
