package org.nem.peer.node;

/**
 * An interface for checking the compatibility of two node versions.
 */
@FunctionalInterface
public interface NodeVersionCheck {

	/**
	 * Checks the local and remote versions for compatibility.
	 *
	 * @param local The local version
	 * @param remote The remote version.
	 * @return true if the versions are compatible.
	 */
	public boolean check(final NodeVersion local, final NodeVersion remote);
}