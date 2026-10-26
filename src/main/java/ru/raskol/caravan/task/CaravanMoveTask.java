package ru.raskol.caravan.task;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import ru.raskol.caravan.RaskolCaravan;
import ru.raskol.caravan.model.Caravan;
import ru.raskol.caravan.store.CaravanStore;

import java.util.ArrayList;

/** Каждые 5 секунд двигает караван по маршруту и проверяет прибытие. */
public final class CaravanMoveTask implements Runnable {

    private static final long STEP_MS = 5000L;

    private final RaskolCaravan plugin;
    private final CaravanStore store;

    public CaravanMoveTask(RaskolCaravan plugin, CaravanStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis() / 1000L;

        for (Caravan c : new ArrayList<>(store.caravans())) {
            if (now >= c.arriveAtEpoch) {
                plugin.getCaravanService().arrive(c);
                continue;
            }

            World w = Bukkit.getWorld(c.world);
            if (w == null) continue;

            ArmorStand stand = findStand(w, c.entityId);
            if (stand == null || stand.isDead()) {
                // NPC исчез/мёртв без события — считаем ограбленным (нет убийц)
                plugin.getCaravanService().rob(c, new ArrayList<>());
                continue;
            }

            double total = c.arriveAtEpoch - c.startedAtEpoch;
            double elapsed = now - c.startedAtEpoch;
            double t = Math.min(1.0, elapsed / total);
            double x = c.startX + (c.endX - c.startX) * t;
            double z = c.startZ + (c.endZ - c.startZ) * t;

            // Y: на земле по пути
            double y = w.getHighestBlockYAt((int) Math.round(x), (int) Math.round(z)) + 1.0;

            stand.teleport(new Location(w, x, y, z));

            // Обновляем HP в имени
            c.hp = stand.getHealth();
            stand.setCustomName("§6⚑ Караван §e" + c.nation1 + " §7→ §e" + c.nation2
                    + " §c♥" + ((int) c.hp) + "/" + ((int) c.maxHp));
        }
    }

    private ArmorStand findStand(World w, java.util.UUID id) {
        if (id == null) return null;
        for (Entity e : w.getEntities()) {
            if (e.getUniqueId().equals(id) && e instanceof ArmorStand a) return a;
        }
        return null;
    }
}
