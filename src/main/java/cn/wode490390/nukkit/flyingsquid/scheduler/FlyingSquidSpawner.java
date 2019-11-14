package cn.wode490390.nukkit.flyingsquid.scheduler;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.entity.Entity;
import cn.nukkit.level.ChunkPosition;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.level.format.generic.BaseFullChunk;
import cn.nukkit.math.MathHelper;
import cn.nukkit.math.Vector3;
import cn.wode490390.nukkit.flyingsquid.FlyingSquidPlugin;
import cn.wode490390.nukkit.flyingsquid.config.LevelConfig;
import cn.wode490390.nukkit.flyingsquid.entity.FlyingSquidType;
import cn.wode490390.nukkit.flyingsquid.util.LevelReader;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class FlyingSquidSpawner extends Thread {

    private final FlyingSquidPlugin plugin;
    private final Server server;
    private final Map<String, LevelConfig> levelConfigs;
    private final int sleepTime;

    private final ThreadLocalRandom rand = ThreadLocalRandom.current();
    private final LevelReader levelReader  = new LevelReader();
    private final Map<Level, Set<ChunkPosition>> eligible = Maps.newHashMap();

    public FlyingSquidSpawner(FlyingSquidPlugin plugin, Map<String, LevelConfig> levelConfigs, int sleepTime) {
        this.plugin = plugin;
        this.server = plugin.getServer();
        this.levelConfigs = levelConfigs;
        this.sleepTime = sleepTime;
    }

    @Override
    public void run() {
        while (this.plugin.isEnabled()) {
            this.onRun();
            try {
                sleep(this.sleepTime);
            } catch (InterruptedException ignore) {

            }
        }
    }

    public void onRun() {
        this.eligible.clear();
        int _chunkCount = 0;
        for (Player player : this.server.getOnlinePlayers().values()) {
            Level level;
            if (player.spawned && player.isAlive() && !player.isSpectator() && this.levelConfigs.get((level = player.level).getName()) != null) {
                Set<ChunkPosition> chunks = this.eligible.getOrDefault(level, Sets.newHashSet());
                int chunkX = player.getChunkX();
                int chunkZ = player.getChunkZ();
                for (int offsetX = -8; offsetX <= 8; ++offsetX) {
                    for (int offsetZ = -8; offsetZ <= 8; ++offsetZ) {
                        ChunkPosition chunkpos = new ChunkPosition(chunkX + offsetX, 0, chunkZ + offsetZ);
                        if (!chunks.contains(chunkpos)) {
                            ++_chunkCount;
                            if (level.isChunkLoaded(chunkpos.x, chunkpos.z)) {
                                chunks.add(chunkpos);
                            }
                        }
                    }
                }
                this.eligible.putIfAbsent(level, chunks);
            }
        }
        int chunkCount = _chunkCount;
        Vector3 vec = new Vector3();
        this.levelConfigs.forEach((name, config) -> {
            Level level = this.server.getLevelByName(name);
            if (level != null) {
                Set<ChunkPosition> chunks = this.eligible.get(level);
                if (chunks != null && chunks.size() > 0) {
                    Entity[] entities = level.getEntities();
                    if (entities.length > 0) {
                        Vector3 spawnPoint = level.getSpawnLocation();
                        this.levelReader.setLevel(level);
                        for (FlyingSquidType type : FlyingSquidType.values()) {
                            int count = 0;
                            for (Entity entity : entities) {
                                if (!entity.closed && entity.isAlive() && type.getClazz().isAssignableFrom(entity.getClass())) {
                                    ++count;
                                }
                            }
                            int max = (int) (config.limitsById[type.getId()] * chunkCount / 289f);
                            if (count <= max) {
                                world:
                                for (ChunkPosition chunkPos : chunks) {
                                    int chunkX = chunkPos.x;
                                    int chunkZ = chunkPos.z;
                                    BaseFullChunk chunk = level.getChunkIfLoaded(chunkX, chunkZ);
                                    if (chunk != null) {
                                        int x = (chunkX << 4) + this.rand.nextInt(16);
                                        int z = (chunkZ << 4) + this.rand.nextInt(16);
                                        int y = this.rand.nextInt(chunk.getHighestBlockAt(x & 0xf, z & 0xf) + 1);
                                        Block block = this.levelReader.getNormalBlockIfLoaded(x, y, z);
                                        if (block != null && !block.isNormalBlock()) {
                                            int cluster = 0;
                                            for (int i = 0; i < 3; ++i) {
                                                int tx = x;
                                                int ty = y;
                                                int tz = z;
                                                for (int chance = 0; chance < MathHelper.ceil(this.rand.nextFloat() * 4); ++chance) {
                                                    tx += this.rand.nextInt(6) - this.rand.nextInt(6);
                                                    ty += this.rand.nextInt(1) - this.rand.nextInt(1);
                                                    tz += this.rand.nextInt(6) - this.rand.nextInt(6);
                                                    float xCenter = tx + 0.5f;
                                                    float zCenter = tz + 0.5f;
                                                    int range = 24;
                                                    vec.setComponents(xCenter, ty, zCenter);
                                                    if (spawnPoint.distanceSquared(vec) >= 576) {
                                                        boolean noPlayersNearby = true;
                                                        for (Player player : ImmutableMap.copyOf(level.getPlayers()).values()) {
                                                            if (!player.isSpectator() && player.distanceSquared(vec) < Math.pow(range, 2)) {
                                                                noPlayersNearby = false;
                                                                break;
                                                            }
                                                        }
                                                        if (noPlayersNearby) {
                                                            Block down = this.levelReader.getNormalBlockIfLoaded(tx, ty - 1, tz);
                                                            if (down != null && !down.isTransparent() && down.isSolid()) {
                                                                int id = down.getId();
                                                                if (id != Block.BEDROCK && id != Block.INVISIBLE_BEDROCK && this.levelReader.isNormalAir(tx, ty, tz) && this.levelReader.isNormalAir(tx, ty + 1, tz)) {
                                                                    Entity entity = Entity.createEntity(type.getNamespacedId(), Position.fromObject(vec, level));
                                                                    if (entity != null) {
                                                                        entity.yaw = this.rand.nextFloat() * 360;
                                                                        if (true || !entity.isCollided) { //TODO
                                                                            ++cluster;
                                                                            entity.spawnToAll();
                                                                            if (cluster >= 4) {
                                                                                continue world;
                                                                            }
                                                                        } else {
                                                                            entity.close();
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });
    }
}
