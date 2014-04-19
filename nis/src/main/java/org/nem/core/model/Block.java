package org.nem.core.model;

import org.nem.core.serialization.*;
import org.nem.core.time.TimeInstant;

import java.util.*;

/**
 * A NEM block.
 * <p/>
 * The forger is an alias for the signer.
 * The forger proof is the signature.
 */
public class Block extends VerifiableEntity {

	private final static int BLOCK_TYPE = 1;
	private final static int BLOCK_VERSION = 1;

	private final Hash prevBlockHash;
	private final BlockHeight height;
	private Amount totalFee = Amount.ZERO;

	private final List<Transaction> transactions;

	// these are helper fields and shouldn't be serialized
	private BlockDifficulty difficulty;

	private Hash generationHash;

	/**
	 * Creates a new block.
	 *
	 * @param forger        The forger.
	 * @param prevBlockHash The hash of the previous block.
	 * @param timestamp     The block timestamp.
	 * @param height        The block height.
	 */
	public Block(final Account forger, final Hash prevBlockHash, final TimeInstant timestamp, final BlockHeight height) {
		super(BLOCK_TYPE, BLOCK_VERSION, timestamp, forger);
		this.transactions = new ArrayList<>();
		this.prevBlockHash = prevBlockHash;
		this.height = height;

		this.difficulty = BlockDifficulty.INITIAL_DIFFICULTY;
	}

	/**
	 * Creates a new block.
	 *
	 * @param forger    The forger.
	 * @param timestamp The block timestamp.
	 * @param prevBlock The previous block.
	 */
	public Block(final Account forger, final Block prevBlock, final TimeInstant timestamp) {
		this(forger, HashUtils.calculateHash(prevBlock), timestamp, prevBlock.getHeight().next());
		setGenerationHash(HashUtils.nextHash(prevBlock.getGenerationHash()));
	}

	/**
	 * Deserializes a new block.
	 *
	 * @param type         The block type.
	 * @param deserializer The deserializer to use.
	 */
	public Block(final int type, final DeserializationOptions options, final Deserializer deserializer) {
		super(type, options, deserializer);

		this.prevBlockHash = deserializer.readObject("prevBlockHash", Hash.DESERIALIZER);
		this.height = BlockHeight.readFrom(deserializer, "height");
		this.totalFee = Amount.readFrom(deserializer, "totalFee");

		this.transactions = deserializer.readObjectArray("transactions", TransactionFactory.VERIFIABLE);

		this.difficulty = BlockDifficulty.INITIAL_DIFFICULTY;
	}

	//region Getters

	/**
	 * Gets the height of this block in the block chain.
	 *
	 * @return The height of this block in the block chain.
	 */
	public BlockHeight getHeight() {
		return this.height;
	}

	/**
	 * Gets total amount of fees of all transactions stored in this block.
	 *
	 * @return The total amount of fees of all transactions stored in this block.
	 */
	public Amount getTotalFee() {
		return this.totalFee;
	}

	/**
	 * Gets the hash of the previous block.
	 *
	 * @return The hash of the previous block.
	 */
	public Hash getPreviousBlockHash() {
		return this.prevBlockHash;
	}

	/**
	 * Gets the transactions associated with this block.
	 *
	 * @return The transactions associated with this block.
	 */
	public List<Transaction> getTransactions() {
		return this.transactions;
	}

	/**
	 * Gets the difficulty associated with this block.
	 *
	 * @return Difficulty of this block.
	 */
	public BlockDifficulty getDifficulty() {
		return this.difficulty;
	}

	/**
	 * Gets the generation hash associated with this block.
	 *
	 * @return Generation hash of this block.
	 */
	public Hash getGenerationHash() { return this.generationHash; }

	public void setGenerationHash(Hash generationHash) {
		this.generationHash = generationHash;
	}

	//endregion

	public void setDifficulty(final BlockDifficulty difficulty) {
		this.difficulty = difficulty;
	}

	/**
	 * Adds a new transaction to this block.
	 *
	 * @param transaction The transaction to add.
	 */
	public void addTransaction(final Transaction transaction) {
		this.transactions.add(transaction);
		this.totalFee = this.totalFee.add(transaction.getFee());
	}

	/**
	 * Adds new transactions to this block.
	 *
	 * @param transactions The transactions to add.
	 */
	public void addTransactions(final Collection<Transaction> transactions) {
		for (final Transaction transaction : transactions)
			this.addTransaction(transaction);
	}

	@Override
	protected void serializeImpl(final Serializer serializer) {
		serializer.writeObject("prevBlockHash", this.prevBlockHash);
		BlockHeight.writeTo(serializer, "height", this.height);
		Amount.writeTo(serializer, "totalFee", this.totalFee);

		serializer.writeObjectArray("transactions", this.transactions);
	}

	@Override
	public String toString() {
		return String.format("height: %d, #tx: %d", this.height.getRaw(), this.transactions.size());
	}
}
