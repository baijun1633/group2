import pymysql
from pymysql.cursors import DictCursor
from app.config.settings import settings
import logging

logger = logging.getLogger(__name__)


class MysqlClient:
    def __init__(self):
        self.connection = None
        self._connect()

    def _connect(self):
        """建立 MySQL 连接"""
        self.connection = pymysql.connect(
            host=settings.mysql_host,
            port=settings.mysql_port,
            user=settings.mysql_user,
            password=settings.mysql_password,
            database=settings.mysql_db,
            charset="utf8mb4",
            cursorclass=DictCursor,
            autocommit=True  # 自动提交，避免连接超时后事务挂起
        )

    def _ensure_connection(self):
        """检查连接是否有效，失效则重连"""
        try:
            self.connection.ping(reconnect=True)
        except Exception:
            logger.warning("MySQL 连接失效，正在重连...")
            self._connect()
            logger.info("MySQL 重连成功")

    def execute_query(self, sql: str, params: dict = None):
        """执行查询语句，返回结果列表（带自动重连）"""
        self._ensure_connection()
        try:
            with self.connection.cursor() as cursor:
                cursor.execute(sql, params)
                return cursor.fetchall()
        except pymysql.OperationalError:
            # 连接断开，重连后重试一次
            logger.warning("MySQL 查询失败，尝试重连后重试...")
            self._connect()
            with self.connection.cursor() as cursor:
                cursor.execute(sql, params)
                return cursor.fetchall()

    def close(self):
        if self.connection:
            self.connection.close()


# 全局单例
mysql_client = MysqlClient()
