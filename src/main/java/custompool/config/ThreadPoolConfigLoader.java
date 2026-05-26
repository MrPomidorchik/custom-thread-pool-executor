package custompool.config;

import custompool.rejection.AbortPolicy;
import custompool.rejection.CallerRunsPolicy;
import custompool.rejection.CustomRejectedExecutionHandler;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class ThreadPoolConfigLoader {

    private final Properties properties = new Properties();

    public ThreadPoolConfigLoader(String fileName) {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("Config file not found: " + fileName);
            }

            properties.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config file: " + fileName, e);
        }
    }

    public String getPoolName() {
        return getString("POOL_NAME");
    }

    public ThreadPoolConfig getThreadPoolConfig() {
        return new ThreadPoolConfig(
                getInt("CORE_POOL_SIZE"),
                getInt("MAX_POOL_SIZE"),
                getLong("KEEP_ALIVE_TIME"),
                TimeUnit.valueOf(getString("TIME_UNIT")),
                getInt("QUEUE_SIZE"),
                getInt("MIN_SPARE_THREADS")
        );
    }

    public CustomRejectedExecutionHandler getRejectedExecutionHandler() {
        String policy = getString("REJECTION_POLICY");

        return switch (policy) {
            case "ABORT" -> new AbortPolicy();
            case "CALLER_RUNS" -> new CallerRunsPolicy();
            default -> throw new IllegalArgumentException("Unknown rejection policy: " + policy);
        };
    }

    private String getString(String key) {
        String value = properties.getProperty(key);

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing config value: " + key);
        }

        return value.trim();
    }

    private int getInt(String key) {
        return Integer.parseInt(getString(key));
    }

    private long getLong(String key) {
        return Long.parseLong(getString(key));
    }
}
