package com.github.francescociraolo.trace.peepers;

import com.github.francescociraolo.datastructures.SlightlyMutableArray;
import com.github.francescociraolo.trace.Record;
import com.github.francescociraolo.trace.RecordSpecification;
import com.github.francescociraolo.trace.reader.RecordPeeper;

import com.github.francescociraolo.trace.ProcessInfo;

import java.util.List;

public class ProcessesStatus implements RecordPeeper {
    private SlightlyMutableArray<ProcessInfo> processes;


    public ProcessesStatus() {
        var cores = Runtime.getRuntime().availableProcessors();
        this.processes = new SlightlyMutableArray<>(cores);
    }

    @Override
    public void peep(Record record) {
        var specification = record.getSpecification();
        if (specification.getType() == RecordSpecification.Type.SCHED_SWITCH) {
            var spec = ((RecordSpecification.SchedSwitchSpec) specification);
            var core = record.getCore();
            processes.replace(core, spec.getNextProcess());
        }
    }

    public List<ProcessInfo> getProcesses() {
        return processes;
    }
}
