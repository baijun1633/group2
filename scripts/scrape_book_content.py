"""
批量爬取真实书籍内容脚本 v2
============================
从豆瓣读书等公开网站获取真实书籍信息，替换模板生成的占位内容。

数据来源：
- 豆瓣读书（API + 网页）：内容简介、作者介绍、目录、试读摘录

使用方法：
    python scripts/scrape_book_content.py              # 处理所有书
    python scripts/scrape_book_content.py --limit 10   # 先处理10本测试
    python scripts/scrape_book_content.py --dry-run    # 仅预览不修改
    python scripts/scrape_book_content.py --book-id 76 # 处理指定书籍

依赖安装：
    pip install requests beautifulsoup4 lxml pymysql
"""

import os
import sys
import json
import time
import re
import argparse
import random
import pymysql
import requests
from pathlib import Path
from bs4 import BeautifulSoup
from urllib.parse import quote, urljoin

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'python_service'))
from config import MYSQL_HOST, MYSQL_PORT, MYSQL_USER, MYSQL_PASSWORD, MYSQL_DB

# ── 配置 ──────────────────────────────────────────────
EBOOK_DIR = "d:/code/library_sys/uploads/ebooks"
PREVIEW_LENGTH = 2000
REQUEST_TIMEOUT = 15
MIN_DELAY = 2.0
MAX_DELAY = 4.0
MAX_RETRIES = 2
CACHE_DIR = "d:/code/library_sys/scripts/.scrape_cache"
PROGRESS_FILE = "d:/code/library_sys/scripts/.scrape_progress.json"

# 多个 User-Agent 轮换
USER_AGENTS = [
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36',
    'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0',
    'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Safari/605.1.15',
]


# ── 数据库 ──────────────────────────────────────────────

def get_db():
    return pymysql.connect(
        host=MYSQL_HOST, port=MYSQL_PORT,
        user=MYSQL_USER, password=MYSQL_PASSWORD,
        database=MYSQL_DB, charset="utf8mb4",
        cursorclass=pymysql.cursors.DictCursor,
    )


def load_all_books(cursor):
    cursor.execute("""
        SELECT book_id, title, author, isbn, publisher, description,
               tags, pages, price, category_id
        FROM books WHERE status = 1
    """)
    return cursor.fetchall()


# ── 缓存 ──────────────────────────────────────────────

def _ensure_cache_dir():
    os.makedirs(CACHE_DIR, exist_ok=True)


def cache_get(key):
    """读缓存"""
    _ensure_cache_dir()
    cache_file = Path(CACHE_DIR) / f"{key}.json"
    if cache_file.exists():
        try:
            data = json.loads(cache_file.read_text(encoding='utf-8'))
            return data
        except Exception:
            pass
    return None


def cache_set(key, value):
    """写缓存"""
    _ensure_cache_dir()
    cache_file = Path(CACHE_DIR) / f"{key}.json"
    cache_file.write_text(json.dumps(value, ensure_ascii=False, indent=2), encoding='utf-8')


# ── HTTP 请求 ──────────────────────────────────────────

_session = requests.Session()


def smart_delay():
    time.sleep(random.uniform(MIN_DELAY, MAX_DELAY))


def get_headers(referer=''):
    headers = {
        'User-Agent': random.choice(USER_AGENTS),
        'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8',
        'Accept-Language': 'zh-CN,zh;q=0.9,en;q=0.8',
        'Accept-Encoding': 'gzip, deflate, br',
        'Connection': 'keep-alive',
        'Cache-Control': 'no-cache',
    }
    if referer:
        headers['Referer'] = referer
    return headers


def fetch_url(url, retries=MAX_RETRIES, referer=''):
    for attempt in range(retries + 1):
        try:
            resp = _session.get(
                url,
                headers=get_headers(referer),
                timeout=REQUEST_TIMEOUT,
                allow_redirects=True,
            )
            resp.encoding = resp.apparent_encoding or 'utf-8'
            if resp.status_code == 200:
                return resp.text
            elif resp.status_code == 403:
                wait = random.uniform(15, 30) * (attempt + 1)
                print(f"    [403] 等待 {wait:.0f}s (尝试{attempt+1}/{retries+1})...")
                time.sleep(wait)
            elif resp.status_code == 301 or resp.status_code == 302:
                time.sleep(2)
            else:
                print(f"    [HTTP {resp.status_code}]")
        except requests.RequestException as e:
            print(f"    [请求失败] {e}")
            if attempt < retries:
                time.sleep(random.uniform(5, 10))
    return None


