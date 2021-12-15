package com.github.francescociraolo.trace.peepers;

import com.github.francescociraolo.datastructures.SlightlyMutableArray;
import com.github.francescociraolo.trace.Record;
import com.github.francescociraolo.trace.RecordSpecification;
import com.github.francescociraolo.trace.reader.RecordPeeper;
import com.github.francescociraolo.trace.ProcessInfo;

import java.util.HashMap;
import java.util.Map;

public class ProcessesPinInvestigator implements RecordPeeper {
//TODO CLEANUP THIS MESS
    private final Map<ProcessInfo, SlightlyMutableArray<Integer>> data;

    public ProcessesPinInvestigator() {
        data = new HashMap<>();
    }

    @Override
    public void peep(Record record) {
        ProcessInfo process = null;
        int dest = -1;

        switch (record.getSpecification().getType()) {
            case SCHED_WAKEUP:
                RecordSpecification.SchedWakeupFamilySpec schedWakeupNewSpec
                        = (RecordSpecification.SchedWakeupFamilySpec) record.getSpecification();
                process = schedWakeupNewSpec.getWakingProcess();
                dest = ((RecordSpecification.SchedWakeupFamilySpec) record.getSpecification()).getDestinationCpu();
                break;
            case SCHED_MIGRATE_TASK:
                break;
        }

        if (process != null) {
            if (!data.containsKey(process))
                data.put(process, new SlightlyMutableArray<>(dest + 1, () -> 0));
            SlightlyMutableArray<Integer> array = data.get(process);
            array.replace(dest, array.get(dest) + 1);
        }
    }

    public Map<ProcessInfo, SlightlyMutableArray<Integer>> getData() {
        return data;
    }
}
