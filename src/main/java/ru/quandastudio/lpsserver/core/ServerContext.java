package ru.quandastudio.lpsserver.core;

import java.util.concurrent.ArrayBlockingQueue;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class ServerContext {
	
	@Getter
	private ArrayBlockingQueue<Player> playersQueue = new ArrayBlockingQueue<Player>(256);

}
