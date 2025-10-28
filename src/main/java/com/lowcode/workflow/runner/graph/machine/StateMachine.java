package com.lowcode.workflow.runner.graph.machine;

public interface StateMachine<T> {

    void transition(T entity, String event);
}
