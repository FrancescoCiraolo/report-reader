package com.github.francescociraolo.trace.reader;

import com.github.francescociraolo.trace.RecordSpecification;
import com.github.francescociraolo.trace.StackTraceEntry;
import com.github.francescociraolo.trace.ProcessInfo;
import com.github.francescociraolo.trace.Record;
import com.github.francescociraolo.trace.SimpleRecord;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AsciiTrace extends Trace implements Closeable {

    private static final Pattern LINE_PATTERN = Pattern
            .compile("(?<process>\\S+)-(?<pid>\\S+)\\s+\\[(?<core>\\d+)] +(?<timestamp>\\d+\\.\\d+):\\s+(?<type>\\S+):\\s+(?<msg>\\S.*)");
    private static final Pattern STACK_ENTRY_PATTERN = Pattern
            .compile("=> (\\S*)((?: \\((\\S*)\\))?)");

    private final InputStream inputStream;
    private final StringBuilder blockBuilder;
    private final int cpus;
    private int nextChar;

    public AsciiTrace(InputStream inputStream) throws IOException {
        this.inputStream = inputStream;
        this.blockBuilder = new StringBuilder();

        nextChar = inputStream.read();
        for (char c : "cpus=".toCharArray()) {
            if (nextChar != c)
                throw new RuntimeException("Wrong report format!");
            nextChar = inputStream.read();
        }

        int cpus = 0;
        while (nextChar != '\n') {
            cpus *= 10;
            cpus += nextChar - '0';
            nextChar = inputStream.read();
        }

        this.cpus = cpus;
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }

    @Override
    public boolean hasNext() {
        return nextChar != -1;
    }

    private void readBlock() throws IOException {
        blockBuilder.setLength(0);
        boolean end = false;

        while (!end) {
            switch (nextChar) {
                case '\n':
                    nextChar = inputStream.read();
                    end = nextChar == ' ';
                    if (!end) blockBuilder.append('\n');
                    break;
                case -1:
                    end = true;
                    break;
                default:
                    blockBuilder.append((char) nextChar);
                    nextChar = inputStream.read();
                    break;
            }
        }
    }

    @Override
    public int getCpusCount() {
        return cpus;
    }

    @Override
    public synchronized Record next() throws IOException {
        if (!hasNext()) throw new EOFException();

        readBlock();

        Matcher recordMatcher = LINE_PATTERN.matcher(blockBuilder.toString());
        while (!recordMatcher.find()) {
            readBlock();
            recordMatcher = LINE_PATTERN.matcher(blockBuilder.toString());
        }

        String processName = recordMatcher.group("process"),
                timestamp = recordMatcher.group("timestamp");

        int pid = Integer.parseInt(recordMatcher.group("pid")),
                core = Integer.parseInt(recordMatcher.group("core"));
        RecordSpecification recordSpec = RecordSpecification.parse(
                recordMatcher.group("type"),
                recordMatcher.group("msg"));

        LinkedList<StackTraceEntry> stackTrace = new LinkedList<>();
        if (recordSpec.getType().isStackTrace()) {
            Matcher entryMatcher;
            for (String line : blockBuilder.toString().split("\n")) {
                entryMatcher = STACK_ENTRY_PATTERN.matcher(line);
                if (entryMatcher.find()) {
                    stackTrace.add("".equals(entryMatcher.group(2)) ?
                            new StackTraceEntry(entryMatcher.group(1)) :
                            new StackTraceEntry(entryMatcher.group(1), entryMatcher.group(3)));
                }
            }
        }

        return new SimpleRecord(new ProcessInfo(pid, processName), core, timestamp, recordSpec, stackTrace);
    }
}
