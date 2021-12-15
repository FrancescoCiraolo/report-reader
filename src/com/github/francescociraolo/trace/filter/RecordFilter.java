package com.github.francescociraolo.trace.filter;

import com.github.francescociraolo.trace.Record;

import java.util.function.Predicate;

public abstract class RecordFilter implements Predicate<Record> {

    public static final RecordFilter ALL = new RecordFilter(null, null) {
        @Override
        public boolean test(Record record) {
            return true;
        }

        @Override
        public String toString() {
            return "ALL";
        }
    };

    private final String field;
    private final String value;

    protected RecordFilter(String field, String value) {
        this.field = field;
        this.value = value;
    }

    public String getField() {
        return field;
    }

    @Override
    public String toString() {
        return String.format("Filter by %s: %s", field, value);
    }
}
