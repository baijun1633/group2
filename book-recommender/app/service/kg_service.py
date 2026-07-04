from app.database.neo4j_client import neo4j_client
from app.schemas.kg import GraphResponse, GraphNode, GraphEdge

class KgService:
    @staticmethod
    def get_book_graph(book_id: int, depth: int = 2) -> GraphResponse:
        """
        查询指定图书的关联图谱
        :param book_id: 图书ID
        :param depth: 关联深度，默认2跳
        """
        # 节点查询：用 path 变量获取所有节点
        node_cypher = f"""
        MATCH path = (b:Book {{bookId: $book_id}})-[*1..{depth}]-(n)
        WHERE b <> n
        UNWIND nodes(path) AS node
        WITH DISTINCT node
        WITH node, labels(node)[0] AS label
        RETURN 
            id(node) AS id,
            label AS label,
            coalesce(node.title, node.name, '') AS name,
            properties(node) AS properties
        """
        nodes_data = neo4j_client.execute_query(node_cypher, {"book_id": book_id})

        # 边查询：直接 UNWIND 关系列表，不要套 relationships()
        edge_cypher = f"""
        MATCH (b:Book {{bookId: $book_id}})-[rels*1..{depth}]-(n)
        UNWIND rels AS rel
        WITH DISTINCT rel
        RETURN 
            id(startNode(rel)) AS source,
            id(endNode(rel)) AS target,
            type(rel) AS relation
        """
        edges_data = neo4j_client.execute_query(edge_cypher, {"book_id": book_id})

        # 转成前端需要的格式
        nodes = [
            GraphNode(
                id=str(row["id"]),
                label=row["label"],
                name=row["name"],
                properties=row["properties"]
            ) for row in nodes_data
        ]
        edges = [
            GraphEdge(
                source=str(row["source"]),
                target=str(row["target"]),
                relation=row["relation"]
            ) for row in edges_data
        ]

        return GraphResponse(nodes=nodes, edges=edges)

kg_service = KgService()