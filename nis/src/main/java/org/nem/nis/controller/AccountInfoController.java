package org.nem.nis.controller;

import org.nem.core.model.*;
import org.nem.core.model.ncc.*;
import org.nem.core.model.ncc.AccountInfo;
import org.nem.core.model.primitive.*;
import org.nem.core.node.NodeFeature;
import org.nem.core.serialization.*;
import org.nem.deploy.NisConfiguration;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.controller.annotations.ClientApi;
import org.nem.nis.controller.requests.*;
import org.nem.nis.controller.viewmodels.AccountHistoricalDataViewModel;
import org.nem.nis.harvesting.*;
import org.nem.nis.poi.GroupedHeight;
import org.nem.nis.service.*;
import org.nem.nis.state.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST API for retrieving account related information
 */
@RestController
public class AccountInfoController {
	private final UnlockedAccounts unlockedAccounts;
	private final UnconfirmedTransactionsFilter unconfirmedTransactions;
	private final BlockChainLastBlockLayer blockChainLastBlockLayer;
	private final AccountInfoFactory accountInfoFactory;
	private final ReadOnlyAccountStateCache accountStateCache;
	private final NisConfiguration nisConfiguration;

	@Autowired(required = true)
	AccountInfoController(
			final UnlockedAccounts unlockedAccounts,
			final UnconfirmedTransactionsFilter unconfirmedTransactions,
			final BlockChainLastBlockLayer blockChainLastBlockLayer,
			final AccountInfoFactory accountInfoFactory,
			final ReadOnlyAccountStateCache accountStateCache,
			final NisConfiguration nisConfiguration) {
		this.unlockedAccounts = unlockedAccounts;
		this.unconfirmedTransactions = unconfirmedTransactions;
		this.blockChainLastBlockLayer = blockChainLastBlockLayer;
		this.accountInfoFactory = accountInfoFactory;
		this.accountStateCache = accountStateCache;
		this.nisConfiguration = nisConfiguration;
	}

	/**
	 * Gets information about an account.
	 *
	 * @param builder The account id builder.
	 * @return The account information.
	 */
	@RequestMapping(value = "/account/get", method = RequestMethod.GET)
	@ClientApi
	public AccountMetaDataPair accountGet(final AccountIdBuilder builder) {
		final Address address = builder.build().getAddress();
		return this.getMetaDataPair(address);
	}

	/**
	 * Gets a list of account information.
	 *
	 * @param deserializer The deserializer.
	 * @return The list of account information.
	 */
	@RequestMapping(value = "/account/get/batch", method = RequestMethod.POST)
	@ClientApi
	public SerializableList<AccountMetaDataPair> accountGetBatch(@RequestBody final Deserializer deserializer) {
		final DeserializableList<AccountId> accounts = new DeserializableList<>(deserializer, AccountId::new);
		final Collection<AccountMetaDataPair> pairs = accounts.asCollection().stream()
				.map(a -> this.getMetaDataPair(a.getAddress()))
				.collect(Collectors.toList());
		return new SerializableList<>(pairs);
	}

	/**
	 * Gets historical information about an account.
	 *
	 * @param builder The account id builder.
	 * @return The account information.
	 */
	@RequestMapping(value = "/account/historical/get", method = RequestMethod.GET)
	@ClientApi
	public SerializableList<AccountHistoricalDataViewModel> accountHistoricalDataGet(final AccountHistoricalDataRequestBuilder builder) {
		if (!this.nisConfiguration.isFeatureSupported(NodeFeature.HISTORICAL_ACCOUNT_DATA)) {
			throw new UnsupportedOperationException("this node does not support historical account data retrieval");
		}

		final AccountHistoricalDataRequest request = builder.build();
		final long endHeight = Math.min(request.getEndHeight().getRaw(), this.blockChainLastBlockLayer.getLastBlockHeight().getRaw());
		final List<AccountHistoricalDataViewModel> views = new ArrayList<>();
		for (long i = request.getStartHeight().getRaw(); i <= endHeight; i += request.getIncrement()) {
			views.add(this.getAccountHistoricalData(request.getAddress(), new BlockHeight(i)));
		}
		return new SerializableList<>(views);
	}

	@RequestMapping(value = "/account/status", method = RequestMethod.GET)
	@ClientApi
	public AccountMetaData accountStatus(final AccountIdBuilder builder) {
		final Address address = builder.build().getAddress();
		return this.getMetaData(address);
	}

	private AccountMetaDataPair getMetaDataPair(final Address address) {
		final org.nem.core.model.ncc.AccountInfo accountInfo = this.accountInfoFactory.createInfo(address);
		final AccountMetaData metaData = this.getMetaData(address);
		return new AccountMetaDataPair(accountInfo, metaData);
	}

	private AccountHistoricalDataViewModel getAccountHistoricalData(final Address address, final BlockHeight height) {
		final BlockHeight groupedHeight = GroupedHeight.fromHeight(height);
		final ReadOnlyAccountState accountState = this.accountStateCache.findStateByAddress(address);
		final ReadOnlyWeightedBalances weightedBalances = accountState.getWeightedBalances();
		final Amount vested = weightedBalances.getVested(height);
		final Amount unvested = weightedBalances.getUnvested(height);
		final ReadOnlyHistoricalImportances importances = accountState.getHistoricalImportances();
		return new AccountHistoricalDataViewModel(
				height,
				address,
				vested.add(unvested),
				vested,
				unvested,
				importances.getHistoricalImportance(groupedHeight),
				importances.getHistoricalPageRank(groupedHeight));
	}

	private AccountMetaData getMetaData(final Address address) {
		final BlockHeight height = this.blockChainLastBlockLayer.getLastBlockHeight();
		final ReadOnlyAccountState accountState = this.accountStateCache.findStateByAddress(address);
		AccountRemoteStatus remoteStatus = this.getRemoteStatus(accountState, height);
		if (this.hasPendingImportanceTransfer(address)) {
			switch (remoteStatus) {
				case INACTIVE:
					remoteStatus = AccountRemoteStatus.ACTIVATING;
					break;

				case ACTIVE:
					remoteStatus = AccountRemoteStatus.DEACTIVATING;
					break;

				default:
					throw new IllegalStateException("unexpected remote state for account with pending importance transfer");
			}
		}

		final List<AccountInfo> cosignatoryOf = accountState.getMultisigLinks().getCosignatoriesOf().stream()
				.map(this.accountInfoFactory::createInfo)
				.collect(Collectors.toList());
		final List<AccountInfo> cosignatories = accountState.getMultisigLinks().getCosignatories().stream()
				.map(this.accountInfoFactory::createInfo)
				.collect(Collectors.toList());
		return new AccountMetaData(
				this.getAccountStatus(address),
				remoteStatus,
				cosignatoryOf,
				cosignatories);
	}

	private AccountRemoteStatus getRemoteStatus(final ReadOnlyAccountState accountState, final BlockHeight height) {
		final RemoteStatus remoteStatus = accountState.getRemoteLinks().getRemoteStatus(height);
		return remoteStatus.toAccountRemoteStatus();
	}

	private boolean hasPendingImportanceTransfer(final Address address) {
		final List<Transaction> transactions = this.unconfirmedTransactions.getMostRecentTransactionsForAccount(address, Integer.MAX_VALUE);
		return transactions.stream().anyMatch(transaction -> TransactionTypes.IMPORTANCE_TRANSFER == transaction.getType());
	}

	private AccountStatus getAccountStatus(final Address address) {
		return this.unlockedAccounts.isAccountUnlocked(address) ? AccountStatus.UNLOCKED : AccountStatus.LOCKED;
	}
}
