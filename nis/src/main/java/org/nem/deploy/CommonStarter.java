package org.nem.deploy;

import java.util.EnumSet;
import java.util.logging.Logger;

import javax.servlet.*;
import javax.servlet.annotation.WebListener;

import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Did not find a better way of launching Jetty in combination with WebStart. The
 * physical location of the downloaded files is not pre-known, so passing a WAR
 * file to the Jetty runner does not work.
 * <p/>
 * I had to switch to the Servlet API 3.x with programmatic configuration.
 *
 * @author Thies1965
 */

@WebListener
public class CommonStarter implements ServletContextListener {
	private static final Logger LOGGER = Logger.getLogger(CommonStarter.class.getName());

	public static void main(String[] args) throws Exception {
		LOGGER.info("Starting embedded Jetty Server.");

		// https://code.google.com/p/json-smart/wiki/ParserConfiguration
		//JSONParser.DEFAULT_PERMISSIVE_MODE = JSONParser.MODE_JSON_SIMPLE;

		//Taken from Jetty doc
		Server server = new Server(createThreadPool());
		server.addBean(new ScheduledExecutorScheduler());

		server.addConnector(createConnector(server));
		server.setHandler(createHandlers());

		server.setDumpAfterStart(false);
		server.setDumpBeforeStop(false);
		server.setStopAtShutdown(true);

		LOGGER.info("Calling start().");
		server.start();
		server.join();
	}

	private static Handler createHandlers() {
		HandlerCollection handlers = new HandlerCollection();
		ServletContextHandler servletContext = new ServletContextHandler();

		//Special Listener to set-up the environment for Spring
		servletContext.addEventListener(new CommonStarter());
		servletContext.addEventListener(new ContextLoaderListener());
		servletContext.setErrorHandler(new JsonErrorHandler());

		handlers.setHandlers(new Handler[] { servletContext });

		return handlers;
	}

	public static Connector createConnector(Server server) {
		HttpConfiguration http_config = new HttpConfiguration();
		http_config.setSecureScheme("https");
		//PORT
		http_config.setSecurePort(7891);
		http_config.setOutputBufferSize(32768);
		http_config.setRequestHeaderSize(8192);
		http_config.setResponseHeaderSize(8192);
		http_config.setSendServerVersion(true);
		http_config.setSendDateHeader(false);

		ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(http_config));
		http.setPort(7890);
		http.setIdleTimeout(30000);
		return http;
	}

	public static QueuedThreadPool createThreadPool() {
		QueuedThreadPool threadPool = new QueuedThreadPool();
		threadPool.setMaxThreads(500);
		return threadPool;
	}

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		// nothing
	}

	@Override
	public void contextInitialized(ServletContextEvent event) {
		// This is the replacement for the web.xml
		// New with Servlet 3.0
		AnnotationConfigApplicationContext appCtx = new AnnotationConfigApplicationContext(NisAppConfig.class);

		AnnotationConfigWebApplicationContext webCtx = new AnnotationConfigWebApplicationContext();
		webCtx.register(NisWebAppInitializer.class);
		webCtx.setParent(appCtx);

		ServletContext context = event.getServletContext();
		ServletRegistration.Dynamic dispatcher = context.addServlet("Spring MVC Dispatcher Servlet", new DispatcherServlet(webCtx));
		dispatcher.setLoadOnStartup(1);
		dispatcher.addMapping("/");

		context.setInitParameter("contextClass", "org.springframework.web.context.support.AnnotationConfigWebApplicationContext");

		// Denial of server Filter
		javax.servlet.FilterRegistration.Dynamic dosFilter = context.addFilter("DoSFilter", "org.eclipse.jetty.servlets.DoSFilter");
		dosFilter.setInitParameter("delayMs", "1000");
		dosFilter.setInitParameter("trackSessions", "false");
		dosFilter.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");

		// GZIP filter
		dosFilter = context.addFilter("GzipFilter", "org.eclipse.jetty.servlets.GzipFilter");
		dosFilter.setInitParameter("mimeTypes",
				"text/html,text/plain,text/xml,application/xhtml+xml,text/css,application/javascript,image/svg+xml");
		dosFilter.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");
	}
}
