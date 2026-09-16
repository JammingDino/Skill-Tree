package com.jd_skill_tree.networking;

import net.minecraft.network.RegistryByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Convenience holder for creating bufs for payloads.
 * 1.21.x payloads are written into a RegistryByteBuf (fabric-api payload handlers
 * supply one automatically; server->client sends build one here).
 */
public final class SkillBufs {
    private SkillBufs() {}

    public static RegistryByteBuf create() {
        // Replaces the old PacketByteBufs.create() path; the registry variant is what
        // 1.21 CustomPayload writing requires. Registry-dependent entries cannot be
        // written through this buffer (none of our payloads use them).
        return new RegistryByteBuf(Unpooled.buffer(), null);
    }
}
