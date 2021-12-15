package com.github.francescociraolo.trace.peepers;

import com.github.francescociraolo.datastructures.SlightlyMutableArray;
import com.github.francescociraolo.trace.ProcessInfo;
import com.github.francescociraolo.trace.Record;
import com.github.francescociraolo.trace.RecordSpecification;
import com.github.francescociraolo.trace.reader.RecordPeeper;

import java.util.*;
import java.util.stream.Collectors;

public class SliceCollector implements RecordPeeper {

    private final SlightlyMutableArray<TmpData> tmp;
    private final SlightlyMutableArray<Map<ProcessInfo, Double>> results;
    private final SlightlyMutableArray<Map<ProcessInfo, Record.Timestamp>> firstSwitch;
    private Record.Timestamp lastResetTimestamp;

    public SliceCollector() {
        int coresCount = Runtime.getRuntime().availableProcessors();
        tmp = new SlightlyMutableArray<>(coresCount, () -> new TmpData(null, new Record.Timestamp("0")));
        results = new SlightlyMutableArray<>(coresCount, HashMap::new);
        firstSwitch = new SlightlyMutableArray<>(coresCount, HashMap::new);
    }

    @Override
    public void peep(Record record) {
        RecordSpecification specification = record.getSpecification();
        if (lastResetTimestamp == null)
            lastResetTimestamp = record.getTimestamp();
        if (specification.getType() == RecordSpecification.Type.SCHED_SWITCH) {
            int core = record.getCore();
            Record.Timestamp timestamp = record.getTimestamp();
            RecordSpecification.SchedSwitchSpec spec = (RecordSpecification.SchedSwitchSpec) specification;
            ProcessInfo previous = spec.getPreviousProcess(), next = spec.getNextProcess();
            double delta = tmp.get(core).update(timestamp, previous, next);
            Map<ProcessInfo, Double> result = results.get(core);
            double value = result.getOrDefault(previous, 0D) + delta;
            result.put(previous, value);
            Map<ProcessInfo, Record.Timestamp> first = firstSwitch.get(core);
            if (!first.containsKey(next))
                first.put(next, timestamp);
        }
    }

    public static class ProcessData {
        public final ProcessInfo process;
        public final Record.Timestamp timestamp;
        public final double ratio;

        public ProcessData(ProcessInfo process, Record.Timestamp timestamp, double ratio) {
            this.process = process;
            this.timestamp = timestamp;
            this.ratio = ratio;
        }

        public double getRatio() {
            return ratio;
        }
    }

    @FunctionalInterface
    public interface RecordPrinter {

        void printCoreData(int core, List<ProcessData> data);
    }

    public double getTimeAndReset(Record.Timestamp timestamp, int core, ProcessInfo processInfo) {
        Double res = results.get(core).get(processInfo);

        int i = 0;
        for (; i < results.size(); i++) {
            tmp.get(i).reset(timestamp);
            results.get(i).clear();
            firstSwitch.get(i).clear();
        }

        for (; i < tmp.size(); i++) {
            tmp.get(i).reset(timestamp);
            firstSwitch.get(i).clear();
        }

        lastResetTimestamp = timestamp;

        return res;
    }

    public ProcessData extractAndReset(Record.Timestamp timestamp, int core, ProcessInfo processInfo) {
        Map<ProcessInfo, Double> map = results.get(core);
        double delta = timestamp.timeFrom(lastResetTimestamp);
        ProcessData data = new ProcessData(processInfo,
                firstSwitch.get(core).get(processInfo),
                map.get(processInfo) / delta);

        int i = 0;
        for (; i < results.size(); i++) {
            tmp.get(i).reset(timestamp);
            results.get(i).clear();
            firstSwitch.get(i).clear();
        }

        for (; i < tmp.size(); i++) {
            tmp.get(i).reset(timestamp);
            firstSwitch.get(i).clear();
        }

        lastResetTimestamp = timestamp;

        return data;
    }



    public HashMap<Integer, List<ProcessData>> collectAndReset(Record.Timestamp timestamp, int... cores) {
        HashMap<Integer, List<ProcessData>> resultsMap = new HashMap<>();

        double delta = timestamp.timeFrom(lastResetTimestamp);

        for (int core : cores) {

            Map<ProcessInfo, Double> map = results.get(core);
            ArrayList<ProcessData> data = new ArrayList<>();

            for (ProcessInfo processInfo : map.keySet())
                data.add(new ProcessData(processInfo, firstSwitch.get(core).get(processInfo), map.get(processInfo) / delta));

            resultsMap.put(core, data);
        }

        int i = 0;
        for (; i < results.size(); i++) {
            tmp.get(i).reset(timestamp);
            results.get(i).clear();
            firstSwitch.get(i).clear();
        }

        for (; i < tmp.size(); i++) {
            tmp.get(i).reset(timestamp);
            firstSwitch.get(i).clear();
        }

        lastResetTimestamp = timestamp;

        return resultsMap;
    }


    public void printAndReset(Record.Timestamp timestamp, RecordPrinter printer, long size, int... toPrintCores) {
        double delta = timestamp.timeFrom(lastResetTimestamp);
        for (int core : toPrintCores) {
            Map<ProcessInfo, Double> map = results.get(core);
            List<ProcessData> data = map.keySet()
                    .stream()
                    .sorted(Comparator.comparingDouble(map::get).reversed())
                    .limit(size)
                    .map(processInfo -> new ProcessData(processInfo,
                            firstSwitch.get(core).get(processInfo),
                            map.get(processInfo) / delta))
                    .collect(Collectors.toList());
            printer.printCoreData(core, data);
        }

        int i = 0;
        for (; i < results.size(); i++) {
            tmp.get(i).reset(timestamp);
            results.get(i).clear();
            firstSwitch.get(i).clear();
        }

        for (; i < tmp.size(); i++) {
            tmp.get(i).reset(timestamp);
            firstSwitch.get(i).clear();
        }

        lastResetTimestamp = timestamp;
    }

    private static class TmpData {
        private ProcessInfo process;
        private Record.Timestamp timestamp;

        private TmpData(ProcessInfo process, Record.Timestamp timestamp) {
            this.process = process;
            this.timestamp = timestamp;
        }

        private double update(Record.Timestamp timestamp, ProcessInfo previous, ProcessInfo next) {
            double delta = 0;
            if (previous.equals(process))
                delta = timestamp.timeFrom(this.timestamp);
            process = next;
            this.timestamp = timestamp;
            return delta;
        }

        private void reset(Record.Timestamp timestamp) {
            this.timestamp = timestamp;
        }
    }
}
