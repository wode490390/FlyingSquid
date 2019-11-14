package cn.wode490390.nukkit.flyingsquid.entity;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityExplosive;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.entity.EntityDamageEvent.DamageCause;
import cn.nukkit.event.entity.EntityExplosionPrimeEvent;
import cn.nukkit.level.Explosion;
import cn.nukkit.level.GameRule;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.particle.HugeExplodeSeedParticle;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.utils.TextFormat;

public class EntityFlyingSquidExplosive extends EntityFlyingSquid implements EntityExplosive {

    public EntityFlyingSquidExplosive(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    @Override
    public String getName() {
        return TextFormat.RED + "Explosive" + TextFormat.RESET + " Flying Squid";
    }

    @Override
    public boolean attack(EntityDamageEvent source) {
        boolean success = super.attack(source);

        DamageCause cause = source.getCause();
        if (success && (cause == DamageCause.BLOCK_EXPLOSION ||
                cause == DamageCause.ENTITY_ATTACK ||
                cause == DamageCause.ENTITY_EXPLOSION ||
                cause == DamageCause.FIRE ||
                cause == DamageCause.FIRE_TICK ||
                cause == DamageCause.LAVA ||
                cause == DamageCause.LIGHTNING ||
                cause == DamageCause.MAGIC ||
                cause == DamageCause.PROJECTILE)) {
            this.explode();

            return true;
        }

        return success;
    }

    @Override
    public boolean onUpdate(int currentTick) {
        boolean hasUpdate = super.onUpdate(currentTick);

        if (hasUpdate) {
            for (Entity entity : this.level.getNearbyEntities(this.boundingBox.grow(0.2, 0, 0.2), this)) {
                if (entity instanceof Player) {
                    Player player = (Player) entity;
                    if (player.spawned && player.isAlive() && (player.gamemode & 0x1) == 0) {
                        this.spawnInk();
                        this.explode();
                    }
                }
            }
        }

        return hasUpdate;
    }

    @Override
    public void explode() {
        EntityExplosionPrimeEvent event = new EntityExplosionPrimeEvent(this, 2.5);
        this.server.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            this.close();

            Explosion explode = new Explosion(this, event.getForce(), this);
            if (event.isBlockBreaking() && this.level.getGameRules().getBoolean(GameRule.MOB_GRIEFING)) {
                explode.explodeA();
            }
            explode.explodeB();

            this.level.addParticle(new HugeExplodeSeedParticle(this));
        }
    }
}
