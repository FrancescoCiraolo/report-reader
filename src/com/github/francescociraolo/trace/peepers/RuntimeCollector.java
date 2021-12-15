package com.github.francescociraolo.trace.peepers;

import com.github.francescociraolo.datastructures.SlightlyMutableArray;
import com.github.francescociraolo.trace.ProcessInfo;
import com.github.francescociraolo.trace.Record;
import com.github.francescociraolo.trace.RecordSpecification;
import com.github.francescociraolo.trace.reader.RecordPeeper;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

public class RuntimeCollector implements RecordPeeper {

    private final SlightlyMutableArray<Record> records;
    private final List<RuntimeData> runtimeDataList;

    public RuntimeCollector() {
        int coresCount = Runtime.getRuntime().availableProcessors();
        records = new SlightlyMutableArray<>(coresCount);
        runtimeDataList = new LinkedList<>();
    }

    @Override
    public void peep(Record record) {
        if (record.getType() == RecordSpecification.Type.SCHED_SWITCH) {
            int core = record.getCore();
            Record oldRecord = records.get(core);
            if (oldRecord != null) {
                RecordSpecification.SchedSwitchSpec oldSpec
                        = (RecordSpecification.SchedSwitchSpec) oldRecord.getSpecification(),
                        recordSpec = ((RecordSpecification.SchedSwitchSpec) record.getSpecification());
                if (oldSpec.getNextProcess().equals(recordSpec.getPreviousProcess())) {
                    RuntimeData runtimeData = new RuntimeData(oldSpec.getNextProcess(),
                            recordSpec.getPreviousState(),
                            record.getTimestamp().timeFrom(oldRecord.getTimestamp()),
                            core);
                    runtimeDataList.add(runtimeData);
                } /*else
                    System.err.printf("Something wrong\n\tOld=%s\n\tNew=%s\n", oldRecord, record);*/
            }
            records.replace(core, record);
        }
    }

    public StatisticalResult doSomeStatistics(Character state, ProcessInfo process) {
        Stream<RuntimeData> stream = runtimeDataList.parallelStream();
        if (state != null) stream = stream.filter(runtimeData -> runtimeData.getOutState() == state);
        if (process != null) stream = stream.filter(runtimeData -> runtimeData.getProcess().equals(process));
        double[] values = stream.mapToDouble(RuntimeData::getTime).toArray();
        return new StatisticalResult(values);
    }


    public List<RuntimeData> getRuntimeDataList() {
        return List.copyOf(runtimeDataList);
    }

    public static class StatisticalResult {
        public final double average;
        public final double variance;
        public final double standardDeviation;
        public final double min;
        public final double max;

        public StatisticalResult(double[] values) {
            double avg = 0, var = 0, max = 0, min = Double.MAX_VALUE;
            int size = values.length;

            for (double value : values) {
                avg += value / size;
                if (value > max) max = value;
                if (value < min) min = value;
            }

            for (double value : values)
                var += Math.pow(value - avg, 2) / size;

            average = avg;
            standardDeviation = Math.sqrt(var);
            variance = var;
            this.min = min;
            this.max = max;
        }

        @Override
        public String toString() {
            return String.format("Min: %7.4e\tMax: %7.4e\nAvg: %7.4e\tSD: %7.4e\tVar: %7.4e",
                    min, max, average, standardDeviation, variance);
        }
    }

    public static class RuntimeData {

        private final ProcessInfo process;
        private final char outState;
        private final double time;
        private final int core;

        public RuntimeData(ProcessInfo process, char outState, double time, int core) {
            this.process = process;
            this.outState = outState;
            this.time = time;
            this.core = core;
        }

        public ProcessInfo getProcess() {
            return process;
        }

        public char getOutState() {
            return outState;
        }

        public double getTime() {
            return time;
        }

        public int getCore() {
            return core;
        }

    }

}
