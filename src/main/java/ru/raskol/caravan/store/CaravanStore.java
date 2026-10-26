package ru.raskol.caravan.store;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.raskol.caravan.RaskolCaravan;
import ru.raskol.caravan.model.Caravan;
import ru.raskol.caravan.model.Offer;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * caravan.yml: активные караваны, офферы, КД между парами королевств.
 */
public final class CaravanStore {

    private final RaskolCaravan plugin;
    private final List<Caravan> caravans = new ArrayList<>();
    private final List<Offer> offers = new ArrayList<>();
    // ключ "A|B" (лексикографически) -> timestamp последнего каравана
    private final Map<String, Long> cooldowns = new HashMap<>();

    public CaravanStore(RaskolCaravan plugin) { this.plugin = plugin; }

    public void load() {
        caravans.clear();
        offers.clear();
        cooldowns.clear();
        File file = new File(plugin.getDataFolder(), "caravans.yml");
        if (!file.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection cs = cfg.getConfigurationSection("caravans");
        if (cs != null) {
            for (String id : cs.getKeys(false)) {
                ConfigurationSection s = cs.getConfigurationSection(id);
                if (s == null) continue;
                Caravan c = new Caravan();
                c.id = id;
                c.nation1 = s.getString("nation1");
                c.nation2 = s.getString("nation2");
                c.king1 = s.getString("king1");
                c.king2 = s.getString("king2");
                String eid = s.getString("entity");
                if (eid != null && !eid.isEmpty()) {
                    try { c.entityId = java.util.UUID.fromString(eid); }
                    catch (Throwable ignored) {}
                }
                c.deposit1 = s.getDouble("deposit1");
                c.deposit2 = s.getDouble("deposit2");
                c.percent1 = s.getDouble("percent1");
                c.percent2 = s.getDouble("percent2");
                c.guards = s.getInt("guards");
                c.hp = s.getDouble("hp");
                c.maxHp = s.getDouble("max-hp");
                c.startedAtEpoch = s.getLong("started-at");
                c.arriveAtEpoch = s.getLong("arrive-at");
                c.startX = s.getDouble("start-x");
                c.startY = s.getDouble("start-y");
                c.startZ = s.getDouble("start-z");
                c.endX = s.getDouble("end-x");
                c.endY = s.getDouble("end-y");
                c.endZ = s.getDouble("end-z");
                c.world = s.getString("world");
                caravans.add(c);
            }
        }

        ConfigurationSection ks = cfg.getConfigurationSection("cooldowns");
        if (ks != null) {
            for (String key : ks.getKeys(false)) {
                cooldowns.put(key, ks.getLong(key, 0));
            }
        }
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (Caravan c : caravans) {
            ConfigurationSection s = cfg.createSection("caravans." + c.id);
            s.set("nation1", c.nation1);
            s.set("nation2", c.nation2);
            s.set("king1", c.king1);
            s.set("king2", c.king2);
            s.set("entity", c.entityId == null ? "" : c.entityId.toString());
            s.set("deposit1", c.deposit1);
            s.set("deposit2", c.deposit2);
            s.set("percent1", c.percent1);
            s.set("percent2", c.percent2);
            s.set("guards", c.guards);
            s.set("hp", c.hp);
            s.set("max-hp", c.maxHp);
            s.set("started-at", c.startedAtEpoch);
            s.set("arrive-at", c.arriveAtEpoch);
            s.set("start-x", c.startX);
            s.set("start-y", c.startY);
            s.set("start-z", c.startZ);
            s.set("end-x", c.endX);
            s.set("end-y", c.endY);
            s.set("end-z", c.endZ);
            s.set("world", c.world);
        }
        for (Map.Entry<String, Long> e : cooldowns.entrySet()) {
            cfg.set("cooldowns." + e.getKey(), e.getValue());
        }
        try {
            cfg.save(new File(plugin.getDataFolder(), "caravans.yml"));
        } catch (Exception e) {
            plugin.getLogger().severe("[Caravan] не удалось сохранить caravans.yml: " + e.getMessage());
        }
    }

    public void addCaravan(Caravan c) { caravans.add(c); save(); }
    public void removeCaravan(Caravan c) { caravans.remove(c); save(); }
    public List<Caravan> caravans() { return caravans; }

    public void addOffer(Offer o) { offers.add(o); }
    public List<Offer> offers() { return offers; }
    public void removeOffer(Offer o) { offers.remove(o); }
    public Offer offerById(String id) {
        for (Offer o : offers) if (o.id.equals(id)) return o;
        return null;
    }

    /** Ключ пары королевств: лексикографически меньший|больший. */
    public String pairKey(String a, String b) {
        return a.compareTo(b) <= 0 ? a + "|" + b : b + "|" + a;
    }

    public long lastCaravan(String a, String b) {
        return cooldowns.getOrDefault(pairKey(a, b), 0L);
    }

    public void setLastCaravan(String a, String b, long epoch) {
        cooldowns.put(pairKey(a, b), epoch);
        save();
    }
}
