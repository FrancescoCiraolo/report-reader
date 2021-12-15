package com.github.francescociraolo.datastructures;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class GroupedCollection<T, K extends Comparable<K>, C extends Collection<T>> implements Collection<T> {

    private final Map<K, C> collectionMap;

    private final Supplier<C> collectionSupplier;
    private final Function<T, K> keyExtractor;

    public GroupedCollection(Supplier<C> collectionSupplier, Function<T, K> keyExtractor) {
        this.collectionMap = new HashMap<>();
        this.collectionSupplier = collectionSupplier;
        this.keyExtractor = keyExtractor;
    }

    public C getCollection(K key) {
        return collectionMap.get(key);
    }

    @Override
    public int size() {
        return collectionMap.values().stream().mapToInt(Collection::size).sum();
    }

    @Override
    public boolean isEmpty() {
        return collectionMap.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return collectionMap.values().stream().anyMatch(c -> c.contains(o));
    }

    @Override
    public Iterator<T> iterator() {
        return stream().iterator();
    }

    @Override
    public Object[] toArray() {
        //noinspection SimplifyStreamApiCallChains
        return stream().toArray();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T1> T1[] toArray(T1[] a) {
        var size = size();
        var elementData = toArray();
        if (a.length < size)
            // Make a new array of a's runtime type, but my contents:
            return (T1[]) Arrays.copyOf(elementData, size, a.getClass());
        //noinspection SuspiciousSystemArraycopy
        System.arraycopy(elementData, 0, a, 0, size);
        if (a.length > size)
            a[size] = null;
        return a;
    }

    @Override
    public boolean add(T t) {
        var k = keyExtractor.apply(t);
        if (!collectionMap.containsKey(k))
            collectionMap.put(k, collectionSupplier.get());
        return collectionMap.get(k).add(t);
    }

    @Override
    public boolean remove(Object o) {
        return collectionMap.values().stream().anyMatch(c -> c.remove(o));
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        //noinspection SuspiciousMethodCalls
        return c.parallelStream().allMatch(e -> collectionMap.values().stream().anyMatch(c1 -> c1.contains(e)));
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        var res = false;
        for (var e : c) {
            res |= add(e);
        }
        return res;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return collectionMap.values().stream().anyMatch(c1 -> c1.removeAll(c));
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return collectionMap.values().stream().anyMatch(c1 -> c1.retainAll(c));
    }

    @Override
    public void clear() {
        collectionMap.values().forEach(Collection::clear);
        collectionMap.clear();
    }

    @Override
    public Stream<T> stream() {
        return collectionMap
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue).flatMap(Collection::stream);
    }
}