def fetch_json(url, retries=MAX_RETRIES):
    """请求并解析JSON"""
    for attempt in range(retries + 1):
        try:
            resp = _session.get(
                url,
                headers={**get_headers(), 'Accept': 'application/json'},
                timeout=REQUEST_TIMEOUT,
            )
            if resp.status_code == 200:
                return resp.json()
            elif resp.status_code == 403:
                time.sleep(random.uniform(8, 15))
        except Exception as e:
            if attempt < retries:
                time.sleep(random.uniform(3, 6))
    return None


# ── 豆瓣 API + 网页 ──────────────────────────────────

def search_douban_api(title, author=''):
    """通过豆瓣API搜索书籍，返回subject_id"""
    keyword = f"{title} {author}".strip()
    url = f"https://api.douban.com/v2/book/search?q={quote(keyword)}&count=5&fields=id,title,author"
    data = fetch_json(url)
    if data and 'books' in data:
        for book in data['books']:
            bt = book.get('title', '')
            # 模糊匹配书名
            if title in bt or bt in title:
                return book.get('id')
        # 如果精确匹配失败，返回第一个
        if data['books']:
            return data['books'][0].get('id')
    return None


def search_douban_html(title, author=''):
    """通过豆瓣搜索页面查找书籍subject_id（备用）"""
    keyword = f"{title} {author}".strip()
    url = f"https://search.douban.com/book/subject_search?search_text={quote(keyword)}&cat=1001"
    html = fetch_url(url, referer='https://www.douban.com/')
    if not html:
        return None

    # 用正则从整个HTML中提取所有subject ID（去重保序）
    all_ids = list(dict.fromkeys(re.findall(r'subject/(\d+)', html)))
    return all_ids[0] if all_ids else None


def find_best_douban_id(title, author=''):
    """搜索多个版本，返回信息最丰富的那个"""
    keyword = f"{title} {author}".strip()
    url = f"https://search.douban.com/book/subject_search?search_text={quote(keyword)}&cat=1001"
    html = fetch_url(url, referer='https://www.douban.com/')
    if not html:
        return None

    all_ids = list(dict.fromkeys(re.findall(r'subject/(\d+)', html)))
    if not all_ids:
        return None

    # 快速检查前3个版本，选信息最多的
    best_id = all_ids[0]
    best_score = 0
    for sid in all_ids[:5]:
        try:
            detail = scrape_douban_detail(sid)
            if not detail:
                continue
            score = 0
            if detail.get('summary') and len(detail['summary']) > 30:
                score += len(detail['summary'])
            if detail.get('chapters'):
                score += len(detail['chapters']) * 10
            if detail.get('author_info') and len(detail['author_info']) > 30:
                score += 50
            if score > best_score:
                best_score = score
                best_id = sid
            # 如果找到内容丰富的版本就直接用
            if score > 200:
                break
            smart_delay()
        except Exception:
            continue

    return best_id


