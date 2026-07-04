from pydantic import BaseModel
from typing import List, Optional


class RecommendBook(BaseModel):
    book_id: int
    book_title: str
    match_score: float
    recommend_reason: str
    path_hops: int
    recommend_type: str


class RecommendResponse(BaseModel):
    code: int
    data: List[RecommendBook]
    msg: str
