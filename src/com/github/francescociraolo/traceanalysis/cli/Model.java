package com.github.francescociraolo.traceanalysis.cli;

import com.github.francescociraolo.datastructures.GroupedCollection;
import com.github.francescociraolo.datastructures.MaxSizeList;
import com.github.francescociraolo.listenable.ListenableList;
import com.github.francescociraolo.listenable.ListenableObject;
import com.github.francescociraolo.listenable.Listener;
import com.github.francescociraolo.trace.filter.RecordFilter;
import com.github.francescociraolo.trace.reader.FilteredTrace;
import com.github.francescociraolo.trace.reader.PeepTrace;
import com.github.francescociraolo.trace.reader.Trace;
import com.github.francescociraolo.trace.Record;
import com.github.francescociraolo.trace.peepers.CPUsLoad;
import com.github.francescociraolo.trace.peepers.ProcessesPinInvestigator;
import com.github.francescociraolo.trace.peepers.ProcessesStatus;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Model {

    private final ListenableObject<Path> tracePath;
    private final ListenableObject<Trace> trace;

    private final ListenableList<RecordFilter> filters = new ListenableList<>(new LinkedList<>());
    private Predicate<Record> cumulativeFilter = RecordFilter.ALL;

    private final ListenableObject<Record> currentRecord;
    private final GroupedCollection<Record, Integer, List<Record>> recordsHistory;
    private final ProcessesStatus processesStatus;
    private final CPUsLoad infoCPUsLoad;
    private final ProcessesPinInvestigator processesPinInvestigator;

    public Model() {
        tracePath = new ListenableObject<>();
        trace = new ListenableObject<>();
        currentRecord = new ListenableObject<>();
        recordsHistory = new GroupedCollection<>(() -> new MaxSizeList<>(1000), Record::getCore);
        processesStatus = new ProcessesStatus();
        infoCPUsLoad = new CPUsLoad();
        this.processesPinInvestigator = new ProcessesPinInvestigator();

//        tracePath.addListener(this::updateTrace);
    }

    private void updateTrace(Path path) throws IOException {
        infoCPUsLoad.clear();
        currentRecord.set(null);
        recordsHistory.clear();

        Trace trace = Trace.fromDatFile(path);
        trace = new PeepTrace(trace, processesStatus, infoCPUsLoad, processesPinInvestigator);
        trace = new FilteredTrace(trace, this::filterRecord);
        this.trace.set(trace);
    }

    public void restart() throws IOException {
        updateTrace(tracePath.get());
    }

    public Trace getTrace() {
        return trace.get();
    }
    /*

     */

    public void addTracePathListener(Listener<Path> listener) {
        tracePath.addListener(listener);
    }

    public Path getTracePath() {
        return tracePath.get();
    }

    public void setTracePath(Path path) throws IOException {
        tracePath.set(path);
        updateTrace(path);
    }
    /*

     */

    private boolean filterRecord(Record record) {
        return cumulativeFilter.test(record);
    }

    /*

     */

    private void updateFilter() {
        Map<String, List<RecordFilter>> map = filters.stream().collect(Collectors.groupingBy(RecordFilter::getField));
        cumulativeFilter = null;

        for (List<RecordFilter> filters : map.values())
            if (!filters.isEmpty()) {
                Predicate<Record> fieldFilter = filters.remove(0);
                for (RecordFilter filter : filters)
                    fieldFilter = fieldFilter.or(filter);
                if (cumulativeFilter == null)
                    cumulativeFilter = fieldFilter;
                else
                    cumulativeFilter = cumulativeFilter.and(fieldFilter);
            }
    }

    public void addFilter(RecordFilter filter) {
        filters.add(filter);
        updateFilter();
    }

    public List<RecordFilter> getFilters() {
        return filters;
    }

    public void removeFilter(RecordFilter filter) {
        filters.remove(filter);
        updateFilter();
    }

    public Record getCurrentRecord() {
        return currentRecord.get();
    }

    public void setCurrentRecord(Record currentRecord) {
        this.currentRecord.set(currentRecord);
    }

    public GroupedCollection<Record, Integer, List<Record>> getRecordsHistory() {
        return recordsHistory;
    }

    public ProcessesStatus getProcessesStatus() {
        return processesStatus;
    }

    public CPUsLoad getCPUsLoad() {
        return infoCPUsLoad;
    }

    public ProcessesPinInvestigator getProcessesPinInvestigator() {
        return processesPinInvestigator;
    }

    public void addRecordListener(Listener<Record> recordListener) {
        currentRecord.addListener(recordListener);
    }
}
