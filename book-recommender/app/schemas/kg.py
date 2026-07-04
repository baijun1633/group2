from pydantic import BaseModel
from typing import List, Dict


class GraphNode(BaseModel):
    id: str
    label: str
    name: str
    properties: Dict


class GraphEdge(BaseModel):
    source: str
    target: str
    relation: str


class GraphResponse(BaseModel):
    nodes: List[GraphNode]
    edges: List[GraphEdge]
