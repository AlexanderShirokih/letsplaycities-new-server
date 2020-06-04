package ru.quandastudio.lpsserver.core;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import ru.quandastudio.lpsserver.LPSException;

@Slf4j
@Component
@Scope("singleton")
public class Dictionary {

    private static final Random rand = new Random();

    @Getter
    @Setter
    private boolean isCorrectionEnabled = true;

    private ArrayList<CityInfo> additionalData;
    private ArrayList<String> data;

    protected DatabasePair loadDictionary() {
        log.info("Loading dictionary...");

        if (!isCorrectionEnabled)
            log.warn("Note that correction is DISABLED");

        ArrayList<String> data = null;
        ArrayList<CityInfo> additionalData = null;
        final String database = getDatabasePath(String.valueOf(getLastDatabaseVersion()));

        log.info("Reading database file {}", database);

        try (DataInputStream in = new DataInputStream(new FileInputStream(database))) {
            int count = in.readInt();
            int version = in.readInt();
            int countTest = in.readInt();
            if (count != countTest >> 12)
                throw new LPSException("Invalid dictionary file! Header doesn't matches");

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
                        throw new LPSException("Error parsing file!");
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

    private String getDatabasePath(String version) {
        return "database/data-" + version + ".bin";
    }

    public int getLastDatabaseVersion() {
        final File databaseFolder = new File("database");

        return Arrays
                .stream(Objects.requireNonNull(databaseFolder.list((File dir, String name) -> name.matches("data-\\d+.bin"))))
                .mapToInt((name) -> Integer.parseInt(name.substring(5, name.length() - 4)))
                .max()
                .orElseThrow();
    }

    public Dictionary() {
        DatabasePair dp = loadDictionary();
        data = dp.data;
        additionalData = dp.additionalData;
    }

    public void reloadDictionary() {
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
            oldAddData.clear();
            log.info("Old data was cleared");
        }
    }

    public boolean contains(String word) {
        return !isCorrectionEnabled || data.contains(word);
    }

    public String getCity(char firstChar, byte diff, int[] prefCountries) {
        ArrayList<Integer> d = new ArrayList<>(1024);

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
        int index = d.get(idx);
        d.clear();

        return data.get(index);
    }

    public Optional<File> getDatabaseFile(String version) {
        if (version.length() > 12)
            return Optional.empty();

        final String dbFilename = getDatabasePath(version);
        final File file = new File(dbFilename);
        if (file.exists())
            return Optional.of(file);

        return Optional.empty();
    }

    @RequiredArgsConstructor
    @Getter
    public static class DatabasePair {
        private final ArrayList<String> data;
        private final ArrayList<CityInfo> additionalData;
    }

    public static class CityInfo {
        byte diff;
        short countryCode;
    }
}
