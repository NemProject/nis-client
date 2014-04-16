package org.nem.core.serialization;

import org.nem.core.utils.StringEncoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Collection;

/**
 * A binary serializer that supports forward-only serialization.
 */
public class BinarySerializer implements AutoCloseable, Serializer {

	/**
	 * Sentinel value used to indicate that a serialized byte array should be deserialized as null.
	 */
	public static final int NULL_BYTES_SENTINEL_VALUE = 0xFFFFFFFF;

	private final ByteArrayOutputStream stream;

	/**
	 * Creates a new binary serializer.
	 */
	public BinarySerializer() {
		this.stream = new ByteArrayOutputStream();
	}

	@Override
	public void writeInt(final String label, final int i) {
		byte[] bytes = {
				(byte)(i & 0xFF),
				(byte)((i >> 8) & 0xFF),
				(byte)((i >> 16) & 0xFF),
				(byte)((i >> 24) & 0xFF),
		};
		this.writeBytesInternal(bytes);
	}

	@Override
	public void writeLong(final String label, final long l) {
		this.writeInt(null, (int)l);
		this.writeInt(null, (int)(l >> 32));
	}

	@Override
	public void writeBigInteger(final String label, final BigInteger i) {
		this.writeBytes(null, i.toByteArray());
	}

	@Override
	public void writeBytes(final String label, final byte[] bytes) {
		if (null == bytes) {
			this.writeInt(null, NULL_BYTES_SENTINEL_VALUE);
		}
		else {
			this.writeInt(null, bytes.length);
			this.writeBytesInternal(bytes);
		}
	}

	@Override
	public void writeString(final String label, final String s) {
		this.writeBytes(null, StringEncoder.getBytes(s));
	}

	@Override
	public void writeObject(final String label, final SerializableEntity object) {
		this.writeBytes(null, serializeObject(object));
	}

	@Override
	public void writeObjectArray(final String label, final Collection<? extends SerializableEntity> objects) {
		this.writeInt(null, objects.size());
		for (SerializableEntity object : objects)
			this.writeBytes(null, serializeObject(object));
	}

	@Override
	public void close() throws IOException {
		this.stream.close();
	}

	private static byte[] serializeObject(final SerializableEntity object) {
		if (null == object)
			return new byte[0];

		try {
			try (BinarySerializer serializer = new BinarySerializer()) {
				object.serialize(serializer);
				return serializer.getBytes();
			}
		} catch (Exception ex) {
			throw new SerializationException(ex);
		}
	}

	/**
	 * Gets the underlying byte buffer.
	 *
	 * @return The underlying byte buffer.
	 */
	public byte[] getBytes() {
		return this.stream.toByteArray();
	}

	private void writeBytesInternal(final byte[] bytes) {
		this.stream.write(bytes, 0, bytes.length);
	}

	/**
	 * Helper function that serializes a SerializableEntity to a byte array.
	 *
	 * @param entity The entity to serialize.
	 *
	 * @return The resulting byte array.
	 */
	public static byte[] serializeToBytes(final SerializableEntity entity) {
		try {
			try (BinarySerializer binarySerializer = new BinarySerializer()) {
				entity.serialize(binarySerializer);
				return binarySerializer.getBytes();
			}
		} catch (Exception e) {
			throw new SerializationException(e);
		}
	}
}
