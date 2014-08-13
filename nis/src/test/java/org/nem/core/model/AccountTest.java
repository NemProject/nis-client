package org.nem.core.model;

import net.minidev.json.JSONObject;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.messages.PlainMessage;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.*;
import org.nem.core.test.*;

import java.math.BigInteger;

public class AccountTest {

	//region Constructor

	@Test
	public void accountCanBeCreatedAroundKeyPair() {
		// Arrange:
		final KeyPair kp = new KeyPair();
		final Address expectedAccountId = Address.fromPublicKey(kp.getPublicKey());
		final Account account = new Account(kp);

		// Assert:
		Assert.assertThat(account.getKeyPair(), IsEqual.equalTo(kp));
		Assert.assertThat(account.getAddress(), IsEqual.equalTo(expectedAccountId));
		Assert.assertThat(account.getBalance(), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(account.getForagedBlocks(), IsEqual.equalTo(BlockAmount.ZERO));
		Assert.assertThat(account.getMessages().size(), IsEqual.equalTo(0));
		Assert.assertThat(account.getLabel(), IsNull.nullValue());
	}

	@Test
	public void accountCanBeCreatedAroundAddressWithoutPublicKey() {
		// Arrange:
		final Address expectedAccountId = Utils.generateRandomAddress();
		final Account account = new Account(expectedAccountId);

		// Assert:
		Assert.assertThat(account.getKeyPair(), IsNull.nullValue());
		Assert.assertThat(account.getAddress(), IsEqual.equalTo(expectedAccountId));
		Assert.assertThat(account.getBalance(), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(account.getForagedBlocks(), IsEqual.equalTo(BlockAmount.ZERO));
		Assert.assertThat(account.getMessages().size(), IsEqual.equalTo(0));
		Assert.assertThat(account.getLabel(), IsNull.nullValue());
	}

	@Test
	public void accountCanBeCreatedAroundAddressWithPublicKey() {
		// Arrange:
		final PublicKey publicKey = new KeyPair().getPublicKey();
		final Address expectedAccountId = Address.fromPublicKey(publicKey);
		final Account account = new Account(expectedAccountId);

		// Assert:
		Assert.assertThat(account.getKeyPair().hasPrivateKey(), IsEqual.equalTo(false));
		Assert.assertThat(account.getKeyPair().getPublicKey(), IsEqual.equalTo(publicKey));
		Assert.assertThat(account.getAddress(), IsEqual.equalTo(expectedAccountId));
		Assert.assertThat(account.getBalance(), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(account.getForagedBlocks(), IsEqual.equalTo(BlockAmount.ZERO));
		Assert.assertThat(account.getMessages().size(), IsEqual.equalTo(0));
		Assert.assertThat(account.getLabel(), IsNull.nullValue());
	}

	@Test
	public void accountCanBeCreatedAroundAccountInformation() {
		// Arrange:
		final Address expectedAccountId = Utils.generateRandomAddressWithPublicKey();
		final Account account = new Account(expectedAccountId, Amount.fromNem(124), new BlockAmount(4), "blah");

		// Assert:
		Assert.assertThat(account.getKeyPair().hasPublicKey(), IsEqual.equalTo(true));
		Assert.assertThat(account.getAddress(), IsEqual.equalTo(expectedAccountId));
		Assert.assertThat(account.getBalance(), IsEqual.equalTo(Amount.fromNem(124)));
		Assert.assertThat(account.getForagedBlocks(), IsEqual.equalTo(new BlockAmount(4)));
		Assert.assertThat(account.getLabel(), IsEqual.equalTo("blah"));
		Assert.assertThat(account.getMessages().size(), IsEqual.equalTo(0));
	}

	//endregion

	//region setPublicKey

	@Test(expected = IllegalArgumentException.class)
	public void inconsistentPublicKeyCannotBeSet() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final PublicKey publicKey = Utils.generateRandomPublicKey();

		// Act: the set fails because the public key is not consistent with the account's address
		account.setPublicKey(publicKey);
	}

	@Test
	public void consistentPublicKeyCanBeSet() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final Address address = Address.fromEncoded(Address.fromPublicKey(keyPair.getPublicKey()).getEncoded());
		final Account account = new Account(address);

		// Act:
		account.setPublicKey(keyPair.getPublicKey());

		// Assert:
		Assert.assertThat(account.getKeyPair().hasPublicKey(), IsEqual.equalTo(true));
		Assert.assertThat(account.getKeyPair().getPublicKey(), IsEqual.equalTo(keyPair.getPublicKey()));
	}

	@Test
	public void settingConsistentPublicKeyDoesNotOverwritePrivateKey() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final Account account = new Account(keyPair);

		// Act:
		account.setPublicKey(keyPair.getPublicKey());

		// Assert:
		Assert.assertThat(account.getKeyPair().hasPublicKey(), IsEqual.equalTo(true));
		Assert.assertThat(account.getKeyPair().getPublicKey(), IsEqual.equalTo(keyPair.getPublicKey()));
		Assert.assertThat(account.getKeyPair().hasPrivateKey(), IsEqual.equalTo(true));
		Assert.assertThat(account.getKeyPair().getPrivateKey(), IsEqual.equalTo(keyPair.getPrivateKey()));
	}

