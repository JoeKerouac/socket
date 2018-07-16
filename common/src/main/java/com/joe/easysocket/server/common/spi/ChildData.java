package com.joe.easysocket.server.common.spi;

import org.apache.curator.utils.PathUtils;

import lombok.Data;

/**
 * @author joe
 */
@Data
public class ChildData implements Comparable<ChildData> {
    private final String path;
    private final byte[] data;

    public ChildData(String path, byte[] data) {
        this.path = PathUtils.validatePath(path);
        this.data = data;
    }

    @Override
    public int compareTo(ChildData rhs) {
        if (this == rhs) {
            return 0;
        }
        if (rhs == null || getClass() != rhs.getClass()) {
            return -1;
        }

        return path.compareTo(rhs.path);
    }
}
