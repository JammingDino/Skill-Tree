package com.jd_skill_tree.skills.actions;

import com.google.gson.JsonObject;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;

public class RaycastActionEffect implements SkillActionEffect {

    private final double length;
    private final boolean stopOnFluids;
    private final boolean hitEntities;
    private final SkillActionEffect childEffect;

    public RaycastActionEffect(double length, boolean stopOnFluids, boolean hitEntities, SkillActionEffect childEffect) {
        this.length = length;
        this.stopOnFluids = stopOnFluids;
        this.hitEntities = hitEntities;
        this.childEffect = childEffect;
    }

    @Override
    public void execute(Entity source, Level world, BlockPos pos) {
        if (childEffect == null) return;

        Vec3 start = source.getEyePosition();
        Vec3 rotation = source.getLookAngle();
        Vec3 end = start.add(rotation.multiply(length));

        // 1. Raycast Blocks
        ClipContext.FluidHandling fluidMode = stopOnFluids ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE;
        BlockHitResult blockHit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, fluidMode, source));

        BlockPos hitPos = blockHit.blockPosition();
        Entity hitEntity = null;

        // 2. Raycast Entities (Check if closer)
        if (hitEntities) {
            double blockDistSq = blockHit.getPos().squaredDistanceTo(start);
            AABB box = source.getBoundingBox().stretch(rotation.multiply(length)).expand(1.0D);
            EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(source, start, end, box, (e) -> !e.isSpectator() && e.isPickable(), blockDistSq);

            if (entityHit != null) {
                hitEntity = entityHit.getEntity();
                hitPos = entityHit.getEntity().blockPosition(); // Update hit pos to entity location
            }
        }

        // 3. Execute Child
        // If entity hit, target changes. If block hit, target remains source (player).
        Entity nextTarget = (hitEntity != null) ? hitEntity : source;

        // PASS THE HIT POS
        childEffect.execute(nextTarget, world, hitPos);
    }

    // ... Getters and fromJson ...
    public double getLength() { return length; }
    public boolean isStopOnFluids() { return stopOnFluids; }
    public boolean isHitEntities() { return hitEntities; }
    public SkillActionEffect getChildEffect() { return childEffect; }

    public static RaycastActionEffect fromJson(JsonObject json) {
        double len = GsonHelper.getDouble(json, "length", 5.0);
        boolean fluids = GsonHelper.getBoolean(json, "fluids", false);
        boolean entities = GsonHelper.getBoolean(json, "entities", true);

        JsonObject effectJson = json.getAsJsonObject("effect");
        SkillActionEffect child = SkillActionEffectType.create(effectJson);

        return new RaycastActionEffect(len, fluids, entities, child);
    }
}