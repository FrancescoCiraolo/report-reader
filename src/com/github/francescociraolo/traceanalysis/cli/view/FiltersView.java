package com.github.francescociraolo.traceanalysis.cli.view;

import com.github.francescociraolo.datastructures.Pair;
import com.github.francescociraolo.trace.filter.RecordFilter;
import com.github.francescociraolo.trace.filter.Tools;
import com.github.francescociraolo.traceanalysis.cli.Control;
import com.github.francescociraolo.trace.RecordSpecification.Type;

import java.io.PrintStream;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

enum FiltersState {
    MAIN,
    ADD,
    ADD_EXPR,
    ADDING,
    SHOW,
    REMOVE
}

public class FiltersView extends View<FiltersState> {

    Pair<String, Function<String, RecordFilter>> addingEntry = null;

    private MainView mainView;

    private static List<ViewMenuEntry> menu;
    private static List<Pair<String, Function<String, RecordFilter>>> addMenu;

    private static <I, T, R> Function<I, R> join(Function<I, T> f1, Function<T, R> f2) {
        return f1.andThen(f2);
    }

    public FiltersView() {
        menu = List.of(
                new ViewMenuEntry("Add filter", justUpdateState(FiltersState.ADD)),
                new ViewMenuEntry("Add expression filter", justUpdateState(FiltersState.ADD_EXPR)),
                new ViewMenuEntry("Show filters", justUpdateState(FiltersState.SHOW)),
                new ViewMenuEntry("Remove filter", justUpdateState(FiltersState.REMOVE)),
                new ViewMenuEntry("Back to main menu", this::mainMenu)
        );
        addMenu = List.of(
                new Pair<>("pid", join(Integer::parseInt, Tools::pidFilter)),
                new Pair<>("specification pid", join(Integer::parseInt, Tools::specificationPidFilter)),
                new Pair<>("type", join(Type::findTypeByString, Optional::get).andThen(Tools::typeFilter)),
                new Pair<>("timestamp greater than", s -> {
                    var d = Double.parseDouble(s);
                    return Tools.timestampFilter(v -> v >= d, ">= " + s);
                }),
                new Pair<>("timestamp lower than", s -> {
                    var d = Double.parseDouble(s);
                    return Tools.timestampFilter(v -> v <= d, "<= " + s);
                }),
                new Pair<>("core", join(Integer::parseInt, Tools::coreFilter)),
                new Pair<>("core (with specification)", join(Integer::parseInt, Tools::coreAndSpecificationFilter))
        );
        state = FiltersState.MAIN;
    }

    public void setup(MainView mainView) {
        this.mainView = mainView;
    }

    private View<?> mainMenu() {
        reset();
        mainView.called();
        return mainView;
    }

    @Override
    FiltersState prepareNewState() {
        reset();
        return FiltersState.MAIN;
    }

    @Override
    public void printContent(PrintStream printStream) {
        switch (state) {
            case MAIN:
                printTitle(printStream, "Filters");
                printList(printStream, menu, PrintOptions.ENUMERATE);
                askForInput(printStream, "Insert option index");
                break;
            case SHOW:
                printTitle(printStream, "Current filters");
                printList(printStream, Control.getFilters(), PrintOptions.ENUMERATE);
                askForInput(printStream, "Press enter to continue");
                break;
            case ADD:
                printTitle(printStream, "Add filter, select type");
                printList(printStream, addMenu, View.ElementListString.simple(Pair::getFirst), PrintOptions.ENUMERATE);
                askForInput(printStream, "Insert type index");
                break;
            case ADD_EXPR:
                printTitle(printStream, "Add expression filter");
                askForInput(printStream, "Insert filtering expression");
                break;
            case ADDING:
                printTitle(printStream, String.format("Add %s filter, insert value", addingEntry.getFirst()));
                askForInput(printStream, "Insert filtering value");
                break;
            case REMOVE:
                printTitle(printStream, "Remove filter, select which one");
                printList(printStream, Control.getFilters(), PrintOptions.ENUMERATE);
                askForInput(printStream, "Insert filter index");
                break;
        }
    }

    @Override
    public View<?> userInput(String input) {
        View<?> view = this;
        switch (state) {
            case MAIN:
                var entry = Control.extractUserSelected(menu, input);
                view = entry.run();
                break;
            case SHOW:
                state = FiltersState.MAIN;
                break;
            case ADD:
                addingEntry = Control.extractUserSelected(addMenu, input);
                state = FiltersState.ADDING;
                break;
            case ADD_EXPR:
                Control.userInputFilterExpr(input);
                reset();
                break;
            case ADDING:
                Control.userInputFilter(input, addingEntry.getSecond());
                reset();
                break;
            case REMOVE:
                var filter = Control.extractUserSelected(Control.getFilters(), input);
                state = FiltersState.MAIN;
                Control.removeFilter(filter);
                break;
        }
        return view;
    }

    @Override
    void restore(FiltersState state) {

    }

    private void reset() {
        addingEntry = null;
        state = FiltersState.MAIN;
    }
}
