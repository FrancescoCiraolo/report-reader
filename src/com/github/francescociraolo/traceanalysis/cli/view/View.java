package com.github.francescociraolo.traceanalysis.cli.view;

import java.io.PrintStream;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public abstract class View<T> {

    T state;

    abstract T prepareNewState();

    public final void called() {
        state = prepareNewState();
    }

    public abstract void printContent(PrintStream printStream);

    public abstract View<?> userInput(String input);

    public T getState() {
        return state;
    }

    abstract void restore(T state);

    public final void restoreState(T state) {
        this.state = state;
//        state.update();
        restore(state);
    }


    ViewMenuAction justUpdateState(T state) {
        return () -> {
            this.state = state;
            return this;
        };
    }


    static void printSepLine(PrintStream printStream) {
        printStream.println("#".repeat(25));
    }

    static void printTitle(PrintStream printStream, String title) {
        printStream.println("\n\n");
        printSepLine(printStream);
        printStream.println(title);
        printSepLine(printStream);
        printStream.println();
    }

    enum PrintOptions {
        ENUMERATE
    }

    static <T> void printList(PrintStream printStream, List<T> list, PrintOptions... options) {
        printList(printStream, list, ElementListString.simple(Objects::toString), options);
    }

    static <T> void printList(PrintStream printStream,
                              List<T> list,
                              ElementListString<T> toStringFunc,
                              PrintOptions... options) {
        var opts = Set.of(options);
        var enumerate = opts.contains(PrintOptions.ENUMERATE);

        var size = list.size();
        var numPattern = enumerate ? "%" + Math.round(Math.ceil(Math.log10(size + 1))) + "d) " : "";

        for (int i = 0; i < size; i++)
            printStream.printf("\t%s%s\n", String.format(numPattern, i + 1), toStringFunc.getString(list.get(i), i));
    }

    static void askForInput(PrintStream printStream, String msg) {
        printStream.printf("\n%s [q to exit]: ", msg);
    }

    public interface ViewMenuAction {

        View<?> run();
    }

    public static class ViewMenuEntry {
        private final String text;
        private final ViewMenuAction action;

        public ViewMenuEntry(String text, ViewMenuAction action) {
            this.text = text;
            this.action = action;
        }

        public View<?> run() {
            return action.run();
        }

        @Override
        public String toString() {
            return text;
        }
    }

    @FunctionalInterface
    public interface ElementListString<E> {

        String getString(E element, int index);

        static <E> ElementListString<E> simple(Function<E, String> toString) {
            return (element, index) -> toString.apply(element);
        }
    }
}
