package com.github.francescociraolo.listenable;

import java.util.Objects;

public class ListenableObject<T> extends AbstractListenable<T> {

    private T value;

    public ListenableObject() {
        this.value = null;
    }

    public ListenableObject(T initValue) {
        this.value = initValue;
    }

    public T get() {
        return value;
    }

    public void set(T value) {
        this.value = value;
        listeners.forEach(listener -> listener.changed(value));
    }
}
