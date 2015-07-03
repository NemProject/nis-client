package org.nem.nis.mappers;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.primitive.GenericAmount;
import org.nem.core.test.*;
import org.nem.nis.dbmodel.*;

import java.util.*;

public class MosaicDbModelToModelMappingTest {

	@Test
	public void canMapDbMosaicToMosaic() {
		// Arrange:
		final TestContext context = new TestContext();
		final DbMosaic dbMosaic = new DbMosaic();
		dbMosaic.setCreator(context.dbCreator);
		dbMosaic.setMosaicId("Alice's gift vouchers");
		dbMosaic.setDescription("precious vouchers");
		dbMosaic.setNamespaceId("alice.vouchers");
		dbMosaic.setAmount(123L);
		dbMosaic.setProperties(context.propertiesMap.keySet());

		// Act:
		final Mosaic mosaic = context.mapping.map(dbMosaic);

		// Assert:
		Mockito.verify(context.mapper, Mockito.times(1)).map(context.dbCreator, Account.class);
		Mockito.verify(context.mapper, Mockito.times(2)).map(Mockito.any(), Mockito.eq(NemProperty.class));

		Assert.assertThat(mosaic.getCreator(), IsEqual.equalTo(context.creator));
		Assert.assertThat(mosaic.getId(), IsEqual.equalTo(new MosaicId("Alice's gift vouchers")));
		Assert.assertThat(mosaic.getDescriptor(), IsEqual.equalTo(new MosaicDescriptor("precious vouchers")));
		Assert.assertThat(mosaic.getNamespaceId(), IsEqual.equalTo(new NamespaceId("alice.vouchers")));
		Assert.assertThat(mosaic.getAmount(), IsEqual.equalTo(GenericAmount.fromValue(123)));
		Assert.assertThat(mosaic.getProperties().asCollection().size(), IsEqual.equalTo(2));
		Assert.assertThat(mosaic.getProperties().asCollection(), IsEquivalent.equivalentTo(context.propertiesMap.values()));
		Assert.assertThat(mosaic.getChildren(), IsEqual.equalTo(Collections.emptyList()));
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final DbAccount dbCreator = Mockito.mock(DbAccount.class);
		private final Account creator = Utils.generateRandomAccount();

		private final Map<DbMosaicProperty, NemProperty> propertiesMap = new HashMap<DbMosaicProperty, NemProperty>() {
			{
				this.put(new DbMosaicProperty(), new NemProperty("name", "foo"));
				this.put(new DbMosaicProperty(), new NemProperty("namespace", "bar"));
			}
		};

		private final MosaicDbModelToModelMapping mapping = new MosaicDbModelToModelMapping(this.mapper);

		public TestContext() {
			Mockito.when(this.mapper.map(this.dbCreator, Account.class)).thenReturn(this.creator);

			for (final Map.Entry<DbMosaicProperty, NemProperty> entry : this.propertiesMap.entrySet()) {
				Mockito.when(this.mapper.map(entry.getKey(), NemProperty.class)).thenReturn(entry.getValue());
			}
		}
	}
}
