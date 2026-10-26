package ru.raskol.caravan.service;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import ru.raskol.caravan.RaskolCaravan;
import ru.raskol.caravan.hook.TownyHook;
import ru.raskol.caravan.model.Caravan;
import ru.raskol.caravan.model.Offer;
import ru.raskol.caravan.store.CaravanStore;

import java.util.UUID;

/** Логика офферов, старта караванов, прибытия и ограблений. */
public final class CaravanService {

    private final RaskolCaravan plugin;
    private final CaravanStore store;
    private final TownyHook towny;

    public CaravanService(RaskolCaravan plugin, CaravanStore store, TownyHook towny) {
        this.plugin = plugin;
        this.store = store;
        this.towny = towny;
    }

    /* ================= Офферы ================= */

    public OfferResult createOffer(Player king, String targetNation,
                                   double deposit1, double deposit2,
                                   double percent1, double percent2, int guards) {
        String myNation = towny.residentNationName(king);
        if (myNation == null) return new OfferResult(false, "§cТы должен быть королём королевства.");
        if (!towny.isKing(king)) return new OfferResult(false, "§cТолько король может отправлять караваны.");
        if (myNation.equalsIgnoreCase(targetNation))
            return new OfferResult(false, "§cНельзя отправить караван в своё королевство.");

        long cdHours = plugin.getConfig().getLong("caravan.cooldown-hours", 24);
        long last = store.lastCaravan(myNation, targetNation);
        long remainSec = last + cdHours * 3600L - System.currentTimeMillis() / 1000L;
        if (remainSec > 0) {
            long h = remainSec / 3600;
            long m = (remainSec % 3600) / 60;
            return new OfferResult(false, "§cКД между королевствами " + myNation + " и "
                    + targetNation + ": ещё " + h + " ч " + m + " мин.");
        }

        double profit = plugin.getConfig().getDouble("caravan.profit-percent", 10.0);
        if (Math.abs((percent1 + percent2) - profit) > 0.001) {
            return new OfferResult(false, "§cСумма процентов должна быть равна "
                    + profit + "%. Сейчас: " + (percent1 + percent2) + "%.");
        }
        if (percent1 < 0 || percent2 < 0)
            return new OfferResult(false, "§cПроценты не могут быть отрицательными.");
        if (deposit1 <= 0 || deposit2 <= 0)
            return new OfferResult(false, "§cВложения должны быть > 0.");
        if (guards < 0) return new OfferResult(false, "§cСтража не может быть отрицательной.");

        double guardCost = guards * plugin.getConfig().getDouble("caravan.guard-price", 10000.0);
        double need = deposit1 + guardCost;
        double have = towny.nationBalance(myNation);
        if (have < need) {
            return new OfferResult(false, "§cВ казне §e" + myNation + " §cнедостаточно: нужно §b"
                    + plugin.getVaultHook().format(need) + " §c(вложение + стража), есть §b"
                    + plugin.getVaultHook().format(have) + "§c.");
        }

        String king2Name = towny.kingName(targetNation);
        if (king2Name == null)
            return new OfferResult(false, "§cКоролевство §e" + targetNation + " §cне найдено.");

        Location start = towny.capitalLocation(myNation);
        Location end = towny.capitalLocation(targetNation);
        if (start == null || end == null)
            return new OfferResult(false, "§cНе удалось определить столицы королевств.");
        if (!start.getWorld().equals(end.getWorld()))
            return new OfferResult(false, "§cСтолицы в разных мирах — караван невозможен.");

        Offer o = new Offer();
        o.id = UUID.randomUUID().toString().substring(0, 8);
        o.fromNation = myNation;
        o.toNation = targetNation;
        o.fromKing = king.getName();
        o.toKing = king2Name;
        o.deposit1 = deposit1;
        o.deposit2 = deposit2;
        o.percent1 = percent1;
        o.percent2 = percent2;
        o.guards = guards;
        o.expiresAtEpoch = System.currentTimeMillis() / 1000L
                + plugin.getConfig().getLong("caravan.offer-timeout-seconds", 600);
        store.addOffer(o);

        Player king2 = Bukkit.getPlayerExact(king2Name);
        if (king2 != null) {
            king2.sendMessage("§6✦ §e" + king.getName() + " §6(король §e" + myNation + "§6) предлагает караван!");
            king2.sendMessage("§7Вложения: §b" + plugin.getVaultHook().format(deposit1)
                    + " §7+ §b" + plugin.getVaultHook().format(deposit2)
                    + " §7· прибыль: " + (percent1 + percent2) + "% · стража: " + guards);
            king2.sendMessage("§aПринять: §e/tradecaravan accept " + o.id
                    + "  §cОтклонить: §e/tradecaravan reject " + o.id);
        }

        return new OfferResult(true, "§a✦ Оффер §e" + o.id + " §aотправлен королю §e"
                + king2Name + " §a(§e" + targetNation + "§a). Жди ответа 10 минут.");
    }

    /* ================= Акцепт ================= */

