package com.github.francescociraolo.trace.peepers;

import com.github.francescociraolo.trace.ProcessInfo;
import com.github.francescociraolo.trace.Record;
import com.github.francescociraolo.trace.RecordSpecification;
import com.github.francescociraolo.trace.reader.RecordPeeper;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MigrationTimePeeper implements RecordPeeper {

    private final Map<ProcessInfo, ProcessStatus> data;
    private final Set<ProcessInfo> filters;

    public MigrationTimePeeper() {
        data = new HashMap<>();
        this.filters = null;
    }

    public MigrationTimePeeper(ProcessInfo... filters) {
        data = new HashMap<>();
        this.filters = Set.of(filters);
    }

    private ProcessStatus getProcessStatus(ProcessInfo process) {
        if (!data.containsKey(process))
            data.put(process, new ProcessStatus(process));
        return data.get(process);
    }

    @Override
    public void peep(Record record) {
        switch (record.getType()) {
            case SCHED_MIGRATE_TASK:
                RecordSpecification.SchedMigrateTaskSpec migrateTaskSpec
                        = (RecordSpecification.SchedMigrateTaskSpec) record.getSpecification();
                ProcessInfo key = migrateTaskSpec.getProcess();

                ProcessStatus processStatus = getProcessStatus(key);
                processStatus.migrating(migrateTaskSpec.getSourceCpu(),
                        migrateTaskSpec.getDestinationCpu(),
                        record.getTimestamp());
                break;
            case SCHED_SWITCH:
                RecordSpecification.SchedSwitchSpec switchSpec
                        = (RecordSpecification.SchedSwitchSpec) record.getSpecification();

                ProcessStatus previousStatus = getProcessStatus(switchSpec.getPreviousProcess()),
                        nextStatus = getProcessStatus(switchSpec.getNextProcess());

                previousStatus.leavingCore(record.getCore(), record.getTimestamp());
                nextStatus.enteringCore(record.getCore(), record.getTimestamp());
                break;
        }
    }

    private void addMigrationDuration(ProcessInfo process, double duration, Record.Timestamp migrationTimestamp) {
        if (filters == null || filters.contains(process)) {
            System.out.printf("Migration of %s at %s: %7e seconds", process.toTwoPointsString(), migrationTimestamp, duration);
        }
    }

    private void addMigrationDuration(ProcessInfo process, Record.Timestamp leavingTime, Record.Timestamp enteringTime, Record.Timestamp migrationTimestamp) {
        if (filters == null || filters.contains(process)) {
            System.out.printf("Migration of %s [%e, %e] at %e: %e\n",
                    process.toTwoPointsString(),
                    leavingTime.getAsDouble(),
                    enteringTime.getAsDouble(),
                    migrationTimestamp.getAsDouble(),
                    enteringTime.timeFrom(leavingTime));
        }
    }

    private class ProcessStatus {

        private final ProcessInfo process;
        private Status status;
        private Record.Timestamp leavingTime, migrationTime, enteringTime;
        private int leavingCore, enteringCore;

        private ProcessStatus(ProcessInfo process) {
            this.process = process;
            reset();
        }

        public void leavingCore(int core, Record.Timestamp timestamp) {
            switch (status) {
                case RUNNING:
                    if (coreMismatching(leavingCore, core))
                        break;
                case UNKNOWN:
                    status = Status.NOT_RUNNING;
                    leavingTime = timestamp;
                    leavingCore = core;
                    break;
                default:
                    unexpectedStatus();
                    break;
            }
        }

        public void migrating(int srcCore, int dstCore, Record.Timestamp timestamp) {
            switch (status) {
                case NOT_RUNNING:
                    if (coreMismatching(leavingCore, srcCore))
                        break;
                    enteringCore = dstCore;
                    status = Status.MIGRATING;
                    migrationTime = timestamp;
                    break;
                case UNKNOWN:
                    break;
                default:
                    unexpectedStatus();
                    break;
            }
        }

        public void enteringCore(int core, Record.Timestamp timestamp) {
            switch (status) {
                case MIGRATING:
                    if (coreMismatching(enteringCore, core))
                        break;
                    addMigrationDuration(process, leavingTime, timestamp, migrationTime);
                case RUNNING:
                    if (coreMismatching(enteringCore, core))
                        break;
                    leavingCore = core;
                    status = Status.RUNNING;
                case UNKNOWN:
                    break;
                default:
                    unexpectedStatus();
                    break;
            }
        }

        private boolean coreMismatching(int expected, int found) {
            boolean mismatching = expected != found;
            if (mismatching) {
//                System.err.printf("Core mismatch for %s; expected %d, found %d; events lost?\n",
//                        process.toTwoPointsString(),
//                        expected,
//                        found);
                reset();
            }
            return mismatching;
        }

        private void unexpectedStatus() {
//            System.err.printf("Process %s is in an unexpected state; events lost?\n", process.toTwoPointsString());
            reset();
        }

        void reset() {
            status = Status.UNKNOWN;
            leavingCore = enteringCore = -1;
            leavingTime = migrationTime = enteringTime = null;
        }

    }

    private enum Status {
        UNKNOWN,
        RUNNING,
        NOT_RUNNING,
        MIGRATING
    }

    private static class RecordSet {

        Record lastExitingSwitch;
        Record migrationRecord;
    }
}
