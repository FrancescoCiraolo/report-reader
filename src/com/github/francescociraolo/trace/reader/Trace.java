package com.github.francescociraolo.trace.reader;

import com.github.francescociraolo.trace.Record;

import java.io.IOException;
import java.nio.file.Path;

public abstract class Trace {

    public abstract boolean hasNext();

    public abstract Record next() throws IOException;

    public void parseAll() throws IOException {
        while (hasNext())
            next();
    }

    public abstract int getCpusCount();

    public static Trace fromDatFile(Path datFilePath) throws IOException {
        return new AsciiTrace(new DatReader(datFilePath));
    }
}
