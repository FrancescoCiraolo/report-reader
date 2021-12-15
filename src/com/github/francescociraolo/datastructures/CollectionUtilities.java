package com.github.francescociraolo.datastructures;

import java.util.Map;
import java.util.Set;
import java.util.function.BinaryOperator;

public class CollectionUtilities {

    public static <K, V> Map<K, V> mergeMaps(Map<K, V> m1, Map<K, V> m2, BinaryOperator<V> operator) {

        for (var k2 : m2.keySet())
            if (m1.containsKey(k2))
                m1.put(k2, operator.apply(m1.get(k2), m2.get(k2)));
            else
                m1.put(k2, m2.get(k2));

        return m1;
    }

    public static <T> Set<T> mergeSets(Set<T> s1, Set<T> s2) {
        if (s1.isEmpty())
            return s2;

        s1.addAll(s2);
        return s1;
    }
}
