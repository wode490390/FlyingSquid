package cn.wode490390.nukkit.flyingsquid.entity;

public enum FlyingSquidType {
    NORMAL(0, "wode:flying_squid", EntityFlyingSquid.class),
    EXPLOSIVE(1, "wode:flying_squid_explosive", EntityFlyingSquidExplosive.class),
    ;

    private final int id;
    private final String namespacedId;
    private final Class<? extends EntityFlyingSquid> clazz;

    private FlyingSquidType(int id, String namespacedId, Class<? extends EntityFlyingSquid> clazz) {
        this.id = id;
        this.namespacedId = namespacedId;
        this.clazz = clazz;
    }

    public int getId() {
        return this.id;
    }

    public String getNamespacedId() {
        return this.namespacedId;
    }

    public Class<? extends EntityFlyingSquid> getClazz() {
        return this.clazz;
    }
}
