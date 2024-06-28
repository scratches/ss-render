package com.example.jsdemo;

import java.time.Duration;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import reactor.core.publisher.Flux;

@SpringBootApplication
@Controller
public class DemoApplication {

	@GetMapping(path = "/", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ModelAndView> stream() {
		return Flux.interval(Duration.ofSeconds(5)).map(value -> new ModelAndView("stream", Map.of("greeting", new Greeting("bar"))));
	}

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

}