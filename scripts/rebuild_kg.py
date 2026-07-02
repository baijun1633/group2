#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""重建知识图谱：清空 Neo4j 后从 MySQL 重新导入所有节点和关系"""

import pymysql
import json
from neo4j import GraphDatabase

# MySQL 配置
MYSQL_CONFIG = {
    'host': 'localhost',
    'user': 'root',
    'password': 'root123',
    'database': 'library_db',
    'charset': 'utf8mb4',
}

# Neo4j 配置
NEO4J_URI = "bolt://localhost:7687"
NEO4J_USER = "neo4j"
NEO4J_PASSWORD = "neo4j123"


def parse_tags(tags_json):
    if not tags_json:
        return []
    try:
        return json.loads(tags_json)
    except:
        cleaned = tags_json.replace('[', '').replace(']', '').replace('"', '')
        if not cleaned.strip():
            return []
        return [t.strip() for t in cleaned.split(',') if t.strip()]


def main():
    # 连接 Neo4j
    driver = GraphDatabase.driver(NEO4J_URI, auth=(NEO4J_USER, NEO4J_PASSWORD))

    # 连接 MySQL
    conn = pymysql.connect(**MYSQL_CONFIG)
    cursor = conn.cursor(pymysql.cursors.DictCursor)

    with driver.session() as session:
        # 1. 清空所有节点和关系
        print("清空 Neo4j 图谱数据...")
        session.run("MATCH (n) DETACH DELETE n")

        # 2. 创建 Category 节点
        cursor.execute("SELECT category_id, name FROM categories")
        categories = cursor.fetchall()
        for cat in categories:
            session.run(
                "MERGE (c:Category {categoryId: $categoryId}) SET c.name = $name",
                categoryId=cat['category_id'], name=cat['name']
            )
        print(f"创建 {len(categories)} 个 Category 节点")

        # 3. 查询所有书籍
        cursor.execute("""
            SELECT b.book_id, b.title, b.author, b.isbn, b.publisher,
                   b.description, b.avg_rating, b.cover_image, b.tags,
                   b.category_id, c.name as category_name
            FROM books b
            JOIN categories c ON b.category_id = c.category_id
            ORDER BY b.book_id
        """)
        books = cursor.fetchall()
        print(f"共 {len(books)} 本书需要导入")

        # 4. 逐本创建节点和关系
        tag_set = set()
        author_set = set()
        publisher_set = set()

        for i, book in enumerate(books):
            avg_rating = float(book['avg_rating']) if book['avg_rating'] else None

            # Book 节点
            session.run(
                "MERGE (b:Book {bookId: $bookId}) "
                "SET b.title = $title, b.isbn = $isbn, b.description = $description, "
                "b.avgRating = $avgRating, b.coverImage = $coverImage",
                bookId=book['book_id'], title=book['title'],
                isbn=book['isbn'], description=book['description'],
                avgRating=avg_rating, coverImage=book['cover_image']
            )

            # Author 关系
            if book['author'] and book['author'].strip():
                author_name = book['author'].strip()
                author_set.add(author_name)
                session.run(
                    "MATCH (b:Book {bookId: $bookId}) "
                    "MERGE (a:Author {name: $authorName}) "
                    "MERGE (b)-[:WRITTEN_BY]->(a)",
                    bookId=book['book_id'], authorName=author_name
                )

            # Publisher 关系
            if book['publisher'] and book['publisher'].strip():
                pub_name = book['publisher'].strip()
                publisher_set.add(pub_name)
                session.run(
                    "MATCH (b:Book {bookId: $bookId}) "
                    "MERGE (p:Publisher {name: $publisherName}) "
                    "MERGE (b)-[:PUBLISHED_BY]->(p)",
                    bookId=book['book_id'], publisherName=pub_name
                )

            # Category 关系
            if book['category_id']:
                session.run(
                    "MATCH (b:Book {bookId: $bookId}) "
                    "MERGE (c:Category {categoryId: $categoryId}) "
                    "MERGE (b)-[:BELONGS_TO]->(c)",
                    bookId=book['book_id'], categoryId=book['category_id']
                )

            # Tag 关系
            tags = parse_tags(book['tags'])
            for tag in tags:
                if tag and tag.strip():
                    tag_name = tag.strip()
                    tag_set.add(tag_name)
                    session.run(
                        "MATCH (b:Book {bookId: $bookId}) "
                        "MERGE (t:Tag {name: $tagName}) "
                        "MERGE (b)-[:TAGGED_AS]->(t)",
                        bookId=book['book_id'], tagName=tag_name
                    )

            if (i + 1) % 100 == 0:
                print(f"  已处理 {i + 1}/{len(books)} 本书...")

        print(f"\n图谱构建完成！")
        print(f"  Book 节点: {len(books)}")
        print(f"  Author 节点: {len(author_set)}")
        print(f"  Publisher 节点: {len(publisher_set)}")
        print(f"  Category 节点: {len(categories)}")
        print(f"  Tag 节点: {len(tag_set)}")

        # 5. 统计
        result = session.run("MATCH (n) RETURN labels(n) AS label, count(n) AS cnt")
        print("\n节点统计:")
        for record in result:
            print(f"  {record['label']}: {record['cnt']}")

        result = session.run("MATCH ()-[r]->() RETURN type(r) AS rel, count(r) AS cnt")
        print("\n关系统计:")
        for record in result:
            print(f"  {record['rel']}: {record['cnt']}")

    cursor.close()
    conn.close()
    driver.close()


if __name__ == '__main__':
    main()
