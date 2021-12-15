package com.github.francescociraolo.trace;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class Trace {

    private TraceTokenizer tokenizer;
    private TraceTokenAnalyzer analyzer;

    public static class SimpleTraceTokenizer implements TraceTokenizer {

        private static final int BUFFER_SIZE = 16384;

        private final BufferedInputStream inputStream;
        private final StringBuilder stringBuilder;
        private int nextChar;

        public SimpleTraceTokenizer(InputStream inputStream) {
            this(new BufferedInputStream(inputStream, BUFFER_SIZE));
        }

        public SimpleTraceTokenizer(BufferedInputStream inputStream) {
            this.inputStream = inputStream;
            stringBuilder = new StringBuilder();
        }

        @Override
        public void close() throws IOException {
            inputStream.close();
        }

        @Override
        public boolean hasNext() {
            return nextChar != -1;
        }

        @Override
        public synchronized String next() throws IOException {
            if (nextChar == -1) throw new EOFException();

            for (boolean newBlock = false, newLine = false; step() && !newBlock; ) {
                if (newLine) {
                    if (!(newBlock = nextChar == ' ')) stringBuilder.append('\n');
                    newLine = false;
                }

                if (!newBlock && !(newLine = nextChar == '\n'))
                    stringBuilder.append((char) nextChar);
            }

            String token = stringBuilder.toString();
            stringBuilder.setLength(0);
            return token;
        }

        private boolean step() throws IOException {
            nextChar = inputStream.read();
            return nextChar != -1;
        }
    }
}
