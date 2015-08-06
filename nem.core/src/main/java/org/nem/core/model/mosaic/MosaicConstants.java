package org.nem.core.model.mosaic;

import org.nem.core.crypto.PublicKey;
import org.nem.core.model.*;
import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.BlockHeight;

import java.util.Properties;

/**
 * Common place to have Mosaic-related constants accessible from all modules.
 */
public class MosaicConstants {
	private static final PublicKey NAMESPACE_OWNER_NEM_KEY = PublicKey.fromHexString("3e82e1c1e4a75adaa3cba8c101c3cd31d9817a2eb966eb3b511fb2ed45b8e262");
	// TODO 20150805 J-B: i really don't like the name "admitter" we should at least be consistent with provision namespace, which uses "lessor"
	// > although really, this account is more of a "fee sink" vs a true lessor
	// TODO 20150806 BR -> J: so far mosaics are not leased, therefore lessor isn't really correct. If you don't like admitter, fell free to change it.
	// > But if doing so, please change it everywhere including the column name in the db (or tell me what name you like and i will do the changes).
	// > I added a separate public key because i wanted a separate account for it. My vanity generator is searching for an address that start with NAMOSAIC.
	// > You want to use the same account as for the namespace fees?
	private static final PublicKey MOSAIC_ADMITTER_KEY = NAMESPACE_OWNER_NEM_KEY;

	/**
	 * The maximum allowable quantity of a mosaic.
	 */
	public static final long MAX_QUANTITY = 9_000_000_000_000_000L;

	/**
	 * The 'nem' namespace owner.
	 */
	public static final Account NAMESPACE_OWNER_NEM = new Account(Address.fromPublicKey(NAMESPACE_OWNER_NEM_KEY));

	/**
	 * The 'nem' namespace id.
	 */
	public static final NamespaceId NAMESPACE_ID_NEM = new NamespaceId("nem");

	/**
	 * The 'nem' namespace.
	 */
	public static final Namespace NAMESPACE_NEM = new Namespace(NAMESPACE_ID_NEM, NAMESPACE_OWNER_NEM, BlockHeight.MAX);

	/**
	 * The mosaic admitter.
	 */
	public static final Account MOSAIC_ADMITTER = new Account(Address.fromPublicKey(MOSAIC_ADMITTER_KEY));

	/**
	 * The xem mosaic id.
	 */
	public static final MosaicId MOSAIC_ID_XEM = new MosaicId(NAMESPACE_ID_NEM, "xem");

	/**
	 * The 'nem.xem' mosaic definition.
	 */
	public static final MosaicDefinition MOSAIC_DEFINITION_XEM = createXemMosaicDefinition();

	private static MosaicDefinition createXemMosaicDefinition() {
		final MosaicDescriptor descriptor = new MosaicDescriptor("reserved xem mosaic");
		final Properties properties = new Properties();
		properties.put("divisibility", "6");
		properties.put("initialSupply", "8999999999");
		properties.put("mutableSupply", "false");
		properties.put("transferable", "true");
		properties.put("transferFeeEnabled", "false");
		return new MosaicDefinition(
				NAMESPACE_OWNER_NEM,
				MOSAIC_ID_XEM,
				descriptor,
				new DefaultMosaicProperties(properties));
	}
}
