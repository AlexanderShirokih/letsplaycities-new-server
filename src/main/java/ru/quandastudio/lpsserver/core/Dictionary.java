package ru.quandastudio.lpsserver.core;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Dictionary {
	public static class DatabasePair {
		ArrayList<String> data;
		ArrayList<CityInfo> additionalData;

		public DatabasePair(ArrayList<String> data, ArrayList<CityInfo> additionalData) {
			this.data = data;
			this.additionalData = additionalData;
		}
	}

	public static class CityInfo {
		byte diff;
		short countryCode;
	}

	private static final Random rand = new Random();

	private static boolean CORRECTION = true;

	private static ArrayList<CityInfo> additionalData;
	private static ArrayList<String> data;

	static DatabasePair loadDictionary() {
		log.info("Loading dictionary...");

		if (!CORRECTION)
			log.warn("Note that correction is DISABLED");

		ArrayList<String> data = null;
		ArrayList<CityInfo> additionalData = null;
		final String database = getLastDatabaseName();
		try (DataInputStream in = new DataInputStream(new FileInputStream(database))) {
			int count = in.readInt();
			int version = in.readInt();
			int countTest = in.readInt();
			if (count != countTest >> 12)
				throw new RuntimeException("Invalid dictionary file! Header doesn't matches");

			log.info("File version: " + version);
			log.info("Found " + count + " entities");

			data = new ArrayList<>(count);
			additionalData = new ArrayList<>(count);

			for (int i = 0; i < count + 1; i++) {
				int len = in.readUnsignedByte();
				CityInfo city = new CityInfo();
				StringBuilder sb = new StringBuilder(len);
				for (int l = 0; l < len; l++) {
					sb.append((char) (in.readChar() - 513));
				}
				String name = sb.toString();
				city.diff = in.readByte();
				city.countryCode = in.readShort();

				if (i == count) {
					if (Integer.parseInt(name.substring(0, name.length() - 6)) != count) {
						in.close();
						throw new RuntimeException("Error parsing file!");
					}
				} else {
					data.add(name);
					additionalData.add(city);
				}
			}

			log.info("Parsed {} unique entities", data.size());
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (data == null) {
				log.warn("!!! DATA IS NULL !!!");
			}
		}
		return new DatabasePair(data, additionalData);
	}

	private static String getLastDatabaseName() {
		String lastVersion = "";

		try (BufferedReader br = new BufferedReader(new FileReader("www/getdata"))) {
			lastVersion = br.readLine().trim();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return "www/data-" + lastVersion + ".bin";
	}

	static {
		DatabasePair dp = loadDictionary();
		data = dp.data;
		additionalData = dp.additionalData;
	}

	public static void setCorrectionEnabled(boolean b) {
		CORRECTION = b;
	}

	public static void reloadDictionary() {
		synchronized (data) {
			DatabasePair dp = loadDictionary();
			ArrayList<String> newData = dp.data;
			ArrayList<String> oldData = data;
			ArrayList<CityInfo> newAddData = dp.additionalData;
			ArrayList<CityInfo> oldAddData = additionalData;
			data = newData;
			additionalData = newAddData;
			log.info("Data was swapped");
			oldData.clear();
			oldData = null;
			oldAddData.clear();
			oldAddData = null;
			log.info("Old data was cleared");
		}
	}

	public boolean contains(String word) {
		return !CORRECTION || data.contains(word);
	}

	public static String getCity(char firstChar, byte diff, int[] prefCountries) {

//		log.info("Looking for the next word fc=" + firstChar + "; diff=" + diff + "; pref="
//				+ Arrays.toString(prefCountries));
		ArrayList<Integer> d = new ArrayList<Integer>(1024);

		for (int i = 0; i < additionalData.size(); i++) {
			final CityInfo c = additionalData.get(i);
			if (c.diff > diff)
				continue;
			if (prefCountries == null) {
				if (firstChar == data.get(i).charAt(0))
					d.add(i);
			} else {
				for (int j : prefCountries)
					if (j == c.countryCode && firstChar == data.get(i).charAt(0))
						d.add(i);
			}
		}

		if (d.size() == 0) {
			log.info("No more words...");
			return null;
		}
		int idx = rand.ints(0, d.size()).findFirst().getAsInt();
//		log.info("found size=" + d.size());
//		log.info("gen idx=" + idx);

		int index = d.get(idx);
		d.clear();

//		log.info("Found " + data.get(index));
		return data.get(index);
	}
}
