"""
=============================================================
  通用书籍内容爬虫框架
=============================================================
  你可以自由配置数据源，爬取任意网站上的书籍内容。

  使用方法：
    python scripts/my_scraper.py --book-id 76          # 爬一本书
    python scripts/my_scraper.py --title "三体"        # 按书名搜
    python scripts/my_scraper.py --limit 10            # 爬前10本
    python scripts/my_scraper.py --source gushiwen     # 只用古诗文网
    python scripts/my_scraper.py --all                 # 爬所有书

  依赖安装：
    pip install requests beautifulsoup4 lxml pymysql

  如何添加新的数据源：
    1. 实现 search_xxx(title, author) -> (url, None) 或 (None, error)
    2. 实现 fetch_xxx(url) -> (data_dict, None) 或 (None, error)
    3. 在 SOURCES 字典中注册
    4. 返回格式：{"title", "summary", "chapters": [...], ...}
=============================================================
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
import urllib3
from pathlib import Path
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
from bs4 import BeautifulSoup
from urllib.parse import quote, urljoin

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'python_service'))
from config import MYSQL_HOST, MYSQL_PORT, MYSQL_USER, MYSQL_PASSWORD, MYSQL_DB

# ══════════════════════════════════════════════════════
#  配置区
# ══════════════════════════════════════════════════════

EBOOK_DIR = "d:/code/library_sys/uploads/ebooks"
PROGRESS_FILE = "d:/code/library_sys/scripts/.my_scraper_progress.json"
DELAY_MIN = 2        # 请求最小间隔（秒）
DELAY_MAX = 5        # 请求最大间隔（秒）
TIMEOUT = 15         # 请求超时（秒）

HEADERS = {
    'User-Agent': (
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) '
        'AppleWebKit/537.36 (KHTML, like Gecko) '
        'Chrome/120.0.0.0 Safari/537.36'
    ),
    'Accept-Language': 'zh-CN,zh;q=0.9,en;q=0.8',
}

# 代理设置（需要时取消注释）
# PROXIES = {'http': 'http://127.0.0.1:7890', 'https': 'http://127.0.0.1:7890'}
PROXIES = None


# ══════════════════════════════════════════════════════
#  HTTP 工具
# ══════════════════════════════════════════════════════

_session = requests.Session()
_session.headers.update(HEADERS)


def smart_delay():
    time.sleep(random.uniform(DELAY_MIN, DELAY_MAX))


def fetch(url, retries=2, referer=''):
    headers = dict(HEADERS)
    if referer:
        headers['Referer'] = referer
    for attempt in range(retries + 1):
        try:
            resp = _session.get(url, headers=headers, timeout=TIMEOUT, proxies=PROXIES, allow_redirects=True, verify=False)
            resp.encoding = resp.apparent_encoding or 'utf-8'
            if resp.status_code == 200:
                return resp.text, None
            elif resp.status_code == 403:
                wait = random.uniform(10, 20) * (attempt + 1)
                print(f"      [403] 等待 {wait:.0f}s...")
                time.sleep(wait)
            else:
                return None, f"HTTP {resp.status_code}"
        except requests.RequestException as e:
            if attempt < retries:
                time.sleep(random.uniform(3, 6))
            else:
                return None, str(e)
    return None, "重试次数用尽"


def fetch_json(url, **kwargs):
    text, err = fetch(url, **kwargs)
    if text:
        try:
            return json.loads(text), None
        except json.JSONDecodeError as e:
            return None, f"JSON解析失败: {e}"
    return None, err


def make_soup(html):
    return BeautifulSoup(html, 'lxml')


# ══════════════════════════════════════════════════════
#  数据源 1：豆瓣读书（简介 + 目录 + 作者信息）
# ══════════════════════════════════════════════════════

def search_douban(title, author=''):
    keyword = f"{title} {author}".strip()
    url = f"https://search.douban.com/book/subject_search?search_text={quote(keyword)}&cat=1001"
    html, err = fetch(url, referer='https://www.douban.com/')
    if not html:
        return None, err
    ids = list(dict.fromkeys(re.findall(r'subject/(\d+)', html)))
    return ids[0] if ids else None, None


def fetch_douban(subject_id):
    url = f"https://book.douban.com/subject/{subject_id}/"
    html, err = fetch(url, referer='https://www.douban.com/')
    if not html:
        return None, err

    soup = make_soup(html)
    result = {
        'title': '', 'summary': '', 'author_info': '',
        'info': {}, 'tags': [], 'chapters': [], 'sample_texts': [], 'url': url,
    }

    el = soup.select_one('span[property="v:itemreviewed"]')
    if el:
        result['title'] = el.get_text(strip=True)

    el = soup.select_one('strong[property="v:average"]')
    if el:
        result['info']['豆瓣评分'] = el.get_text(strip=True)

    info_div = soup.select_one('#info')
    if info_div:
        for line in info_div.get_text('\n', strip=True).split('\n'):
            line = line.strip()
            sep = '：' if '：' in line else ':'
            if sep in line:
                k, v = line.split(sep, 1)
                if k.strip() and v.strip():
                    result['info'][k.strip()] = v.strip()

    all_intro = soup.select_one('#link-report .all')
    intro = all_intro or soup.select_one('#link-report .intro')
    if intro:
        ps = intro.select('p')
        texts = [p.get_text(strip=True) for p in ps if p.get_text(strip=True)]
        result['summary'] = '\n'.join(texts) if texts else intro.get_text('\n', strip=True)

    for h2 in soup.select('h2'):
        if '作者简介' in h2.get_text(strip=True):
            sibling = h2.find_next_sibling()
            while sibling:
                if sibling.name == 'div':
                    intro_el = sibling.select_one('.intro')
                    if intro_el:
                        text = intro_el.get_text(strip=True)
                        if len(text) > 30:
                            result['author_info'] = text
                            break
                sibling = sibling.find_next_sibling()
            break

    dir_id = f"dir_{subject_id}_full"
    toc_el = soup.select_one(f'#{dir_id}')
    if toc_el:
        for line in toc_el.get_text('\n', strip=True).split('\n'):
            line = line.strip()
            if line and '· ·' not in line and line not in ('(收起)', '(更多)', '收起', '更多'):
                result['chapters'].append(line)

    tag_els = soup.select('#db-tags-section .tag a')
    result['tags'] = [t.get_text(strip=True) for t in tag_els]

    for h2 in soup.select('h2'):
        if '原文摘录' in h2.get_text(strip=True):
            parent = h2.find_parent()
            if parent:
                for el in parent.select('p, .quote'):
                    text = el.get_text(strip=True)
                    if text and len(text) > 30 and '查看原文' not in text:
                        result['sample_texts'].append(text[:300])
            break

    return result, None


# ══════════════════════════════════════════════════════
#  数据源 2：古诗文网（公版书章节正文）
# ══════════════════════════════════════════════════════

def search_gushiwen(title, author=''):
    url = f"https://www.gushiwen.cn/search.aspx?value={quote(title)}"
    html, err = fetch(url)
    if not html:
        return None, err
    soup = make_soup(html)
    for a in soup.select('.result a[href*="book"], .sons a[href*="book"]'):
        text = a.get_text(strip=True)
        if title in text or text in title:
            return urljoin('https://www.gushiwen.cn/', a.get('href', '')), None
    return None, None


def fetch_gushiwen(book_url):
    html, err = fetch(book_url)
    if not html:
        return None, err

    soup = make_soup(html)
    result = {
        'title': '', 'summary': '', 'author_info': '',
        'info': {}, 'tags': [], 'chapters': [], 'sample_texts': [], 'url': book_url,
    }

    # 书名
    el = soup.select_one('h1, .sonstitle')
    if el:
        result['title'] = el.get_text(strip=True)

    # 目录和章节
    for a in soup.select('.catalog a, .mulu a, .dirListBox a'):
        href = a.get('href', '')
        title = a.get_text(strip=True)
        if href and title:
            chapter_url = urljoin(book_url, href)
            content, _ = fetch(chapter_url)
            if content:
                ch_soup = make_soup(content)
                content_el = ch_soup.select_one('#constr, .contson, .sons')
                if content_el:
                    for tag in content_el.select('script, style, .tool, .footnote'):
                        tag.decompose()
                    chapter_content = content_el.get_text('\n', strip=True)
                    result['chapters'].append({
                        'title': title,
                        'content': chapter_content[:5000]
                    })
                    smart_delay()

    # 如果没有目录结构，尝试直接获取页面内容
    if not result['chapters']:
        content_el = soup.select_one('#constr, .contson, .sons')
        if content_el:
            for tag in content_el.select('script, style, .tool, .footnote'):
                tag.decompose()
            result['chapters'].append({
                'title': result['title'] or title,
                'content': content_el.get_text('\n', strip=True)[:5000]
            })

    return result, None


# ══════════════════════════════════════════════════════
#  数据源 3：自定义源（你自己填写）
# ══════════════════════════════════════════════════════

def search_custom(title, author=''):
    """
    在奇书网(https://m.qishu33.com/)搜索书籍。
    搜索接口：/search.html?searchkey=书名&searchtype=all
    返回书的详情页URL，找不到返回None。
    """
    url = f"https://m.qishu33.com/search.html?searchkey={quote(title)}&searchtype=all"
    html, err = fetch(url)
    if not html:
        return None, err

    # 从搜索结果页提取 novelid（数字ID）
    match = re.search(r'novelid=(\d+)', html)
    if match:
        book_id = match.group(1)
        return f"https://m.qishu33.com/xiaoshuo/{book_id}/", None

    return None, None


def fetch_custom(url):
    """
    从奇书网小说页面爬取：简介 + 章节目录 + 前3章正文。
    """
    html, err = fetch(url)
    if not html:
        return None, err

    soup = make_soup(html)
    result = {
        'title': '', 'summary': '', 'author_info': '',
        'info': {}, 'tags': [], 'chapters': [], 'sample_texts': [], 'url': url,
    }

    # 书名 - h1（可能有多个，取非空的）
    for h1_tag in soup.select('h1'):
        t = h1_tag.get_text(strip=True)
        if t:
            result['title'] = t
            break

    # 作者、分类、章数等信息
    for el in soup.select('.gray'):
        text = el.get_text(strip=True)
        for prefix, key in [('作者：', '作者'), ('作者:', '作者'),
                            ('分类：', '分类'), ('分类:', '分类'),
                            ('章数：', '章数'), ('章数:', '章数'),
                            ('进度：', '进度'), ('进度:', '进度'),
                            ('更新：', '更新时间'), ('更新:', '更新时间')]:
            if text.startswith(prefix):
                result['info'][key] = text[len(prefix):]

    # 作者链接
    author_link = soup.select_one('a[href*="/author/"]')
    if author_link:
        result['author_info'] = author_link.get_text(strip=True)

    # 作品简介 - 在 .bookintro .con 中
    intro_el = soup.select_one('.bookintro .con')
    if intro_el:
        text = intro_el.get_text(strip=True)
        # 去掉 [+展开] 或 ] 前缀
        text = re.sub(r'^[\[\]\+\s]*展开[\]\s]*', '', text)
        text = text.lstrip(']').strip()
        result['summary'] = text.strip()

    # 获取章节目录页
    dir_link = soup.select_one('a[href*="/dir/"]')
    if dir_link:
        dir_url = dir_link.get('href', '')
        if not dir_url.startswith('http'):
            dir_url = f"https://m.qishu33.com{dir_url}"

        dir_html, _ = fetch(dir_url)
        if dir_html:
            dir_soup = make_soup(dir_html)
            all_chapters = []
            for a in dir_soup.select('a[href*="/tc_"]'):
                ch_title = a.get_text(strip=True)
                ch_href = a.get('href', '')
                if ch_title and ch_href:
                    if not ch_href.startswith('http'):
                        ch_href = f"https://m.qishu33.com{ch_href}"
                    all_chapters.append({'title': ch_title, 'url': ch_href, 'content': ''})

            result['chapters'] = [ch['title'] for ch in all_chapters]

            # 用 read_N.html 格式爬取前10章正文（tc格式内容是JS加载的）
            print(f"      共{len(all_chapters)}章，爬取前10章正文...")
            for i in range(min(10, len(all_chapters))):
                ch = all_chapters[i]
                # 把 tc_XXX 链接转成 read_N.html 格式（tc格式内容是JS加载的）
                read_url = re.sub(r'/tc_\d+\.html', f'/read_{i+1}.html', ch['url'])
                if not read_url.startswith('http'):
                    read_url = f"https://m.qishu33.com{read_url}"

                smart_delay()
                ch_html, _ = fetch(read_url)
                if ch_html:
                    ch_soup = make_soup(ch_html)
                    # 正文在 .articlecon 中
                    content_el = ch_soup.select_one('.articlecon')
                    if content_el:
                        for tag in content_el.select('script, style, .ad, .tip'):
                            tag.decompose()
                        ch['content'] = content_el.get_text('\n', strip=True)[:5000]

            # 把有正文的章节加入 sample_texts
            for ch in all_chapters[:10]:
                if ch['content']:
                    result['sample_texts'].append(f"【{ch['title']}】\n{ch['content'][:1500]}")

    return result, None


# ══════════════════════════════════════════════════════
#  数据源注册表
# ══════════════════════════════════════════════════════

SOURCES = {
    "qishu33": {
        "search": search_custom,
        "fetch": fetch_custom,
        "has_content": True,
        "desc": "奇书网 - 简介/目录/前10章正文",
    },
}


# ══════════════════════════════════════════════════════
#  数据库
# ══════════════════════════════════════════════════════

def get_db():
    return pymysql.connect(
        host=MYSQL_HOST, port=MYSQL_PORT,
        user=MYSQL_USER, password=MYSQL_PASSWORD,
        database=MYSQL_DB, charset="utf8mb4",
        cursorclass=pymysql.cursors.DictCursor,
    )


def load_books(cursor, book_id=None, title=None):
    if book_id:
        cursor.execute("SELECT * FROM books WHERE book_id = %s", (book_id,))
    elif title:
        cursor.execute("SELECT * FROM books WHERE title LIKE %s AND status = 1", (f"%{title}%",))
    else:
        cursor.execute("SELECT * FROM books WHERE status = 1")
    return cursor.fetchall()


# ══════════════════════════════════════════════════════
#  内容格式化
# ══════════════════════════════════════════════════════

def format_content(book_db, source_data):
    lines = []

    # 内容简介
    summary = source_data.get('summary', '')
    if summary:
        lines.append(f"【内容简介】\n\n{summary.strip()}\n\n{'-' * 40}\n")

    # 目录（只保留前10章）
    chapters = source_data.get('chapters', [])
    if chapters:
        lines.append("【目录】\n")
        for ch in chapters[:10]:
            if isinstance(ch, dict):
                lines.append(ch.get('title', ''))
            else:
                lines.append(str(ch))
        lines.append(f"\n{'-' * 40}\n")

    # 章节正文（只保留前10章）
    samples = source_data.get('sample_texts', [])
    if samples:
        lines.append("【章节正文】\n")
        for text in samples[:10]:
            lines.append(f"{text}\n\n{'-' * 40}\n")

    return "\n".join(lines)


# ══════════════════════════════════════════════════════
#  进度管理
# ══════════════════════════════════════════════════════

def load_progress():
    try:
        return set(json.loads(Path(PROGRESS_FILE).read_text('utf-8')).get('done', []))
    except Exception:
        return set()


def save_progress(done_ids):
    Path(PROGRESS_FILE).write_text(
        json.dumps({'done': list(done_ids)}, ensure_ascii=False), 'utf-8'
    )


# ════════════════════════════════════════════════════
#  检测没有正文的书
# ════════════════════════════════════════════════════

def detect_empty_books():
    """扫描 uploads/ebooks/ 下的txt文件，找出没有正文的书"""
    empty = []
    has_content = []
    ebooks_dir = Path(EBOOK_DIR)
    
    if not ebooks_dir.exists():
        return empty, has_content

    for txt_file in sorted(ebooks_dir.glob('*.txt')):
        book_id = txt_file.stem
        try:
            text = txt_file.read_text(encoding='utf-8')
        except Exception:
            continue

        # 检测是否有章节正文
        if '【章节正文】' in text:
            # 检查正文部分是否有实际内容（不只是目录标题）
            body_start = text.index('【章节正文】')
            body = text[body_start:]
            # 正文长度超过500字才算有内容
            if len(body) > 500:
                has_content.append(book_id)
                continue

        # 没有正文或正文太短
        title = text.split('\n')[0] if text else f'ID:{book_id}'
        empty.append({'id': book_id, 'title': title, 'size': len(text)})

    return empty, has_content


def print_detect_result():
    """打印检测结果"""
    empty, has = detect_empty_books()
    total = len(empty) + len(has)

    print(f"共扫描 {total} 本书:")
    print(f"  有正文: {len(has)} 本")
    print(f"  无正文: {len(empty)} 本")

    if empty:
        print(f"\n【没有正文的书】:")
        for book in empty:
            print(f"  ID={book['id']:>4s}  {book['title'][:40]:<40s}  {book['size']}字")

    return empty


TEMPLATE_KEYWORDS = [
    '第一章 书籍导读', '第一章 作品概述', '第一章 作品信息',
    '基础概念\n\n在深入学习', '核心原理\n\n掌握了基础概念',
    '第一章 作品介绍', '第一章 作品概览',
]


def is_template(filepath):
    try:
        text = Path(filepath).read_text('utf-8', errors='ignore')[:800]
        return any(kw in text for kw in TEMPLATE_KEYWORDS)
    except Exception:
        return False


def process_one_book(book, sources, dry_run=False):
    book_id = book['book_id']
    title = book['title'] or ''
    author = book['author'] or ''
    txt_path = Path(EBOOK_DIR) / f"{book_id}.txt"

    print(f"\n{'─' * 50}")
    print(f"  Book#{book_id} 《{title}》 - {author}")

    best_data = None
    best_score = 0

    for src_name in sources:
        src = SOURCES.get(src_name)
        if not src:
            print(f"    ⚠ 未知数据源: {src_name}")
            continue

        print(f"    ▸ [{src_name}] {src['desc']}")

        search_fn = src.get('search')
        if not search_fn:
            continue

        found_id, err = search_fn(title, author)
        if err:
            print(f"      搜索失败: {err}")
            continue
        if not found_id:
            print(f"      未找到")
            continue

        print(f"      找到: {found_id}")
        smart_delay()

        fetch_fn = src.get('fetch')
        if not fetch_fn:
            continue

        data, err = fetch_fn(found_id)
        if err:
            print(f"      获取失败: {err}")
            continue

        # 评分
        score = len(data.get('summary', ''))
        score += len(data.get('chapters', [])) * 10
        if data.get('author_info') and len(data['author_info']) > 30:
            score += 50
        has_content = any(isinstance(ch, dict) and ch.get('content') for ch in data.get('chapters', []))
        if has_content:
            score += 500

        print(f"      评分: {score} (摘要:{len(data.get('summary',''))}字 "
              f"目录:{len(data.get('chapters',[]))}条 "
              f"正文:{'有' if has_content else '无'})")

        if score > best_score:
            best_score = score
            best_data = data

        if has_content:
            break
        smart_delay()

    if not best_data:
        print(f"    ✗ 所有数据源均未获取到内容")
        return False

    content = format_content(book, best_data)

    if dry_run:
        print(f"\n    [预览] 内容长度: {len(content)} 字符")
        for line in content[:300].split('\n'):
            print(f"    {line}")
        return True

    txt_path.write_text(content, encoding='utf-8')
    print(f"    ✓ 写入 {txt_path.name} ({len(content)/1024:.1f}KB)")

    preview = content[:2000] + "\n\n[... 以下内容需登录后阅读 ...]" if len(content) > 2000 else content
    return content, preview


def main():
    parser = argparse.ArgumentParser(
        description='奇书网书籍内容爬虫',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
示例：
  %(prog)s --book-id 76                        爬取指定ID的书
  %(prog)s --title "三体"                      按书名搜索
  %(prog)s --title "诡秘之主" --limit 10       搜书名，爬前10本
  %(prog)s --all                               爬所有书
  %(prog)s --list-sources                      查看可用数据源
        """)
    parser.add_argument('--book-id', type=int, help='指定book_id')
    parser.add_argument('--title', type=str, help='按书名搜索')
    parser.add_argument('--limit', type=int, default=0, help='只处理前N本')
    parser.add_argument('--source', type=str, nargs='+', default=['qishu33'],
                        help='使用哪些数据源 (默认: qishu33)')
    parser.add_argument('--all', action='store_true', help='处理所有书')
    parser.add_argument('--force', action='store_true', help='强制更新')
    parser.add_argument('--dry-run', action='store_true', help='仅预览不写入')
    parser.add_argument('--list-sources', action='store_true', help='列出所有数据源')
    parser.add_argument('--detect', action='store_true', help='检测哪些书没有正文')
    args = parser.parse_args()

    if args.list_sources:
        print("\n可用数据源：")
        print("─" * 60)
        for name, src in SOURCES.items():
            flag = "✓ 有正文" if src['has_content'] else "✗ 仅元数据"
            print(f"  {name:12s} {src['desc']:30s} {flag}")
        print("─" * 60)
        return

    if args.detect:
        print_detect_result()
        return

    conn = get_db()
    cursor = conn.cursor()
    books = load_books(cursor, args.book_id, args.title)

    if args.limit:
        books = books[:args.limit]
    if not args.all and not args.book_id and not args.title and not args.limit:
        print("请指定 --book-id, --title, --limit 或 --all")
        return

    done_ids = set() if args.force else load_progress()

    total = len(books)
    sources = args.source
    print(f"\n{'═' * 50}")
    print(f"  书籍内容爬虫  |  {total}本书  |  数据源: {', '.join(sources)}")
    if args.dry_run:
        print(f"  模式: 预览")
    print(f"{'═' * 50}")

    success = failed = skipped = updated_db = 0

    for i, book in enumerate(books, 1):
        book_id = book['book_id']

        if book_id in done_ids and not args.force:
            skipped += 1
            continue

        if not args.force:
            txt_path = Path(EBOOK_DIR) / f"{book_id}.txt"
            if txt_path.exists() and not is_template(txt_path) and txt_path.stat().st_size > 1000:
                skipped += 1
                done_ids.add(book_id)
                continue

        print(f"[{i}/{total}]", end='')

        try:
            result = process_one_book(book, sources, dry_run=args.dry_run)
            if result:
                success += 1
                done_ids.add(book_id)
                if not args.dry_run and isinstance(result, tuple):
                    _, preview = result
                    cursor.execute(
                        "UPDATE books SET preview_content = %s, update_time = NOW() WHERE book_id = %s",
                        (preview, book_id)
                    )
                    updated_db += 1
            else:
                failed += 1
        except Exception as e:
            print(f"    ✗ 异常: {e}")
            failed += 1

        if i % 5 == 0:
            save_progress(done_ids)
            if not args.dry_run:
                conn.commit()
            print(f"\n  ── 进度: {i}/{total} | ✓{success} ✗{failed} ⊘{skipped} ──")

    save_progress(done_ids)
    if not args.dry_run:
        conn.commit()
    cursor.close()
    conn.close()

    print(f"\n{'═' * 50}")
    print(f"  完成！成功:{success}  失败:{failed}  跳过:{skipped}  数据库更新:{updated_db}")
    print(f"{'═' * 50}")


if __name__ == '__main__':
    main()
