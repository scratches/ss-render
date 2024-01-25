package com.example.jsdemo;

import java.time.Duration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import reactor.core.publisher.Flux;

@SpringBootApplication
@Controller
public class DemoApplication {

	private static Log log = LogFactory.getLog(DemoApplication.class);

	@GetMapping(path = "/", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<Greeting> stream() {
		return Flux.interval(Duration.ofSeconds(5)).map(value -> new Greeting("bar"));
	}

	@Bean
	public BeanPostProcessor taskExecutorInserter() {
		return new BeanPostProcessor() {
			@Override
			public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
				if (bean instanceof RequestMappingHandlerAdapter adapter) {
					adapter.setTaskExecutor(threadPoolTaskScheduler());
				}
				return bean;
			}
		};
	}

	static ThreadPoolTaskExecutor threadPoolTaskScheduler() {
		ThreadPoolTaskExecutor threadPoolTaskScheduler = new ThreadPoolTaskExecutor() {
			@Override
			public void execute(Runnable task) {
				RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
				log.info("Executing: " + attrs);
				super.execute(() -> {
					RequestContextHolder.setRequestAttributes(attrs);
					log.info("Running: " + attrs);
					task.run();
				});
			}
		};
		threadPoolTaskScheduler.initialize();
		return threadPoolTaskScheduler;
	}

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

}