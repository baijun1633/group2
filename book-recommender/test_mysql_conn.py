from app.database.mysql_client import mysql_client

if __name__ == "__main__":
    try:
        # 1. 测试基础连接
        result = mysql_client.execute_query("SELECT 1 AS test")
        print("✅ MySQL连接成功，测试结果：", result)

        # 2. 测试查图书表
        book_count = mysql_client.execute_query("SELECT count(*) as cnt FROM books")
        print("📚 图书总数：", book_count[0]["cnt"])

        # 3. 测试查分类表
        categories = mysql_client.execute_query("SELECT * FROM categories LIMIT 5")
        print("📂 前5条分类：", categories)

    except Exception as e:
        print("❌ 连接失败，错误信息：", e)
    finally:
        mysql_client.close()
