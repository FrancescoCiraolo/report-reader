package com.github.francescociraolo.listenable;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class ListenableCollection<T, C extends Collection<T>>
        extends AbstractListenable<Collection<T>>
        implements Collection<T> {

    private final C collection;

    public ListenableCollection(C collection) {
        this.collection = collection;
        throw new UnsupportedOperationException();
    }

    public ListenableCollection(C collection, List<Listener<Collection<T>>> listeners) {
        super(listeners);
        this.collection = collection;
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        return collection.size();
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean contains(Object o) {
        return false;
    }

    @Override
    public Iterator<T> iterator() {
        return null;
    }

    @Override
    public Object[] toArray() {
        return new Object[0];
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        return null;
    }

    @Override
    public boolean add(T t) {
        return false;
    }

    @Override
    public boolean remove(Object o) {
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return false;
    }

    @Override
    public void clear() {

    }
}
