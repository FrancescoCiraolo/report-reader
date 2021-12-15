package com.github.francescociraolo.trace;

import java.io.PrintStream;
import java.util.List;

public abstract class Record {

    /*
    Process identifier
    */
    protected final ProcessInfo process;
    protected final int core;

    /*
    Keeping it in string form in order to avoid floating point errors
    */
    protected final Timestamp timestamp;
    protected final RecordSpecification specification;
    protected final List<StackTraceEntry> stackTrace;

    public Record(ProcessInfo process,
                  int core,
                  String timestamp,
                  RecordSpecification specification,
                  List<StackTraceEntry> stackTrace) {
        this.process = process;
        this.core = core;
        this.timestamp = new Timestamp(timestamp);
        this.specification = specification;
        this.stackTrace = stackTrace;
    }

    public Record(ProcessInfo process,
                  int core,
                  Timestamp timestamp,
                  RecordSpecification specification,
                  List<StackTraceEntry> stackTrace) {
        this.process = process;
        this.core = core;
        this.timestamp = timestamp;
        this.specification = specification;
        this.stackTrace = stackTrace;
    }

    public ProcessInfo getProcess() {
        return process;
    }

    public String getProcessName() {
        return process.getName();
    }

    public int getProcessPid() {
        return process.getPid();
    }

    public int getCore() {
        return core;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public List<StackTraceEntry> getStackTrace() {
        return stackTrace;
    }

    public RecordSpecification getSpecification() {
        return specification;
    }

    public abstract boolean isPidRelated(int pid);

    public void print(PrintStream printStream) {
        print(printStream, true, "");
    }

    public void print(PrintStream printStream, boolean printStackTrace) {
        print(printStream, printStackTrace, "");
    }

    public abstract void print(PrintStream printStream, boolean printStackTrace, String linePrefix);

    public static boolean isEvent(Record record) {
        return record.getSpecification().getType().getCategory() == RecordSpecification.TypeCategory.EVENT;
    }

    public RecordSpecification.Type getType() {
        return getSpecification().getType();
    }

    public static class Timestamp implements Comparable<Timestamp> {

        private final String asString;
        private final double asDouble;

        public Timestamp(String timestamp) {
            this.asString = timestamp;
            this.asDouble = Double.parseDouble(timestamp);
        }

        public Timestamp(double timestamp) {
            this.asString = String.valueOf(timestamp);
            this.asDouble = timestamp;
        }

        @Override
        public int compareTo(Timestamp o) {
            return compareTo(o.asDouble);
        }

        public int compareTo(double time) {
            return Double.compare(asDouble, time);
        }

        public double getAsDouble() {
            return asDouble;
        }

        public double timeFrom(Timestamp other) {
            return asDouble - other.asDouble;
        }

        @Override
        public String toString() {
            return asString;
        }
    }
}
