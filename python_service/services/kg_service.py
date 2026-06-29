import logging
from db import get_neo4j_driver

log = logging.getLogger(__name__)


def query_cypher(cypher: str, limit: int = 100) -> list[dict]:
    """执行 Cypher 查询"""
    if not cypher.strip():
        raise ValueError("Cypher 语句不能为空")

    # 简单校验：禁止破坏性操作
    forbidden = ["DELETE", "DETACH", "REMOVE", "SET", "CREATE", "MERGE"]
    upper = cypher.upper()
    for kw in forbidden:
        if kw in upper:
            raise ValueError(f"禁止执行 {kw} 操作")

    if "LIMIT" not in upper:
        cypher = cypher.rstrip(";") + f" LIMIT {min(limit, 1000)}"

    driver = get_neo4j_driver()
    try:
        with driver.session() as session:
            result = session.run(cypher)
            return [dict(r) for r in result]
    finally:
        driver.close()


def get_graph_stats() -> dict:
    """获取图谱统计信息"""
    driver = get_neo4j_driver()
    try:
        with driver.session() as session:
            nodes = session.run("MATCH (n) RETURN count(n) AS cnt").single()["cnt"]
            rels = session.run("MATCH ()-[r]->() RETURN count(r) AS cnt").single()["cnt"]
            labels = session.run("CALL db.labels()").data()
            return {
                "totalNodes": nodes,
                "totalRelations": rels,
                "labels": [l["label"] for l in labels],
            }
    finally:
        driver.close()


def get_subgraph(book_id: int = None, depth: int = 2, limit: int = 50) -> dict:
    """获取子图数据（节点+边）"""
    driver = get_neo4j_driver()
    try:
        with driver.session() as session:
            if book_id:
                cypher = """
                    MATCH path = (b:Book {bookId: $bookId})-[*1..""" + str(depth) + """]->(n)
                    WITH nodes(path) AS ns, relationships(path) AS rs
                    UNWIND ns AS node UNWIND rs AS rel
                    RETURN DISTINCT
                        collect(DISTINCT {id: id(node), labels: labels(node), properties: properties(node)}) AS nodes,
                        collect(DISTINCT {id: id(rel), type: type(rel), start: id(startNode(rel)), end: id(endNode(rel))}) AS edges
                    LIMIT 1
                """
                record = session.run(cypher, bookId=str(book_id)).single()
            else:
                cypher = """
                    MATCH (n)-[r]->(m)
                    WITH n, r, m LIMIT $limit
                    RETURN
                        collect(DISTINCT {id: id(n), labels: labels(n), properties: properties(n)}) +
                        collect(DISTINCT {id: id(m), labels: labels(m), properties: properties(m)}) AS nodes,
                        collect(DISTINCT {id: id(r), type: type(r), start: id(startNode(r)), end: id(endNode(r))}) AS edges
                """
                record = session.run(cypher, limit=limit).single()

            if record:
                return {"nodes": record["nodes"], "edges": record["edges"]}
            return {"nodes": [], "edges": []}
    finally:
        driver.close()
