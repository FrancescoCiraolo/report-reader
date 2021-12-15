package com.github.francescociraolo.traceanalysis;

import com.github.francescociraolo.datastructures.SlightlyMutableArray;
import com.github.francescociraolo.trace.peepers.*;
import com.github.francescociraolo.trace.reader.*;
import com.github.francescociraolo.trace.ProcessInfo;
import com.github.francescociraolo.trace.Record;
import com.github.francescociraolo.trace.RecordSpecification;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class UnreasonableMigrations {

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

    public UnreasonableMigrations(Path datFilePath, int pid, String processName) throws IOException {
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

    public static void main(String[] args) throws IOException {
        Path path = Path.of("/home/francesco/trace.dat");
        UnreasonableMigrations migrations = new UnreasonableMigrations(Path.of("/home/francesco/Research/why-migrations/benchmarks/last", "trace_unpinned_0.dat"), 4849, "while1");
        migrations.printReport();
    }
}
