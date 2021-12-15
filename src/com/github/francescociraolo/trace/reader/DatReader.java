package com.github.francescociraolo.trace.reader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Arrays;

class DatReader extends InputStream {

    private final Process process;
    private final InputStream inputStream;

    public DatReader(Path datFilePath) throws IOException {
        var cmd = new String[]{"trace-cmd", "report", datFilePath.toString()};
        System.out.println(Arrays.toString(cmd));
        this.process = Runtime.getRuntime().exec(cmd);

        this.inputStream = process.getInputStream();
    }

    @Override
    public int read() throws IOException {
        return inputStream.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return inputStream.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return inputStream.read(b, off, len);
    }

    @Override
    public byte[] readAllBytes() throws IOException {
        return inputStream.readAllBytes();
    }

    @Override
    public byte[] readNBytes(int len) throws IOException {
        return inputStream.readNBytes(len);
    }

    @Override
    public int readNBytes(byte[] b, int off, int len) throws IOException {
        return inputStream.readNBytes(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return inputStream.skip(n);
    }

    @Override
    public int available() throws IOException {
        return inputStream.available();
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
        process.destroy();
    }

    @Override
    public void mark(int readlimit) {
        inputStream.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
        inputStream.reset();
    }

    @Override
    public boolean markSupported() {
        return inputStream.markSupported();
    }

    @Override
    public long transferTo(OutputStream out) throws IOException {
        return inputStream.transferTo(out);
    }
}
