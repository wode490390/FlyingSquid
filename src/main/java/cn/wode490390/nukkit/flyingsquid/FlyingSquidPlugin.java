package cn.wode490390.nukkit.flyingsquid;

import cn.nukkit.entity.Entity;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import cn.wode490390.nukkit.flyingsquid.config.LevelConfig;
import cn.wode490390.nukkit.flyingsquid.entity.FlyingSquidType;
import cn.wode490390.nukkit.flyingsquid.scheduler.FlyingSquidSpawner;
import cn.wode490390.nukkit.flyingsquid.util.MetricsLite;
import com.google.common.collect.Maps;
import java.util.Map;

public class FlyingSquidPlugin extends PluginBase {

    private FlyingSquidSpawner spawner;

    @Override
    public void onEnable() {
        try {
            //new MetricsLite(this);
        } catch (Throwable ignore) {

        }

        this.saveDefaultConfig();
        Config config = this.getConfig();

        String node = "spawn-every-x-seconds";
        int sleepSec = 20;
        try {
            sleepSec = config.getInt(node, sleepSec);
        } catch (Exception e) {
            this.logConfigException(node, e);
        }

        node = "levels";
        Map<String, Object> section;
        try {
            section = config.getSection(node);
        } catch (Exception e) {
            this.logConfigException(node, e);
            return;
        }

        Map<String, LevelConfig> levelConfigs = Maps.newHashMap();
        for (String key : section.keySet()) {
            node = "levels." + key + ".flying-squids";
            int normalLimit = 8;
            try {
                normalLimit = config.getInt(node, normalLimit);
            } catch (Exception ignore) {

            }
            node = "levels." + key + ".explosive-flying-squids";
            int explosiveLimit = 8;
            try {
                explosiveLimit = config.getInt(node, explosiveLimit);
            } catch (Exception ignore) {

            }
            levelConfigs.put(key, new LevelConfig(normalLimit, explosiveLimit));
        }

        this.registerEntities();

        if (!levelConfigs.isEmpty()) {
            this.spawner = new FlyingSquidSpawner(this, levelConfigs, sleepSec * 1000);
            this.spawner.start();
        }
    }

    private void registerEntities() {
        this.registerEntity(FlyingSquidType.NORMAL);
        this.registerEntity(FlyingSquidType.EXPLOSIVE);
    }

    private void registerEntity(FlyingSquidType type) {
        Entity.registerEntity(type.getNamespacedId(), type.getClazz());
    }

    private void logConfigException(String node, Throwable t) {
        this.getLogger().alert("An error occurred while reading the configuration '" + node + "'. Use the default value.", t);
    }
}
