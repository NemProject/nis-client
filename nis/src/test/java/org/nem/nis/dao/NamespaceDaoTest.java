package org.nem.nis.dao;

import org.hibernate.*;
import org.hibernate.type.LongType;
import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.model.Account;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.test.Utils;
import org.nem.nis.dao.retrievers.NamespaceRetriever;

public class NamespaceDaoTest {

	@Test
	public void getNamespacesForAccountDelegatesToRetriever() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.namespaceDao.getNamespacesForAccount(context.account, new NamespaceId("foo"), 25);

		// Assert:
		Mockito.verify(context.retriever, Mockito.only()).getNamespacesForAccount(
				context.session,
				1L,
				new NamespaceId("foo"),
				25);
	}

	@Test
	public void getNamespaceDelegatesToRetriever() {
		// Arrange:
		final NamespaceId id = new NamespaceId("foo");
		final TestContext context = new TestContext();

		// Act:
		context.namespaceDao.getNamespace(id);

		// Assert:
		Mockito.verify(context.retriever, Mockito.only()).getNamespace(context.session, id);
	}

	@Test
	public void getRootNamespacesDelegatesToRetriever() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.namespaceDao.getRootNamespaces(25);

		// Assert:
		Mockito.verify(context.retriever, Mockito.only()).getRootNamespaces(context.session, 25);
	}

	private class TestContext {
		private final Account account = Utils.generateRandomAccount();
		private final SessionFactory sessionFactory = Mockito.mock(SessionFactory.class);
		private final NamespaceRetriever retriever = Mockito.mock(NamespaceRetriever.class);
		private final Session session = Mockito.mock(Session.class);
		private final SQLQuery sqlQuery = Mockito.mock(SQLQuery.class);
		private final NamespaceDaoImpl namespaceDao = new NamespaceDaoImpl(this.sessionFactory, this.retriever);

		private TestContext() {
			Mockito.when(this.sessionFactory.getCurrentSession()).thenReturn(this.session);
			Mockito.when(this.session.createSQLQuery(Mockito.anyString())).thenReturn(this.sqlQuery);
			Mockito.when(this.sqlQuery.addScalar(Mockito.any(), Mockito.any())).thenReturn(this.sqlQuery);
			Mockito.when(this.sqlQuery.setParameter(Mockito.any(String.class), Mockito.any(LongType.class))).thenReturn(this.sqlQuery);
			Mockito.when(this.sqlQuery.uniqueResult()).thenReturn(1L);
		}
	}
}
