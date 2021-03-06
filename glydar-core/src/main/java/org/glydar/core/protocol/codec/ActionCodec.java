package org.glydar.core.protocol.codec;

import io.netty.buffer.ByteBuf;

import org.glydar.api.model.geom.FloatVector3;
import org.glydar.core.model.actions.DamageAction;
import org.glydar.core.model.actions.KillAction;
import org.glydar.core.model.actions.PickupAction;
import org.glydar.core.model.actions.SoundAction;
import org.glydar.core.model.actions.SoundAction.SoundType;
import org.glydar.core.model.item.CoreItem;

/* Structures and data discovered by cuwo (http://github.com/matpow2) */
public final class ActionCodec {

    private ActionCodec() {
    }

    public static DamageAction readDamageAction(ByteBuf buf) {
        long damagerId = buf.readLong();
        long targetId = buf.readLong();
        DamageAction action = new DamageAction(damagerId, targetId);
        action.setDamage(buf.readFloat());
        buf.skipBytes(4);

        return action;
    }

    public static void writeDamageAction(ByteBuf buf, DamageAction action) {
        buf.writeLong(action.getDamagerId());
        buf.writeLong(action.getTargetId());
        buf.writeFloat(action.getDamage());
        buf.writeZero(4);
    }

    public static KillAction readKillAction(ByteBuf buf) {
        long killerId = buf.readLong();
        long targetId = buf.readLong();
        KillAction action = new KillAction(killerId, targetId);
        buf.skipBytes(4);
        action.setXp(buf.readInt());
        return action;
    }

    public static void writeKillAction(ByteBuf buf, KillAction action) {
        buf.writeLong(action.getKillerId());
        buf.writeLong(action.getTargetId());
        buf.writeZero(4);
        buf.writeInt(action.getXp());
    }

    public static PickupAction readPickupAction(ByteBuf buf) {
        long entityId = buf.readLong();
        CoreItem item = ItemCodec.readItem(buf);
        return new PickupAction(entityId, item);
    }

    public static void writePickupAction(ByteBuf buf, PickupAction action) {
        buf.writeLong(action.getEntityId());
        ItemCodec.writeItem(buf, action.getItem());
    }

    public static SoundAction readSoundAction(ByteBuf buf) {
        FloatVector3 position = GeomCodec.readFloatVector3(buf);
        int soundType = buf.readInt();
        SoundAction action = new SoundAction(position, soundType);
        action.setPitch(buf.readFloat());
        action.setVolume(buf.readFloat());
        return action;
    }

    public static void writeSoundAction(ByteBuf buf, SoundAction action) {
        GeomCodec.writeFloatVector3(buf, action.getPosition());
        buf.writeInt(action.getSoundType());
        buf.writeFloat(action.getPitch());
        buf.writeFloat(action.getVolume());
    }

    public static SoundType readSoundType(ByteBuf buf) {
        return SoundType.values()[buf.readInt()];
    }

    public static void writeSoundType(ByteBuf buf, SoundType soundType) {
        buf.writeInt(soundType.ordinal());
    }
}
