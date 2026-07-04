import logging
import random
from app.database.neo4j_client import neo4j_client

logger = logging.getLogger(__name__)

def kg_recommend_books(preferred_tags, top3_categories, top5_categories, preferred_authors, liked_book_ids, limit=10):
    """
    基于知识图谱多跳推理的图书推荐
    多路径命中时分数累加，推荐理由根据命中特征组合生成
    """
    if not liked_book_ids:
        liked_book_ids = []
    
    cypher = """
    // 路径1：标签 -> 图书 -> 作者 -> 图书
    MATCH (t:Tag)<-[:tagged_as]-(b:Book)-[:written_by]->(a:Author)<-[:written_by]-(rec:Book)
    WHERE t.name IN $tags AND NOT rec.bookId IN $exclude_ids
    RETURN rec, 3 AS hops, b.title AS source_book, a.name AS match_author, 'author_path' AS path_type, t.name AS match_tag

    UNION ALL

    // 路径2：图书 -> 分类 -> 图书
    MATCH (b:Book)-[:belongs_to]->(c:Category)<-[:belongs_to]-(rec:Book)
    WHERE c.name IN $categories AND NOT rec.bookId IN $exclude_ids
    RETURN rec, 2 AS hops, c.name AS source_book, '' AS match_author, 'category_path' AS path_type, '' AS match_tag

    UNION ALL

    // 路径3：作者 -> 图书 -> 出版社 -> 图书
    MATCH (a:Author)<-[:written_by]-(b:Book)-[:published_by]->(p:Publisher)<-[:published_by]-(rec:Book)
    WHERE a.name IN $authors AND NOT rec.bookId IN $exclude_ids
    RETURN rec, 3 AS hops, b.title AS source_book, p.name AS match_author, 'publisher_path' AS path_type, '' AS match_tag
    """

    try:
        records = neo4j_client.execute_query(
            cypher,
            {
                "tags": preferred_tags,
                "categories": top5_categories,
                "authors": preferred_authors,
                "exclude_ids": liked_book_ids
            }
        )
        
        book_scores = {}
        
        for record in records:
            book = record["rec"]
            book_id = book["bookId"]
            title = book["title"]
            hops = record["hops"]
            path_type = record["path_type"]
            match_author = record["match_author"]
            source_book = record["source_book"]
            match_tag = record.get("match_tag", "")
            
            # 路径权重
            path_weight = 1.0 / hops
            
            # 标签匹配权重
            tag_weight = 1.2 if path_type == 'author_path' else 1.0
            
            # 分类匹配权重
            if path_type == 'category_path' and source_book in top3_categories:
                category_weight = 1.5
            elif path_type == 'category_path':
                category_weight = 1.2
            else:
                category_weight = 1.0
            
            # 作者匹配权重
            author_weight = 1.3 if match_author in preferred_authors else 1.0
            
            # 评分权重（容错）
            avg_rating = book.get("avgRating", 3.0) or 3.0
            rating_count = book.get("ratingCount", 0) or 0
            rating_weight = (avg_rating / 5.0) * (1 + 0.1 * min(rating_count, 100))
            
            # 单路径得分
            single_score = path_weight * tag_weight * category_weight * author_weight * rating_weight
            
            if book_id not in book_scores:
                book_scores[book_id] = {
                    "book_id": book_id,
                    "book_title": title,
                    "match_score": single_score,
                    "path_hops": hops,
                    "recommend_type": "KG",
                    "_features": set(),
                    "_authors": set(),
                    "_categories": set(),
                    "_tags": set(),
                    "_publishers": set()
                }
            else:
                book_scores[book_id]["match_score"] += single_score
            
            # 记录命中的特征
            if path_type == 'author_path' and match_author:
                book_scores[book_id]["_features"].add("author")
                book_scores[book_id]["_authors"].add(match_author)
            if path_type == 'category_path':
                book_scores[book_id]["_features"].add("category")
                book_scores[book_id]["_categories"].add(source_book)
            if path_type == 'publisher_path' and match_author:
                book_scores[book_id]["_features"].add("publisher")
                book_scores[book_id]["_publishers"].add(match_author)
            if match_tag:
                book_scores[book_id]["_features"].add("tag")
                book_scores[book_id]["_tags"].add(match_tag)
        
        # 生成推荐理由
        result_list = []
        for book in book_scores.values():
            features = book["_features"]
            authors = book["_authors"]
            categories = book["_categories"]
            tags = book["_tags"]
            publishers = book["_publishers"]
            title = book["book_title"]
            
            reason_templates = []
            
            # 组合1：作者 + 分类（最精准）
            if "author" in features and "category" in features:
                author_str = "、".join(list(authors)[:2])
                cat_str = "、".join(list(categories)[:2])
                reason_templates.append(f"{author_str}的{cat_str}佳作《{title}》，和您的阅读偏好高度契合")
                reason_templates.append(f"同为{author_str}的{cat_str}代表作，《{title}》也值得一读")
            
            # 组合2：作者 + 标签
            elif "author" in features and "tag" in features:
                author_str = "、".join(list(authors)[:2])
                tag_str = "、".join(list(tags)[:2])
                reason_templates.append(f"您关注的{author_str}作品，{tag_str}题材的《{title}》推荐给您")
            
            # 组合3：分类 + 标签
            elif "category" in features and "tag" in features:
                cat_str = "、".join(list(categories)[:2])
                tag_str = "、".join(list(tags)[:2])
                reason_templates.append(f"{cat_str}分类下，带有{tag_str}标签的热门作品《{title}》")
                reason_templates.append(f"和您喜欢的题材相似，{tag_str}风格的{cat_str}《{title}》")
            
            # 单维度：作者
            elif "author" in features:
                author_str = "、".join(list(authors)[:2])
                reason_templates.append(f"您关注的作者{author_str}的其他作品《{title}》推荐给您")
                reason_templates.append(f"喜欢{author_str}的话，这本《{title}》也别错过")
            
            # 单维度：分类
            elif "category" in features:
                cat_str = "、".join(list(categories)[:2])
                reason_templates.append(f"因为您喜欢{cat_str}题材，同类型的《{title}》也值得一读")
                reason_templates.append(f"{cat_str}分类热门作品《{title}》，符合您的阅读口味")
            
            # 单维度：出版社
            elif "publisher" in features:
                pub_str = "、".join(list(publishers)[:2])
                reason_templates.append(f"{pub_str}出版的优质作品《{title}》推荐给您")
            
            # 兜底
            if not reason_templates:
                reason_templates.append(f"基于您的阅读偏好，为您推荐《{title}》")
            
            # 随机选一条，避免千篇一律
            reason = random.choice(reason_templates)
            
            book["recommend_reason"] = reason
            book["match_score"] = round(book["match_score"], 4)
            # 清理内部字段
            book.pop("_features", None)
            book.pop("_authors", None)
            book.pop("_categories", None)
            book.pop("_tags", None)
            book.pop("_publishers", None)
            
            result_list.append(book)
        
        # 按得分排序
        sorted_books = sorted(result_list, key=lambda x: x["match_score"], reverse=True)
        return sorted_books[:limit]
        
    except Exception as e:
        logger.error(f"知识图谱推荐出错: {e}")
        return []