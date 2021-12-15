package com.github.francescociraolo.trace.peepers;

import com.github.francescociraolo.trace.Record;
import com.github.francescociraolo.trace.reader.PeepTrace;
import com.github.francescociraolo.trace.reader.RecordPeeper;
import com.github.francescociraolo.trace.reader.Trace;

public class RecordsCounter implements RecordPeeper {

    private long count = 0;

    public long getCount() {
        return count;
    }

    @Override
    public void peep(Record record) {
        count++;
    }
}
