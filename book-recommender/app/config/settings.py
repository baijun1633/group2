from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    # Neo4j 配置
    neo4j_uri: str
    neo4j_user: str
    neo4j_password: str

    # MySQL 配置
    mysql_host: str
    mysql_port: int = 3306
    mysql_user: str
    mysql_password: str
    mysql_db: str

    # 服务配置
    server_host: str = "0.0.0.0"
    server_port: int = 8000

    class Config:
        env_file = ".env"


settings = Settings()
