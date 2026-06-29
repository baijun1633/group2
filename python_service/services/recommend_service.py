import logging
from collections import defaultdict
from typing import Optional

from db import get_mysql_conn, get_neo4j_driver

log = logging.getLogger(__name__)


def _dict_fetchall(cursor):
    return cursor.fetchall()


# ==================== 协同过滤 (ItemCF) ====================

def get_user_book_scores(user_id: int) -> dict[int, float]:
    """获取用户交互过的图书及评分（显式评分 + 隐式反馈）"""
    scores = {}
    conn = get_mysql_conn()
    try:
        with conn.cursor() as cur:
            # 显式评分
            cur.execute("SELECT book_id, score FROM book_ratings WHERE user_id = %s", (user_id,))
            for row in cur.fetchall():
                scores[row["book_id"]] = float(row["score"])

            # 隐式反馈（书架）
            cur.execute("""
                SELECT sb.book_id, sb.reading_status
                FROM shelf_books sb JOIN shelves s ON sb.shelf_id = s.shelf_id
                WHERE s.user_id = %s
            """, (user_id,))
            for row in cur.fetchall():
                status = row["reading_status"]
                implicit = 3.0 if status == 2 else 2.0 if status == 1 else 1.0
                scores.setdefault(row["book_id"], implicit)
                scores[row["book_id"]] = max(scores[row["book_id"]], implicit)
    finally:
        conn.close()
    return scores


def get_co_occurred_books(book_id: int) -> list[int]:
    """查找与指定图书共现的其他图书（通过评分表）"""
    conn = get_mysql_conn()
    try:
        with conn.cursor() as cur:
            cur.execute("""
                SELECT DISTINCT r2.book_id
                FROM book_ratings r1
                JOIN book_ratings r2 ON r1.user_id = r2.user_id AND r2.book_id <> r1.book_id
                WHERE r1.book_id = %s
                LIMIT 200
            """, (book_id,))
            return [row["book_id"] for row in cur.fetchall()]
    finally:
        conn.close()


def recommend_by_itemcf(user_id: int, limit: int = 10) -> list[dict]:
    """ItemCF 推荐"""
    user_scores = get_user_book_scores(user_id)
    if not user_scores:
        return []

    user_book_ids = set(user_scores.keys())
    candidate_scores = defaultdict(float)
    candidate_counts = defaultdict(int)

    for book_id, user_score in user_scores.items():
        co_books = get_co_occurred_books(book_id)
        for co_book_id in co_books:
            if co_book_id in user_book_ids:
                continue
            candidate_scores[co_book_id] += user_score
            candidate_counts[co_book_id] += 1

    if not candidate_scores:
        return []

    max_score = max(candidate_scores.values())
    sorted_candidates = sorted(candidate_scores.items(), key=lambda x: x[1], reverse=True)[:limit]

    results = []
    conn = get_mysql_conn()
    try:
        with conn.cursor() as cur:
            for book_id, score in sorted_candidates:
                cur.execute("SELECT book_id, title, author, cover_image, avg_rating FROM books WHERE book_id = %s", (book_id,))
                book = cur.fetchone()
                if not book:
                    continue
                normalized = round(score / max_score, 2)
                co_count = candidate_counts[book_id]
                results.append({
                    "bookId": book["book_id"],
                    "title": book["title"],
                    "author": book["author"],
                    "coverImage": book["cover_image"],
                    "avgRating": float(book["avg_rating"]) if book["avg_rating"] else None,
                    "score": normalized,
                    "reason": f"与您读过的{co_count}本书存在关联",
                    "source": "ITEMCF",
                })
    finally:
        conn.close()
    return results


# ==================== 知识图谱推荐 ====================

def recommend_by_kg(user_id: int, limit: int = 10) -> list[dict]:
    """基于知识图谱的推荐"""
    # 获取用户已读图书
    user_scores = get_user_book_scores(user_id)
    if not user_scores:
        return []

    read_book_ids = list(user_scores.keys())[:5]
    driver = get_neo4j_driver()
    results = []

    try:
        with driver.session() as session:
            # 同作者
            cypher = """
                MATCH (b1:Book)-[:WRITTEN_BY]->(a:Author)<-[:WRITTEN_BY]-(b2:Book)
                WHERE b1.bookId IN $bookIds AND b1.bookId <> b2.bookId
                RETURN DISTINCT b2.bookId AS bookId, b2.title AS title, a.name AS author,
                       b2.avgRating AS avgRating, '同作者' AS relation
                LIMIT $limit
            """
            records = session.run(cypher, bookIds=read_book_ids, limit=limit)
            for r in records:
                book_id_val = r["bookId"]
                results.append({
                    "bookId": int(book_id_val) if book_id_val is not None else None,
                    "title": r["title"],
                    "author": r.get("author"),
                    "avgRating": r["avgRating"],
                    "reason": f"与您读过的书同作者: {r.get('author', '?')}",
                    "source": "KG",
                })

            # 同分类
            cypher = """
                MATCH (b1:Book)-[:BELONGS_TO]->(c:Category)<-[:BELONGS_TO]-(b2:Book)
                WHERE b1.bookId IN $bookIds AND b1.bookId <> b2.bookId
                RETURN DISTINCT b2.bookId AS bookId, b2.title AS title, b2.avgRating AS avgRating,
                       c.name AS categoryName
                LIMIT $limit
            """
            records = session.run(cypher, bookIds=read_book_ids, limit=limit)
            for r in records:
                book_id_val = r["bookId"]
                results.append({
                    "bookId": int(book_id_val) if book_id_val is not None else None,
                    "title": r["title"],
                    "avgRating": r["avgRating"],
                    "reason": f"与您读过的书同分类: {r['categoryName']}",
                    "source": "KG",
                })
    finally:
        driver.close()

    # 去重
    seen = set()
    deduped = []
    for item in results:
        bid = item.get("bookId")
        if bid and bid not in seen:
            seen.add(bid)
            deduped.append(item)
    return deduped[:limit]


