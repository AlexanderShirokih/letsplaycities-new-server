package ru.quandastudio.lpsserver.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Banlist {

	private static final File banLoginFile = new File("database/login.ban");

	private static ArrayList<String> bannedLogin = new ArrayList<>(1);

	public static void loadBanDatabase() {
		if (banLoginFile.exists()) {
			try {
				BufferedReader br = new BufferedReader(new FileReader(banLoginFile));
				String line = null;
				int size = Integer.parseInt(br.readLine());
				bannedLogin = new ArrayList<>(size);
				while ((line = br.readLine()) != null) {
					bannedLogin.add(line);
				}
				br.close();
				log.info("Parsed login ban list");
			} catch (Exception e) {
				log.warn("ERR loadingLoginBanList: {}", e.toString());
			}
		} else {
			log.warn("Log file NOT exists!");
		}
	}

	public static void reloadBanDatabase() {
		ArrayList<String> tempL = bannedLogin;
		loadBanDatabase();
		tempL.clear();
	}

	static {
		loadBanDatabase();
	}

	public static boolean checkLogin(String login) {
		for (String bad : bannedLogin)
			if (login.contains(bad)) {
				log.info("Banned user was detected! Login={}", login);
				return false;
			}
		return true;
	}
}
