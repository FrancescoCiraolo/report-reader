package com.github.francescociraolo.datastructures;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Objects;

public class MaxSizeList<T> extends AbstractList<T> {

    private final Object[] elements;
    private final int maxSize;

    private int pos;
    private int size = 0;

    public MaxSizeList(int maxSize) {
        this.elements = new Object[maxSize];
        this.maxSize = maxSize;

        this.pos = maxSize - 1;
    }

    @Override
    public boolean add(T t) {
        elements[++pos % maxSize] = t;
        if (size < maxSize) size++;
        return true;
    }

    @Override
    public void clear() {
        Arrays.fill(elements, null);
        pos = maxSize - 1;
        size = 0;
    }

    private int fixIndex(int index) {
        Objects.checkIndex(index, size);
        return (1 - size + pos + index) % maxSize;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T get(int index) {
        index = fixIndex(index);
        return (T) elements[index];
    }

    @Override
    @SuppressWarnings("unchecked")
    public T remove(int index) {
        var fixed = fixIndex(index);

        T element = (T) elements[fixed];

        throw new UnsupportedOperationException();
//        return super.remove(index);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T set(int index, T element) {
        index = fixIndex(index);
        T old = (T) elements[index];
        elements[index] = element;
        return old;
    }

    @Override
    public int size() {
        return size;
    }
}
