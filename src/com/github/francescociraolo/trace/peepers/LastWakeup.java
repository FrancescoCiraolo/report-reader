package com.github.francescociraolo.trace.peepers;

import com.github.francescociraolo.datastructures.MaxSizeList;
import com.github.francescociraolo.datastructures.SlightlyMutableArray;
import com.github.francescociraolo.trace.RecordSpecification;
import com.github.francescociraolo.trace.RecordSpecification.SchedWakeupFamilySpec;
import com.github.francescociraolo.trace.reader.RecordPeeper;
import com.github.francescociraolo.trace.ProcessInfo;
import com.github.francescociraolo.trace.Record;

import java.util.List;

public class LastWakeup implements RecordPeeper {

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final SlightlyMutableArray<List<Wakeup>> coreData;

    public LastWakeup() {
        var cores = Runtime.getRuntime().availableProcessors();
        coreData = new SlightlyMutableArray<>(cores, () -> new MaxSizeList<>(2));
    }

    @Override
    public void peep(Record record) {
        RecordSpecification.Type type = record.getSpecification().getType();
        if (type == RecordSpecification.Type.SCHED_WAKEUP || type == RecordSpecification.Type.SCHED_WAKEUP_NEW) {
            SchedWakeupFamilySpec specification
                    = (SchedWakeupFamilySpec) record.getSpecification();
            coreData.get(record.getCore()).add(new Wakeup(specification.getWakingProcess(), record.getTimestamp()));
        }
    }

    public List<Wakeup> get(int source) {
        return coreData.get(source);
    }

    public static class Wakeup {
        private final ProcessInfo process;
        private final Record.Timestamp timestamp;

        public Wakeup(ProcessInfo process, Record.Timestamp timestamp) {
            this.process = process;
            this.timestamp = timestamp;
        }

        public ProcessInfo getProcess() {
            return process;
        }

        public Record.Timestamp getTimestamp() {
            return timestamp;
        }
    }
}
