package com.github.francescociraolo.trace;

import java.util.Objects;

public class ProcessInfo {

    private final int pid;
    private final String name;

    public ProcessInfo(int pid, String name) {
        this.pid = pid;
        this.name = name;
    }

    public int getPid() {
        return pid;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProcessInfo process = (ProcessInfo) o;
        return pid == process.pid && name.equals(process.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pid, name);
    }

    public String toTwoPointsString() {
        return String.format("%s:%d", name, pid);
    }

    @Override
    public String toString() {
        return toTwoPointsString();
    }
}
