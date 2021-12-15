package com.github.francescociraolo.traceanalysis;

import com.github.francescociraolo.datastructures.SlightlyMutableArray;
import com.github.francescociraolo.trace.ProcessInfo;
import com.github.francescociraolo.trace.Record;
import com.github.francescociraolo.trace.RecordSpecification;
import com.github.francescociraolo.trace.peepers.*;
import com.github.francescociraolo.trace.reader.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class MigrationsStatistics {

    private static final long CPU_LOAD_INTERVAL = 1;

    private final LastWakeup lastWakeup;
    private final CPUsLoad cpusLoad;
    private final RuntimeCollector runtimeCollector;
    private final ProcessesStatus processesStatus;
    private final ProcessesPinInvestigator processesPinInvestigator;
    private final MigrationTimePeeper migrationTimePeeper;
    private final Slices slices;
    private final List<MigrationData> migrationData;

    private final Trace trace;
    private final ProcessInfo process;

    private int lastCore = -1;
    private boolean idle = false;
    private boolean nextWakeup = false;

    private final List<Record> migrationsTimestamp;

    public MigrationsStatistics(Path datFilePath, int pid, String processName) throws IOException {
        this.process = new ProcessInfo(pid, processName);
        lastWakeup = new LastWakeup();
        cpusLoad = new CPUsLoad();
        runtimeCollector = new RuntimeCollector();
        processesStatus = new ProcessesStatus();
        processesPinInvestigator = new ProcessesPinInvestigator();

        migrationsTimestamp = new LinkedList<>();
        slices = new Slices();
        migrationData = new LinkedList<>();
        migrationTimePeeper = new MigrationTimePeeper(process);

        {
            Trace trace = Trace.fromDatFile(datFilePath);
//            trace = new HistoryTrace(trace, history);
            trace = new PeepTrace(trace,
                    cpusLoad,
                    processesStatus,
                    processesPinInvestigator,
                    lastWakeup,
                    runtimeCollector,
                    migrationTimePeeper,
                    this.slices,
                    this::nextWakeup,
                    this::idlePercentage,
                    this::extractMigrationData);
            trace = new FilteredTrace(trace, this::filter);
            this.trace = trace;
        }

    }

    private void extractMigrationData(Record record) {
        if (record.getSpecification().getType() == RecordSpecification.Type.SCHED_MIGRATE_TASK) {
            RecordSpecification.SchedMigrateTaskSpec spec
                    = (RecordSpecification.SchedMigrateTaskSpec) record.getSpecification();

            if (spec.getProcess().equals(process)) {

            }
        }
    }

    private boolean filterSchedMigrateTask(Record record) {
        RecordSpecification.SchedMigrateTaskSpec specification
                = (RecordSpecification.SchedMigrateTaskSpec) record.getSpecification();

        return process.equals(new ProcessInfo(specification.getPid(), specification.getProcessName()));
    }

    private boolean filterSchedSwitch(Record record) {
        RecordSpecification.SchedSwitchSpec specification
                = (RecordSpecification.SchedSwitchSpec) record.getSpecification();

        return record.getCore() == lastCore;
    }

    private boolean filter(Record record) {
        switch (record.getSpecification().getType()) {
            case SCHED_MIGRATE_TASK:
                return filterSchedMigrateTask(record);
            case SCHED_SWITCH:
                return filterSchedSwitch(record);
            case SCHED_WAKEUP:
            case SCHED_WAKEUP_NEW:

            default:
                return false;
        }
    }

    public void printReport() throws IOException {
        while (trace.hasNext()) {
            Record record = trace.next();
            if (record != null) {
                if (record.getSpecification().getType() == RecordSpecification.Type.SCHED_MIGRATE_TASK) {
                    RecordSpecification.SchedMigrateTaskSpec specification
                            = (RecordSpecification.SchedMigrateTaskSpec) record.getSpecification();
                    System.out.println();

                    int source = specification.getSourceCpu(), destination = specification.getDestinationCpu();
                    for (LastWakeup.Wakeup woken : lastWakeup.get(source))
                        System.out.printf("%s [%02d] wakeup %s\n",
                                woken.getTimestamp(),
                                source,
                                woken.getProcess().toTwoPointsString());
                    System.out.printf("%s [%02d] to [%02d] migration %s\n",
                            record.getTimestamp(),
                            source,
                            destination,
                            specification.getProcess().toTwoPointsString());
                    System.out.printf("\tcurrent [%02d] process %s\n",
                            source,
                            processesStatus.getProcesses().get(source).toTwoPointsString());


//                    System.out.println(record);
//                    float time = Float.parseFloat(record.getTimestamp());
//                    List<Map<CPUsLoad.CommonStatus, Float>> loadData = cpusLoad.getLoadData(time - 5, time);
//                    float srcIdle = loadData.get(source).getOrDefault(CPUsLoad.CommonStatus.IDLE, 0F),
//                            dstIdle = loadData.get(destination).getOrDefault(CPUsLoad.CommonStatus.IDLE, 0F);
//                    System.out.printf("Source CPU [%d] idle %6.2f%% time\nDestination CPU [%d] idle %6.2f%% time\n",
//                            source, srcIdle * 100, destination, dstIdle * 100);
//                    List<LastWakeup.Wakeup> wakeup = lastWakeup.get(source);
//                    System.out.printf("Last woken process on source core [%d]: %s at %s\n",
//                            source,
//                            wakeup.get(1).getProcess().toTwoPointsString(),
//                            wakeup.get(1).getTimestamp());
//                    System.out.printf("before woken process on source core [%d]: %s at %s\n",
//                            source,
//                            wakeup.get(0).getProcess().toTwoPointsString(),
//                            wakeup.get(0).getTimestamp());
                    migrationsTimestamp.add(record);
                    lastCore = source;
                    idle = true;
                    nextWakeup = true;
                }

                if (record.getSpecification().getType() == RecordSpecification.Type.SCHED_SWITCH) {
                    RecordSpecification.SchedSwitchSpec specification
                            = (RecordSpecification.SchedSwitchSpec) record.getSpecification();
                    ProcessInfo nextProcess = specification.getNextProcess(),
                            previousProcess = specification.getPreviousProcess();
                    String nextProcessName = nextProcess.getName();
                    if (idle || (!nextProcessName.startsWith("swapper"))
                            && !nextProcessName.startsWith("kworker")
                            && !nextProcessName.startsWith("migration")) {
                        System.out.printf("%s [%02d] switch %s to %s\n",
                                record.getTimestamp(),
                                lastCore,
                                previousProcess.toTwoPointsString(),
                                nextProcess.toTwoPointsString());
//                        System.out.printf("Next source [%d] switch: %s at %s\n",
//                                lastCore,
//                                nextProcess.toTwoPointsString(),
//                                record.getTimestamp());
                        if (idle) idle = false;
                        else lastCore = -1;
                    }
                }
            }
        }

        System.out.println(slices.avg());

        System.out.println("Unfiltered");
        RuntimeCollector.StatisticalResult result = runtimeCollector.doSomeStatistics(null, null);
        System.out.println(result);
//        System.out.printf("Avg: %7.4f\nSD: %7.4f\nVar: %7.4f\n", result.average, result.standardDeviation, result.variance);

        System.out.println("Running");
        result = runtimeCollector.doSomeStatistics('R', null);
        System.out.println(result);
//        System.out.printf("Avg: %7.4f\nSD: %7.4f\nVar: %7.4f\n", result.average, result.standardDeviation, result.variance);

        System.out.println("While1");
        result = runtimeCollector.doSomeStatistics(null, process);
        System.out.println(result);
//        System.out.printf("Avg: %7.4f\nSD: %7.4f\nVar: %7.4f\n", result.average, result.standardDeviation, result.variance);


        System.exit(0);
    }

    public void nextWakeup(Record record) {
        if (nextWakeup && record.getCore() == lastCore) {
            ProcessInfo process;

            switch (record.getSpecification().getType()) {
                case SCHED_WAKEUP:
                case SCHED_WAKEUP_NEW:
                    process = ((RecordSpecification.SchedWakeupFamilySpec) record.getSpecification())
                            .getWakingProcess();
                    break;
                default:
                    return;
            }

            System.out.printf("%s [%02d] wakeup %s\n", record.getTimestamp(), lastCore, process.toTwoPointsString());
//            System.out.printf("First woke up process after migration %s at %s\n", process.toTwoPointsString(), record.getTimestamp());

//            System.out.println(process.toTwoPointsString());
            nextWakeup = false;
        }
    }

    public void idlePercentage(Record record) {
        if (!migrationsTimestamp.isEmpty()) {
            double limit = record.getTimestamp().getAsDouble() - CPU_LOAD_INTERVAL;
            Iterator<Record> iterator = migrationsTimestamp.iterator();
            boolean flag = true;
            while (iterator.hasNext() && flag) {
                Record next = iterator.next();
                double time = next.getTimestamp().getAsDouble();
                flag = time < limit;
                if (flag) {
                    iterator.remove();
                    RecordSpecification.SchedMigrateTaskSpec spec
                            = (RecordSpecification.SchedMigrateTaskSpec) next.getSpecification();
                    System.out.printf("# Idle percentage of [%02d] after %d of migration at %s: %5.3f%%\n",
                            spec.getSourceCpu(),
                            CPU_LOAD_INTERVAL,
                            record.getTimestamp(),
                            100 * cpusLoad.getLoadData(time, time + CPU_LOAD_INTERVAL).get(spec.getSourceCpu()).get(CPUsLoad.CommonStatus.IDLE));
                }
            }
        }
    }

    private static class Slices implements RecordPeeper {
        private final List<Double> slices;
        private final SlightlyMutableArray<Record> records;

        public Slices() {
            slices = new LinkedList<>();
            int coresCount = Runtime.getRuntime().availableProcessors();
            records = new SlightlyMutableArray<>(coresCount);
        }

        public float avg() {
            float avg = 0;
            int size = slices.size();
            System.out.println(size);
            for (double value : slices)
                avg += value / size;
            return avg;
        }


        @Override
        public void peep(Record record) {
            if (record.getSpecification().getType() == RecordSpecification.Type.SCHED_SWITCH) {
                Record oldRecord = records.get(record.getCore());
                if (oldRecord != null) {
                    RecordSpecification.SchedSwitchSpec spec
                            = (RecordSpecification.SchedSwitchSpec) record.getSpecification(),
                            oldSpec = (RecordSpecification.SchedSwitchSpec) oldRecord.getSpecification();
                    if (spec.getPreviousProcess().equals(oldSpec.getNextProcess()) && spec.getPreviousState() == 'R')
                        slices.add(record.getTimestamp().timeFrom(oldRecord.getTimestamp()));
                }
                records.replace(record.getCore(), record);
            }
        }
    }

    private class MigrationData {
        final LastWakeup.Wakeup[] previousWakeUp;
        final Record migration;
        final ProcessInfo currentProcess;
        final Record[] nextSwitch;
        Record nextWakeup;

        int nextSwitchState = 0;


        private MigrationData(Record migration) {
            this.previousWakeUp = lastWakeup.get(migration.getCore()).toArray(LastWakeup.Wakeup[]::new);
            this.migration = migration;
            currentProcess = processesStatus.getProcesses().get(migration.getCore());
            nextSwitch = new Record[3];
        }

        boolean update(Record record) {
            if (record.getCore() == migration.getCore()) {
                if (nextWakeup == null
                        && (record.getSpecification().getType() == RecordSpecification.Type.SCHED_WAKEUP_NEW
                        || record.getSpecification().getType() == RecordSpecification.Type.SCHED_WAKEUP)) {
                    nextWakeup = record;
                } else if (nextSwitchState < 2
                        && record.getSpecification().getType() == RecordSpecification.Type.SCHED_SWITCH) {
                    nextSwitch[nextSwitchState++] = record;
                }
            }

            return nextWakeup != null && nextSwitchState == 2;
        }
    }

    static void printLaTeXTable(String[][] table, int headersRows) {
        int largestRowWidth = 0;
        for (String[] row : table)
            largestRowWidth = Math.max(row.length, largestRowWidth);

        String columnsDef = "| " + "c | ".repeat(largestRowWidth - 1) + "c |";

//        System.out.println("\\begin{center}");
        System.out.printf("\\begin{tabular}{%s}\n", columnsDef);
        System.out.println("\\hline");

        int row = 0;
        for (; row < table.length; row++) {
            System.out.printf("%s \\\\\n", String.join(" & ", table[row]));

            if (row < headersRows)
                System.out.println("\\hline");
        }

        System.out.println("\\hline");
        System.out.println("\\end{tabular}");
//        System.out.println("\\end{center}");
    }

    static String multicolumnCell(String text, int columns) {
        return String.format("\\multicolumn{%d}{|c|}{%s}", columns, text);
    }

    static String[][] extractTableData(HashMap<Integer, List<SliceCollector.ProcessData>> processData,
                                       int[] cores,
                                       int entryLimit,
                                       Record.Timestamp lastTimestamp) {

        int entrySize = 0;
        for (List<SliceCollector.ProcessData> entries : processData.values())
            entrySize = Math.max(entrySize, entries.size());
        entrySize = Math.min(entryLimit, entrySize);

        String[][] table = new String[entrySize + 2][cores.length * 3];
        table[0] = new String[cores.length];
        for (int c = 0, col = 0; c < cores.length; c++, col += 3) {
            table[0][c] = multicolumnCell(String.format("CPU%d", cores[c]), 3);
            table[1][col] = "Process";
            table[1][col + 1] = "Switch delta";
            table[1][col + 2] = "Time";
            List<SliceCollector.ProcessData> data = processData.get(cores[c]);
            data.sort(Comparator.comparingDouble(SliceCollector.ProcessData::getRatio).reversed());
            int i = 0, r = 2;
            for (int eSize = data.size(); i < entrySize && i < eSize; i++, r++) {
                SliceCollector.ProcessData cell = data.get(i);
                table[r][col] = cell.process.toTwoPointsString();
                double val = cell.timestamp == null ? 0 : cell.timestamp.timeFrom(lastTimestamp);
                table[r][col + 1] = String.format("%.3f", val);
//                table[r][col + 1] = String.valueOf(cell.timestamp == null ? 0 : cell.timestamp.timeFrom(lastTimestamp));
                table[r][col + 2] = String.format("%.4f\\%%", cell.ratio * 100);
            }
            for (; i < entrySize; i++, r++)
                table[r][col] = table[r][col + 1] = table[r][col + 2] = "";
        }

        return table;
    }

    static void about(Path tracePath, Path pidFile, String processName) throws IOException {
        about(tracePath, Integer.parseInt(Files.readString(pidFile).strip()), processName);
    }
    static void about(Path tracePath, int pid, String processName) throws IOException {
        Trace trace = Trace.fromDatFile(tracePath);
        SliceCollector sliceCollector = new SliceCollector();
        trace = new PeepTrace(trace, sliceCollector);
        trace = new FilteredTrace(trace, record -> {
            RecordSpecification spec = record.getSpecification();
            boolean res = false;

            switch (spec.getType()) {
                case SCHED_SWITCH:
                    RecordSpecification.SchedSwitchSpec switchSpec = (RecordSpecification.SchedSwitchSpec) spec;
                    res = switchSpec.getPreviousProcess().getPid() == pid || switchSpec.getNextProcess().getPid() == pid;
                    break;
                case SCHED_MIGRATE_TASK:
                    RecordSpecification.SchedMigrateTaskSpec schedMigrateTaskSpec = (RecordSpecification.SchedMigrateTaskSpec) spec;
                    res = schedMigrateTaskSpec.getPid() == pid;
                    break;
            }

            return res;
        });

        HashMap<Integer, Double> executionTime1 = new HashMap<>();

        RecordsCounter mixedCounter = new RecordsCounter(),
                migrationCounter = new RecordsCounter();
        ExecutionTimePeeper executionTimePeeper = new ExecutionTimePeeper();
        FirstLastPeeper firstLastPeeper = new FirstLastPeeper();
        trace = new PeepTrace(trace, mixedCounter, executionTimePeeper, firstLastPeeper);
        trace = new FilteredTrace(trace,
                record -> record != null &&
                        record.getSpecification().getType() == RecordSpecification.Type.SCHED_MIGRATE_TASK);
        trace = new PeepTrace(trace, migrationCounter);
        Record.Timestamp lastTimestamp = null;
        int lastLeavingCore = -1;
        while (trace.hasNext()) {
            Record record = trace.next();
            if (lastTimestamp == null && record != null) {
                lastTimestamp = record.getTimestamp();
            }
            if (record != null) {
                RecordSpecification.SchedMigrateTaskSpec spec
                        = (RecordSpecification.SchedMigrateTaskSpec) record.getSpecification();
                boolean threeColumn = lastLeavingCore != -1
                        && lastLeavingCore != spec.getSourceCpu()
                        && lastLeavingCore != spec.getDestinationCpu();
                int[] cores = new int[threeColumn ? 3 : 2];
                int i = 0;
                if (threeColumn) cores[i++] = lastLeavingCore;
                cores[i++] = spec.getSourceCpu();
                cores[i] = spec.getDestinationCpu();

                HashMap<Integer, List<SliceCollector.ProcessData>> processData = sliceCollector.collectAndReset(record.getTimestamp(), cores);
                double delta = record.getTimestamp().timeFrom(lastTimestamp);
                String[][] table = extractTableData(processData, cores, 10, lastTimestamp);
                lastTimestamp = record.getTimestamp();
                lastLeavingCore = spec.getSourceCpu();

                double time = 0, ratio = 0;
                List<SliceCollector.ProcessData> processData1 = processData.get(spec.getSourceCpu());
                for (var d : processData1) {
                    if (d.process.getName().equals("while1")) {
                        ratio = d.ratio;
                        time += d.ratio * delta;
                    }
                }

//                spec.getSourceCpu();
//                System.out.printf("Timestamp:%s\nDelta time: %f\nRatio:%f\nTime:%f\n\n", lastTimestamp, delta, ratio, ratio*delta);
                executionTime1.put(spec.getSourceCpu(), executionTime1.getOrDefault(spec.getSourceCpu(), 0D) + time);

//                int maxSize = 0;
//                System.out.print("| ");
//                for (int core : cores) {
//                    maxSize = Math.max(maxSize, processData.get(core).size());
//                    System.out.printf("%-57s | ", String.format("Core %03d", core));
//                }
//                maxSize = Math.min(10, maxSize);
//                System.out.println();
//                System.out.print("| ");
//                for (int ignored : cores) {
//                    System.out.printf("%-29s | %-12s | %-10s | ", "Process", "First in", "Time usage");
//                }
//                System.out.println();
//
//                SliceCollector.ProcessData[][] data = new SliceCollector.ProcessData[cores.length][maxSize];
//                for (int c = 0; c < cores.length; c++) {
//                    List<SliceCollector.ProcessData> list = processData.get(cores[c]);
//                    list.sort(Comparator.comparingDouble(SliceCollector.ProcessData::getRatio).reversed());
//                    int r = 0;
//                    for (; r < maxSize && r < list.size(); r++) data[c][r] = list.get(r);
//                    for (; r < maxSize; r++) data[c][r] = null;
//                }
//
//                String process, firstIn, usage;
//
//                for (int r = 0; r < maxSize; r++) {
//                    System.out.print("| ");
//                    for (int c = 0; c < cores.length; c++) {
//                        if (data[c][r] != null) {
//                            process = data[c][r].process.toTwoPointsString();
//                            firstIn = String.valueOf(data[c][r].timestamp);
//                            usage = String.format("%-9f%%", data[c][r].ratio * 100);
//                        } else process = firstIn = usage = "";
//                        System.out.printf("%-29s | %-12s | %-10s | ", process, firstIn, usage);
//                    }
//                    System.out.println();
//                }

                printLaTeXTable(table, 2);
//                System.out.println(record);
                System.out.printf("\\verb|%s|\n", record);
            }
        }

        ProcessInfo processInfo = new ProcessInfo(pid, processName);
        Double executionTime = executionTimePeeper.getExecutionTime().get(processInfo);
        Record.Timestamp first = firstLastPeeper.getFirst(processInfo),
                last = firstLastPeeper.getLast(processInfo);
        double delta = last.timeFrom(first);

        long migrationCount = migrationCounter.getCount();
        long mixedCount = mixedCounter.getCount();

        System.out.printf("First: %s; last: %s; delta: %f\n", first, last, delta);
        System.out.printf("Migration count: %d; switch count: %d\n", migrationCount, mixedCount - migrationCount);
        System.out.printf("Execution time: %f; time ratio: %f\n", executionTime, executionTime / delta);

        for (var c : executionTime1.keySet())
            System.out.printf("Core %d: %f\n", c, executionTime1.get(c));
    }

    public static void main(String[] args) throws IOException {
        Path crossCoreMigrationDirectory = Path.of("/", "home", "francesco", "Research", "why-migrations", "traces", "test0"),
                ccmTrace = crossCoreMigrationDirectory.resolve("trace.dat"),
                ccmPid = crossCoreMigrationDirectory.resolve("while1_pid");

        Path directory = Path.of("/home/francesco/Research/why-migrations/scripts/");

        Path unpinnedTrace = directory.resolve("unpinned_trace.dat"),
                unpinnedPidFile = directory.resolve("while1_unpinned_pid"),
                pinnedTrace = directory.resolve("pinned_trace.dat"),
                pinnedPidFile = directory.resolve("while1_pinned_pid");

//        about(ccmTrace, ccmPid, "while1");
        about(Path.of("/home/francesco/Research/why-migrations/benchmarks/last", "trace_unpinned_3.dat"), 5659, "while1");
//        about(pinnedTrace, pinnedPidFile, "while1");
    }
}
