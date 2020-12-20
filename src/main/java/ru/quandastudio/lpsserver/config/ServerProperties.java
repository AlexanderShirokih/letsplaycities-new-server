package ru.quandastudio.lpsserver.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import ru.quandastudio.lpsserver.LPSException;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class ServerProperties {

    private static final String CONFIG_FILE = "data/env.config.properties";

    private final Properties props = new Properties();

    public ServerProperties() {
        loadConfig();
    }

    public void loadConfig() {
        log.info("Searching config in path: {}", new File(CONFIG_FILE).getAbsolutePath());

        try (InputStream is = new FileInputStream(CONFIG_FILE)) {
            props.load(is);
        } catch (IOException e) {
            log.warn("Error loading config file");
            log.warn(e.toString());
        }
    }

    public int getNewerBuild() {
        return getIntOrThrow("client.newer_build");
    }

    public boolean isLoggingMsgsEnabled() {
        return getBoolOrThrow("server.msg_on_log");
    }

    public boolean isBotsEnabled() {
        return getBoolOrThrow("server.bots_enabled");
    }

    private int getIntOrThrow(String key) {
        String ret = props.getProperty(key);
        if (ret == null)
            throw new LPSException("Requested property \"" + key + "\" not found!");
        int retVal;
        try {
            retVal = Integer.parseInt(ret);
        } catch (NumberFormatException e) {
            throw new LPSException("Property \"" + key + "\" value is not an integer!");
        }
        return retVal;
    }

    private boolean getBoolOrThrow(String key) {
        String ret = props.getProperty(key);
        if (ret == null)
            throw new LPSException("Requested property \"" + key + "\" not found!");
        return Boolean.parseBoolean(ret);
    }
}
