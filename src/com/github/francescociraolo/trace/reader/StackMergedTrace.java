package com.github.francescociraolo.trace.reader;

import com.github.francescociraolo.trace.RecordSpecification;
import com.github.francescociraolo.trace.Record;
import com.github.francescociraolo.trace.SimpleRecord;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

public class StackMergedTrace extends Trace {

    private final Trace trace;
    private final LinkedList<Record> recordQueue;
    private final int queueMaxSize;
    private final Set<RecordSpecification.TypeCategory> categories;
    private final Set<RecordSpecification.Type> types;

    public StackMergedTrace(Trace trace,
                            int queueMaxSize,
                            RecordSpecification.TypeCategory... categories) {
        this(trace, queueMaxSize, Set.of(categories), Set.of());
    }

    public StackMergedTrace(Trace trace,
                            int queueMaxSize,
                            RecordSpecification.Type... types) {
        this(trace, queueMaxSize, Set.of(), Set.of(types));
    }

    public StackMergedTrace(Trace trace,
                            int queueMaxSize,
                            Set<RecordSpecification.TypeCategory> categories,
                            Set<RecordSpecification.Type> types) {
        this.trace = trace;
        this.queueMaxSize = queueMaxSize;
        this.recordQueue = new LinkedList<>();
        this.categories = categories;
        this.types = types;
    }

    @Override
    public boolean hasNext() {
        return trace.hasNext() || !recordQueue.isEmpty();
    }

    @Override
    public int getCpusCount() {
        return trace.getCpusCount();
    }

    private boolean areCorresponding(Record record, Record stackTrace) {
        return stackTrace.getSpecification().getType().isStackTrace()
                && record.getProcess().equals(stackTrace.getProcess())
                && record.getCore() == stackTrace.getCore();
    }

    @Override
    public Record next() throws IOException {
        Record record;

        if (!recordQueue.isEmpty())
            record = recordQueue.poll();
        else
            record = trace.next();

        if (types.contains(record.getSpecification().getType()) || categories.contains(record.getSpecification().getType().getCategory())) {
            Record stackTrace = null;
            Iterator<Record> iterator = recordQueue.iterator();

            while (stackTrace == null && iterator.hasNext()) {
                stackTrace = iterator.next();

                if (areCorresponding(record, stackTrace))
                    iterator.remove();
                else
                    stackTrace = null;
            }

            while (stackTrace == null && queueMaxSize > recordQueue.size() && trace.hasNext()) {
                stackTrace = trace.next();

                if (!areCorresponding(record, stackTrace)) {
                    recordQueue.add(stackTrace);
                    stackTrace = null;
                }
            }

            if (stackTrace != null)
                record = new SimpleRecord(
                        record.getProcess(),
                        record.getCore(),
                        record.getTimestamp(),
                        record.getSpecification(),
                        stackTrace.getStackTrace()
                );
        }

        return record;
    }
}
