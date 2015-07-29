package org.nem.nis.controller.interceptors;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.test.ExceptionAssert;
import org.nem.core.utils.ExceptionUtils;
import org.nem.nis.controller.annotations.TrustedApi;
import org.springframework.web.method.HandlerMethod;

import javax.servlet.http.*;
import java.lang.reflect.Method;

public class LocalHostInterceptorTest {
	private static final String LOCAL_ADDRESS = "127.0.0.1";

	@Test
	public void remoteAccessIsAllowedForUnannotatedMethod() {
		// Assert:
		assertAccessGranted("unannotatedMethod", "194.66.82.11");
	}

	@Test
	public void remoteAccessIsNotAllowedForTrustedApi() {
		// Assert:
		assertAccessDenied("trustedMethod", "194.66.82.11");
	}

	@Test
	public void localAccessIsAllowedForUnannotatedMethod() {
		// Assert:
		assertAccessGranted("unannotatedMethod", LOCAL_ADDRESS);
	}

	@Test
	public void localAccessIsAllowedForTrustedApi() {
		// Assert:
		assertAccessGranted("trustedMethod", LOCAL_ADDRESS);
	}

	private static void assertAccessGranted(final String methodName, final String remoteAddress) {
		// Arrange:
		final LocalHostInterceptor interceptor = createInterceptor();

		// Act:
		final boolean result = preHandle(interceptor, methodName, remoteAddress);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(true));
	}

	private static void assertAccessDenied(final String methodName, final String remoteAddress) {
		// Arrange:
		final LocalHostInterceptor interceptor = createInterceptor();

		// Act / Assert:
		ExceptionAssert.assertThrows(v -> preHandle(interceptor, methodName, remoteAddress), UnauthorizedAccessException.class);
	}

	private static LocalHostInterceptor createInterceptor() {
		// Arrange:
		final LocalHostDetector detector = Mockito.mock(LocalHostDetector.class);
		Mockito.when(detector.isLocal(Mockito.any()))
				.thenAnswer(invocationOnMock -> {
					final HttpServletRequest request = (HttpServletRequest)invocationOnMock.getArguments()[0];
					return request.getRemoteAddr().equals(LOCAL_ADDRESS);
				});
		return new LocalHostInterceptor(detector);
	}

	public static boolean preHandle(final LocalHostInterceptor interceptor, final String methodName, final String remoteAddress) {
		// Arrange:
		final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Mockito.when(request.getRemoteAddr()).thenReturn(remoteAddress);
		final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

		final HandlerMethod handlerMethod = Mockito.mock(HandlerMethod.class);
		final Method method = ExceptionUtils.propagate(() -> LocalHostInterceptorTest.class.getMethod(methodName));
		Mockito.when(handlerMethod.getMethod()).thenReturn(method);

		// Act:
		return ExceptionUtils.propagate(() -> interceptor.preHandle(request, response, handlerMethod));
	}

	//region annotated methods

	public static void unannotatedMethod() {
	}

	@TrustedApi
	public static void trustedMethod() {
	}

	//endregion
}