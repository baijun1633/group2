/** 知识图谱单个节点 */
export interface GraphNode {
  /** 节点唯一ID */
  id: string
  /** 节点展示名称 */
  label: string
  /** 节点类型：图书/作者/标签/分类 */
  type: 'book' | 'author' | 'tag' | 'category'
}

/** 知识图谱关系边 */
export interface GraphEdge {
  /** 源节点id */
  source: string
  /** 目标节点id */
  target: string
  /** 关系名称 */
  relation: string
}

/** 图谱完整返回数据结构 */
export interface GraphData {
  /** 所有实体节点数组 */
  nodes: GraphNode[]
  /** 实体之间关联关系数组 */
  edges: GraphEdge[]
}