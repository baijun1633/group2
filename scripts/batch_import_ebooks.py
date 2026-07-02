"""
批量导入TXT电子书脚本
=====================
将 TXT 文件批量导入到图书系统。

使用方法：
1. 把 TXT 文件放入指定文件夹（默认 d:/code/library_sys/uploads/txt_books/）
2. 文件命名方式（按优先级匹配）：
   - {bookId}.txt    → 直接匹配 bookId（如 1.txt, 42.txt）
   - {书名}.txt      → 按书名模糊匹配（如 三体.txt → 匹配 title 含"三体"的书）
3. 运行脚本：
   python scripts/batch_import_ebooks.py

可选参数：
   --txt-dir    TXT文件所在目录（默认 d:/code/library_sys/uploads/txt_books/）
   --preview    同时将前2000字写入 preview_content 字段（默认开启）
   --no-preview 仅导入电子书文件，不更新 preview_content
   --dry-run    仅预览匹配结果，不实际导入
"""

import os
import sys
import shutil
import argparse
import pymysql
from pathlib import Path

# 添加 python_service 到路径以复用配置
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'python_service'))
from config import MYSQL_HOST, MYSQL_PORT, MYSQL_USER, MYSQL_PASSWORD, MYSQL_DB

EBOOK_DIR = "d:/code/library_sys/uploads/ebooks"
DEFAULT_TXT_DIR = "d:/code/library_sys/uploads/txt_books"
PREVIEW_LENGTH = 2000  # preview_content 截取前2000字


def get_db():
    return pymysql.connect(
        host=MYSQL_HOST, port=MYSQL_PORT,
        user=MYSQL_USER, password=MYSQL_PASSWORD,
        database=MYSQL_DB, charset="utf8mb4",
        cursorclass=pymysql.cursors.DictCursor,
    )


def load_all_books(cursor):
    """加载所有图书信息，用于匹配"""
    cursor.execute("SELECT book_id, title FROM books WHERE status = 1")
    return cursor.fetchall()


def match_file_to_book(filename, books):
    """
    将文件名匹配到图书
    返回 (book_id, title) 或 None
    """
    stem = Path(filename).stem  # 去掉扩展名

    # 优先：按 bookId 匹配
    if stem.isdigit():
        book_id = int(stem)
        for b in books:
            if b['book_id'] == book_id:
                return b
        return None

    # 其次：按书名精确匹配
    for b in books:
        if b['title'] == stem:
            return b

    # 再次：按书名模糊匹配（文件名包含书名，或书名包含文件名）
    for b in books:
        if stem in b['title'] or b['title'] in stem:
            return b

    return None


def batch_import(txt_dir, update_preview=True, dry_run=False):
    """批量导入 TXT 电子书"""
    txt_dir = Path(txt_dir)
    if not txt_dir.exists():
        print(f"[错误] 目录不存在: {txt_dir}")
        print(f"请先创建目录并放入 TXT 文件: mkdir -p \"{txt_dir}\"")
        return

    txt_files = sorted([f for f in txt_dir.iterdir()
                        if f.suffix.lower() == '.txt' and f.is_file()])
    if not txt_files:
        print(f"[错误] 目录中没有 TXT 文件: {txt_dir}")
        return

    print(f"找到 {len(txt_files)} 个 TXT 文件")

    conn = get_db()
    cursor = conn.cursor()
    books = load_all_books(cursor)
    print(f"数据库中共 {len(books)} 本上架图书\n")

    # 确保电子书目录存在
    os.makedirs(EBOOK_DIR, exist_ok=True)

    matched = []
    unmatched = []
    for txt_file in txt_files:
        book = match_file_to_book(txt_file.name, books)
        if book:
            matched.append((txt_file, book))
        else:
            unmatched.append(txt_file)

    # 打印匹配结果
    print(f"=== 匹配结果 ===")
    print(f"  成功匹配: {len(matched)} 个")
    print(f"  未匹配:   {len(unmatched)} 个")

    if unmatched:
        print(f"\n未匹配的文件:")
        for f in unmatched:
            print(f"  - {f.name}")

    if dry_run:
        print(f"\n[预览模式] 不执行实际导入")
        print(f"\n将要导入的文件:")
        for txt_file, book in matched:
            size_kb = txt_file.stat().st_size / 1024
            print(f"  {txt_file.name} → Book#{book['book_id']}《{book['title']}》 ({size_kb:.1f}KB)")
        return

    # 执行导入
    print(f"\n=== 开始导入 ===")
    success = 0
    failed = 0
    for txt_file, book in matched:
        book_id = book['book_id']
        title = book['title']
        try:
            # 复制文件到 ebooks 目录
            target = Path(EBOOK_DIR) / f"{book_id}.txt"
            shutil.copy2(str(txt_file), str(target))
            file_size = txt_file.stat().st_size

            # 更新数据库
            ebook_url = f"/uploads/ebooks/{book_id}.txt"
            update_sql = """
                UPDATE books 
                SET ebook_url = %s, ebook_type = 'txt', ebook_size = %s, update_time = NOW()
                WHERE book_id = %s
            """
            cursor.execute(update_sql, (ebook_url, file_size, book_id))

            # 可选：更新 preview_content
            if update_preview:
                content = txt_file.read_text(encoding='utf-8', errors='ignore')
                preview = content[:PREVIEW_LENGTH]
                if len(content) > PREVIEW_LENGTH:
                    preview += "\n\n[... 以下内容需登录后阅读 ...]"
                cursor.execute(
                    "UPDATE books SET preview_content = %s WHERE book_id = %s",
                    (preview, book_id)
                )

            success += 1
            print(f"  [OK] Book#{book_id}《{title}》 - {file_size/1024:.1f}KB")

        except Exception as e:
            failed += 1
            print(f"  [FAIL] Book#{book_id}《{title}》 - {e}")

    conn.commit()
    cursor.close()
    conn.close()

    print(f"\n=== 导入完成 ===")
    print(f"  成功: {success}")
    print(f"  失败: {failed}")


def main():
    parser = argparse.ArgumentParser(description='批量导入TXT电子书')
    parser.add_argument('--txt-dir', default=DEFAULT_TXT_DIR,
                        help=f'TXT文件目录 (默认: {DEFAULT_TXT_DIR})')
    parser.add_argument('--preview', action='store_true', default=True,
                        help='同时更新 preview_content (默认开启)')
    parser.add_argument('--no-preview', action='store_true',
                        help='仅导入电子书，不更新 preview_content')
    parser.add_argument('--dry-run', action='store_true',
                        help='仅预览匹配结果，不实际导入')
    args = parser.parse_args()

    update_preview = not args.no_preview
    batch_import(args.txt_dir, update_preview=update_preview, dry_run=args.dry_run)


if __name__ == '__main__':
    main()
