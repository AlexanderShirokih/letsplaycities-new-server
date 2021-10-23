package ru.quandastudio.lpsserver;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.quandastudio.lpsserver.core.ServerContext;

import javax.annotation.PreDestroy;

@Slf4j
@RequiredArgsConstructor
@Component
public class LPSServer {


    private final ServerContext context;

    public void start() {
        context.getBotManager().init();
        context.getTaskLooper().start();
    }

    @PreDestroy
    public void stop() {
        log.info("Shutting down LPS server...");
        context.getBotManager().shutdown();
        context.getTaskLooper().shutdown();
    }

}
