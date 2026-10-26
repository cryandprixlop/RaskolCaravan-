package ru.raskol.caravan.listener;

import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import ru.raskol.caravan.RaskolCaravan;
import ru.raskol.caravan.model.Caravan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Ограбление каравана: считаем уникальных атакующих игроков,
 * при убийстве NPC раздаём им 5-35% от суммы каравана.
 */
public final class CaravanDamageListener implements Listener {

    private final RaskolCaravan plugin;
    // entityId -> Set<UUID> грабителей
    private final Map<UUID, Set<UUID>> attackers = new HashMap<>();

    public CaravanDamageListener(RaskolCaravan plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof ArmorStand stand)) return;
        Caravan c = findByEntity(stand.getUniqueId());
        if (c == null) return;

        if (!(event.getDamager() instanceof Player p)) {
            // не-игроки не грабят, просто не наносят урон
            event.setCancelled(true);
            return;
        }
        if (!p.hasPermission("caravan.rob")) {
            event.setCancelled(true);
            p.sendMessage("§cУ тебя нет прав грабить караваны.");
            return;
        }

        attackers.computeIfAbsent(stand.getUniqueId(), k -> new HashSet<>()).add(p.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof ArmorStand stand)) return;
        Caravan c = findByEntity(stand.getUniqueId());
        if (c == null) return;

        Set<UUID> ids = attackers.remove(stand.getUniqueId());
        List<Player> robbers = new ArrayList<>();
        if (ids != null) {
            for (UUID id : ids) {
                Player p = org.bukkit.Bukkit.getPlayer(id);
                if (p != null) robbers.add(p);
            }
        }
        plugin.getCaravanService().rob(c, robbers);
    }

    private Caravan findByEntity(UUID id) {
        for (Caravan c : plugin.getStore().caravans()) {
            if (id.equals(c.entityId)) return c;
        }
        return null;
    }
}
