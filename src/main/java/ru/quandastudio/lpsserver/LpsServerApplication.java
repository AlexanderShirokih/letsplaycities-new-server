package ru.quandastudio.lpsserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;

import lombok.RequiredArgsConstructor;
import ru.quandastudio.lpsserver.netty.LPSServer;

@RequiredArgsConstructor
@SpringBootApplication
public class LpsServerApplication {

	private final LPSServer lpsServer;

	public static void main(String[] args) {
		SpringApplication.run(LpsServerApplication.class, args);
	}

	@Bean
	public ApplicationListener<ApplicationReadyEvent> readyEventApplicationListener() {
		return new ApplicationListener<ApplicationReadyEvent>() {
			@Override
			public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
				lpsServer.start();
			}
		};
	}
}