def scrape_douban_detail(subject_id):
    """爬取豆瓣书籍详情页，获取完整信息"""
    url = f"https://book.douban.com/subject/{subject_id}/"

    # 先检查缓存
    cached = cache_get(f"douban_{subject_id}")
    if cached:
        return cached

    html = fetch_url(url, referer='https://www.douban.com/')
    if not html:
        return None

    soup = BeautifulSoup(html, 'lxml')
    result = {
        'subject_id': subject_id,
        'title': '',
        'rating': '',
        'summary': '',
        'author_info': '',
        'info': {},
        'tags': [],
        'chapters': [],      # 目录章节
        'sample_texts': [],  # 试读摘录
        'url': url,
    }

    # ── 标题 ──
    title_el = soup.select_one('span[property="v:itemreviewed"]')
    if title_el:
        result['title'] = title_el.get_text(strip=True)

    # ── 评分 ──
    rating_el = soup.select_one('strong.rating_num, strong[property="v:average"]')
    if rating_el:
        result['rating'] = rating_el.get_text(strip=True)

    # ── 书籍信息 ──
    info_div = soup.select_one('#info')
    if info_div:
        info_text = info_div.get_text('\n', strip=True)
        for line in info_text.split('\n'):
            line = line.strip()
            if not line:
                continue
            sep = '：' if '：' in line else ':'
            if sep in line:
                parts = line.split(sep, 1)
                if len(parts) == 2:
                    key = parts[0].strip()
                    val = parts[1].strip()
                    if key and val:
                        result['info'][key] = val

    # ── 内容简介 ──
    # 豆瓣有两种展示：完整版(all hidden)和折叠版
    all_intro = soup.select_one('#link-report .all')
    if all_intro:
        ps = all_intro.select('p')
        if ps:
            result['summary'] = '\n'.join(p.get_text(strip=True) for p in ps if p.get_text(strip=True))
        else:
            result['summary'] = all_intro.get_text('\n', strip=True)
    else:
        intro = soup.select_one('#link-report .intro')
        if intro:
            ps = intro.select('p')
            if ps:
                result['summary'] = '\n'.join(p.get_text(strip=True) for p in ps if p.get_text(strip=True))
            else:
                result['summary'] = intro.get_text('\n', strip=True)

    # ── 作者简介 ──
    # 豆瓣页面结构：h2 "作者简介" 后紧跟 div.indent > div.intro
    for h2 in soup.select('h2'):
        h2_text = h2.get_text(strip=True)
        if '作者简介' in h2_text:
            # 向后遍历 siblings 找到第一个 div.indent
            sibling = h2.find_next_sibling()
            while sibling:
                if sibling.name == 'div':
                    intro_el = sibling.select_one('.intro')
                    if intro_el:
                        author_text = intro_el.get_text(strip=True)
                        # 验证：作者简介不应是书的内容简介
                        if '内容简介' not in author_text[:20] and len(author_text) > 50:
                            result['author_info'] = author_text
                            break
                    indent_text = sibling.get_text(strip=True)
                    if indent_text and len(indent_text) > 50:
                        if '内容简介' not in indent_text[:20]:
                            result['author_info'] = indent_text[:800]
                            break
                sibling = sibling.find_next_sibling()
            break

    # ── 标签 ──
    tag_els = soup.select('#db-tags-section .tag a, .tags-body a')
    result['tags'] = [t.get_text(strip=True) for t in tag_els if t.get_text(strip=True)]

    # ── 目录 ──
    # 豆瓣使用 div#dir_{subject_id}_full 来存放完整目录
    dir_id = f"dir_{subject_id}_full"
    toc_el = soup.select_one(f'#{dir_id}')
    if toc_el:
        toc_text = toc_el.get_text('\n', strip=True)
        for line in toc_text.split('\n'):
            line = line.strip()
            if not line or len(line) <= 1:
                continue
            # 过滤噪音
            if '· ·' in line or line in ('(收起)', '(更多)', '收起', '更多', ''):
                continue
            result['chapters'].append(line)
    else:
        for h2 in soup.select('h2'):
            if '目录' in h2.get_text(strip=True):
                parent = h2.find_parent()
                if parent:
                    indent = parent.select_one('.indent')
                    if indent:
                        toc_text = indent.get_text('\n', strip=True)
                        for line in toc_text.split('\n'):
                            line = line.strip()
                            if line and len(line) > 1:
                                if '· ·' not in line and line not in ('(收起)', '(更多)', '收起', '更多'):
                                    result['chapters'].append(line)
                break

    # ── 试读章节 ──
    # 试读内容在 h2 含"试读"的 section 后面
    for h2 in soup.select('h2'):
        if '试读' in h2.get_text(strip=True):
            parent = h2.find_parent()
            if parent:
                # 获取试读章节列表和正文摘要
                links = parent.select('a[href*="reading"]')
                for link in links:
                    text = link.get_text(strip=True)
                    href = link.get('href', '')
                    if text and '查看全部' not in text and '试读' not in text:
                        result['chapters'].append(f"【试读】{text}")

                # 获取试读正文（前几段）
                for p in parent.select('p')[:3]:
                    text = p.get_text(strip=True)
                    if text and len(text) > 30:
                        result['sample_texts'].append(text)
            break

    # ── 原文摘录 ──
    for h2 in soup.select('h2'):
        if '原文摘录' in h2.get_text(strip=True):
            parent = h2.find_parent()
            if parent:
                # 获取摘录内容
                for el in parent.select('p, .quote'):
                    text = el.get_text(strip=True)
                    if text and len(text) > 30 and '查看原文' not in text:
                        result['sample_texts'][:0] = [text[:300]]  # 插到最前面
            break

    cache_set(f"douban_{subject_id}", result)
    return result


