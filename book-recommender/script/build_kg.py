import json
import sys
import os

# 导入项目路径，方便引用 app 模块
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from app.database.mysql_client import mysql_client
from app.database.neo4j_client import neo4j_client
import logging

logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(levelname)s - %(message)s")
logger = logging.getLogger(__name__)


def build_category_nodes():
    """构建分类节点 + 层级关系"""
    logger.info("正在构建分类节点...")
    categories = mysql_client.execute_query("SELECT category_id, name, parent_id FROM categories")

    for cate in categories:
        # 创建分类节点
        neo4j_client.execute_query("""
            MERGE (c:Category {category_id: $category_id})
            SET c.name = $name
        """, {"category_id": cate["category_id"], "name": cate["name"]})

        # 如果有父分类，创建父子关系（关系类型统一小写）
        if cate["parent_id"] and cate["parent_id"] != 0:
            neo4j_client.execute_query("""
                MATCH (child:Category {category_id: $child_id})
                MATCH (parent:Category {category_id: $parent_id})
                MERGE (child)-[:belongs_to_parent]->(parent)
            """, {"child_id": cate["category_id"], "parent_id": cate["parent_id"]})

    logger.info(f"分类节点构建完成，共 {len(categories)} 个")


def build_book_and_relations():
    """构建图书、作者、出版社、标签节点，以及关联关系"""
    logger.info("正在读取图书数据...")
    books = mysql_client.execute_query("""
        SELECT book_id, title, author, publisher, category_id, tags, description
        FROM books
        WHERE status = 1
    """)

    total = len(books)
    logger.info(f"共读取到 {total} 本有效图书，开始构建图谱...")

    for index, book in enumerate(books, 1):
        book_id = book["book_id"]
        title = book["title"]
        author_name = book["author"].strip() if book["author"] else ""
        publisher_name = book["publisher"].strip() if book["publisher"] else ""
        category_id = book["category_id"]
        tags_str = book["tags"]

        # 1. 创建图书节点（注意：属性名用 bookId，驼峰命名，与查询代码一致）
        neo4j_client.execute_query("""
            MERGE (b:Book {bookId: $book_id})
            SET b.title = $title, b.description = $description
        """, {"book_id": book_id, "title": title, "description": book["description"] or ""})

        # 2. 创建作者节点 + written_by 关系（关系类型统一小写）
        if author_name:
            neo4j_client.execute_query("""
                MERGE (a:Author {name: $author_name})
                WITH a
                MATCH (b:Book {bookId: $book_id})
                MERGE (b)-[:written_by]->(a)
            """, {"book_id": book_id, "author_name": author_name})

        # 3. 创建出版社节点 + published_by 关系（关系类型统一小写）
        if publisher_name:
            neo4j_client.execute_query("""
                MERGE (p:Publisher {name: $publisher_name})
                WITH p
                MATCH (b:Book {bookId: $book_id})
                MERGE (b)-[:published_by]->(p)
            """, {"book_id": book_id, "publisher_name": publisher_name})

        # 4. 关联分类 belongs_to 关系（关系类型统一小写）
        if category_id:
            neo4j_client.execute_query("""
                MATCH (b:Book {bookId: $book_id})
                MATCH (c:Category {category_id: $category_id})
                MERGE (b)-[:belongs_to]->(c)
            """, {"book_id": book_id, "category_id": category_id})

        # 5. 解析标签JSON，创建标签节点 + tagged_as 关系（关系类型统一小写）
        # 5. 解析标签，创建标签节点 + tagged_as 关系
        if tags_str:
            try:
                tag_list = []
                # 先尝试按 JSON 解析
                if isinstance(tags_str, str):
                    # 去掉首尾可能的多余引号和括号
                    cleaned = tags_str.strip()
                    # 尝试 JSON 解析
                    try:
                        parsed = json.loads(cleaned)
                        if isinstance(parsed, list):
                            tag_list = parsed
                        elif isinstance(parsed, str):
                            # 解析出来还是字符串，按逗号分割
                            tag_list = [parsed]
                    except Exception:
                        # JSON 解析失败，按逗号分割
                        tag_list = cleaned.replace('[', '').replace(']', '').split(',')
                else:
                    tag_list = tags_str if isinstance(tags_str, list) else [str(tags_str)]
                
                for tag_name in tag_list:
                    # 强力去引号、去空格、去括号
                    tag_name = str(tag_name).strip().strip('"').strip("'").strip()
                    if tag_name:
                        neo4j_client.execute_query("""
                            MERGE (t:Tag {name: $tag_name})
                            WITH t
                            MATCH (b:Book {bookId: $book_id})
                            MERGE (b)-[:tagged_as]->(t)
                        """, {"book_id": book_id, "tag_name": tag_name})
            except Exception as e:
                logger.warning(f"  图书《{title}》标签解析失败：{e}")

        # 打印进度
        if index % 100 == 0:
            logger.info(f"  已处理 {index}/{total} 本")

    logger.info("图书及关联关系构建完成")


def main():
    try:
        logger.info("===== 开始构建知识图谱 =====")
        build_category_nodes()
        build_book_and_relations()
        logger.info("===== 知识图谱构建全部完成 =====")
    except Exception as e:
        logger.error(f"构建失败，错误信息：{e}", exc_info=True)
    finally:
        # 关闭连接
        mysql_client.close()
        neo4j_client.close()


if __name__ == "__main__":
    main()
