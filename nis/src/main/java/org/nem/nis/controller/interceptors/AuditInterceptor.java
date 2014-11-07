package org.nem.nis.controller.interceptors;

import org.nem.nis.audit.AuditCollection;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * Interceptor that audits requests.
 */
public class AuditInterceptor extends HandlerInterceptorAdapter {
	private static final Logger LOGGER = Logger.getLogger(AuditInterceptor.class.getName());
	// TODO 20141106 J-*: i was wondering if we should read this list from the config.properties file so it is easy to turn things off
	// > unless there is a better way to configure Java logging files
	private static final List<String> IGNORED_API_PATHS = Arrays.asList("/heartbeat", "/status", "/chain/height");

	private final AuditCollection auditCollection;

	/**
	 * Creates a new audit interceptor.
	 *
	 * @param auditCollection The audit collection.
	 */
	public AuditInterceptor(final AuditCollection auditCollection) {
		this.auditCollection = auditCollection;
	}

	@Override
	public boolean preHandle(
			final HttpServletRequest request,
			final HttpServletResponse response,
			final Object handler) throws Exception {
		final AuditEntry entry = new AuditEntry(request);
		if (entry.shouldIgnore()) {
			return true;
		}

		LOGGER.info(String.format("entering %s [%s]", entry.path, entry.host));
		this.auditCollection.add(entry.host, entry.path);
		return true;
	}

	@Override
	public void afterCompletion(
			final HttpServletRequest request,
			final HttpServletResponse response,
			final Object handler,
			final Exception ex)
			throws Exception {
		final AuditEntry entry = new AuditEntry(request);
		if (entry.shouldIgnore()) {
			return;
		}

		this.auditCollection.remove(entry.host, entry.path);
		LOGGER.info(String.format("exiting %s [%s]", entry.path, entry.host));
	}

	private static class AuditEntry {
		private final String host;
		private final String path;

		private AuditEntry(final HttpServletRequest request) {
			this.host = request.getRemoteAddr();
			this.path = request.getRequestURI();
		}

		private boolean shouldIgnore() {
			return IGNORED_API_PATHS.stream().anyMatch(this.path::equalsIgnoreCase);
		}
	}
}
