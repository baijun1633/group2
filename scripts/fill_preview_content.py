"""
批量填充 preview_content 脚本
==============================
为所有缺少试读内容的图书生成 preview_content。
基于每本书的 title、author、description、tags、publisher 等信息组合生成。
"""

import os
import sys
import json
import pymysql

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'python_service'))
from config import MYSQL_HOST, MYSQL_PORT, MYSQL_USER, MYSQL_PASSWORD, MYSQL_DB


def get_db():
    return pymysql.connect(
        host=MYSQL_HOST, port=MYSQL_PORT,
        user=MYSQL_USER, password=MYSQL_PASSWORD,
        database=MYSQL_DB, charset="utf8mb4",
        cursorclass=pymysql.cursors.DictCursor,
    )


def generate_preview(book):
    """根据图书信息生成试读内容"""
    title = book['title'] or ''
    author = book['author'] or ''
    publisher = book['publisher'] or ''
    description = book['description'] or ''
    isbn = book['isbn'] or ''
    pages = book['pages']
    price = book['price']

    # 解析 tags
    tags = []
    if book['tags']:
        try:
            tags = json.loads(book['tags'])
        except Exception:
            pass

    lines = []

    # 第一段：书籍信息概览
    lines.append(f"━━━ 《{title}》 ━━━")
    info_parts = []
    if author:
        info_parts.append(f"作者：{author}")
    if publisher:
        info_parts.append(f"出版社：{publisher}")
    if isbn:
        info_parts.append(f"ISBN：{isbn}")
    if pages:
        info_parts.append(f"页数：{pages}页")
    if price:
        info_parts.append(f"定价：¥{price}")
    if info_parts:
        lines.append(" | ".join(info_parts))
    lines.append("")

    # 第二段：内容简介
    if description and len(description.strip()) > 5:
        lines.append("【内容简介】")
        lines.append(description.strip())
        lines.append("")

    # 第三段：标签信息
    if tags:
        lines.append("【标签】")
        lines.append("、".join(tags))
        lines.append("")

    # 第四段：试读提示
    lines.append("【试读说明】")
    lines.append(f"本书《{title}》的完整电子书内容需购买后阅读。")
    lines.append("您可以通过以下方式获取本书：")
    lines.append("  · 在线购买电子书")
    lines.append("  · 前往图书馆借阅实体书")
    lines.append("  · 在各大书店购买")
    if publisher:
        lines.append(f"\n本书由{publisher}出版。")
    lines.append("")
    lines.append("━━━━━━━━━━━━━━━━━━━━━━")

    return "\n".join(lines)


def main():
    conn = get_db()
    cursor = conn.cursor()

    # 获取所有没有 preview_content 的上架图书
    cursor.execute("""
        SELECT book_id, title, author, isbn, publisher, description, tags, pages, price
        FROM books 
        WHERE status = 1 
          AND (preview_content IS NULL OR preview_content = '')
    """)
    books = cursor.fetchall()
    print(f"需要填充 preview_content 的图书: {len(books)} 本\n")

    if not books:
        print("所有图书都已有 preview_content，无需处理。")
        return

    success = 0
    failed = 0
    for book in books:
        try:
            preview = generate_preview(book)
            cursor.execute(
                "UPDATE books SET preview_content = %s, update_time = NOW() WHERE book_id = %s",
                (preview, book['book_id'])
            )
            success += 1
            if success % 100 == 0:
                print(f"  已处理 {success} 本...")
        except Exception as e:
            failed += 1
            print(f"  [FAIL] #{book['book_id']}《{book['title']}》: {e}")

    conn.commit()
    cursor.close()
    conn.close()

    print(f"\n=== 完成 ===")
    print(f"  成功: {success}")
    print(f"  失败: {failed}")


if __name__ == '__main__':
    main()
