import logging
import uvicorn
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from config import SERVICE_PORT

# 日志配置
logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(name)s: %(message)s")
log = logging.getLogger(__name__)

# FastAPI 实例
app = FastAPI(
    title="图书推荐算法服务",
    description="基于知识图谱 + 协同过滤的混合推荐、图谱查询、离线任务",
    version="1.0.0",
)

# CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 注册路由
from routers.recommend import router as recommend_router
from routers.kg import router as kg_router
from routers.tasks import router as tasks_router

app.include_router(recommend_router)
app.include_router(kg_router)
app.include_router(tasks_router)


@app.get("/")
def root():
    return {
        "service": "图书推荐算法服务",
        "version": "1.0.0",
        "endpoints": {
            "推荐服务": "/api/v1/recommend/*",
            "知识图谱": "/api/v1/kg/*",
            "离线任务": "/api/v1/tasks/*",
            "文档": "/docs",
        },
    }


if __name__ == "__main__":
    log.info(f"启动 FastAPI 服务，端口: {SERVICE_PORT}")
    uvicorn.run("main:app", host="0.0.0.0", port=SERVICE_PORT, reload=True)
