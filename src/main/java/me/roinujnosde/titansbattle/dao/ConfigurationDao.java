package me.roinujnosde.titansbattle.dao;

import me.roinujnosde.titansbattle.BaseGameConfiguration;
import me.roinujnosde.titansbattle.challenges.ArenaConfiguration;
import me.roinujnosde.titansbattle.types.GameConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConfigurationDao {

    private final Logger logger = Logger.getLogger("TitansBattle");
    private final Map<Class<? extends BaseGameConfiguration>, Metadata> metadataMap;
    private final Set<BaseGameConfiguration> configurations;

    public ConfigurationDao(@NotNull final File dataFolder) {
        metadataMap = new HashMap<>();
        metadataMap.put(ArenaConfiguration.class, new Metadata(new File(dataFolder, "arenas"), "arena"));
        metadataMap.put(GameConfiguration.class, new Metadata(new File(dataFolder, "games"), "game"));
        configurations = new HashSet<>();

        loadConfigurations();
    }

    public void loadConfigurations() {
        configurations.clear();
        for (final Map.Entry<Class<? extends BaseGameConfiguration>, Metadata> entry : metadataMap.entrySet()) {
            final File folder = entry.getValue().folder;
            if (!folder.exists() && !folder.mkdirs()) {
                logger.log(Level.SEVERE, "Error creating folder {0}", folder.getAbsolutePath());
                continue;
            }
            final File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml") || name.endsWith(".yaml"));
            //noinspection ConstantConditions
            for (final File file : files) {
                final YamlConfiguration yamlConfig = YamlConfiguration.loadConfiguration(file);
                final BaseGameConfiguration gc = (BaseGameConfiguration) yamlConfig.get(entry.getValue().configKey);
                if (gc != null) {
                    configurations.add(gc);
                    gc.setFile(file);
                    gc.setFileConfiguration(yamlConfig);
                }
            }
        }
    }

    public @NotNull <T extends BaseGameConfiguration> Set<T> getConfigurations(@NotNull final Class<T> clazz) {
        final Set<T> set = new HashSet<>();
        for (final BaseGameConfiguration configuration : configurations) {
            if (clazz.isInstance(configuration)) {
                set.add(clazz.cast(configuration));
            }
        }
        return set;
    }

    public @NotNull <T extends BaseGameConfiguration> Optional<T> getConfiguration(@NotNull final String name,
                                                                                   @NotNull final Class<T> clazz) {
        final Set<T> configurations = getConfigurations(clazz);
        for (final T configuration : configurations) {
            if (configuration.getName().equalsIgnoreCase(name)) {
                return Optional.of(configuration);
            }
        }
        return Optional.empty();
    }

    public <T extends BaseGameConfiguration> boolean create(@NotNull String name, @NotNull final Class<T> clazz) {
        name = name.replace(" ", "_").replace(".", "");
        final Metadata metadata = metadataMap.get(clazz);
        if (metadata == null) {
            throw new IllegalArgumentException(String.format("Invalid config class: %s", clazz.getName()));
        }

        final File file = new File(metadata.folder, name + ".yml");
        try {
            if (!file.createNewFile()) {
                logger.log(Level.SEVERE, String.format("Error creating the config %s's file. Maybe it already exists?",
                        name));
                return false;
            }
            final T config = clazz.getConstructor().newInstance();
            config.setName(name);
            config.setFile(file);

            final YamlConfiguration yamlConfiguration = new YamlConfiguration();
            config.setFileConfiguration(yamlConfiguration);
            yamlConfiguration.set(metadata.configKey, config);
            yamlConfiguration.save(file);
            configurations.add(config);
            return true;
        } catch (final IOException | ReflectiveOperationException ex) {
            logger.log(Level.SEVERE, String.format("Error creating the config %s", name), ex);
        }
        return false;
    }

    public <T extends BaseGameConfiguration> boolean save(final T config) {
        final Metadata metadata = metadataMap.get(config.getClass());
        if (metadata == null) {
            logger.log(Level.SEVERE, "Invalid config class {0}", config.getClass().getName());
            return false;
        }
        final FileConfiguration fileConfiguration = config.getFileConfiguration();
        fileConfiguration.set(metadata.configKey, config);
        try {
            fileConfiguration.save(config.getFile());
            return true;
        } catch (final IOException e) {
            logger.log(Level.SEVERE, "Error saving config", e);
        }
        return false;
    }

    private static class Metadata {
        File folder;
        String configKey;

        public Metadata(final File folder, final String configKey) {
            this.folder = folder;
            this.configKey = configKey;
        }
    }

}
