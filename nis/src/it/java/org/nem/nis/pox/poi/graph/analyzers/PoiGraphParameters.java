package org.nem.nis.pox.poi.graph.analyzers;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Property bag that can contain arbitrary poi graph parameters.
 */
public class PoiGraphParameters {
	private final ConcurrentHashMap<String, String> params = new ConcurrentHashMap<>();

	/**
	 * Sets a parameter.
	 *
	 * @param name The name of the parameter.
	 * @param value The value of the parameter.
	 */
	public void set(final String name, final String value) {
		this.params.put(name, value);
	}

	/**
	 * Gets a parameter value by its name or a default value.
	 *
	 * @param name The name of the parameter.
	 * @param defaultValue The default value.
	 * @return The value of the parameter.
	 */
	public String get(final String name, final String defaultValue) {
		final String value = this.params.get(name);
		return null == value ? defaultValue : value;
	}

	/**
	 * Gets an int parameter by its name.
	 *
	 * @param name The name of the parameter.
	 * @param defaultValue The default value.
	 * @return The value of the parameter.
	 */
	public int getAsInteger(final String name, final int defaultValue) {
		final String value = this.get(name, null);
		return null == value ? defaultValue : Integer.parseInt(value);
	}

	public static PoiGraphParameters getDefaultParams() {
		final PoiGraphParameters params = new PoiGraphParameters();
		params.set("layout", Integer.toString(PoiGraphViewer.Layouts.KAMADA_KAWAI_LAYOUT));
		params.set("width", "800");
		params.set("height", "800");
		params.set("bgColor", "0xFFFFFF");
		params.set("vertexFillColor", "0x00FF00");
		params.set("edgeType", Integer.toString(PoiGraphViewer.EdgeTypes.EDGE_TYPE_UNDIRECTED));
		return params;
	}
}