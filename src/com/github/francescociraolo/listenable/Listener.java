package com.github.francescociraolo.listenable;

public interface Listener<T> {

    void changed(T t);
}
