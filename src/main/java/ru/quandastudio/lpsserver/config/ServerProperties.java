package ru.quandastudio.lpsserver.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServerProperties {

	private static final String CONFIG_FILE = "env.config.properties";
	private static ServerProperties inst;

	private Properties props = new Properties();

	private ServerProperties() {
		loadConfig();
	}

	public static ServerProperties getInstance() {
		if (inst == null)
			inst = new ServerProperties();
		return inst;
	}

	public static void reloadConfig() {
		getInstance().loadConfig();
	}

	private void loadConfig() {
		try (InputStream is = new FileInputStream(new File(CONFIG_FILE))) {
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
			throw new RuntimeException("Requested property \"" + key + "\" not found!");
		int retVal = 0;
		try {
			retVal = Integer.parseInt(ret);
		} catch (NumberFormatException e) {
			throw new RuntimeException("Property \"" + key + "\" value is not an integer!");
		}
		return retVal;
	}

	private boolean getBoolOrThrow(String key) {
		String ret = props.getProperty(key);
		if (ret == null)
			throw new RuntimeException("Requested property \"" + key + "\" not found!");
		return Boolean.parseBoolean(ret);
	}
}