def scrape_douban_chapters(subject_id):
    """获取豆瓣上的试读章节内容"""
    url = f"https://book.douban.com/subject/{subject_id}/reading"
    html = fetch_url(url, referer=f'https://book.douban.com/subject/{subject_id}/')
    if not html:
        return []

    soup = BeautifulSoup(html, 'lxml')
    chapters = []
    for item in soup.select('.reading-chapter, .chapter-item, .article'):
        title = ''
        title_el = item.select_one('h3, .title, a')
        if title_el:
            title = title_el.get_text(strip=True)
        content = item.get_text('\n', strip=True)
        if content:
            chapters.append({'title': title, 'content': content[:1000]})
    return chapters


# ── 内容生成 ──────────────────────────────────────────

def clean_text(text):
    """清理文本中的多余空白"""
    if not text:
        return ''
    text = re.sub(r'\n{3,}', '\n\n', text)
    text = re.sub(r' {2,}', ' ', text)
    return text.strip()


def build_book_content(book, douban_info):
    """根据豆瓣真实信息生成书籍内容txt"""
    title = book['title'] or ''
    author = book['author'] or ''
    publisher = book['publisher'] or ''

    lines = []

    # ═══ 书名 ═══
    lines.append(f"《{title}》")
    lines.append("")

    # ═══ 基本信息 ═══
    info_parts = []

    if douban_info and douban_info.get('info'):
        di = douban_info['info']
        # 豆瓣info里有完整的出版信息
        for key in ['作者', '出版社', '原作名', '译者', '出版年', '页数',
                     '定价', '装帧', 'ISBN', '丛书', '副标题']:
            if key in di and di[key]:
                info_parts.append(f"{key}：{di[key]}")
    else:
        if author:
            info_parts.append(f"作者：{author}")
        if publisher:
            info_parts.append(f"出版社：{publisher}")
        if book.get('isbn'):
            info_parts.append(f"ISBN：{book['isbn']}")
        if book.get('pages'):
            info_parts.append(f"页数：{book['pages']}页")

    if douban_info and douban_info.get('rating'):
        info_parts.append(f"豆瓣评分：{douban_info['rating']}")

    if info_parts:
        lines.append('\n'.join(info_parts))
        lines.append("")
        lines.append("=" * 40)
        lines.append("")

    # ═══ 内容简介 ═══
    summary = ''
    if douban_info and douban_info.get('summary'):
        summary = clean_text(douban_info['summary'])
    elif book.get('description'):
        summary = clean_text(book['description'])

    if summary:
        lines.append("【内容简介】")
        lines.append("")
        lines.append(summary)
        lines.append("")
        lines.append("-" * 40)
        lines.append("")

    # ═══ 作者简介 ═══
    author_intro = ''
    if douban_info and douban_info.get('author_info'):
        author_intro = clean_text(douban_info['author_info'])

    if author_intro:
        lines.append("【作者简介】")
        lines.append("")
        lines.append(author_intro)
        lines.append("")
        lines.append("-" * 40)
        lines.append("")

    # ═══ 目录 ═══
    chapters = []
    if douban_info and douban_info.get('chapters'):
        chapters = douban_info['chapters']

    if chapters:
        lines.append("【目录】")
        lines.append("")
        for ch in chapters:
            lines.append(ch)
        lines.append("")
        lines.append("-" * 40)
        lines.append("")

    # ═══ 试读 ═══
    sample_texts = []
    if douban_info and douban_info.get('sample_texts'):
        sample_texts = douban_info['sample_texts']

    if sample_texts:
        lines.append("【试读摘录】")
        lines.append("")
        for text in sample_texts[:3]:
            lines.append(text)
            lines.append("")
        lines.append("-" * 40)
        lines.append("")

    # ═══ 标签 ═══
    tags = []
    if douban_info and douban_info.get('tags'):
        tags = douban_info['tags']
    elif book.get('tags'):
        try:
            tags = json.loads(book['tags'])
        except Exception:
            pass

    if tags:
        lines.append("【相关标签】")
        lines.append("、".join(tags[:10]))
        lines.append("")
        lines.append("-" * 40)
        lines.append("")

    # ═══ 尾声 ═══
    lines.append("【关于本书】")
    lines.append("")
    lines.append(f"本书《{title}》信息来源于豆瓣读书公开数据。")
    lines.append("如需阅读完整内容，请购买正版图书或前往图书馆借阅。")
    lines.append("")
    if douban_info and douban_info.get('url'):
        lines.append(f"豆瓣页面：{douban_info['url']}")
    lines.append("")
    lines.append("=" * 40)

    return "\n".join(lines)


