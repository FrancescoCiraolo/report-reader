package com.github.francescociraolo.trace.peepers;

import com.github.francescociraolo.datastructures.SlightlyMutableArray;
import com.github.francescociraolo.trace.Record;
import com.github.francescociraolo.trace.RecordSpecification;
import com.github.francescociraolo.trace.reader.RecordPeeper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CPUsLoad implements RecordPeeper {

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final SlightlyMutableArray<CoreData> coreData;
    private final boolean simplified;

    public CPUsLoad() {
        this(true);
    }

    public CPUsLoad(boolean simplified) {
        var cores = Runtime.getRuntime().availableProcessors();
        this.coreData = new SlightlyMutableArray<>(cores, CoreData::new);
        this.simplified = simplified;
    }

    public void clear() {
        coreData.forEach(CoreData::clear);
    }

    public List<Map<CommonStatus, Double>> getLoadData(double start, double end) {
        return coreData
                .stream()
                .map(c -> c.computeLoad(start, end))
                .collect(Collectors.toList());
    }

    @Override
    public void peep(Record record) {
        RecordSpecification specification;
        specification = record.getSpecification();
        if (specification.getType() == RecordSpecification.Type.SCHED_SWITCH) {
            int core = record.getCore();
            double time = record.getTimestamp().getAsDouble();
            CommonStatus element;
            RecordSpecification.SchedSwitchSpec schedSwitchSpec
                    = (RecordSpecification.SchedSwitchSpec) record.getSpecification();

            String nextProcessName = schedSwitchSpec.getNextProcess().getName();
            if (nextProcessName.matches("swapper/\\d*"))
                element = CommonStatus.IDLE;
            else {
                if (simplified)
                    element = CommonStatus.WORKING;
                else if (nextProcessName.matches("kworker/(.+:)*\\d+"))
                    element = CommonStatus.KERNEL;
                else
                    element = CommonStatus.USER;
            }
            coreData.get(core).update(element, time);
        }
    }

    private static class TimedEntry {
        public final CPUsLoad.CommonStatus entry;
        public final double time;

        TimedEntry(CommonStatus entry, double time) {
            this.entry = entry;
            this.time = time;
        }
    }

    private static class CoreData {
        private final List<TimedEntry> entries;

        private CoreData() {
            this.entries = new ArrayList<>();
        }

        private void update(CommonStatus element, double time) {
            entries.add(new TimedEntry(element, time));
        }

        public void clear() {
            entries.clear();
        }

        Map<CommonStatus, Double> computeLoad(double start, double end) {
            HashMap<CommonStatus, Double> times = new HashMap<>();
            TimedEntry entry;
            double total = 0, nextTime = !entries.isEmpty() ? entries.get(0).time : end, delta;

            if (start < nextTime) {
                total = nextTime - start;
                times.put(CommonStatus.UNKNOWN, total);
            }

            for (int i = 0; i < entries.size() && entries.get(i).time <= end; i++) {
                entry = entries.get(i);
                nextTime = i + 1 < entries.size() ? entries.get(i + 1).time : end;
                if (nextTime >= start) {
                    delta = Math.min(nextTime, end) - Math.max(entry.time, start);
                    total += delta;
                    times.put(entry.entry, delta + times.getOrDefault(entry.entry, 0d));
                }
            }

            for (CommonStatus key : times.keySet())
                times.put(key, times.get(key) / total);

            return times;
        }
    }

    public enum CommonStatus {
        IDLE,
        WORKING,
        KERNEL,
        USER,
        UNKNOWN
    }
}
