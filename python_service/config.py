import os
from dotenv import load_dotenv

load_dotenv()

# MySQL
MYSQL_HOST = os.getenv("MYSQL_HOST", "localhost")
MYSQL_PORT = int(os.getenv("MYSQL_PORT", 3306))
MYSQL_USER = os.getenv("MYSQL_USER", "root")
MYSQL_PASSWORD = os.getenv("MYSQL_PASSWORD", "root123")
MYSQL_DB = os.getenv("MYSQL_DB", "library_db")

# Neo4j
NEO4J_URI = os.getenv("NEO4J_URI", "bolt://localhost:7687")
NEO4J_USER = os.getenv("NEO4J_USER", "neo4j")
NEO4J_PASSWORD = os.getenv("NEO4J_PASSWORD", "neo4j123")

# 推荐权重
WEIGHT_KG = float(os.getenv("WEIGHT_KG", 0.4))
WEIGHT_ITEMCF = float(os.getenv("WEIGHT_ITEMCF", 0.3))
WEIGHT_HOT = float(os.getenv("WEIGHT_HOT", 0.15))
WEIGHT_NEW = float(os.getenv("WEIGHT_NEW", 0.15))

# 服务端口
SERVICE_PORT = int(os.getenv("SERVICE_PORT", 8000))
