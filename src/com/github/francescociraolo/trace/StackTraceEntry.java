package com.github.francescociraolo.trace;

import java.util.Objects;

public class StackTraceEntry {

    private final String functionName;
    private final String address;

    public StackTraceEntry(String address) {
        this(null, address);
    }

    public StackTraceEntry(String functionName,
                           String address) {
        this.functionName = functionName;
        this.address = address;
    }

    public String getFunctionName() {
        return functionName;
    }

    public String getAddress() {
        return address;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StackTraceEntry that = (StackTraceEntry) o;
        return (functionName != null && functionName.equals(that.functionName)) || (functionName == null || that.functionName == null);
    }

    @Override
    public int hashCode() {
        return Objects.hash(functionName);
    }

    @Override
    public String toString() {
        return functionName != null ? getFunctionName() : getAddress();
    }
}
