package com.jd_skill_tree.skills.actions;

import com.google.gson.JsonObject;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

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
    public void execute(Entity source, World world, BlockPos pos) {
        if (childEffect == null) return;

        Vec3d start = source.getEyePos();
        Vec3d rotation = source.getRotationVector();
        Vec3d end = start.add(rotation.multiply(length));

        // 1. Raycast Blocks
        RaycastContext.FluidHandling fluidMode = stopOnFluids ? RaycastContext.FluidHandling.ANY : RaycastContext.FluidHandling.NONE;
        BlockHitResult blockHit = world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, fluidMode, source));

        BlockPos hitPos = blockHit.getBlockPos();
        Entity hitEntity = null;

        // 2. Raycast Entities (Check if closer)
        if (hitEntities) {
            double blockDistSq = blockHit.getPos().squaredDistanceTo(start);
            Box box = source.getBoundingBox().stretch(rotation.multiply(length)).expand(1.0D);
            EntityHitResult entityHit = ProjectileUtil.raycast(source, start, end, box, (e) -> !e.isSpectator() && e.canHit(), blockDistSq);

            if (entityHit != null) {
                hitEntity = entityHit.getEntity();
                hitPos = entityHit.getEntity().getBlockPos(); // Update hit pos to entity location
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
        double len = JsonHelper.getDouble(json, "length", 5.0);
        boolean fluids = JsonHelper.getBoolean(json, "fluids", false);
        boolean entities = JsonHelper.getBoolean(json, "entities", true);

        JsonObject effectJson = json.getAsJsonObject("effect");
        SkillActionEffect child = SkillActionEffectType.create(effectJson);

        return new RaycastActionEffect(len, fluids, entities, child);
    }
}