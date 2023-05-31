package io.github.jshipit;

import com.moandjiezana.toml.Toml;

import java.io.File;
import java.util.List;

public class ConfigParser {
    public String configPath;

    public ConfigParser(String configPath) {
        this.configPath = configPath;
    }

    public void parseConfig() {
        System.out.println("Parsing config");
        Toml toml = new Toml().read(new File(this.configPath));
    }

    public String getString(String key) {
        Toml toml = new Toml().read(new File(this.configPath));
        return toml.getString(key);
    }

    public boolean getBoolean(String key) {
        Toml toml = new Toml().read(new File(this.configPath));
        return toml.getBoolean(key);
    }

    public List<String> getList(String key) {
        Toml toml = new Toml().read(new File(this.configPath));
        return toml.getList(key);
    }

    public long getLong(String key) {
        Toml toml = new Toml().read(new File(this.configPath));
        return toml.getLong(key);
    }

    public double getDouble(String key) {
        Toml toml = new Toml().read(new File(this.configPath));
        return toml.getDouble(key);
    }

}
