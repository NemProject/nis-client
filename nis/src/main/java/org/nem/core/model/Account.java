package org.nem.core.model;

import org.nem.core.crypto.*;
import java.util.*;

/**
 * A NEM account.
 */
public class Account {

    private final KeyPair keyPair;
    private final Address address;
    private final List<Message> messages;
    private long balance;
    private String label;

    /**
     * Creates an account around a key pair.
     *
     * @param keyPair The key pair.
     */
    public Account(final KeyPair keyPair) {
        this.keyPair = keyPair;
        this.address = Address.fromPublicKey(keyPair.getPublicKey());
        this.messages = new ArrayList<>();
        this.balance = 0;
    }

    /**
     * Creates an account around an address. This constructor should only
     * be used if the account's public key is not known.
     *
     * @param address The address.
     */
	public Account(final Address address) {
		this.keyPair = null == address.getPublicKey() ? null : new KeyPair(address.getPublicKey());
		this.address = address;
		this.messages = new ArrayList<>();
		this.balance = 0;
	}

    /**
     * Gets the account's key pair.
     *
     * @return The account's key pair.
     */
    public KeyPair getKeyPair() { return this.keyPair; }

    /**
     * Gets the account's address.
     *
     * @return The account's address.
     */
    public Address getAddress() { return this.address; }

    /**
     * Gets the account's balance.
     *
     * @return The account's balance.
     */
    public long getBalance() { return this.balance; }

    /**
     * Increments the account's balance.
     *
     * @param balance The amount by which to increment the balance.
     */
    public void incrementBalance(final long balance) {
        this.balance += balance;
    }

    /**
     * Gets the account's label.
     *
     * @return The account's label.
     */
    public String getLabel() { return this.label; }

    /**
     * Sets the account's label.
     *
     * @param label The desired label.
     */
    public void setLabel(final String label) { this.label = label; }

    /**
     * Gets all messages associated with the account.
     *
     * @return All messages associated with the account.
     */
    public List<Message> getMessages() { return this.messages; }

    /**
     * Associates message with the account.
     *
     * @param message The message to associate with this account.
     */
    public void addMessage(final Message message) {
        this.messages.add(message);
    }

    @Override
    public int hashCode() {
        return this.address.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Account))
            return false;

        Account rhs = (Account)obj;
        return this.address.equals(rhs.address);
    }
}