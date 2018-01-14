package com.joe.easysocket.server.common.registry;


import lombok.Data;

/**
 * POJO that abstracts a change to a path
 *
 * @author joe
 */
@Data
public class NodeEvent {
    private final Type type;
    private final ChildData data;

    /**
     * Type of change
     */
    public enum Type {
        /**
         * A child was added to the path
         */
        NODE_ADDED,

        /**
         * A child's data was changed
         */
        NODE_UPDATED,

        /**
         * A child was removed from the path
         */
        NODE_REMOVED,
    }

    /**
     * @param type event type
     * @param data event data or null
     */
    public NodeEvent(NodeEvent.Type type, ChildData data) {
        this.type = type;
        this.data = data;
    }
}
