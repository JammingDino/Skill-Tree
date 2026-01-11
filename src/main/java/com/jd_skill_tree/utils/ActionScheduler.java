package com.jd_skill_tree.utils;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ActionScheduler {

    private record Task(int targetTick, Runnable action) {}

    // Using a separate list for new tasks prevents ConcurrentModificationException
    private static final List<Task> activeTasks = new ArrayList<>();
    private static final List<Task> pendingTasks = new ArrayList<>();

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // 1. Add any tasks that were scheduled during the last tick
            if (!pendingTasks.isEmpty()) {
                activeTasks.addAll(pendingTasks);
                pendingTasks.clear();
            }

            if (activeTasks.isEmpty()) return;

            // 2. Use Server Ticks (monotonically increasing) instead of World Time
            int currentTick = server.getTicks();
            Iterator<Task> iterator = activeTasks.iterator();

            while (iterator.hasNext()) {
                Task task = iterator.next();
                if (currentTick >= task.targetTick) {
                    try {
                        task.action.run();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    iterator.remove();
                }
            }
        });
    }

    public static void schedule(int delayTicks, Runnable action, net.minecraft.server.MinecraftServer server) {
        // Calculate target based on the server's master tick count
        int target = server.getTicks() + delayTicks;
        pendingTasks.add(new Task(target, action));
    }
}