package com.github.francescociraolo.trace.reader;

import com.github.francescociraolo.trace.ProcessInfo;
import com.github.francescociraolo.trace.Record;
import com.github.francescociraolo.trace.RecordSpecification;

import java.util.HashMap;
import java.util.Map;

public class FirstLastPeeper implements RecordPeeper {

    private final Map<ProcessInfo, Record.Timestamp> first;
    private final Map<ProcessInfo, Record.Timestamp> last;

    public FirstLastPeeper() {
        first = new HashMap();
        last = new HashMap();
    }

    @Override
    public void peep(Record record) {
        RecordSpecification specification = record.getSpecification();
        if (specification.getType() == RecordSpecification.Type.SCHED_SWITCH) {
            RecordSpecification.SchedSwitchSpec spec = (RecordSpecification.SchedSwitchSpec) specification;
            last.put(spec.getPreviousProcess(), record.getTimestamp());
            if (!first.containsKey(spec.getNextProcess()))
                first.put(spec.getNextProcess(), record.getTimestamp());
        }
    }

    public Record.Timestamp getFirst(ProcessInfo processInfo) {
        return first.get(processInfo);
    }

    public Record.Timestamp getLast(ProcessInfo processInfo) {
        return last.get(processInfo);
    }
}
