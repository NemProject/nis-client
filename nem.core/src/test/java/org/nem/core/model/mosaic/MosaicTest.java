package org.nem.core.model.mosaic;

import net.minidev.json.*;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.Account;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.primitive.GenericAmount;
import org.nem.core.serialization.*;
import org.nem.core.test.*;

import java.util.*;

public class MosaicTest {

	// region ctor

	@Test
	public void canCreateMosaicFromValidParameters() {
		// Arrange:
		final Account creator = Utils.generateRandomAccount();
		final MosaicProperties properties = Utils.createMosaicProperties();

		// Act:
		final Mosaic mosaic = createMosaic(creator, properties);

		// Assert:
		assertMosaicProperties(mosaic, creator, properties);
	}

	@Test
	public void cannotCreateMosaicWithNullParameters() {
		// Assert:
		Arrays.asList("creator", "id", "description", "amount", "properties")
				.forEach(org.nem.core.model.mosaic.MosaicTest::assertMosaicCannotBeCreatedWithNull);
	}

	private static void assertMosaicCannotBeCreatedWithNull(final String parameterName) {
		// Assert:
		ExceptionAssert.assertThrows(
				v -> new Mosaic(
						parameterName.equals("creator") ? null : Utils.generateRandomAccount(),
						parameterName.equals("id") ? null : new MosaicId(new NamespaceId("alice.vouchers"), "Alice's vouchers"),
						parameterName.equals("description") ? null : new MosaicDescriptor("precious vouchers"),
						parameterName.equals("amount") ? null : GenericAmount.fromValue(123),
						parameterName.equals("properties") ? null : Utils.createMosaicProperties()),
				IllegalArgumentException.class,
				ex -> ex.getMessage().contains(parameterName));
	}

