package com.github.francescociraolo.trace.reader;

import com.github.francescociraolo.trace.Record;

import java.io.IOException;
import java.util.function.Predicate;

public class FilteredTrace extends Trace {

    private final Trace trace;
    private Predicate<Record> filter;
    private Record temp;

    public FilteredTrace(Trace trace, Predicate<Record> filter) throws IOException {
        this.trace = trace;
        this.filter = filter;

        nextTemp();
    }

    private void nextTemp() throws IOException {
        temp = trace.hasNext() ? trace.next() : null;
        while (temp == null && trace.hasNext())
            if (!filter.test(temp))
                temp = null;
    }

    @Override
    public boolean hasNext() {
        return trace.hasNext();
    }

    @Override
    public int getCpusCount() {
        return trace.getCpusCount();
    }

    @Override
    public Record next() throws IOException {
        Record record = null;
        while (record == null && trace.hasNext()) {
            record = trace.next();
            if (record != null && !filter.test(record))
                record = null;
        }
        return record;
    }
}
