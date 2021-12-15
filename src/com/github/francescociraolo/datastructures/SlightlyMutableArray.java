package com.github.francescociraolo.datastructures;

import java.util.AbstractList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("unchecked")
public class SlightlyMutableArray<T> extends AbstractList<T> {

    private Object[] elements;
    private final Supplier<T> emptySupplier;

    public SlightlyMutableArray(int size) {
        this(size, null);
    }

    public SlightlyMutableArray(int size, Supplier<T> emptySupplier) {
        this.elements = new Object[size];
        this.emptySupplier = emptySupplier;
        if (emptySupplier != null)
            for (int i = 0; i < size; i++)
                elements[i] = emptySupplier.get();
    }

    private void checkSize(int index) {
        if (index >= elements.length) {
            var tmp = elements;
            elements = new Object[index + 1];
            System.arraycopy(tmp, 0, elements, 0, tmp.length);
            if (emptySupplier != null)
                for (int i = tmp.length; i < elements.length; i++)
                    elements[i] = emptySupplier.get();
        }
    }

    public void replace(int index, T element) {
        checkSize(index);
        elements[index] = element;
    }

    @Override
    public T get(int index) {
        checkSize(index);
        return (T) elements[index];
    }

    @Override
    public int size() {
        return elements.length;
    }
}
