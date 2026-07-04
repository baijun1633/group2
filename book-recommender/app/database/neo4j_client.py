from neo4j import GraphDatabase, Driver
from app.config.settings import settings
from typing import List, Dict, Any
import threading
import logging

logger = logging.getLogger(__name__)


class Neo4jClient:
    _instance = None
    _driver: Driver = None
    _lock = threading.Lock()  # 线程锁，保证单例安全

    def __new__(cls):
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = super().__new__(cls)
        return cls._instance

    def _ensure_driver(self):
        """延迟初始化 Neo4j 驱动（线程安全）"""
        if not self._driver:
            with self._lock:
                if not self._driver:
                    self._driver = GraphDatabase.driver(
                        settings.neo4j_uri,
                        auth=(settings.neo4j_user, settings.neo4j_password)
                    )
                    logger.info("Neo4j 驱动初始化完成")

    def close(self):
        """关闭连接"""
        if self._driver:
            self._driver.close()
            self._driver = None
            logger.info("Neo4j 连接已关闭")

    def execute_query(self, cypher: str, params: Dict[str, Any] = None) -> List[Dict[str, Any]]:
        """
        通用执行 Cypher 查询
        :param cypher: Cypher 语句
        :param params: 查询参数
        :return: 字典列表结果
        """
        self._ensure_driver()
        if params is None:
            params = {}

        with self._driver.session() as session:
            result = session.run(cypher, params)
            return [dict(record) for record in result]

    def test_connection(self) -> bool:
        """测试连通性"""
        try:
            self._ensure_driver()
            self._driver.verify_connectivity()
            return True
        except Exception as e:
            logger.error(f"Neo4j 连接失败: {e}")
            return False


# 全局单例实例
neo4j_client = Neo4jClient()
