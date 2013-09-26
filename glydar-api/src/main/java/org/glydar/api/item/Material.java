package org.glydar.api.item;

public enum Material {

    IRON,

    WOOD,

    UNKNOWN_3,

    UNKNOWN_4,

    OBSIDIAN,

    UNKNOWN_6,

    BONE,

    UNKNOWN_7,

    UNKNOWN_8,

    UNKNOWN_9,

    UNKNOWN_10,

    GOLD,

    SILVER,

    EMERALD,

    SAPPHIRE,

    RUBY,

    DIAMOND,

    SANDSTONE,

    SAURIAN,

    PARROT,

    MAMMOTH,

    PLANT,

    ICE,

    LICHT,

    GLASS,

    SILK,

    LINEN,

    COTTON,

    FIRE_SPIRIT,

    UNHOLY_SPIRIT,

    ICE_SPIRIT,

    WIND_SPIRIT;

    private static final int SPIRITS_OFFSET = 128 - FIRE_SPIRIT.ordinal();

    public byte getId() {
        if (ordinal() >= FIRE_SPIRIT.ordinal()) {
            return (byte) (ordinal() + SPIRITS_OFFSET);
        }

        return (byte) ordinal();
    }

    public static Material getById(byte id) {
        if (id >= 0 || id < FIRE_SPIRIT.ordinal()) {
            return values()[id];
        }

        byte offsetId = (byte) (id - SPIRITS_OFFSET);
        if (offsetId >= FIRE_SPIRIT.ordinal() || offsetId < values().length) {
            return values()[offsetId];
        }

        return null;
    }
}
