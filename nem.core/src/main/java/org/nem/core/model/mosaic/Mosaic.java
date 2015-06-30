package org.nem.core.model.mosaic;

import org.nem.core.model.*;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.serialization.*;

import java.util.*;

/**
 * Class defining a mosaic.
 */
public class Mosaic implements SerializableEntity {
	private final Account creator;
	private final MosaicProperties properties;
	private final List<Mosaic> children;

	/**
	 * Creates a new mosaic.
	 *
	 * @param creator The creator of the mosaic.
	 * @param properties The properties of the mosaic.
	 */
	public Mosaic(final Account creator, final Properties properties) {
		this(creator, new MosaicPropertiesImpl(properties));
	}

	/**
	 * Creates a new mosaic.
	 *
	 * @param creator The creator of the mosaic.
	 * @param properties The mosaic properties.
	 */
	public Mosaic(final Account creator, final MosaicProperties properties) {
		if (null == creator) {
			throw new IllegalArgumentException("creator of the mosaic cannot be null");
		}

		if (null == properties) {
			throw new IllegalArgumentException("properties of the mosaic cannot be null");
		}

		this.creator = creator;
		this.properties = properties;
		this.children = Collections.emptyList();
	}

	/**
	 * Deserializes a mosaic.
	 *
	 * @param deserializer The deserializer.
	 */
	public Mosaic(final Deserializer deserializer) {
		this.creator = Account.readFrom(deserializer, "creator", AddressEncoding.PUBLIC_KEY);
		this.properties = new MosaicPropertiesImpl(deserializer.readObjectArray("properties", NemProperty::new));
		this.children = deserializer.readObjectArray("children", Mosaic::new);
	}

	/**
	 * Gets the creator of the mosaic.
	 *
	 * @return the creator.
	 */
	public Account getCreator() {
		return this.creator;
	}

	public List<Mosaic> getChildren() {
		return this.children;
	}

	/**
	 * Gets the mosaic's name.
	 *
	 * @return The name.
	 */
	public String getName() {
		return this.properties.getName();
	}

	/**
	 * Gets the mosaic's description.
	 *
	 * @return The description.
	 */
	public String getDescription() {
		return this.properties.getDescription();
	}

	/**
	 * Gets the number of decimal places up to which the mosaic instance can be partitioned.
	 *
	 * @return The divisibility.
	 */
	public int getDivisibility() {
		return this.properties.getDivisibility();
	}

	/**
	 * Gets the underlying namespace id.
	 *
	 * @return The namespace id.
	 */
	public NamespaceId getNamespaceId() {
		return this.properties.getNamespaceId();
	}

	/**
	 * Gets a value indicating whether or not the quantity is mutable.
	 *
	 * @return true if the quantity is mutable, false otherwise.
	 */
	public boolean isQuantityMutable() {
		return this.properties.isQuantityMutable();
	}

	/**
	 * Gets a value indicating whether or not the the mosaic can be transferred between accounts different from the creator.
	 *
	 * @return true if it can be transferred, false otherwise.
	 */
	public boolean isTransferable() {
		return this.properties.isTransferable();
	}

	/**
	 * Gets the properties as collection.
	 *
	 * @return The collection of nem properties.
	 */
	public Collection<NemProperty> getProperties() {
		return this.properties.asCollection();
	}

	@Override
	public void serialize(final Serializer serializer) {
		Account.writeTo(serializer, "creator", this.creator, AddressEncoding.PUBLIC_KEY);
		serializer.writeObjectArray("properties", this.properties.asCollection());
		serializer.writeObjectArray("children", this.children);
	}
}
