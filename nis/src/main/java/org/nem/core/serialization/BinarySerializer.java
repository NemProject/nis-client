package org.nem.core.serialization;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;

public class BinarySerializer implements AutoCloseable, Serializer {

    private final ByteArrayOutputStream stream;

    public BinarySerializer() throws Exception {
        this.stream = new ByteArrayOutputStream();
    }

    @Override
    public void writeInt(final int i) throws Exception {
        byte[] bytes = {
            (byte)(i & 0xFF),
            (byte)((i >> 8) & 0xFF),
            (byte)((i >> 16) & 0xFF),
            (byte)((i >> 24) & 0xFF),
        };
        this.writeBytesInternal(bytes);
    }

    @Override
    public void writeLong(final long l) throws Exception {
        this.writeInt((int)l);
        this.writeInt((int)(l >> 32));
    }

    @Override
    public void writeBigInteger(final BigInteger i) throws Exception {
        this.writeBytes(i.toByteArray());
    }

    @Override
    public void writeBytes(final byte[] bytes) throws Exception {
        this.writeInt(bytes.length);
        this.writeBytesInternal(bytes);
    }

    @Override
    public void writeString(final String s) throws Exception {
        this.writeBytes(s.getBytes("UTF-8"));
    }

    @Override
    public void close() throws Exception {
        this.stream.close();
    }

    public byte[] getBytes() {
        return this.stream.toByteArray();
    }

    private void writeBytesInternal(final byte[] bytes) {
        this.stream.write(bytes, 0, bytes.length);
    }
}
