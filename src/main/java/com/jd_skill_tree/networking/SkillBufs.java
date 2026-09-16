package com.jd_skill_tree.networking;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.PacketByteBufs;

/**
 * Convenience holder for creating bufs for payloads.
 * 1.21.x payloads are written into a RegistryByteBuf.
 */
public final class SkillBufs {
    private SkillBufs() {}

    public static RegistryByteBuf create() {
        // PacketByteBufs.create() returns a PacketByteBuf over an Unpooled buffer;
        // in 1.21 the payload writer wants a RegistryByteBuf.
        return new RegistryByteBuf(PacketByteBufs.create(), null);
    }
}
