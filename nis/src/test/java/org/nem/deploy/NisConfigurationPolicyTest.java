package org.nem.deploy;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.deploy.CommonConfiguration;
import org.nem.deploy.appconfig.NisAppConfig;

public class NisConfigurationPolicyTest {
	//region get class

	@Test
	public void canGetNisAppConfigClass() {
		// Arrange:
		final NisConfigurationPolicy policy = new NisConfigurationPolicy();

		// Assert:
		Assert.assertThat(policy.getAppConfigClass(), IsEqual.equalTo(NisAppConfig.class));
	}

	@Test
	public void canGetNisWebAppInitializerClass() {
		// Arrange:
		final NisConfigurationPolicy policy = new NisConfigurationPolicy();

		// Assert:
		Assert.assertThat(policy.getWebAppInitializerClass(), IsEqual.equalTo(NisWebAppInitializer.class));
	}

	//endregion

	//region get raises exception

	@Test(expected = NisConfigurationException.class)
	public void getJarFileServletClassRaisesException() {
		// Arrange:
		final NisConfigurationPolicy policy = new NisConfigurationPolicy();

		// Act:
		policy.getJarFileServletClass();
	}

	@Test(expected = NisConfigurationException.class)
	public void getDefaultServletClassRaisesException() {
		// Arrange:
		final NisConfigurationPolicy policy = new NisConfigurationPolicy();

		// Act:
		policy.getDefaultServletClass();
	}

	//endregion

	@Test
	public void loadConfigReturnsValidConfiguration() {
		// Arrange:
		final NisConfigurationPolicy policy = new NisConfigurationPolicy();

		// Act:
		final CommonConfiguration configuration = policy.loadConfig(new String[] { });

		// Assert:
		Assert.assertThat(configuration, IsNull.notNullValue());
	}
}