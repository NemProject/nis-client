package org.nem.nis.dao;

import org.junit.*;
import org.junit.runner.RunWith;
import org.nem.core.model.Account;
import org.nem.core.test.Utils;
import org.nem.nis.dbmodel.DbAccount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.*;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;

@ContextConfiguration(classes = TestConf.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class AccountDaoTest extends AbstractTransactionalJUnit4SpringContextTests {

	@Autowired
	AccountDao accountDao;

	@Test
	public void canSaveAccount() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final DbAccount entity = new DbAccount(account.getAddress().getEncoded(), account.getAddress().getPublicKey());

		// Act:
		this.accountDao.save(entity);

		// Assert:
		Assert.assertThat(entity.getId(), not(nullValue()));
	}

	@Test
	public void canRetrieveSavedAccount() {
		// Arrange
		final Account account = Utils.generateRandomAccount();
		final DbAccount dbAccount = new DbAccount(account.getAddress().getEncoded(), account.getAddress().getPublicKey());

		// Act:
		this.accountDao.save(dbAccount);
		final DbAccount entity = this.accountDao.getAccountByPrintableAddress(dbAccount.getPrintableKey());

		// Assert:
		Assert.assertThat(entity.getId(), notNullValue());
		Assert.assertThat(entity.getId(), equalTo(dbAccount.getId()));
		Assert.assertThat(entity.getPrintableKey(), equalTo(account.getAddress().getEncoded()));
		Assert.assertThat(entity.getPublicKey(), equalTo(account.getAddress().getPublicKey()));
	}
}