def build_preview_content(book_content):
    if len(book_content) <= PREVIEW_LENGTH:
        return book_content
    return book_content[:PREVIEW_LENGTH] + "\n\n[... 以下内容需登录后阅读 ...]"


# ── 模板内容检测 ──────────────────────────────────────

TEMPLATE_KEYWORDS = [
    '第一章 书籍导读', '第一章 作品概述', '第一章 作品信息',
    '基础概念\n\n在深入学习', '核心原理\n\n掌握了基础概念',
    '第一章 作品介绍', '第一章 作品概览',
]


def is_template_content(filepath):
    """检测文件是否为模板生成的占位内容"""
    try:
        text = Path(filepath).read_text(encoding='utf-8', errors='ignore')[:800]
        return any(kw in text for kw in TEMPLATE_KEYWORDS)
    except Exception:
        return False


# ── 主流程 ──────────────────────────────────────────

def process_book(book, dry_run=False):
    """处理单本书"""
    book_id = book['book_id']
    title = book['title'] or ''
    author = book['author'] or ''

    print(f"\n  Book#{book_id}《{title}》- {author}")

    # 1. 搜索豆瓣
    print(f"    [1/2] 搜索豆瓣...")
    subject_id = None
    try:
        subject_id = search_douban_api(title, author)
    except Exception:
        pass
    if not subject_id:
        try:
            subject_id = search_douban_html(title, author)
        except Exception:
            pass

    if not subject_id:
        print(f"    ✗ 豆瓣未找到")
        return False
    print(f"    ✓ 找到豆瓣 ID: {subject_id}")

    # 2. 获取详细信息
    smart_delay()
    print(f"    [2/2] 获取书籍详情...")
    douban_info = None
    try:
        douban_info = scrape_douban_detail(subject_id)
        if douban_info:
            parts = []
            if douban_info.get('summary'):
                parts.append(f"简介{len(douban_info['summary'])}字")
            if douban_info.get('chapters'):
                parts.append(f"目录{len(douban_info['chapters'])}条")
            if douban_info.get('author_info'):
                parts.append("有作者简介")
            if douban_info.get('info'):
                parts.append(f"信息{len(douban_info['info'])}项")
            detail_str = ', '.join(parts) if parts else '基本信息'
            print(f"    ✓ 获取成功: {detail_str}")
        else:
            print(f"    ✗ 详情页解析失败")
    except Exception as e:
        print(f"    ✗ 异常: {e}")

    if not douban_info:
        return False

    # 3. 生成内容
    content = build_book_content(book, douban_info)
    preview = build_preview_content(content)

    if dry_run:
        print(f"    [预览] 内容长度: {len(content)} 字符")
        for line in content[:200].split('\n'):
            print(f"    {line}")
        return True

    # 4. 写入txt
    txt_path = Path(EBOOK_DIR) / f"{book_id}.txt"
    try:
        txt_path.write_text(content, encoding='utf-8')
        file_size = txt_path.stat().st_size
        print(f"    ✓ 写入: {file_size/1024:.1f}KB")
    except Exception as e:
        print(f"    ✗ 写入失败: {e}")
        return False

    return True


def load_progress():
    """加载已完成的book_id集合"""
    try:
        data = json.loads(Path(PROGRESS_FILE).read_text(encoding='utf-8'))
        return set(data.get('done', []))
    except Exception:
        return set()


