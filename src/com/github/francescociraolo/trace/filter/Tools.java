package com.github.francescociraolo.trace.filter;

import com.github.francescociraolo.trace.RecordSpecification;
import com.github.francescociraolo.trace.Record;

import java.util.Objects;
import java.util.function.Predicate;

public class Tools {

    public static RecordFilter coreFilter(int core) {
        return new RecordFilter("core", String.valueOf(core)) {
            @Override
            public boolean test(Record record) {
                return record.getCore() == core;
            }
        };
    }

    public static RecordFilter coreAndSpecificationFilter(int core) {
        return new RecordFilter("core", String.valueOf(core)) {
            @Override
            public boolean test(Record record) {
                return record.getCore() == core || record.getSpecification().isCoreRelated(core);
            }
        };
    }

    public static RecordFilter pidFilter(int pid) {
        return new RecordFilter("PID", String.valueOf(pid)) {
            @Override
            public boolean test(Record record) {
                return record.isPidRelated(pid);
            }
        };
    }

    public static RecordFilter specificationPidFilter(int pid) {
        return new RecordFilter("PID", String.valueOf(pid)) {
            @Override
            public boolean test(Record record) {
                return record.getSpecification().isPidRelated(pid);
            }
        };
    }

    public static RecordFilter timestampFilter(Predicate<Double> timestampPredicate, String text) {
        return new RecordFilter("Timestamp", text) {
            @Override
            public boolean test(Record record) {
                return timestampPredicate.test(record.getTimestamp().getAsDouble());
            }
        };
    }

    public static RecordFilter typeFilter(RecordSpecification.Type type) {
        Objects.requireNonNull(type);
        return new RecordFilter("type", type.getTypeString()) {
            @Override
            public boolean test(Record record) {
                return record.getSpecification().getType() == type;
            }
        };
    }

    public static Predicate<Record> pidAndCoreFilter(int pid, int core) {
        return record -> record.isPidRelated(pid) && record.getCore() == core;
    }

    public static Predicate<Record> pidAndType(int pid, RecordSpecification.Type type) {
        return record -> record.isPidRelated(pid) && record.getSpecification().getType() == type;
    }

    public static Predicate<Record> processMigration(int pid) {
        return record -> {
            var spec = record.getSpecification();
            return spec.getType() == RecordSpecification.Type.SCHED_MIGRATE_TASK
                    && ((RecordSpecification.SchedMigrateTaskSpec) spec).getPid() == pid;
        };
    }

}
