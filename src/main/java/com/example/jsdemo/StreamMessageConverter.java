package com.example.jsdemo;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributesThreadLocalAccessor;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.util.ContentCachingResponseWrapper;

import io.micrometer.context.ContextRegistry;
import jakarta.servlet.http.HttpServletRequest;
import reactor.core.publisher.Hooks;

@Component
public class StreamMessageConverter
		implements HttpMessageConverter<ModelAndView> {

	private static Log log = LogFactory.getLog(StreamMessageConverter.class);
	private final ViewResolver resolver;

	public StreamMessageConverter(@Qualifier("viewResolver") ViewResolver resolver) {
		this.resolver = resolver;
		Hooks.enableAutomaticContextPropagation();
		ContextRegistry.getInstance().registerThreadLocalAccessor(new RequestAttributesThreadLocalAccessor());
	}

	@Override
	public boolean canRead(Class<?> clazz, MediaType mediaType) {
		return false;
	}

	@Override
	public boolean canWrite(Class<?> clazz, MediaType mediaType) {
		boolean result = ModelAndView.class.isAssignableFrom(clazz);
		if (result) {
			// Reset the attributes otherwise the view resolver will kill it later
			ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
			RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(attrs.getRequest(), attrs.getResponse()));
		}
		return result;
	}

	@Override
	public List<MediaType> getSupportedMediaTypes() {
		return List.of(MediaType.TEXT_HTML);
	}

	@Override
	public ModelAndView read(Class<? extends ModelAndView> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {
		throw new UnsupportedOperationException("Write only");
	}

	@Override
	public void write(ModelAndView rendering, MediaType contentType, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {
		ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		HttpServletRequest request = attrs.getRequest();
		request.setAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, Set.of(MediaType.TEXT_HTML));
		try {
			View view = rendering.getView();
			if (view == null) {
				view = resolver.resolveViewName(rendering.getViewName(), request.getLocale());
			}
			ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(attrs.getResponse());
			view.render(rendering.getModel(), request, wrapper);
			outputMessage.getBody().write(wrapper.getContentAsByteArray());
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

}