    public AcceptResult acceptOffer(Player king, String offerId) {
        Offer o = store.offerById(offerId);
        if (o == null) return new AcceptResult(false, "§cОффер не найден или истёк.");
        if (System.currentTimeMillis() / 1000L > o.expiresAtEpoch) {
            store.removeOffer(o);
            return new AcceptResult(false, "§cОффер истёк.");
        }
        String myNation = towny.residentNationName(king);
        if (myNation == null || !myNation.equalsIgnoreCase(o.toNation))
            return new AcceptResult(false, "§cЭтот оффер адресован другому королевству.");
        if (!towny.isKing(king))
            return new AcceptResult(false, "§cТолько король может принять оффер.");

        double have = towny.nationBalance(myNation);
        if (have < o.deposit2) {
            return new AcceptResult(false, "§cВ казне §e" + myNation + " §cнедостаточно: нужно §b"
                    + plugin.getVaultHook().format(o.deposit2) + "§c, есть §b"
                    + plugin.getVaultHook().format(have) + "§c.");
        }

        if (!towny.withdrawNationBank(o.fromNation, o.deposit1, "TradeCaravan deposit")) {
            return new AcceptResult(false, "§cНе удалось списать с казны §e" + o.fromNation + "§c.");
        }
        if (!towny.withdrawNationBank(o.toNation, o.deposit2, "TradeCaravan deposit")) {
            towny.depositNationBank(o.fromNation, o.deposit1, "TradeCaravan rollback");
            return new AcceptResult(false, "§cНе удалось списать с казны §e" + o.toNation + "§c.");
        }

        Location start = towny.capitalLocation(o.fromNation);
        World w = start.getWorld();

        double baseHp = plugin.getConfig().getDouble("caravan.base-hp", 500.0);
        double perGuard = plugin.getConfig().getDouble("caravan.hp-per-guard", 200.0);
        double maxHp = baseHp + perGuard * o.guards;

        ArmorStand stand = (ArmorStand) w.spawnEntity(start.clone().add(0.5, 0.5, 0.5),
                EntityType.ARMOR_STAND);
        stand.setCustomName("§6⚑ Караван §e" + o.fromNation + " §7→ §e" + o.toNation
                + " §c♥" + ((int) maxHp));
        stand.setCustomNameVisible(true);
        stand.setBasePlate(false);
        stand.setArms(false);
        stand.setGravity(false);
        stand.setInvulnerable(false);

        Attribute maxHealthAttr;
        try {
            maxHealthAttr = Attribute.valueOf("MAX_HEALTH");
        } catch (IllegalArgumentException ex) {
            maxHealthAttr = Attribute.valueOf("GENERIC_MAX_HEALTH");
        }
        AttributeInstance inst = stand.getAttribute(maxHealthAttr);
        if (inst != null) inst.setBaseValue(maxHp);
        stand.setHealth(maxHp);

        Caravan c = new Caravan();
        c.id = UUID.randomUUID().toString().substring(0, 8);
        c.nation1 = o.fromNation;
        c.nation2 = o.toNation;
        c.king1 = o.fromKing;
        c.king2 = o.toKing;
        c.entityId = stand.getUniqueId();
        c.deposit1 = o.deposit1;
        c.deposit2 = o.deposit2;
        c.percent1 = o.percent1;
        c.percent2 = o.percent2;
        c.guards = o.guards;
        c.maxHp = maxHp;
        c.hp = maxHp;
        long now = System.currentTimeMillis() / 1000L;
        long travel = plugin.getConfig().getLong("caravan.travel-seconds", 7200);
        c.startedAtEpoch = now;
        c.arriveAtEpoch = now + travel;
        c.startX = start.getX(); c.startY = start.getY(); c.startZ = start.getZ();
        Location end = towny.capitalLocation(o.toNation);
        c.endX = end.getX(); c.endY = end.getY(); c.endZ = end.getZ();
        c.world = w.getName();
        store.addCaravan(c);
        store.setLastCaravan(c.nation1, c.nation2, now);
        store.removeOffer(o);

        notifyKing(o.fromKing, "§a✦ Караван §e" + c.id + " §aотправлен в §e" + o.toNation
                + " §a(прибытие через " + (travel / 3600) + " ч " + ((travel % 3600) / 60) + " мин).");
        notifyKing(o.toKing, "§a✦ Караван §e" + c.id + " §aиз §e" + o.fromNation
                + " §aв пути. Прибытие через " + (travel / 3600) + " ч " + ((travel % 3600) / 60) + " мин.");

        plugin.getLogger().info("[TradeCaravan] started " + c.id + ": " + o.fromNation
                + " (" + o.deposit1 + ") -> " + o.toNation + " (" + o.deposit2
                + "), guards=" + o.guards + ", hp=" + maxHp);
        return new AcceptResult(true, "§aКараван §e" + c.id + " §aотправлен (HP " + ((int) maxHp) + ").");
    }

