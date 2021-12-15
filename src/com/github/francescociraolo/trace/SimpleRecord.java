package com.github.francescociraolo.trace;

import java.io.PrintStream;
import java.util.List;

public class SimpleRecord extends Record {

    private static final boolean PRINT_STACK_TRACE = false;

    public SimpleRecord(ProcessInfo process,
                        int core,
                        String timestamp,
                        RecordSpecification recordSpecification) {
        this(process, core, timestamp, recordSpecification, List.of());
    }

    public SimpleRecord(ProcessInfo process,
                        int core,
                        String timestamp,
                        RecordSpecification recordSpecification,
                        List<StackTraceEntry> stackTrace) {
        super(process, core, timestamp, recordSpecification, stackTrace);
    }

    public SimpleRecord(ProcessInfo process,
                        int core,
                        Timestamp timestamp,
                        RecordSpecification recordSpecification,
                        List<StackTraceEntry> stackTrace) {
        super(process, core, timestamp, recordSpecification, stackTrace);
    }

    public SimpleRecord(String processName,
                        int pid,
                        int core,
                        String timestamp,
                        RecordSpecification recordSpecification,
                        List<StackTraceEntry> stackTrace) {
        this(new ProcessInfo(pid, processName), core, timestamp, recordSpecification, stackTrace);
        if (getSpecification().getType().getCategory() == RecordSpecification.TypeCategory.EVENT && stackTrace.isEmpty())
            System.err.println("NOPE");
    }

    @Override
    public boolean isPidRelated(int pid) {
        return getProcessPid() == pid || specification.isPidRelated(pid);
    }

    @Override
    public String toString() {
        var res = String.format("   %13s-%-6d [%03d] %12s: %20s: %s ", getProcessName(), getProcessPid(), getCore(), getTimestamp(), specification.getType().getTypeString(), specification);
        if (PRINT_STACK_TRACE) {
            var stringBuilder = new StringBuilder(res);
            for (var entry : getStackTrace())
                stringBuilder.append("\n\t\t\t\t\t\t=>").append(entry);
            res = stringBuilder.toString();
        }
        return res;
    }

    @Override
    public void print(PrintStream printStream, boolean printStackTrace, String linePrefix) {
        printStream.printf("   %13s-%-6d [%03d] %12s: %20s: %s ",
                getProcessName(),
                getProcessPid(),
                getCore(),
                getTimestamp(),
                specification.getType().getTypeString(),
                specification);
        if (printStackTrace)
            for (var entry : getStackTrace())
                printStream.printf("\n%s=>%s", linePrefix, entry);
        printStream.println();
    }
}
