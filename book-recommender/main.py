import uvicorn
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager
from app.api.kg import router as kg_router
from app.api.recommend import router as recommend_router
from app.config.settings import settings
from app.database.neo4j_client import neo4j_client
import logging

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """应用生命周期管理（替代已废弃的 on_event）"""
    # 启动时
    if neo4j_client.test_connection():
        logger.info("[OK] Neo4j connection successful")
    else:
        logger.warning("[FAIL] Neo4j connection failed")
    yield
    # 关闭时
    neo4j_client.close()
    logger.info("[INFO] Neo4j connection closed")


# 创建 FastAPI 实例
app = FastAPI(
    title="图书推荐算法服务",
    description="基于知识图谱的个性化荐书系统 - 算法后端",
    version="1.0.0",
    lifespan=lifespan
)

# 跨域配置
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 生产环境建议改为具体域名
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 注册路由
app.include_router(kg_router, prefix="/api/v1")
app.include_router(recommend_router, prefix="/api/v1")


@app.get("/health", summary="健康检查")
def health_check():
    """服务健康检查接口"""
    return {"status": "ok", "service": "book-recommendation"}


if __name__ == "__main__":
    uvicorn.run(
        "main:app",
        host=settings.server_host,
        port=settings.server_port,
        reload=True
    )
