package com.ccnio.plugin.data;

import java.io.File;

/**
 * Created by jianfeng.li on 20-7-15.
 */
public class ResourceInfo {
    private final String path;
    public String name;
    public String value;
    public String dir;
    public String id;

    public ResourceInfo(String name, String value, String path) {
        this.name = name;
        this.value = value;
        this.path = path;
        this.dir = new File(path).getParentFile().getName().replace("-v4", "");
        id = dir + "@" + name;
    }

    @Override
    public String toString() {
        return "value='" + value + '\'' +
                ", path ='" + path;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ResourceInfo && ((ResourceInfo) o).value.equals(value);
    }

    @Override
    public int hashCode() {
        return 1;
    }
}
