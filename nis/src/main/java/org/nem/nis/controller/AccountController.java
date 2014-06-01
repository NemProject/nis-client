package org.nem.nis.controller;

import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.ncc.TransactionMetaDataPair;
import org.nem.nis.Foraging;
import org.nem.nis.controller.annotations.ClientApi;
import org.nem.nis.service.AccountIo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for interacting with Account objects.
 */
@RestController
public class AccountController {
	private final Foraging foraging;
	private final AccountIo accountIo;

	@Autowired(required = true)
	AccountController(final Foraging foraging, final AccountIo accountIo) {
		this.foraging = foraging;
		this.accountIo = accountIo;
	}

	@RequestMapping(value = "/account/get", method = RequestMethod.GET)
	@ClientApi
	public Account accountGet(@RequestParam(value = "address") final String nemAddress) {
		return this.accountIo.findByAddress(getAddress(nemAddress));
	}

	/**
	 * Unlocks an account for foraging.
	 *
	 * @param privateKey The private key of the account to unlock.
	 */
	@RequestMapping(value = "/account/unlock", method = RequestMethod.POST)
	@ClientApi
	public void accountUnlock(@RequestBody final PrivateKey privateKey) {
		final Account account = new Account(new KeyPair(privateKey));
		this.foraging.addUnlockedAccount(account);
	}

	// TODO: test the following functions
	@RequestMapping(value = "/account/transfers", method = RequestMethod.GET)
	@ClientApi
	public SerializableList<TransactionMetaDataPair> accountTransfers(
			@RequestParam(value = "address") final String nemAddress
			, @RequestParam(value = "timestamp", required = false) final String timestamp
	) {
		return this.accountIo.getAccountTransfers(getAddress(nemAddress), timestamp);
	}

	@RequestMapping(value = "/account/blocks", method = RequestMethod.GET)
	@ClientApi
	public SerializableList<Block> accountBlocks(
			@RequestParam(value = "address") final String nemAddress
			, @RequestParam(value = "timestamp", required = false) final String timestamp
	) {
		return this.accountIo.getAccountBlocks(getAddress(nemAddress), timestamp);
	}

	private Address getAddress(final String nemAddress) {
		Address address = Address.fromEncoded(nemAddress);
		if (!address.isValid()) {
			throw new IllegalArgumentException("address is not valid");
		}

		return address;
	}
}