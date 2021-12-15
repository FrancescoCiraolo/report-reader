package com.github.francescociraolo.traceanalysis;

import com.github.francescociraolo.trace.ProcessInfo;
import com.github.francescociraolo.trace.Record;
import com.github.francescociraolo.trace.RecordSpecification;
import com.github.francescociraolo.trace.peepers.SliceCollector;
import com.github.francescociraolo.trace.reader.FilteredTrace;
import com.github.francescociraolo.trace.reader.PeepTrace;
import com.github.francescociraolo.trace.reader.Trace;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

public class CoreStatistics {

    private final Path tracePath;
    private final ProcessInfo process;

    public CoreStatistics(Path tracePath, ProcessInfo process) throws IOException {
        this.tracePath = tracePath;
        this.process = process;
    }

    private boolean firstFilter(Record record) {
        return record.getType() == RecordSpecification.Type.SCHED_SWITCH
                && ((RecordSpecification.SchedSwitchSpec) record.getSpecification()).getNextProcess().equals(process);
    }

    private boolean filter(Record record) {
        switch (record.getType()) {
            case SCHED_SWITCH:
                RecordSpecification.SchedSwitchSpec schedSwitchSpec
                        = (RecordSpecification.SchedSwitchSpec) record.getSpecification();
                return schedSwitchSpec.getPreviousProcess().equals(process) || schedSwitchSpec.getNextProcess().equals(process);
            case SCHED_MIGRATE_TASK:
                return ((RecordSpecification.SchedMigrateTaskSpec) record.getSpecification())
                        .getProcess().equals(process);
            default:
                return false;
        }
    }

    void analyze() throws IOException {
        Trace trace = Trace.fromDatFile(tracePath);

        int cpusCount = trace.getCpusCount();
        double[] timePerCore = new double[cpusCount];
        int[][] migrationCount = new int[cpusCount][cpusCount];

        Trace filteredTrace = new FilteredTrace(trace, this::firstFilter);
        SliceCollector sliceCollector = new SliceCollector();
        filteredTrace = new PeepTrace(filteredTrace, sliceCollector);

        Record.Timestamp lastTimestamp = filteredTrace.next().getTimestamp();
        int core, lastCore = -1;

        filteredTrace = new FilteredTrace(trace, this::filter);
        trace = new PeepTrace(filteredTrace, sliceCollector);

        while (trace.hasNext()) {
            Record record = trace.next();

            if (record != null)
                if (record.getType() == RecordSpecification.Type.SCHED_SWITCH) {
                    RecordSpecification.SchedSwitchSpec spec
                            = (RecordSpecification.SchedSwitchSpec) record.getSpecification();
                    lastCore = record.getCore();
                } else {
                    RecordSpecification.SchedMigrateTaskSpec spec
                            = (RecordSpecification.SchedMigrateTaskSpec) record.getSpecification();
                    lastTimestamp = record.getTimestamp();

                    core = spec.getSourceCpu();
                    timePerCore[core] += sliceCollector.getTimeAndReset(lastTimestamp, core, process);
                    migrationCount[core][spec.getDestinationCpu()]++;
                }

//            System.out.println(record);
        }

        if (lastCore > -1)
            timePerCore[lastCore] += sliceCollector.getTimeAndReset(lastTimestamp, lastCore, process);

        for (int c = 0; c < cpusCount; c++) {
            if (timePerCore[c] > 0)
                System.out.printf("Core %d: %f\n", c, timePerCore[c]);
        }

        System.out.println();

        for (int[] row : migrationCount) {
            for (int cell : row)
                System.out.printf("%-2d ", cell);
            System.out.println();
        }

        for (int i = 0; i < cpusCount; i++) {
            if (timePerCore[i] > 0) {
                for (int j = 0; j < cpusCount; j++) {
                    if (timePerCore[j] > 0)
                        System.out.printf("%-2d ", migrationCount[i][j]);
                }
                System.out.println();
            }
        }

        double totalTime = Arrays.stream(timePerCore).sum();
        double time;
        int interIn, interOut, crossIn, crossOut;

        for (int i = 0; i < cpusCount; i++)
            if (timePerCore[i] != 0) {
                time = timePerCore[i] / totalTime * 100;
                interIn = interOut = crossIn = crossOut = 0;
                for (int j = 0; j < cpusCount; j++) {
//                    System.out.printf("%d, %d: %s\\", i, j, (i + 6) % 6 == j);
                    if ((i % 6) == (j % 6)) {
                        interIn += migrationCount[j][i];
                        interOut += migrationCount[i][j];
                    } else {
                        crossIn += migrationCount[j][i];
                        crossOut += migrationCount[i][j];
                    }
                }
                System.out.printf("%d & %.3f & %d & %d & %d & %d \\\\\n", i, time, interIn, interOut, crossIn, crossOut);
            }
    }


    static CoreStatistics analyze(Path tracePath, Path pidPath, String processName) throws IOException {
//        Trace trace = Trace.fromDatFile(tracePath);
        int pid = Integer.parseInt(Files.readString(pidPath).strip());
        ProcessInfo process = new ProcessInfo(pid, processName);

        return new CoreStatistics(tracePath, process);
    }

    public static void main(String[] args) throws IOException {
//        Path crossCoreMigrationDirectory = Path.of("/", "home", "francesco", "Research", "why-migrations", "traces", "test0"),
//                ccmTrace = crossCoreMigrationDirectory.resolve("trace.dat"),
//                ccmPid = crossCoreMigrationDirectory.resolve("while1_pid");
//
//        int pid = Integer.parseInt(Files.readString(ccmPid).strip());
//        CoreStatistics while1 = new CoreStatistics(ccmTrace, new ProcessInfo(pid, "while1"));
//        while1.analyze();
        new CoreStatistics(Path.of("/home/francesco/Research/why-migrations/benchmarks/last", "trace_unpinned_0.dat"), new ProcessInfo(4849, "while1")).analyze();
    }
}
