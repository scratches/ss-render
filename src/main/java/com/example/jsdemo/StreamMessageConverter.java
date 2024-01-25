package com.example.jsdemo;

import java.io.IOException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import io.micrometer.context.ContextRegistry;
import io.micrometer.context.ThreadLocalAccessor;
import reactor.core.publisher.Hooks;

@Component
public class StreamMessageConverter
		implements HttpMessageConverter<Greeting> {

	private static Log log = LogFactory.getLog(StreamMessageConverter.class);

	public StreamMessageConverter() {
		Hooks.enableAutomaticContextPropagation();
		ContextRegistry.getInstance().registerThreadLocalAccessor(new RequestContextHolderAccessor());
	}

	class RequestContextHolderAccessor implements ThreadLocalAccessor<RequestAttributes> {

		private Log log = LogFactory.getLog(getClass());

		@Override
		public Object key() {
			return RequestAttributes.class.getName();
		}

		@Override
		public RequestAttributes getValue() {
			log.info("Getting value: " + RequestContextHolder.getRequestAttributes());
			return RequestContextHolder.getRequestAttributes();
		}

		@Override
		public void setValue(RequestAttributes value) {
			log.info("Setting value: " + RequestContextHolder.getRequestAttributes() + ", " + value);
			RequestContextHolder.setRequestAttributes(value);
		}

		@Override
		public void setValue() {
			log.info("Resetting value: " + RequestContextHolder.getRequestAttributes());
			// RequestContextHolder.resetRequestAttributes();
		}

	}

	@Override
	public boolean canRead(Class<?> clazz, MediaType mediaType) {
		return false;
	}

	@Override
	public boolean canWrite(Class<?> clazz, MediaType mediaType) {
		return Greeting.class.isAssignableFrom(clazz);
	}

	@Override
	public List<MediaType> getSupportedMediaTypes() {
		return List.of(MediaType.TEXT_HTML);
	}

	@Override
	public Greeting read(Class<? extends Greeting> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {
		throw new UnsupportedOperationException("Write only");
	}

	@Override
	public void write(Greeting rendering, MediaType contentType, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {
		RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
		log.info("Writing " + rendering + " for " + attrs);
		try {
			outputMessage.getBody().write(("<span> Hello " + rendering.getValue() + "</span>").getBytes());
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

}
