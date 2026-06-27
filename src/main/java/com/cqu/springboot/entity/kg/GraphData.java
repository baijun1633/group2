package com.cqu.springboot.entity.kg;

import java.util.List;
import lombok.Data;

/**
 * 知识图谱可视化数据结构（兼容 D3.js / vis.js）
 */
@Data
public class GraphData {

    private List<GraphNode> nodes;
    private List<GraphEdge> edges;

    @Data
    public static class GraphNode {
        /** 节点唯一ID */
        private String id;
        /** 显示标签 */
        private String label;
        /** 节点类型: Book / Author / Publisher / Category / Tag */
        private String type;
        /** 附加属性 */
        private java.util.Map<String, Object> properties;

        public GraphNode(String id, String label, String type) {
            this.id = id;
            this.label = label;
            this.type = type;
            this.properties = new java.util.HashMap<>();
        }
    }

    @Data
    public static class GraphEdge {
        /** 起始节点ID */
        private String source;
        /** 目标节点ID */
        private String target;
        /** 关系类型: WRITTEN_BY / PUBLISHED_BY / BELONGS_TO / TAGGED_AS */
        private String type;
        /** 显示标签 */
        private String label;

        public GraphEdge(String source, String target, String type) {
            this.source = source;
            this.target = target;
            this.type = type;
            this.label = type;
        }
    }
}
