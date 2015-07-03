package org.nem.nis.dao.mappers;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.nis.dbmodel.DbMosaicProperty;

import java.math.BigInteger;

public class MosaicPropertyRawToDbModelMappingTest {

	@Test
	public void rawDataCanBeMappedToDbModel() {
		// Arrange:
		final Object[] raw = new Object[11];
		raw[8] = BigInteger.valueOf(123L);           // id
		raw[9] = "foo";                              // name
		raw[10] = "bar";                              // value

		// Act:
		final DbMosaicProperty dbModel = new MosaicPropertyRawToDbModelMapping().map(raw);

		// Assert:
		Assert.assertThat(dbModel, IsNull.notNullValue());
		Assert.assertThat(dbModel.getId(), IsEqual.equalTo(123L));
		Assert.assertThat(dbModel.getName(), IsEqual.equalTo("foo"));
		Assert.assertThat(dbModel.getValue(), IsEqual.equalTo("bar"));
	}
}
