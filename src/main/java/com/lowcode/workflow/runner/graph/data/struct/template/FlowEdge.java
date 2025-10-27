package com.lowcode.workflow.runner.graph.data.struct.template;


import lombok.Getter;
import org.jgrapht.graph.DefaultEdge;

/**
 * 自定义边
 */
@Getter
public class FlowEdge extends DefaultEdge {

    private final Edge edge;

    public FlowEdge(Edge edge) {
        this.edge = edge;
    }
}