	@Test
	public void cannotCreateMosaicWithZeroAmount() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new Mosaic(
				Utils.generateRandomAccount(),
				new MosaicId(new NamespaceId("alice.vouchers"), "Alice's vouchers"),
				new MosaicDescriptor("precious vouchers"),
				GenericAmount.ZERO,
				Utils.createMosaicProperties()), IllegalArgumentException.class);
	}

	// endregion

	// region serialization

	@Test
	public void canRoundTripMosaic() {
		// Arrange:
		final Account creator = Utils.generateRandomAccount();
		final MosaicProperties properties = Utils.createMosaicProperties();
		final Mosaic original = createMosaic(creator, properties);

		// Act:
		final Mosaic mosaic = new Mosaic(Utils.roundtripSerializableEntity(original, new MockAccountLookup()));

		// Assert:
		assertMosaicProperties(mosaic, creator, properties);
	}

	@Test
	public void cannotDeserializeMosaicWithMissingRequiredParameter() {
		// Assert:
		Arrays.asList("creator", "id", "description", "amount", "properties", "children")
				.forEach(n -> assertCannotDeserialize(n, null, MissingRequiredPropertyException.class));
	}

	@Test
	public void cannotDeserializeMosaicWithZeroAmount() {
		// Assert:
		assertCannotDeserialize("amount", 0L, IllegalArgumentException.class);
	}

	@Test
	public void cannotDeserializeMosaicWithChildren() {
		// Arrange:
		final JSONArray jsonArray = new JSONArray();
		jsonArray.add(JsonSerializer.serializeToJson(createMosaic("id", "foo")));

		// Assert:
		assertCannotDeserialize("children", jsonArray, IllegalArgumentException.class);
	}

	private static void assertCannotDeserialize(final String key, final Object value, final Class expectedExceptionClass) {
		// Arrange:
		final Mosaic mosaic = Utils.createMosaic(Utils.generateRandomAccount());
		final JSONObject jsonObject = JsonSerializer.serializeToJson(mosaic);
		if (null == value) {
			jsonObject.remove(key);
		} else {
			jsonObject.put(key, value);
		}

		final JsonDeserializer deserializer = new JsonDeserializer(jsonObject, new DeserializationContext(new MockAccountLookup()));

		// Assert:
		ExceptionAssert.assertThrows(v -> new Mosaic(deserializer), expectedExceptionClass);
	}

	private static Mosaic createMosaic(final String namespaceId, final String name) {
		return new Mosaic(
				Utils.generateRandomAccount(),
				new MosaicId(new NamespaceId(namespaceId), name),
				new MosaicDescriptor("precious vouchers"),
				GenericAmount.fromValue(123),
				Utils.createMosaicProperties());
	}

	private static Mosaic createMosaic(final Account creator, final MosaicProperties properties) {
		return new Mosaic(
				creator,
				new MosaicId(new NamespaceId("alice.vouchers"), "Alice's vouchers"),
				new MosaicDescriptor("precious vouchers"),
				GenericAmount.fromValue(123),
				properties);
	}

	private static void assertMosaicProperties(final Mosaic mosaic, final Account creator, final MosaicProperties properties) {
		// Assert:
		Assert.assertThat(mosaic.getCreator(), IsEqual.equalTo(creator));
		Assert.assertThat(mosaic.getId(), IsEqual.equalTo(new MosaicId(new NamespaceId("alice.vouchers"), "Alice's vouchers")));
		Assert.assertThat(mosaic.getDescriptor(), IsEqual.equalTo(new MosaicDescriptor("precious vouchers")));
		Assert.assertThat(mosaic.getAmount(), IsEqual.equalTo(GenericAmount.fromValue(123)));
		Assert.assertThat(mosaic.getProperties().asCollection(), IsEquivalent.equivalentTo(properties.asCollection()));
		Assert.assertThat(mosaic.getChildren().isEmpty(), IsEqual.equalTo(true));
	}

	// endregion

	// region toString

	@Test
	public void toStringReturnAsteriskConcatenatedNamespaceIdAndMosaicId() {
		// Arrange:
		final Mosaic mosaic = createMosaic("alice.vouchers", "Alice's vouchers");

		// Act:
		final String uniqueId = mosaic.toString();

		// Assert:
		Assert.assertThat(uniqueId, IsEqual.equalTo("alice.vouchers * Alice's vouchers"));
	}

	// endregion

	// region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final Mosaic mosaic = createMosaicA("Alice's vouchers");

		// Assert:
		for (final Map.Entry<String, Mosaic> entry : createMosaicsForEqualityTests().entrySet()) {
			Assert.assertThat(
					entry.getValue(),
					isDiffExpected(entry.getKey()) ? IsNot.not(IsEqual.equalTo(mosaic)) : IsEqual.equalTo(mosaic));
		}

		Assert.assertThat(new Object(), IsNot.not(IsEqual.equalTo(mosaic)));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(mosaic)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final int hashCode = createMosaicA("Alice's vouchers").hashCode();

		// Assert:
		for (final Map.Entry<String, Mosaic> entry : createMosaicsForEqualityTests().entrySet()) {
			Assert.assertThat(
					entry.getValue().hashCode(),
					isDiffExpected(entry.getKey()) ? IsNot.not(IsEqual.equalTo(hashCode)) : IsEqual.equalTo(hashCode));
		}
	}

	private static Map<String, Mosaic> createMosaicsForEqualityTests() {
		return new HashMap<String, Mosaic>() {
			{
				this.put("default", createMosaicA("Alice's vouchers"));
				this.put("diff-id", createMosaicA("Bob's vouchers"));
				this.put("diff-id-case", createMosaicA("ALICE'S vouchers"));
				this.put("same-id-diff-everything", createMosaicB("Alice's vouchers"));
				this.put("diff-id-diff-everything", createMosaicB("Bob's vouchers"));
			}
		};
	}

	private static boolean isDiffExpected(final String propertyName) {
		switch (propertyName) {
			case "diff-id":
			case "diff-id-diff-everything":
				return true;
		}

		return false;
	}

	private static Mosaic createMosaicA(final String name) {
		return new Mosaic(
				Utils.generateRandomAccount(),
				new MosaicId(new NamespaceId("xyz"), name),
				new MosaicDescriptor("precious vouchers"),
				GenericAmount.fromValue(123),
				Utils.createMosaicProperties());
	}

	private static Mosaic createMosaicB(final String name) {
		return new Mosaic(
				Utils.generateRandomAccount(),
				new MosaicId(new NamespaceId("xyz"), name),
				new MosaicDescriptor("silver coins"),
				GenericAmount.fromValue(987),
				new MosaicPropertiesImpl(new Properties()));
	}

	// endregion
}
