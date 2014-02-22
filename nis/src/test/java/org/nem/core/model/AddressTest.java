package org.nem.core.model;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.Utils;

public class AddressTest {

    @Test
    public void sameAddressIsGeneratedForSameInputs() {
        // Arrange:
        byte[] input = Utils.generateRandomBytes();

        // Act:
        Address address1 = Address.fromPublicKey(input);
        Address address2 = Address.fromPublicKey(input);

        // Assert:
        Assert.assertThat(address2, IsEqual.equalTo(address1));
    }

    @Test
    public void differentAddressesAreGeneratedForDifferentInputs() {

        // Arrange:
        byte[] input1 = Utils.generateRandomBytes();
        byte[] input2 = Utils.generateRandomBytes();

        // Act:
        Address address1 = Address.fromPublicKey(input1);
        Address address2 = Address.fromPublicKey(input2);

        // Assert:
        Assert.assertThat(address2, IsNot.not(IsEqual.equalTo(address1)));
    }

    @Test
    public void generatedAddressIsValid() {
        // Arrange:
        byte[] input = Utils.generateRandomBytes();

        // Act:
        Address address = Address.fromPublicKey(input);

        // Assert:
        Assert.assertThat(address.isValid(), IsEqual.equalTo(true));
    }

    @Test
    public void generatedAddressHas40CharLength() {
        // Arrange:
        byte[] input = Utils.generateRandomBytes();

        // Act:
        Address address = Address.fromPublicKey(input);

        // Assert:
        Assert.assertThat(address.getEncoded().length(), IsEqual.equalTo(40));
    }

    @Test
    public void generatedAddressBeginsWithN() {
        // Arrange:
        byte[] input = Utils.generateRandomBytes();

        // Act:
        Address address = Address.fromPublicKey(input);

        // Assert:
        Assert.assertThat(address.getEncoded().charAt(0), IsEqual.equalTo('N'));
    }

    @Test
    public void addressWithIncorrectLengthIsNotValid() {
        // Arrange:
        byte[] input = Utils.generateRandomBytes();

        // Act:
        Address address = Address.fromPublicKey(input);
        String realAddress = address.getEncoded();
        String fakeAddress = realAddress.substring(0, realAddress.length() - 1);

        // Assert:
        Assert.assertThat(Address.isValid(realAddress), IsEqual.equalTo(true));
        Assert.assertThat(Address.isValid(fakeAddress), IsEqual.equalTo(false));
    }

    @Test
    public void addressWithIncorrectVersionIsNotValid() {
        // Assert:
        assertAddressIsNotValidIfChangedAtIndex(0);
    }

    @Test
    public void addressWithIncorrectHashIsNotValid() {
        // Assert:
        assertAddressIsNotValidIfChangedAtIndex(5);
    }

    @Test
    public void addressWithIncorrectChecksumIsNotValid() {
        // Assert:
        assertAddressIsNotValidIfChangedAtIndex(39);
    }

    private void assertAddressIsNotValidIfChangedAtIndex(final int index) {
        // Arrange:
        byte[] input = Utils.generateRandomBytes();

        // Act:
        Address address = Address.fromPublicKey(input);
        String fakeAddress = incrementCharAtIndex(address.getEncoded(), index);

        // Assert:
        Assert.assertThat(Address.isValid(fakeAddress), IsEqual.equalTo(false));
    }

    private static String incrementCharAtIndex(final String s, final int index) {
        char[] chars = s.toCharArray();
        chars[index] = (char)(chars[index] + 1);
        return new String(chars);
    }
}
