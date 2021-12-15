package com.github.francescociraolo.trace.reader;

import com.github.francescociraolo.trace.Record;

import java.io.IOException;
import java.util.Set;

public class PeepTrace extends Trace {

    private final Trace trace;
    private final Set<RecordPeeper> recordPeepers;

    public PeepTrace(Trace trace, RecordPeeper... peepers) {
        this.trace = trace;
        this.recordPeepers = Set.of(peepers);
    }

    @Override
    public boolean hasNext() {
        return trace.hasNext();
    }

    @Override
    public Record next() throws IOException {
        Record next = trace.next();
        if (next != null)
            for (RecordPeeper recordPeeper : recordPeepers)
                recordPeeper.peep(next);
        return next;
    }

    @Override
    public int getCpusCount() {
        return trace.getCpusCount();
    }
}