def save_progress(done_ids):
    """保存进度"""
    Path(PROGRESS_FILE).write_text(
        json.dumps({'done': list(done_ids)}, ensure_ascii=False),
        encoding='utf-8'
    )


def main():
    parser = argparse.ArgumentParser(description='爬取真实书籍内容 v2')
    parser.add_argument('--limit', type=int, default=0,
                        help='只处理前N本书（测试用）')
    parser.add_argument('--book-id', type=int, default=0,
                        help='只处理指定book_id的书')
    parser.add_argument('--dry-run', action='store_true',
                        help='仅预览，不写入文件和数据库')
    parser.add_argument('--force', action='store_true',
                        help='强制更新所有书（即使已有内容）')
    parser.add_argument('--reset', action='store_true',
                        help='重置进度，从头开始')
    args = parser.parse_args()

    conn = get_db()
    cursor = conn.cursor()
    books = load_all_books(cursor)

    if args.book_id:
        books = [b for b in books if b['book_id'] == args.book_id]
        if not books:
            print(f"未找到 Book#{args.book_id}")
            return
    elif args.limit:
        books = books[:args.limit]

    # 加载进度（用于断点续传）
    if args.reset:
        done_ids = set()
        if Path(PROGRESS_FILE).exists():
            Path(PROGRESS_FILE).unlink()
    else:
        done_ids = load_progress()

    total = len(books)
    print(f"═══ 书籍内容爬取工具 v2 ═══")
    print(f"共 {total} 本书待处理")
    print(f"数据来源: 豆瓣读书")
    if done_ids:
        print(f"已跳过 {len(done_ids)} 本（已完成）")
    if args.dry_run:
        print(f"模式: 预览（不写入）")
    print()

    success = 0
    failed = 0
    skipped = 0
    updated_db = 0
    consecutive_fails = 0

    for i, book in enumerate(books, 1):
        book_id = book['book_id']

        # 跳过已完成的
        if book_id in done_ids and not args.force:
            skipped += 1
            continue

        print(f"[{i}/{total}]", end='')

        # 跳过已有真实内容的
        if not args.force:
            txt_path = Path(EBOOK_DIR) / f"{book_id}.txt"
            if txt_path.exists() and not is_template_content(txt_path):
                if txt_path.stat().st_size > 1000:
                    skipped += 1
                    done_ids.add(book_id)
                    continue

        try:
            ok = process_book(book, dry_run=args.dry_run)
            if ok:
                success += 1
                consecutive_fails = 0
                done_ids.add(book_id)
                # 更新数据库preview_content
                if not args.dry_run:
                    txt_path = Path(EBOOK_DIR) / f"{book_id}.txt"
                    if txt_path.exists():
                        content = txt_path.read_text(encoding='utf-8', errors='ignore')
                        preview = build_preview_content(content)
                        cursor.execute(
                            "UPDATE books SET preview_content = %s, update_time = NOW() WHERE book_id = %s",
                            (preview, book_id)
                        )
                        updated_db += 1
            else:
                failed += 1
                consecutive_fails += 1
        except Exception as e:
            print(f"    ✗ 异常: {e}")
            failed += 1
            consecutive_fails += 1

        # 每10本保存进度并打印
        if i % 10 == 0:
            conn.commit()
            save_progress(done_ids)
            print(f"\n  --- 进度: {i}/{total} | 成功:{success} 失败:{failed} 跳过:{skipped} ---\n")

        # 连续失败5次，等待更长时间
        if consecutive_fails >= 5:
            wait = random.uniform(30, 60)
            print(f"\n  [警告] 连续失败{consecutive_fails}次，等待 {wait:.0f}s...\n")
            time.sleep(wait)
            consecutive_fails = 0

    if not args.dry_run:
        conn.commit()
    save_progress(done_ids)

    cursor.close()
    conn.close()

    print(f"\n═══ 完成 ═══")
    print(f"  成功爬取: {success}")
    print(f"  更新数据库: {updated_db}")
    print(f"  失败: {failed}")
    print(f"  跳过(已有内容): {skipped}")
    print(f"  总计: {total}")


if __name__ == '__main__':
    main()
