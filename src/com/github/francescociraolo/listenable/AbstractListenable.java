package com.github.francescociraolo.listenable;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

abstract class AbstractListenable<T> implements Listenable<T> {

    final List<Listener<T>> listeners;

    AbstractListenable() {
        this.listeners = new LinkedList<>();
    }

    AbstractListenable(List<Listener<T>> listeners) {
        this.listeners = listeners;
    }

    @Override
    public void addListener(Listener<T> listener) {
        listeners.add(listener);
    }

    @Override
    public boolean hasListener(Listener<T> listener) {
        return listeners.contains(listener);
    }

    @Override
    public boolean removeListener(Listener<T> listener) {
        return listeners.remove(listener);
    }

    @Override
    public List<Listener<T>> listenerStream() {
        return List.copyOf(listeners);
    }

    protected void changed(T value) throws ListenerException {
        for (var listener : listeners) {
            listener.changed(value);
        }
    }
}
