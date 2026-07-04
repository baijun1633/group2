from app.algorithm.kg_reasoning import kg_recommend_books
from typing import List, Dict
import logging

logger = logging.getLogger(__name__)


class RecommendService:
    @staticmethod
    def kg_recommend(
        preferred_tags: List[str],
        top3_categories: List[str],
        top5_categories: List[str],
        preferred_authors: List[str],
        liked_book_ids: List[int],
        limit: int = 10
    ) -> List[Dict]:
        """知识图谱推荐通路"""
        return kg_recommend_books(
            preferred_tags, top3_categories, top5_categories,
            preferred_authors, liked_book_ids, limit
        )


recommend_service = RecommendService()
