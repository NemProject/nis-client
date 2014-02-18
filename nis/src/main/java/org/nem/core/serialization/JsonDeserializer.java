package org.nem.core.serialization;

import org.bouncycastle.util.encoders.Base64;
import org.json.*;

import java.math.BigInteger;

public class JsonDeserializer implements Deserializer {

    private final JSONObject object;
    private int counter;

    public JsonDeserializer(final JSONObject object) throws Exception {
        this.object = object;
        this.counter = 1;
    }

    @Override
    public int readInt() throws Exception {
        return this.object.getInt(this.getNextKey());
    }

    @Override
    public long readLong() throws Exception {
        return this.object.getLong(this.getNextKey());
    }

    @Override
    public BigInteger readBigInteger() throws Exception {
        final byte[] bytes = this.readBytes();
        return new BigInteger(bytes);
    }

    @Override
    public byte[] readBytes() throws Exception {
        final String s = this.readString();
        return Base64.decode(s.getBytes("UTF-8"));
    }

    @Override
    public String readString() throws Exception {
        return this.object.getString(this.getNextKey());
    }

    private String getNextKey() {
        final String key = String.format("%d", this.counter);
        ++this.counter;
        return key;
    }
}
