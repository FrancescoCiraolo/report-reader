package com.github.francescociraolo.listenable;

import java.util.List;

public interface Listenable<T> {

    void addListener(Listener<T> listener);

    boolean hasListener(Listener<T> listener);

    boolean removeListener(Listener<T> listener);

    List<Listener<T>> listenerStream();

}
