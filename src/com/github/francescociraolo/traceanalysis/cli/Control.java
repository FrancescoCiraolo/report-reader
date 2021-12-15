package com.github.francescociraolo.traceanalysis.cli;

import com.github.francescociraolo.listenable.Listener;
import com.github.francescociraolo.trace.filter.FilterParser;
import com.github.francescociraolo.datastructures.GroupedCollection;
import com.github.francescociraolo.trace.filter.RecordFilter;
import com.github.francescociraolo.trace.peepers.CPUsLoad;
import com.github.francescociraolo.trace.peepers.ProcessesPinInvestigator;
import com.github.francescociraolo.trace.peepers.ProcessesStatus;
import com.github.francescociraolo.trace.Record;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class Control {

    private static final Model MODEL = new Model();
    private static final List<Record> HISTORY = new LinkedList<>();

    /*
    Trace path
     */
    public static Path getTracePath() {
        var path = MODEL.getTracePath();
        return path != null ? path.toAbsolutePath() : null;
    }

    public static void addTracePathListener(Listener<Path> listener) {
        MODEL.addTracePathListener(listener);
    }

    public static void parseUserInputPath(String userInput) throws IOException {
        var tracePath = Path.of(userInput);

        if (Files.notExists(tracePath))
            throw new FileNotFoundException(userInput);

        MODEL.setTracePath(tracePath);
    }

    public static void reloadTrace() throws IOException {
        MODEL.restart();
    }

    /*
    Filters
     */
    public static void userInputFilter(String input, Function<String, RecordFilter> model) {
        var filter = model.apply(input);
        MODEL.addFilter(filter);
    }

    public static void userInputFilterExpr(String input) {
        var filter = FilterParser.parse(input);
        MODEL.addFilter(filter);
    }

    public static List<RecordFilter> getFilters() {
        return MODEL.getFilters();
    }

    public static void removeFilter(RecordFilter filter) {
        MODEL.removeFilter(filter);
    }

    /*

     */
    public static void addRecordListener(Listener<Record> recordListener) {
        MODEL.addRecordListener(recordListener);
    }

    public static void searchNextRecord() throws IOException {
        Record currentRecord = MODEL.getTrace().next();
        if (currentRecord != null) HISTORY.add(currentRecord);
        else storeHistory();
        MODEL.setCurrentRecord(currentRecord);
    }

    public static boolean hasNext() {
        return MODEL.getTrace().hasNext();
    }

    public static Record getCurrentRecord() {
        return MODEL.getCurrentRecord();
    }

    public static GroupedCollection<Record, Integer, List<Record>> getRecordsHistory() {
        return MODEL.getRecordsHistory();
    }

    public static ProcessesStatus getProcessesStatus() {
        return MODEL.getProcessesStatus();
    }

    public static CPUsLoad getCPUsLoad() {
        return MODEL.getCPUsLoad();
    }

    public static ProcessesPinInvestigator getProcessesPinInvestigator() {
        return MODEL.getProcessesPinInvestigator();
    }

    public static void storeHistory() throws IOException {
        var filePath = Objects
                .requireNonNull(getTracePath())
                .getParent()
                .resolve(String.format("%d", System.currentTimeMillis()));

        try (var out = new PrintStream(Files.newOutputStream(filePath, StandardOpenOption.CREATE))) {
            for (var record : HISTORY)
                record.print(out, false);
            HISTORY.clear();
        }
    }

    /*

     */
    public static <T> T extractUserSelected(List<T> list, String userInput) {
        var pos = Integer.parseInt(userInput) - 1;
        return list.get(pos);
    }

    public static <T> T extractUserSelected(List<T> list, String userInput, int defIndex) {
        try {
            defIndex = Integer.parseInt(userInput) - 1;
        } catch (Throwable e) {}
        return list.get(defIndex);
    }

    public static <T> T parseOrDefault(String userInput, Function<String, T> parser, T defValue) {
        try {
            defValue = parser.apply(userInput);
        } catch (Throwable ignored) {

        }
        return defValue;
    }
}
