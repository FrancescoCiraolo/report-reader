package com.github.francescociraolo.trace.peepers;

import com.github.francescociraolo.datastructures.SlightlyMutableArray;
import com.github.francescociraolo.trace.ProcessInfo;
import com.github.francescociraolo.trace.Record;
import com.github.francescociraolo.trace.RecordSpecification;
import com.github.francescociraolo.trace.reader.RecordPeeper;

import java.util.HashMap;
import java.util.Map;

public class ExecutionTimePeeper implements RecordPeeper {

    private final SlightlyMutableArray<EnteringPointData> tmp;
    private final Map<ProcessInfo, Double> executionTime;

    public ExecutionTimePeeper() {
        int processorsCount = Runtime.getRuntime().availableProcessors();
        tmp = new SlightlyMutableArray<>(processorsCount, () -> new EnteringPointData(0, null));
        executionTime = new HashMap<>();
    }

    @Override
    public void peep(Record record) {
        RecordSpecification specification = record.getSpecification();
        if (specification.getType() == RecordSpecification.Type.SCHED_SWITCH) {
            RecordSpecification.SchedSwitchSpec spec = (RecordSpecification.SchedSwitchSpec) specification;

            double timestamp = record.getTimestamp().getAsDouble();

            ProcessInfo previousProcess = spec.getPreviousProcess();

            EnteringPointData enteringPointData = tmp.get(record.getCore());
            if (previousProcess.equals(enteringPointData.processInfo)) {
                double lastTimestamp = executionTime.getOrDefault(previousProcess, 0D),
                        deltaTimestamp = timestamp - enteringPointData.enteringTime;
                executionTime.put(previousProcess, deltaTimestamp + lastTimestamp);
            }

            tmp.replace(record.getCore(), new EnteringPointData(timestamp, spec.getNextProcess()));
        }
    }

    public Map<ProcessInfo, Double> getExecutionTime() {
        return Map.copyOf(executionTime);
    }

    private static class EnteringPointData {
        private final double enteringTime;
        private final ProcessInfo processInfo;

        private EnteringPointData(double enteringTime, ProcessInfo processInfo) {
            this.enteringTime = enteringTime;
            this.processInfo = processInfo;
        }
    }
}