# ==================== 热门/新书 ====================

def get_hot_books(limit: int = 10) -> list[dict]:
    """热门图书"""
    conn = get_mysql_conn()
    try:
        with conn.cursor() as cur:
            cur.execute("""
                SELECT book_id, title, author, cover_image, avg_rating, view_count, rating_count
                FROM books WHERE status = 1
                ORDER BY view_count DESC, rating_count DESC, avg_rating DESC
                LIMIT %s
            """, (limit,))
            results = []
            for b in cur.fetchall():
                results.append({
                    "bookId": b["book_id"],
                    "title": b["title"],
                    "author": b["author"],
                    "coverImage": b["cover_image"],
                    "avgRating": float(b["avg_rating"]) if b["avg_rating"] else None,
                    "score": 0.5,
                    "reason": f"热门图书({b['view_count']}次浏览，{b['rating_count']}人评价)",
                    "source": "HOT",
                })
            return results
    finally:
        conn.close()


def get_new_books(months: int = 6, limit: int = 10) -> list[dict]:
    """新书推荐"""
    conn = get_mysql_conn()
    try:
        with conn.cursor() as cur:
            cur.execute("""
                SELECT book_id, title, author, cover_image, avg_rating, publish_date
                FROM books WHERE status = 1 AND publish_date >= DATE_SUB(CURDATE(), INTERVAL %s MONTH)
                ORDER BY publish_date DESC, create_time DESC
                LIMIT %s
            """, (months, limit))
            results = []
            for b in cur.fetchall():
                pub = b["publish_date"].isoformat() if b["publish_date"] else "近期"
                results.append({
                    "bookId": b["book_id"],
                    "title": b["title"],
                    "author": b["author"],
                    "coverImage": b["cover_image"],
                    "avgRating": float(b["avg_rating"]) if b["avg_rating"] else None,
                    "score": 0.5,
                    "reason": f"新书推荐(出版于{pub})",
                    "source": "NEW",
                })
            return results
    finally:
        conn.close()


# ==================== 混合推荐 ====================

def get_home_recommend(user_id: Optional[int], limit: int = 10,
                       w_kg: float = 0.4, w_cf: float = 0.3,
                       w_hot: float = 0.15, w_new: float = 0.15) -> list[dict]:
    """混合推荐入口"""
    if user_id is None:
        # 未登录：热门 + 新书兜底
        hot = get_hot_books(limit // 2 + 1)
        new = get_new_books(6, limit // 2 + 1)
        merged = _merge_and_dedup(hot, new, limit)
        for item in merged:
            item["source"] = "FALLBACK"
        return merged

    # 登录用户：混合策略
    kg_limit = int(limit * w_kg) + 2
    cf_limit = int(limit * w_cf) + 2
    hot_limit = int(limit * w_hot) + 1
    new_limit = int(limit * w_new) + 1

    kg_items = recommend_by_kg(user_id, kg_limit)
    cf_items = recommend_by_itemcf(user_id, cf_limit)
    hot_items = get_hot_books(hot_limit)
    new_items = get_new_books(6, new_limit)

    return _weighted_merge(kg_items, cf_items, hot_items, new_items,
                           w_kg, w_cf, w_hot, w_new, limit, user_id)


def _merge_and_dedup(list1: list[dict], list2: list[dict], limit: int) -> list[dict]:
    seen = set()
    merged = []
    for item in list1 + list2:
        bid = item.get("bookId")
        if bid not in seen:
            seen.add(bid)
            merged.append(item)
    return merged[:limit]


def _weighted_merge(kg, cf, hot, new, w_kg, w_cf, w_hot, w_new, limit, user_id):
    """加权合并各路推荐结果"""
    book_scores = {}  # bookId -> (weighted_score, item, sources)

    def process(items, weight, source):
        for rank, item in enumerate(items):
            bid = item.get("bookId")
            if bid is None:
                continue
            # 基于排名的分数 (rank 0 -> 1.0, rank 1 -> 0.9, ...)
            rank_score = max(0.1, 1.0 - rank * 0.1)
            weighted = rank_score * weight
            if bid in book_scores:
                old_score, old_item, old_sources = book_scores[bid]
                book_scores[bid] = (old_score + weighted, old_item, old_sources + [source])
            else:
                book_scores[bid] = (weighted, item, [source])

    process(kg, w_kg, "KG")
    process(cf, w_cf, "ITEMCF")
    process(hot, w_hot, "HOT")
    process(new, w_new, "NEW")

    # 排除已读
    if user_id:
        read_ids = set(get_user_book_scores(user_id).keys())
        book_scores = {k: v for k, v in book_scores.items() if k not in read_ids}

    # 排序
    sorted_items = sorted(book_scores.values(), key=lambda x: x[0], reverse=True)[:limit]

    results = []
    for score, item, sources in sorted_items:
        result = dict(item)
        result["score"] = round(score, 3)
        result["sources"] = list(set(sources))
        results.append(result)
    return results
