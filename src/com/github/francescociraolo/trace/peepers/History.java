package com.github.francescociraolo.trace.peepers;

import com.github.francescociraolo.datastructures.GroupedCollection;
import com.github.francescociraolo.datastructures.MaxSizeList;
import com.github.francescociraolo.trace.Record;
import com.github.francescociraolo.trace.reader.RecordPeeper;

public class History implements RecordPeeper {

    private final GroupedCollection<Record, Integer, MaxSizeList<Record>> records;

    public History() {
        records = new GroupedCollection<>(() -> new MaxSizeList<>(1000), Record::getCore);
    }

    @Override
    public void peep(Record record) {
        records.add(record);
    }
}
