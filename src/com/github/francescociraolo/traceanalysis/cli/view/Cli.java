package com.github.francescociraolo.traceanalysis.cli.view;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Cli {

    MainView mainView;
    FiltersView filtersView;
    SearchView searchView;

    public Cli() {
        mainView = new MainView();
        filtersView = new FiltersView();
        searchView = new SearchView();

        mainView.setup(filtersView, searchView);
        filtersView.setup(mainView);
        searchView.setup(mainView);
    }

    public static void main(String[] args) {
        new Cli().start();
    }

    private void start() {
        String input;
        View<?> view = mainView;
        view.called();
        var run = true;
        try (var reader = new BufferedReader(new InputStreamReader(System.in))) {
            while (run) {
                try {
                    view.printContent(System.out);
                    input = reader.readLine();
                    switch (input) {
                        case "q":
                            run = false;
                            break;
                        case "":
                            try {
                                view = view.userInput(input);
                            } catch (Throwable throwable) {
                                System.err.println("Please, insert something");
                            }
                            break;
                        default:
                            view = view.userInput(input);
                    }
                } catch (RuntimeException e) {
                    e.printStackTrace(System.err);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
