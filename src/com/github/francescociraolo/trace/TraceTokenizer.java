package com.github.francescociraolo.trace;

import java.io.Closeable;
import java.io.IOException;

public interface TraceTokenizer extends Closeable {

    @Override
    void close() throws IOException;

    boolean hasNext();

    String next() throws IOException;
}
