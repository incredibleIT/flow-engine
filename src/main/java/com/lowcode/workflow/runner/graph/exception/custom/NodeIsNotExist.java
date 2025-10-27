package com.lowcode.workflow.runner.graph.exception.custom;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NodeIsNotExist extends RuntimeException{
    private int code;

    public NodeIsNotExist(String message) {
        super(message);
        this.code = 500;
    }

    public NodeIsNotExist(int code, String message) {
        super(message);
        this.code = code;
    }
}
