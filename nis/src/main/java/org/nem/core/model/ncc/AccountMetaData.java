package org.nem.core.model.ncc;

import org.nem.core.model.*;
import org.nem.core.serialization.*;

import java.util.List;

/**
 * Class for holding additional information about an account required by ncc.
 */
public class AccountMetaData implements SerializableEntity {
	private final AccountStatus status;
	private final AccountRemoteStatus remoteStatus;
	private final List<AccountInfo> cosignatoryOf;

	/**
	 * Creates a new meta data.
	 *
	 * @param status The account status.
	 * @param remoteStatus The remote status.
	 * @param cosignatoryOf The list of multisig accounts.
	 */
	public AccountMetaData(final AccountStatus status, final AccountRemoteStatus remoteStatus, final List<AccountInfo> cosignatoryOf) {
		this.status = status;
		this.remoteStatus = remoteStatus;
		this.cosignatoryOf = cosignatoryOf;
	}

	/**
	 * Deserializes a meta data.
	 *
	 * @param deserializer The deserializer.
	 */
	public AccountMetaData(final Deserializer deserializer) {
		this.status = AccountStatus.readFrom(deserializer, "status");
		this.remoteStatus = AccountRemoteStatus.readFrom(deserializer, "remoteStatus");
		this.cosignatoryOf = deserializer.readObjectArray("cosignatoryOf", AccountInfo::new);
	}

	/**
	 * Returns The status of the account (locked/unlocked).
	 *
	 * @return The status.
	 */
	public AccountStatus getStatus() {
		return this.status;
	}

	/**
	 * Gets the remote account status
	 *
	 * @return The remote account status.
	 */
	public AccountRemoteStatus getRemoteStatus() {
		return this.remoteStatus;
	}

	/**
	 * Gets the list of multisig accounts, for which this account is cosignatory.
	 *
	 * @return The list of multisig accounts.
	 */
	public List<AccountInfo> getCosignatoryOf() {
		return cosignatoryOf;
	}

	@Override
	public void serialize(final Serializer serializer) {
		AccountStatus.writeTo(serializer, "status", this.status);
		AccountRemoteStatus.writeTo(serializer, "remoteStatus", this.getRemoteStatus());
		serializer.writeObjectArray("cosignatoryOf", this.cosignatoryOf);
	}
}
