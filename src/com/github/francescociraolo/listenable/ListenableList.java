package com.github.francescociraolo.listenable;

import java.util.*;
import java.util.function.UnaryOperator;

public class ListenableList<T> extends AbstractListenable<List<T>> implements List<T> {

    private final List<T> list;

    public ListenableList(List<T> list) {
        this.list = list;
    }

    ListenableList(List<T> list, List<Listener<List<T>>> listeners) {
        super(listeners);
        this.list = list;
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return list.contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {

            final Iterator<T> iterator = list.iterator();

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public T next() {
                return iterator.next();
            }
        };
    }

    @Override
    public Object[] toArray() {
        return list.toArray();
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        return list.toArray(a);
    }

    @Override
    public boolean add(T t) {
        var res = list.add(t);
        if (res) listeners.forEach(l -> l.changed(list));
        return res;
    }

    @Override
    public boolean remove(Object o) {
        var res = list.remove(o);
        if (res) listeners.forEach(l -> l.changed(list));
        return res;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return list.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        var res = list.addAll(c);
        if (res) listeners.forEach(l -> l.changed(list));
        return res;
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        var res = list.addAll(index, c);
        if (res) listeners.forEach(l -> l.changed(list));
        return res;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        var res = list.removeAll(c);
        if (res) listeners.forEach(l -> l.changed(list));
        return res;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        var res = list.retainAll(c);
        if (res) listeners.forEach(l -> l.changed(list));
        return res;
    }

    @Override
    public void replaceAll(UnaryOperator<T> operator) {
        var tmp = List.copyOf(list);
        list.replaceAll(operator);
        if (!tmp.equals(list)) listeners.forEach(l -> l.changed(list));
    }

    @Override
    public void sort(Comparator<? super T> c) {
        var tmp = List.copyOf(list);
        list.sort(c);
        if (!tmp.equals(list)) listeners.forEach(l -> l.changed(list));
    }

    @Override
    public void clear() {
        var res = !list.isEmpty();
        list.clear();
        if (res) listeners.forEach(listener -> listener.changed(list));
    }

    @Override
    public T get(int index) {
        return list.get(index);
    }

    @Override
    public T set(int index, T element) {
        return list.set(index, element);
    }

    @Override
    public void add(int index, T element) {
        list.add(index, element);
        listeners.forEach(listener -> listener.changed(list));
    }

    @Override
    public T remove(int index) {
        var remove = list.remove(index);
        listeners.forEach(listener -> listener.changed(list));
        return remove;
    }

    @Override
    public int indexOf(Object o) {
        return list.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return list.lastIndexOf(o);
    }

    @Override
    public ListIterator<T> listIterator() {
        return list.listIterator();
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        return list.listIterator(index);
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        var subList = list.subList(fromIndex, toIndex);
        subList = new ListenableList<>(subList, listeners);
        return subList;
    }

    @Override
    public Spliterator<T> spliterator() {
        return list.spliterator();
    }
}
