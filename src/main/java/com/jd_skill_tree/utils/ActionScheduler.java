package com.jd_skill_tree.utils;

import net.minecraft.server.MinecraftServer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ActionScheduler {

    private record Task(int targetTick, Runnable action) {}

    private static final List<Task> activeTasks = new ArrayList<>();
    private static final List<Task> pendingTasks = new ArrayList<>();

    public static void register() {
        // No-op: ForgeEventHandlers calls tick() on server tick
    }

    public static void tick(MinecraftServer server) {
        if (!pendingTasks.isEmpty()) {
            activeTasks.addAll(pendingTasks);
            pendingTasks.clear();
        }
        if (activeTasks.isEmpty()) return;
        int currentTick = server.getTickCount();
        Iterator<Task> iterator = activeTasks.iterator();
        while (iterator.hasNext()) {
            Task task = iterator.next();
            if (currentTick >= task.targetTick()) {
                try { task.action().run(); } catch (Exception e) { e.printStackTrace(); }
                iterator.remove();
            }
        }
    }

    public static void schedule(int delayTicks, Runnable action, MinecraftServer server) {
        int target = server.getTickCount() + delayTicks;
        pendingTasks.add(new Task(target, action));
    }
}
