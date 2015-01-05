package org.nem.nis.dbmodel;

import javax.persistence.*;

@Entity
@Table(name = "multisigsignatures")
public class MultisigSignature extends AbstractTransfer {
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "multisigTransactionId")
	private MultisigTransaction multisigTransaction;

	// TODO 20150103 J-G: I'm unsure why the signature needs to know the owning multisig transaction, can you elaborate?
	// TODO 20150105 G-J: erm MultisigTransaction -> MultisigSignature is @OneToMany
	// > we need to join both tables together
	public MultisigTransaction getMultisigTransaction() {
		return this.multisigTransaction;
	}

	public void setMultisigTransaction(final MultisigTransaction multisigTransaction) {
		this.multisigTransaction = multisigTransaction;
	}
}
