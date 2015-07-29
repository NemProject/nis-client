package org.nem.specific.deploy;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.model.Address;
import org.nem.core.node.NodeFeature;
import org.nem.core.test.*;

import java.net.URL;
import java.nio.file.Path;
import java.util.*;

public class NisConfigurationTest {
	private final static List<String> REQUIRED_PROPERTY_NAMES = Arrays.asList(
			"nem.shortServerName",
			"nem.httpPort",
			"nem.httpsPort",
			"nem.webContext",
			"nem.apiContext",
			"nem.homePath",
			"nem.maxThreads");

	private final static List<String> OPTIONAL_PROPERTY_NAMES = Arrays.asList(
			"nem.folder",
			"nem.protocol",
			"nem.host",
			"nem.shutdownPath",
			"nem.useDosFilter",
			"nem.nonAuditedApiPaths",
			"nem.network",
			"nis.bootName",
			"nis.bootKey",
			"nis.shouldAutoHarvestOnBoot",
			"nis.additionalHarvesterPrivateKeys",
			"nis.nodeLimit",
			"nis.timeSyncNodeLimit",
			"nis.useBinaryTransport",
			"nis.useNetworkTime",
			"nis.ipDetectionMode",
			"nis.unlockedLimit",
			"nis.maxTransactions",
			"nis.transactionHashRetentionTime",
			"nis.additionalLocalIps",
			"nis.optionalFeatures",
			"nis.allowedHarvesterAddresses",
			"nis.delayBlockLoading");

	@Test
	public void canReadDefaultConfiguration() {
		// Arrange:
		final Properties properties = getCommonProperties();

		// Act:
		final NisConfiguration config = new NisConfiguration(properties);

		// Assert:
		assertDefaultConfiguration(config);
	}

	@Test
	public void canReadDefaultConfigurationFromResources() {
		// Act:
		final NisConfiguration config = new NisConfiguration();

		// Assert:
		assertDefaultConfiguration(config);
	}

	private static void assertDefaultConfiguration(final NisConfiguration config) {
		// Assert:
		Assert.assertThat(config.getAutoBootKey(), IsNull.nullValue());
		Assert.assertThat(config.getAutoBootName(), IsNull.nullValue());
		Assert.assertThat(config.shouldAutoHarvestOnBoot(), IsEqual.equalTo(true));
		Assert.assertThat(config.getAdditionalHarvesterPrivateKeys(), IsEqual.equalTo(new PrivateKey[] {}));

		Assert.assertThat(config.getNodeLimit(), IsEqual.equalTo(5));
		Assert.assertThat(config.getTimeSyncNodeLimit(), IsEqual.equalTo(20));
		Assert.assertThat(config.useBinaryTransport(), IsEqual.equalTo(true));
		Assert.assertThat(config.useNetworkTime(), IsEqual.equalTo(true));
		Assert.assertThat(config.getIpDetectionMode(), IsEqual.equalTo(IpDetectionMode.AutoRequired));

		Assert.assertThat(config.getUnlockedLimit(), IsEqual.equalTo(1));
		Assert.assertThat(config.getMaxTransactions(), IsEqual.equalTo(10000));
		Assert.assertThat(config.getTransactionHashRetentionTime(), IsEqual.equalTo(36));
		Assert.assertThat(config.getAdditionalLocalIps(), IsEqual.equalTo(new String[] {}));
		Assert.assertThat(config.getOptionalFeatures(), IsEqual.equalTo(new NodeFeature[] { NodeFeature.TRANSACTION_HASH_LOOKUP }));
		Assert.assertThat(config.getAllowedHarvesterAddresses(), IsEqual.equalTo(new Address[] {}));
		Assert.assertThat(config.delayBlockLoading(), IsEqual.equalTo(true));
	}

