import json
import logging
import os
import time
from collections import defaultdict
from datetime import datetime
from math import sqrt

from db import get_mysql_conn, get_neo4j_driver

log = logging.getLogger(__name__)

EXPORT_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), "tmp", "kg_export")


def compute_similarity_matrix() -> int:
    """计算图书相似度矩阵（全量重算）"""
    start = time.time()
    conn = get_mysql_conn()
    try:
        with conn.cursor() as cur:
            # 1. 构建用户-物品评分矩阵
            user_item = defaultdict(dict)

            # 显式评分
            cur.execute("SELECT user_id, book_id, score FROM book_ratings")
            for row in cur.fetchall():
                user_item[row["user_id"]][row["book_id"]] = float(row["score"])

            # 隐式反馈
            cur.execute("""
                SELECT s.user_id, sb.book_id, sb.reading_status
                FROM shelf_books sb JOIN shelves s ON sb.shelf_id = s.shelf_id
            """)
            for row in cur.fetchall():
                uid, bid = row["user_id"], row["book_id"]
                implicit = 3.0 if row["reading_status"] == 2 else 2.0 if row["reading_status"] == 1 else 1.0
                user_item[uid][bid] = max(user_item[uid].get(bid, 0), implicit)

            if not user_item:
                log.info("无用户行为数据，跳过相似度计算")
                return 0

            # 2. 构建物品-用户倒排表
            item_users = defaultdict(set)
            for uid, books in user_item.items():
                for bid in books:
                    item_users[bid].add(uid)

            # 3. 计算余弦相似度
            book_ids = list(item_users.keys())
            similarities = []

            for i in range(len(book_ids)):
                book_a = book_ids[i]
                users_a = item_users[book_a]
                for j in range(i + 1, len(book_ids)):
                    book_b = book_ids[j]
                    users_b = item_users[book_b]
                    common = users_a & users_b
                    if len(common) < 1:
                        continue

                    dot = norm_a = norm_b = 0.0
                    for uid in common:
                        sa = user_item[uid].get(book_a, 0)
                        sb = user_item[uid].get(book_b, 0)
                        dot += sa * sb
                        norm_a += sa * sa
                        norm_b += sb * sb

                    denom = sqrt(norm_a) * sqrt(norm_b)
                    if denom == 0:
                        continue
                    sim = dot / denom
                    if sim < 0.01:
                        continue

                    similarities.append((
                        min(book_a, book_b), max(book_a, book_b),
                        round(sim, 6), len(common),
                    ))

            # 4. 清空旧数据，插入新数据
            cur.execute("DELETE FROM book_similarity")
            if similarities:
                cur.executemany(
                    "INSERT INTO book_similarity (book_id_a, book_id_b, similarity, co_count, compute_time) "
                    "VALUES (%s, %s, %s, %s, %s)",
                    [(a, b, s, c, datetime.now()) for a, b, s, c in similarities]
                )
            conn.commit()

            elapsed = time.time() - start
            log.info(f"相似度矩阵计算完成: 用户={len(user_item)}, 图书={len(book_ids)}, "
                     f"对数={len(similarities)}, 耗时={elapsed:.2f}s")
            return len(similarities)
    finally:
        conn.close()


def sync_kg_data() -> int:
    """导出知识图谱数据到 JSON"""
    os.makedirs(EXPORT_DIR, exist_ok=True)
    driver = get_neo4j_driver()
    total = 0

    try:
        with driver.session() as session:
            # 图书节点
            books = [dict(r) for r in session.run(
                "MATCH (b:Book) RETURN b.bookId AS bookId, b.title AS title, "
                "b.isbn AS isbn, b.avgRating AS avgRating")]
            _write_json("books.json", books)
            total += len(books)

            # 作者节点
            authors = [dict(r) for r in session.run("MATCH (a:Author) RETURN a.name AS name")]
            _write_json("authors.json", authors)
            total += len(authors)

            # 分类节点
            categories = [dict(r) for r in session.run(
                "MATCH (c:Category) RETURN c.categoryId AS categoryId, c.name AS name")]
            _write_json("categories.json", categories)
            total += len(categories)

            # 出版社节点
            publishers = [dict(r) for r in session.run("MATCH (p:Publisher) RETURN p.name AS name")]
            _write_json("publishers.json", publishers)
            total += len(publishers)

            # 标签节点
            tags = [dict(r) for r in session.run("MATCH (t:Tag) RETURN t.name AS name")]
            _write_json("tags.json", tags)
            total += len(tags)

            log.info(f"KG同步完成: 图书={len(books)}, 作者={len(authors)}, "
                     f"分类={len(categories)}, 出版社={len(publishers)}, 标签={len(tags)}")
    finally:
        driver.close()

    # 记录日志到 MySQL
    conn = get_mysql_conn()
    try:
        with conn.cursor() as cur:
            cur.execute(
                "INSERT INTO kg_sync_log (sync_type, status, records_synced, start_time, end_time) "
                "VALUES (%s, %s, %s, %s, %s)",
                ("full", "success", total, datetime.now(), datetime.now()))
            conn.commit()
    finally:
        conn.close()

    return total


def _write_json(filename: str, data):
    path = os.path.join(EXPORT_DIR, filename)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