	//endregion

	//region Label

	@Test
	public void labelCanBeSet() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();

		// Act:
		account.setLabel("Beta Gamma");

		// Assert:
		Assert.assertThat(account.getLabel(), IsEqual.equalTo("Beta Gamma"));
	}

	//endregion

	//region Balance

	@Test
	public void balanceCanBeIncremented() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();

		// Act:
		account.incrementBalance(new Amount(7));

		// Assert:
		Assert.assertThat(account.getBalance(), IsEqual.equalTo(new Amount(7)));
	}

	@Test
	public void balanceCanBeDecremented() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();

		// Act:
		account.incrementBalance(new Amount(100));
		account.decrementBalance(new Amount(12));

		// Assert:
		Assert.assertThat(account.getBalance(), IsEqual.equalTo(new Amount(88)));
	}

	@Test
	public void balanceCanBeIncrementedAndDecrementedMultipleTimes() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();

		// Act:
		account.incrementBalance(new Amount(100));
		account.decrementBalance(new Amount(12));
		account.incrementBalance(new Amount(22));
		account.decrementBalance(new Amount(25));

		// Assert:
		Assert.assertThat(account.getBalance(), IsEqual.equalTo(new Amount(85)));
	}

	//endregion

	//region refCount

	@Test
	public void referenceCountIsZeroForNewAccount() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();

		// Assert:
		Assert.assertThat(account.getReferenceCount(), IsEqual.equalTo(new ReferenceCount(0)));
	}

	@Test
	public void referenceCountCanBeIncremented() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();

		// Act:
		final ReferenceCount result = account.incrementReferenceCount();

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(new ReferenceCount(1)));
		Assert.assertThat(account.getReferenceCount(), IsEqual.equalTo(new ReferenceCount(1)));
	}

	@Test
	public void referenceCountCanBeDecremented() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		account.incrementReferenceCount();

		// Act:
		final ReferenceCount result = account.decrementReferenceCount();

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(new ReferenceCount(0)));
		Assert.assertThat(account.getReferenceCount(), IsEqual.equalTo(new ReferenceCount(0)));
	}

	//endregion

	//region foraged blocks

	@Test
	public void foragedBlocksCanBeIncremented() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();

		// Act:
		account.incrementForagedBlocks();
		account.incrementForagedBlocks();

		// Assert:
		Assert.assertThat(account.getForagedBlocks(), IsEqual.equalTo(new BlockAmount(2)));
	}

	@Test
	public void foragedBlocksCanBeDecremented() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();

		// Act:
		account.incrementForagedBlocks();
		account.incrementForagedBlocks();
		account.decrementForagedBlocks();

		// Assert:
		Assert.assertThat(account.getForagedBlocks(), IsEqual.equalTo(new BlockAmount(1)));
	}

	@Test
	public void foragedBlocksCanBeIncrementedAndDecrementedMultipleTimes() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();

		// Act:
		account.incrementForagedBlocks();
		account.incrementForagedBlocks();
		account.decrementForagedBlocks();
		account.incrementForagedBlocks();
		account.incrementForagedBlocks();
		account.decrementForagedBlocks();

		// Assert:
		Assert.assertThat(account.getForagedBlocks(), IsEqual.equalTo(new BlockAmount(2)));
	}

	//endregion

	//region Message

	@Test
	public void singleMessageCanBeAdded() {
		// Arrange:
		final byte[] input = Utils.generateRandomBytes();
		final Account account = new Account(new KeyPair());

		// Act:
		account.addMessage(new PlainMessage(input));

		// Assert:
		Assert.assertThat(account.getMessages().size(), IsEqual.equalTo(1));
		Assert.assertThat(account.getMessages().get(0).getDecodedPayload(), IsEqual.equalTo(input));
	}

	@Test
	public void multipleMessagesCanBeAdded() {
		// Arrange:
		final byte[] input1 = Utils.generateRandomBytes();
		final byte[] input2 = Utils.generateRandomBytes();
		final Account account = new Account(new KeyPair());

		// Act:
		account.addMessage(new PlainMessage(input1));
		account.addMessage(new PlainMessage(input2));

		// Assert:
		Assert.assertThat(account.getMessages().size(), IsEqual.equalTo(2));
		Assert.assertThat(account.getMessages().get(0).getDecodedPayload(), IsEqual.equalTo(input1));
		Assert.assertThat(account.getMessages().get(1).getDecodedPayload(), IsEqual.equalTo(input2));
	}

	@Test
	public void messageCanBeRemoved() {
		// Arrange:
		final byte[] input1 = Utils.generateRandomBytes();
		final byte[] input2 = Utils.generateRandomBytes();
		final Account account = new Account(new KeyPair());

		// Act:
		account.addMessage(new PlainMessage(input1));
		account.addMessage(new PlainMessage(input2));
		account.removeMessage(new PlainMessage(input1));

		// Assert:
		Assert.assertThat(account.getMessages().size(), IsEqual.equalTo(1));
		Assert.assertThat(account.getMessages().get(0).getDecodedPayload(), IsEqual.equalTo(input2));
	}

	@Test
	public void lastMatchingMessageIsRemoved() {
		// Arrange:
		final byte[] input1 = Utils.generateRandomBytes();
		final byte[] input2 = Utils.generateRandomBytes();
		final Account account = new Account(new KeyPair());

		// Act:
		account.addMessage(new PlainMessage(input1));
		account.addMessage(new PlainMessage(input2));
		account.addMessage(new PlainMessage(input1));
		account.addMessage(new PlainMessage(input2));
		account.removeMessage(new PlainMessage(input1));

		// Assert:
		Assert.assertThat(account.getMessages().size(), IsEqual.equalTo(3));
		Assert.assertThat(account.getMessages().get(0).getDecodedPayload(), IsEqual.equalTo(input1));
		Assert.assertThat(account.getMessages().get(1).getDecodedPayload(), IsEqual.equalTo(input2));
		Assert.assertThat(account.getMessages().get(2).getDecodedPayload(), IsEqual.equalTo(input2));
	}

	@Test
	public void nothingHappensIfMessageNotAssociatedWithAccountIsRemoved() {
		// Arrange:
		final byte[] input1 = Utils.generateRandomBytes();
		final byte[] input2 = Utils.generateRandomBytes();
		final Account account = new Account(new KeyPair());

		// Act:
		account.addMessage(new PlainMessage(input1));
		account.removeMessage(new PlainMessage(input2));

		// Assert:
		Assert.assertThat(account.getMessages().size(), IsEqual.equalTo(1));
		Assert.assertThat(account.getMessages().get(0).getDecodedPayload(), IsEqual.equalTo(input1));
	}

	//endregion

	//region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final KeyPair kp = new KeyPair();
		final Account account = new Account(kp);

		// Assert:
		for (final Account account2 : createEquivalentAccounts(kp)) {
			Assert.assertThat(account2, IsEqual.equalTo(account));
		}

		for (final Account account2 : createNonEquivalentAccounts(kp)) {
			Assert.assertThat(account2, IsNot.not(IsEqual.equalTo(account)));
		}

		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(account)));
		Assert.assertThat(new BigInteger("1235"), IsNot.not(IsEqual.equalTo((Object)account)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final KeyPair kp = new KeyPair();
		final Account account = new Account(kp);
		final int hashCode = account.hashCode();

		// Assert:
		for (final Account account2 : createEquivalentAccounts(kp)) {
			Assert.assertThat(account2.hashCode(), IsEqual.equalTo(hashCode));
		}

		for (final Account account2 : createNonEquivalentAccounts(kp)) {
			Assert.assertThat(account2.hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		}
	}

	private static Account[] createEquivalentAccounts(final KeyPair keyPair) {
		return new Account[] {
				new Account(keyPair),
				new Account(new KeyPair(keyPair.getPublicKey())),
				new Account(new KeyPair(keyPair.getPrivateKey()))
		};
	}

	private static Account[] createNonEquivalentAccounts(final KeyPair keyPair) {
		return new Account[] {
				Utils.generateRandomAccount(),
				new Account(new KeyPair(Utils.mutate(keyPair.getPublicKey()))),
				new Account(new KeyPair(Utils.mutate(keyPair.getPrivateKey())))
		};
	}

	//endregion

	//region inline serialization

	@Test
	public void canWriteAccountWithDefaultEncoding() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final Address address = Address.fromEncoded("MockAcc");

		// Act:
		Account.writeTo(serializer, "Account", new Account(address));

		// Assert:
		final JSONObject object = serializer.getObject();
		Assert.assertThat(object.size(), IsEqual.equalTo(1));
		Assert.assertThat(object.get("Account"), IsEqual.equalTo(address.getEncoded()));
	}

	@Test
	public void canWriteAccountWithAddressEncoding() {
		// Arrange:
		final Address address = Address.fromEncoded("MockAcc");

		// Assert:
		assertCanWriteAccountWithEncoding(
				new Account(address),
				AddressEncoding.COMPRESSED,
				address.getEncoded());
	}

	@Test
	public void canWriteAccountWithPublicKeyEncoding() {
		// Arrange:
		final KeyPair kp = new KeyPair();

		// Assert:
		assertCanWriteAccountWithEncoding(
				new Account(kp),
				AddressEncoding.PUBLIC_KEY,
				kp.getPublicKey().toString());
	}

	@Test
	public void canWriteAccountThatDoesNotHavePublicKeyWithPublicKeyEncoding() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();

		// Assert:
		assertCanWriteAccountWithEncoding(
				new Account(address),
				AddressEncoding.PUBLIC_KEY,
				null);
	}

	private static void assertCanWriteAccountWithEncoding(
			final Account account,
			final AddressEncoding encoding,
			final String expectedSerializedString) {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();

		// Act:
		Account.writeTo(serializer, "Account", account, encoding);

		// Assert:
		final JSONObject object = serializer.getObject();
		Assert.assertThat(object.size(), IsEqual.equalTo(1));
		Assert.assertThat(object.get("Account"), IsEqual.equalTo(expectedSerializedString));
	}

	@Test
	public void canRoundtripAccountWithDefaultEncoding() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final Address address = Address.fromEncoded("MockAcc");
		final MockAccountLookup accountLookup = new MockAccountLookup();

		// Act:
		Account.writeTo(serializer, "Account", new Account(address));

		final JsonDeserializer deserializer = new JsonDeserializer(
				serializer.getObject(),
				new DeserializationContext(accountLookup));
		final Account account = Account.readFrom(deserializer, "Account");

		// Assert:
		Assert.assertThat(account.getAddress(), IsEqual.equalTo(address));
		Assert.assertThat(accountLookup.getNumFindByIdCalls(), IsEqual.equalTo(1));
	}

	@Test
	public void canRoundtripAccountWithAddressEncoding() {
		// Assert:
		this.assertAccountRoundTripInMode(AddressEncoding.COMPRESSED);
	}

	@Test
	public void canRoundtripAccountWithPublicKeyEncoding() {
		// Assert:
		this.assertAccountRoundTripInMode(AddressEncoding.PUBLIC_KEY);
	}

	private void assertAccountRoundTripInMode(final AddressEncoding encoding) {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final Account originalAccount = Utils.generateRandomAccountWithoutPrivateKey();
		final MockAccountLookup accountLookup = new MockAccountLookup();

		// Act:
		Account.writeTo(serializer, "Account", originalAccount, encoding);

		final JsonDeserializer deserializer = new JsonDeserializer(
				serializer.getObject(),
				new DeserializationContext(accountLookup));
		final Account account = Account.readFrom(deserializer, "Account", encoding);

		// Assert:
		Assert.assertThat(account.getAddress(), IsEqual.equalTo(originalAccount.getAddress()));
		Assert.assertThat(accountLookup.getNumFindByIdCalls(), IsEqual.equalTo(1));
	}

	//endregion

	//region copy

	@Test
	public void copyCreatesUnlinkedCopyOfAccountWithoutPublicKey() {
		// Arrange:
		final Account account = new Account(Utils.generateRandomAddress());

		// Assert:
		final Account copyAccount = assertCopyCreatesUnlinkedAccount(account);
		Assert.assertThat(copyAccount.getAddress().getEncoded(), IsNull.notNullValue());
		Assert.assertThat(copyAccount.getAddress().getPublicKey(), IsNull.nullValue());
		Assert.assertThat(copyAccount.getKeyPair(), IsNull.nullValue());
	}

	@Test
	public void copyCreatesUnlinkedCopyOfAccountWithPublicKey() {
		// Arrange:
		final Account account = new Account(Utils.generateRandomAddressWithPublicKey());

		// Assert:
		final Account copyAccount = assertCopyCreatesUnlinkedAccount(account);
		Assert.assertThat(copyAccount.getAddress().getEncoded(), IsNull.notNullValue());
		Assert.assertThat(copyAccount.getAddress().getPublicKey(), IsNull.notNullValue());
		Assert.assertThat(copyAccount.getKeyPair().getPublicKey(), IsNull.notNullValue());
		Assert.assertThat(copyAccount.getKeyPair().getPrivateKey(), IsNull.nullValue());
	}

	@Test
	public void copyCreatesUnlinkedCopyOfAccountWithPrivateKey() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();

		// Assert:
		final Account copyAccount = assertCopyCreatesUnlinkedAccount(account);
		Assert.assertThat(copyAccount.getAddress().getEncoded(), IsNull.notNullValue());
		Assert.assertThat(copyAccount.getAddress().getPublicKey(), IsNull.notNullValue());
		Assert.assertThat(copyAccount.getKeyPair().getPublicKey(), IsNull.notNullValue());
		Assert.assertThat(copyAccount.getKeyPair().getPrivateKey(), IsNull.notNullValue());
	}

	@Test
	public void copyCreatesUnlinkedCopyOfMessages() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		account.addMessage(new PlainMessage(new byte[] { 1, 2, 3 }));
		account.addMessage(new PlainMessage(new byte[] { 7, 9, 8 }));

		// Act:
		final Account copyAccount = account.copy();

		// Assert:
		Assert.assertThat(copyAccount.getMessages(), IsNot.not(IsSame.sameInstance(account.getMessages())));
		Assert.assertThat(copyAccount.getMessages().size(), IsEqual.equalTo(2));
		Assert.assertThat(getEncodedMessageAt(copyAccount, 0), IsEqual.equalTo(new byte[] { 1, 2, 3 }));
		Assert.assertThat(getEncodedMessageAt(copyAccount, 1), IsEqual.equalTo(new byte[] { 7, 9, 8 }));
	}

	public static Account assertCopyCreatesUnlinkedAccount(final Account account) {
		// Arrange:
		setAccountValuesForCopyTests(account);

		// Act:
		final Account copyAccount = account.copy();

		// Assert:
		Assert.assertThat(copyAccount.getAddress(), IsEqual.equalTo(account.getAddress()));
		Assert.assertThat(copyAccount.getAddress().getPublicKey(), IsEqual.equalTo(account.getAddress().getPublicKey()));
		assertKeyPairsAreEquivalent(copyAccount.getKeyPair(), account.getKeyPair());

		Assert.assertThat(copyAccount.getBalance(), IsEqual.equalTo(new Amount(1000)));
		Assert.assertThat(copyAccount.getForagedBlocks(), IsEqual.equalTo(new BlockAmount(3)));
		Assert.assertThat(copyAccount.getLabel(), IsEqual.equalTo("Alpha Sigma"));
		Assert.assertThat(copyAccount.getReferenceCount(), IsEqual.equalTo(new ReferenceCount(2)));

		// verify that the mutable objects are not the same
		Assert.assertThat(copyAccount.getMessages(), IsNot.not(IsSame.sameInstance(account.getMessages())));
		return copyAccount;
	}

	private static void setAccountValuesForCopyTests(final Account account) {
		account.incrementBalance(new Amount(1000));
		account.incrementForagedBlocks();
		account.incrementForagedBlocks();
		account.incrementForagedBlocks();
		account.setLabel("Alpha Sigma");
		account.incrementReferenceCount();
		account.incrementReferenceCount();
		account.addMessage(new PlainMessage(new byte[] { 1, 2, 3 }));
		account.addMessage(new PlainMessage(new byte[] { 7, 9, 8 }));
	}

	private static void assertKeyPairsAreEquivalent(final KeyPair actual, final KeyPair expected) {
		if (null == actual || null == expected) {
			Assert.assertThat(actual, IsEqual.equalTo(expected));
		} else {
			Assert.assertThat(actual.getPublicKey(), IsEqual.equalTo(expected.getPublicKey()));
			Assert.assertThat(actual.getPrivateKey(), IsEqual.equalTo(expected.getPrivateKey()));
		}
	}

	private static byte[] getEncodedMessageAt(final Account account, final int index) {
		return account.getMessages().get(index).getEncodedPayload();
	}

	//endregion

	//region shallow copy

	@Test
	public void canCreateShallowCopyWithNewKeyPair() {
		// Arrange:
		final Account original = new Account(Utils.generateRandomAddress());
		setAccountValuesForCopyTests(original);
		final KeyPair keyPair = new KeyPair();

		// Act:
		final Account copy = original.shallowCopyWithKeyPair(keyPair);

		// Assert:
		Assert.assertThat(copy.getAddress(), IsEqual.equalTo(Address.fromPublicKey(keyPair.getPublicKey())));
		assertKeyPairsAreEquivalent(copy.getKeyPair(), keyPair);
		assertShallowCopy(original, copy);
	}

	private static void assertShallowCopy(final Account original, final Account copy) {
		// Assert:
		Assert.assertThat(copy.getBalance(), IsEqual.equalTo(original.getBalance()));
		Assert.assertThat(copy.getForagedBlocks(), IsEqual.equalTo(original.getForagedBlocks()));
		Assert.assertThat(copy.getLabel(), IsEqual.equalTo(original.getLabel()));
		Assert.assertThat(copy.getReferenceCount(), IsEqual.equalTo(original.getReferenceCount()));
		Assert.assertThat(copy.getMessages(), IsSame.sameInstance(original.getMessages()));
	}

	//endregion
}