	@Test
	public void canReadCustomConfiguration() {
		// Arrange:
		final PrivateKey originalPrivateKey = new KeyPair().getPrivateKey();
		final PrivateKey additionalPrivateKey1 = new KeyPair().getPrivateKey();
		final PrivateKey additionalPrivateKey2 = new KeyPair().getPrivateKey();
		final Properties properties = getCommonProperties();
		properties.setProperty("nis.bootKey", originalPrivateKey.toString());
		properties.setProperty("nis.bootName", "my name");
		properties.setProperty("nis.shouldAutoHarvestOnBoot", "false");
		properties.setProperty("nis.additionalHarvesterPrivateKeys", additionalPrivateKey1.toString() + "|" + additionalPrivateKey2.toString());
		properties.setProperty("nis.nodeLimit", "8");
		properties.setProperty("nis.timeSyncNodeLimit", "12");
		properties.setProperty("nis.useBinaryTransport", "false");
		properties.setProperty("nis.useNetworkTime", "false");
		properties.setProperty("nis.ipDetectionMode", "Disabled");
		properties.setProperty("nis.unlockedLimit", "123");
		properties.setProperty("nis.maxTransactions", "234");
		properties.setProperty("nis.transactionHashRetentionTime", "567");
		properties.setProperty("nis.additionalLocalIps", "10.0.0.10|10.0.0.20");
		properties.setProperty("nis.optionalFeatures", "TRANSACTION_HASH_LOOKUP|HISTORICAL_ACCOUNT_DATA");
		properties.setProperty("nis.allowedHarvesterAddresses", "FOO|BAR|BAZ");
		properties.setProperty("nis.delayBlockLoading", "false");

		// Act:
		final NisConfiguration config = new NisConfiguration(properties);

		// Assert:
		Assert.assertThat(config.getAutoBootKey(), IsEqual.equalTo(originalPrivateKey));
		Assert.assertThat(config.getAutoBootName(), IsEqual.equalTo("my name"));
		Assert.assertThat(config.shouldAutoHarvestOnBoot(), IsEqual.equalTo(false));
		Assert.assertThat(
				config.getAdditionalHarvesterPrivateKeys(),
				IsEqual.equalTo(new PrivateKey[] { additionalPrivateKey1, additionalPrivateKey2 }));

		Assert.assertThat(config.getNodeLimit(), IsEqual.equalTo(8));
		Assert.assertThat(config.getTimeSyncNodeLimit(), IsEqual.equalTo(12));
		Assert.assertThat(config.useBinaryTransport(), IsEqual.equalTo(false));
		Assert.assertThat(config.useNetworkTime(), IsEqual.equalTo(false));
		Assert.assertThat(config.getIpDetectionMode(), IsEqual.equalTo(IpDetectionMode.Disabled));

		Assert.assertThat(config.getUnlockedLimit(), IsEqual.equalTo(123));
		Assert.assertThat(config.getMaxTransactions(), IsEqual.equalTo(234));
		Assert.assertThat(config.getTransactionHashRetentionTime(), IsEqual.equalTo(567));
		Assert.assertThat(
				config.getAdditionalLocalIps(),
				IsEqual.equalTo(new String[] { "10.0.0.10", "10.0.0.20" }));
		Assert.assertThat(
				config.getOptionalFeatures(),
				IsEqual.equalTo(new NodeFeature[] { NodeFeature.TRANSACTION_HASH_LOOKUP, NodeFeature.HISTORICAL_ACCOUNT_DATA }));
		Assert.assertThat(
				config.getAllowedHarvesterAddresses(),
				IsEqual.equalTo(new Address[] { Address.fromEncoded("FOO"), Address.fromEncoded("BAR"), Address.fromEncoded("BAZ") }));
		Assert.assertThat(config.delayBlockLoading(), IsEqual.equalTo(false));
	}

	//endregion

	//region property required status

	@Test
	public void requiredPropertiesAreDetectedCorrectly() {
		// Arrange:
		final MockNemProperties properties = new MockNemProperties(getCommonProperties());

		// Act:
		new NisConfiguration(properties);

		// Assert:
		Assert.assertThat(properties.getRequiredPropertyNames(), IsEquivalent.equivalentTo(REQUIRED_PROPERTY_NAMES));
	}

	@Test
	public void optionalPropertiesAreDetectedCorrectly() {
		// Arrange:
		final MockNemProperties properties = new MockNemProperties(getCommonProperties());

		// Act:
		new NisConfiguration(properties);

		// Assert:
		Assert.assertThat(properties.getOptionalPropertyNames(), IsEquivalent.equivalentTo(OPTIONAL_PROPERTY_NAMES));
	}

	//endregion

