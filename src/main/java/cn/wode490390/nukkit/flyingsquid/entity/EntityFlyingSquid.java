package cn.wode490390.nukkit.flyingsquid.entity;

import cn.nukkit.Server;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.passive.EntitySquid;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.network.protocol.EntityEventPacket;
import java.util.concurrent.ThreadLocalRandom;

public class EntityFlyingSquid extends EntitySquid {

    protected Vector3 direction;
    protected int switchDirectionTicker;

    public EntityFlyingSquid(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    @Override
    public float getWidth() {
        return 0.95f;
    }

    @Override
    public float getHeight() {
        return 0.95f;
    }

    @Override
    public String getName() {
        return "Flying Squid";
    }

    @Override
    public Item[] getDrops() {
        return new Item[0];
    }

    @Override
    public void initEntity() {
        super.initEntity();

        this.setNameTag(this.getName());
        this.setNameTagVisible();
        this.setNameTagAlwaysVisible();
    }

    @Override
    public boolean onUpdate(int currentTick) {
        if (this.closed) {
            return false;
        }

        if (!this.isAlive()) {
            ++this.deadTicks;
            if (this.deadTicks >= 10) {
                this.despawnFromAll();
                if (!this.isPlayer) {
                    this.close();
                }
            }
            return this.deadTicks < 10;
        }

        int tickDiff = currentTick - this.lastUpdate;

        if (tickDiff <= 0) {
            return false;
        }

        this.lastUpdate = currentTick;

        boolean hasUpdate = this.entityBaseTick(tickDiff);

        if (++this.switchDirectionTicker >= 100) {
            this.switchDirectionTicker = 0;
            if (ThreadLocalRandom.current().nextInt(0, 101) < 50) {
                this.direction = null;
            }
        }

        if (this.isAlive()) {
            if (this.age < 12000) { //10min * 60sec * 20tick
                this.setAirTicks(300);
            }

            if (this.y > 250 && this.direction != null) {
                this.direction.y = -0.5;
            }

            if (this.direction != null) {
                Vector3 motion = new Vector3(this.motionX, this.motionY, this.motionZ);
                if (motion.lengthSquared() <= this.direction.lengthSquared()) {
                    motion = this.direction.multiply(this.movementSpeed);
                    this.motionX = motion.x;
                    this.motionY = motion.y;
                    this.motionZ = motion.z;
                }
            } else {
                this.direction = new Vector3(
                        ThreadLocalRandom.current().nextInt(-1000, 1001) / 1000d,
                        ThreadLocalRandom.current().nextInt(-500, 501) / 1000d,
                        ThreadLocalRandom.current().nextInt(-1000, 1001) / 1000d);
                this.movementSpeed = ThreadLocalRandom.current().nextInt(50, 101) / 2000f;
            }

            double f = Math.sqrt(Math.pow(this.motionX, 2) + Math.pow(this.motionZ, 2));
            this.yaw = (-Math.atan2(this.motionX, this.motionZ) * 180 / Math.PI);
            this.pitch = (-Math.atan2(f, this.motionY) * 180 / Math.PI);

            this.move(this.motionX, this.motionY, this.motionZ);
        }

        this.updateMovement();

        return hasUpdate;
    }

    @Override
    public boolean attack(EntityDamageEvent source) {
        boolean success = super.attack(source);

        if (success && source instanceof EntityDamageByEntityEvent) {
            this.movementSpeed = ThreadLocalRandom.current().nextInt(150, 351) / 2000f;
            Entity damager = ((EntityDamageByEntityEvent) source).getDamager();
            if (damager != null) {
                this.direction = this.subtract(damager).normalize();
            }

            this.spawnInk();
        }

        return success;
    }

    protected void spawnInk() {
        EntityEventPacket pk = new EntityEventPacket();
        pk.eid = this.getId();
        pk.event = EntityEventPacket.SQUID_INK_CLOUD;
        Server.broadcastPacket(this.getViewers().values(), pk);
    }
}