    public void rejectOffer(Player king, String offerId) {
        Offer o = store.offerById(offerId);
        if (o == null) { king.sendMessage("§cОффер не найден."); return; }
        String myNation = towny.residentNationName(king);
        if (myNation == null || !myNation.equalsIgnoreCase(o.toNation)) {
            king.sendMessage("§cЭтот оффер адресован другому королевству.");
            return;
        }
        store.removeOffer(o);
        king.sendMessage("§cОффер §e" + offerId + " §cотклонён.");
        notifyKing(o.fromKing, "§cКороль §e" + o.toKing + " §cотклонил караван §e" + offerId + "§c.");
    }

    /* ================= Прибытие ================= */

    public void arrive(Caravan c) {
        double total = c.deposit1 + c.deposit2;
        double profitPct = plugin.getConfig().getDouble("caravan.profit-percent", 10.0);
        double profit = total * profitPct / 100.0;

        double toNation1 = c.deposit1 + profit * c.percent1 / profitPct;
        double toNation2 = c.deposit2 + profit * c.percent2 / profitPct;

        towny.depositNationBank(c.nation1, toNation1, "TradeCaravan " + c.id + " arrived");
        towny.depositNationBank(c.nation2, toNation2, "TradeCaravan " + c.id + " arrived");

        killEntity(c.entityId);

        notifyKing(c.king1, "§a✦ Караван §e" + c.id + " §aприбыл! В казну §e" + c.nation1
                + " §aпоступило §b" + plugin.getVaultHook().format(toNation1)
                + " §a(вложение + прибыль " + c.percent1 + "%).");
        notifyKing(c.king2, "§a✦ Караван §e" + c.id + " §aприбыл! В казну §e" + c.nation2
                + " §aпоступило §b" + plugin.getVaultHook().format(toNation2)
                + " §a(вложение + прибыль " + c.percent2 + "%).");

        store.removeCaravan(c);
        plugin.getLogger().info("[TradeCaravan] arrived " + c.id + ": "
                + c.nation1 + " +" + toNation1 + ", " + c.nation2 + " +" + toNation2);
    }

    /* ================= Ограбление ================= */

    public void rob(Caravan c, java.util.Collection<Player> robbers) {
        killEntity(c.entityId);
        double total = c.deposit1 + c.deposit2;

        if (robbers.isEmpty()) {
            towny.depositNationBank(c.nation1, c.deposit1, "TradeCaravan " + c.id + " returned");
            towny.depositNationBank(c.nation2, c.deposit2, "TradeCaravan " + c.id + " returned");
            store.removeCaravan(c);
            return;
        }

        double minP = plugin.getConfig().getDouble("robbery.min-percent", 5.0);
        double maxP = plugin.getConfig().getDouble("robbery.max-percent", 35.0);
        double pct = minP + Math.random() * (maxP - minP);
        double stolen = total * pct / 100.0;
        double hidden = total - stolen;

        double per = stolen / robbers.size();
        for (Player p : robbers) {
            if (plugin.getVaultHook().depositPlayer(p, per)) {
                p.sendMessage("§6✦ Ограбление удалось! Добыча: §b"
                        + plugin.getVaultHook().format(per) + " §7(" + ((int) pct) + "% каравана)");
            }
        }

        double r1 = c.deposit1 / total;
        double r2 = c.deposit2 / total;
        towny.depositNationBank(c.nation1, hidden * r1, "TradeCaravan " + c.id + " hidden returned");
        towny.depositNationBank(c.nation2, hidden * r2, "TradeCaravan " + c.id + " hidden returned");

        notifyKing(c.king1, "§c✦ Караван §e" + c.id + " §cограблен! Утрачено §b"
                + plugin.getVaultHook().format(stolen) + "§c, спрятано и возвращено §b"
                + plugin.getVaultHook().format(hidden * r1) + "§c.");
        notifyKing(c.king2, "§c✦ Караван §e" + c.id + " §cограблен! Утрачено §b"
                + plugin.getVaultHook().format(stolen) + "§c, спрятано и возвращено §b"
                + plugin.getVaultHook().format(hidden * r2) + "§c.");

        store.removeCaravan(c);
        plugin.getLogger().info("[TradeCaravan] robbed " + c.id + ": stolen=" + stolen
                + " hidden=" + hidden + " robbers=" + robbers.size());
    }

    /* ================= Утилиты ================= */

    private void notifyKing(String name, String msg) {
        Player p = Bukkit.getPlayerExact(name);
        if (p != null) p.sendMessage(msg);
    }

    private void killEntity(UUID id) {
        if (id == null) return;
        for (World w : Bukkit.getWorlds()) {
            for (Entity e : w.getEntities()) {
                if (e.getUniqueId().equals(id)) {
                    e.remove();
                    return;
                }
            }
        }
    }

    public static final class OfferResult {
        public final boolean ok;
        public final String message;
        public OfferResult(boolean ok, String message) { this.ok = ok; this.message = message; }
    }

    public static final class AcceptResult {
        public final boolean ok;
        public final String message;
        public AcceptResult(boolean ok, String message) { this.ok = ok; this.message = message; }
    }
}