	//region specific property tests

	@Test
	public void autoBootNameIsTrimmedOfLeadingAndTrailingWhitespace() {
		// Arrange:
		final Properties properties = getCommonProperties();
		properties.setProperty("nis.bootName", " \t string with spaces\t  ");

		// Act:
		final NisConfiguration config = new NisConfiguration(properties);

		// Assert:
		Assert.assertThat(config.getAutoBootName(), IsEqual.equalTo("string with spaces"));
	}

	@Test
	public void optionalFeaturesCannotBeParsedWithInvalidValue() {
		// Arrange:
		final Properties properties = getCommonProperties();
		properties.setProperty("nis.optionalFeatures", "TRANSACTION_HASH_LOOKUP|PLACEHOLDER9");

		// Act:
		ExceptionAssert.assertThrows(
				v -> new NisConfiguration(properties),
				IllegalArgumentException.class);
	}

	@Test
	public void isFeatureSupportedReturnsTrueIfNodeFeatureArrayContainsFeature() {
		// Arrange:
		final Properties properties = getCommonProperties();
		properties.setProperty("nis.optionalFeatures", "TRANSACTION_HASH_LOOKUP|HISTORICAL_ACCOUNT_DATA");
		final NisConfiguration config = new NisConfiguration(properties);

		// Assert:
		Assert.assertThat(config.isFeatureSupported(NodeFeature.TRANSACTION_HASH_LOOKUP), IsEqual.equalTo(true));
		Assert.assertThat(config.isFeatureSupported(NodeFeature.HISTORICAL_ACCOUNT_DATA), IsEqual.equalTo(true));
	}

	@Test
	public void isFeatureSupportedReturnsFalseIfNodeFeatureArrayDoesNotContainFeature() {
		// Arrange:
		final Properties properties = getCommonProperties();
		properties.setProperty("nis.optionalFeatures", "TRANSACTION_HASH_LOOKUP|HISTORICAL_ACCOUNT_DATA");
		final NisConfiguration config = new NisConfiguration(properties);

		// Assert:
		Assert.assertThat(config.isFeatureSupported(NodeFeature.PLACEHOLDER2), IsEqual.equalTo(false));
	}

	//endregion

	//region sanity

	@Test
	public void configPropertiesCopiesAreConsistent() throws Exception {
		// Arrange:
		// - this is more complicated than it seems in order to make sure that the right config.properties is loaded
		// - use db.properties as a proxy to the production config.properties
		// - use test.properties as a proxy to the test config.properties
		final byte[] sourceConfigBytes = readAllBytes("db.properties", "config.properties");
		final byte[] testConfigBytes = readAllBytes("test.properties", "config.properties");

		// Act:
		final boolean areBuffersEqual = Arrays.equals(sourceConfigBytes, testConfigBytes);

		// Assert:
		Assert.assertThat(areBuffersEqual, IsEqual.equalTo(true));
	}

	//endregion

	private static byte[] readAllBytes(final String proxyResource, final String desiredResource) throws Exception {
		final URL proxyUrl = NisConfigurationTest.class.getClassLoader().getResource(proxyResource);
		if (null == proxyUrl) {
			throw new IllegalArgumentException(String.format("could not find: '%s'", proxyResource));
		}

		final URL url = new URL(proxyUrl.toString().replace(proxyResource, desiredResource));
		final Path resPath = java.nio.file.Paths.get(url.toURI());
		return java.nio.file.Files.readAllBytes(resPath);
	}

	private static Properties getCommonProperties() {
		final Properties properties = new Properties();
		properties.setProperty("nem.shortServerName", "Nis");
		properties.setProperty("nem.folder", "folder");
		properties.setProperty("nem.maxThreads", "1");
		properties.setProperty("nem.protocol", "ftp");
		properties.setProperty("nem.host", "10.0.0.1");
		properties.setProperty("nem.httpPort", "100");
		properties.setProperty("nem.httpsPort", "101");
		properties.setProperty("nem.webContext", "/web");
		properties.setProperty("nem.apiContext", "/api");
		properties.setProperty("nem.homePath", "/home");
		properties.setProperty("nem.shutdownPath", "/shutdown");
		properties.setProperty("nem.useDosFilter", "true");
		return properties;
	}
}