package com.github.francescociraolo.traceanalysis.cli.view;

import com.github.francescociraolo.traceanalysis.cli.Control;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

enum MainViewState {
    NO_PATH,
    MAIN,
    INSERTING_PATH
}

public class MainView extends View<MainViewState> {

    private static final String DEFAULT_TRACE_PATH = "/dev/shm/trace.dat";

    FiltersView filtersView;
    SearchView searchView;

    private final List<ViewMenuEntry> mainMenu;

    public MainView() {
        mainMenu = List.of(
                new ViewMenuEntry("Reload trace from beginning", this::reloadTrace),
                new ViewMenuEntry("Filters menu", this::filtersMenu),
                new ViewMenuEntry("Search menu", this::searchMenu)
        );
    }

    public void setup(FiltersView filtersView, SearchView searchView) {
        this.filtersView = filtersView;
        this.searchView = searchView;
    }

    private View<?> searchMenu() {
        searchView.called();
        return searchView;
    }

    private View<?> filtersMenu() {
        filtersView.called();
        return filtersView;
    }

    private View<?> reloadTrace() {
        try {
            Control.reloadTrace();
            state = MainViewState.MAIN;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    @Override
    MainViewState prepareNewState() {
        return Control.getTracePath() != null ? MainViewState.MAIN : MainViewState.NO_PATH;
    }

    @Override
    public void printContent(PrintStream printStream) {
        printTitle(printStream, "Main Menu");

        switch (state) {
            case NO_PATH:
            case INSERTING_PATH:
                printStream.print("Insert trace.dat path [" + DEFAULT_TRACE_PATH + "]: ");
                break;
            case MAIN:
                printList(printStream, mainMenu, PrintOptions.ENUMERATE);
                askForInput(printStream, "Select menu index");
                break;
        }
    }

    @Override
    public View<?> userInput(String input) {
        View<?> view = this;
        switch (state) {
            case NO_PATH:
            case INSERTING_PATH:
                try {
                    if (input.isBlank())
                        input = DEFAULT_TRACE_PATH;
                    Control.parseUserInputPath(input);
                    state = MainViewState.MAIN;
                } catch (IOException e) {
                    e.printStackTrace(System.err);
                }
                break;
            case MAIN:
                view = Control.extractUserSelected(mainMenu, input).run();
        }
        return view;
    }

    @Override
    public MainViewState getState() {
        return state;
    }

    @Override
    public void restore(MainViewState state) {
        this.state = Control.getTracePath() != null ? MainViewState.MAIN : MainViewState.NO_PATH;
    }
}
