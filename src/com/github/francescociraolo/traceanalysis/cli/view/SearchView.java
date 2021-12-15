package com.github.francescociraolo.traceanalysis.cli.view;

import com.github.francescociraolo.trace.peepers.CPUsLoad;
import com.github.francescociraolo.traceanalysis.cli.Control;
import com.github.francescociraolo.trace.peepers.ProcessesPinInvestigator;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

enum SearchState {
    FIRST,
    MAIN,
    END,
    RUNNING_TASKS,
    CPU_IDLE,
    SELECT_RECORDS_CORE,
    SELECT_LIST_SIZE,
    LAST_RECORDS
}

public class SearchView extends View<SearchState> {

//    SearchState state = SearchState.MAIN;
    private int recordsCore = 0;
    private int listSize = 50;
    private boolean showStacktrace = false;

    private MainView mainView;

    private final List<ViewMenuEntry> mainMenu;
    private final List<ViewMenuEntry> noRecordMenu;
    private final List<ViewMenuEntry> endMenu;

    public SearchView() {
        mainMenu = List.of(
                new ViewMenuEntry("Search next record", this::nextRecord),
                new ViewMenuEntry("Reload trace from beginning", this::reloadTrace),
                new ViewMenuEntry("Cores running tasks", justUpdateState(SearchState.RUNNING_TASKS)),
                new ViewMenuEntry("Cores' idle time", justUpdateState(SearchState.CPU_IDLE)),
                new ViewMenuEntry("Print last records", justUpdateState(SearchState.LAST_RECORDS)),
                new ViewMenuEntry("Save found records", this::saveHistory),
                new ViewMenuEntry("Show stacktrace", this::switchStacktrace) {

                    @Override
                    public String toString() {
                        return showStacktrace ? "Hide stacktrace" : "Show stacktrace";
                    }
                },
                new ViewMenuEntry("Main menu", this::mainMenu)
        );
        noRecordMenu = List.of(
                mainMenu.get(0),
                mainMenu.get(1),
                mainMenu.get(4),
                mainMenu.get(5),
                mainMenu.get(6)
        );
        endMenu = List.of(
                mainMenu.get(1),
                mainMenu.get(4),
                mainMenu.get(6),
                mainMenu.get(5)
        );
    }

    private View<?> saveHistory() {
        try {
            Control.storeHistory();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    private View<?> switchStacktrace() {
        showStacktrace = !showStacktrace;
        return this;
    }

    public void setup(MainView mainView) {
        this.mainView = mainView;
    }

    private View<?> reloadTrace() {
        try {
            Control.reloadTrace();
            state = SearchState.MAIN;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    private View<?> mainMenu() {
        return mainView;
    }

    private View<?> nextRecord() {
        try {
            Control.searchNextRecord();
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
        state = prepareNewState();
        return this;
    }

    @Override
    SearchState prepareNewState() {
        SearchState state;
        if (Control.getCurrentRecord() != null) {
            state = SearchState.MAIN;
        } else if (Control.hasNext()) {
            state = SearchState.FIRST;
        } else {
            ProcessesPinInvestigator processesPinInvestigator = Control.getProcessesPinInvestigator();
            state = SearchState.END;
        }
        return state;
    }

    @Override
    public void printContent(PrintStream printStream) {
        switch (state) {
            case FIRST:
                printTitle(printStream, "Search module");

                printStream.println("No record yet");
                printList(printStream, noRecordMenu, PrintOptions.ENUMERATE);

                askForInput(printStream, "Insert menu index");
                break;
            case MAIN:
                printTitle(printStream, "Search module");

                Control.getCurrentRecord().print(printStream, showStacktrace);
                printList(printStream, mainMenu, PrintOptions.ENUMERATE);

                askForInput(printStream, "Insert menu index");
                break;

            case END:
                printTitle(printStream, "Search module");

                printStream.println("No more records");
                printList(printStream, endMenu, PrintOptions.ENUMERATE);

                askForInput(printStream, "Insert menu index");
                break;
            case RUNNING_TASKS:
                printTitle(printStream, "Running tasks per core");

            {
                var list = Control.getProcessesStatus().getProcesses();
                printList(printStream,
                        list,
                        (processInfo, index) -> String.format("[%03d] => %s", index, processInfo));
            }

                askForInput(printStream, "Press enter to continue");
                break;
            case CPU_IDLE:
                printTitle(printStream, "Cores' idle time");

            {
                double end = Control.getCurrentRecord().getTimestamp().getAsDouble();
                var list = Control.getCPUsLoad().getLoadData(end - 1, end);
                printList(printStream,
                        list,
                        (core, index) -> String.format("[%03d] => %.2f%%", index, core.getOrDefault(CPUsLoad.CommonStatus.IDLE, 0d) * 100));
            }

            askForInput(printStream, "Press enter to continue");
            break;
            case SELECT_RECORDS_CORE:
                printTitle(printStream, "Select which core's records to show");
                askForInput(printStream, "Insert core number");
                break;
            case SELECT_LIST_SIZE:
                printTitle(printStream, "Select list size");
                askForInput(printStream, String.format("Insert list size (default %d)", listSize));
                break;
            case LAST_RECORDS:
                printTitle(printStream, String.format("Last %d records on core [%03d]", listSize, recordsCore));
            {
                var list = Control.getRecordsHistory().getCollection(recordsCore);
                var size = list.size();
                printList(printStream, list.subList(Math.min(0, size - listSize), size));
            }
                askForInput(printStream, "Press enter to continue");
                break;
        }
    }

    @Override
    public View<?> userInput(String input) {
        View<?> view = this;
        switch (state) {
            case FIRST:
                view = Control.extractUserSelected(noRecordMenu, input).run();
                break;
            case MAIN:
                view = Control.extractUserSelected(mainMenu, input, 0).run();
                break;
            case END:
                view = Control.extractUserSelected(endMenu, input).run();
                break;
            case RUNNING_TASKS:
            case CPU_IDLE:
            case LAST_RECORDS:
                state = SearchState.MAIN;
                break;
            case SELECT_RECORDS_CORE:
                recordsCore = Integer.parseInt(input);
                state = SearchState.SELECT_LIST_SIZE;
                break;
            case SELECT_LIST_SIZE:
                listSize = Control.parseOrDefault(input, Integer::parseInt, listSize);
                state = SearchState.LAST_RECORDS;
                break;
        }
        return view;
    }

    @Override
    void restore(SearchState state) {

    }
}
