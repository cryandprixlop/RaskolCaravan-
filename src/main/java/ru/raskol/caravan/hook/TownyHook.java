package ru.raskol.caravan.hook;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import ru.raskol.caravan.RaskolCaravan;
import java.lang.reflect.Method;

public final class TownyHook {

    private final RaskolCaravan plugin;
    public TownyHook(RaskolCaravan plugin) { this.plugin = plugin; }

    public boolean isAvailable() {
        return plugin.getServer().getPluginManager().getPlugin("Towny") != null;
    }

    public boolean isKing(Player player) {
        try {
            Class<?> u = Class.forName("com.palmergames.bukkit.towny.TownyUniverse");
            Object uni = u.getMethod("getInstance").invoke(null);
            Object r = u.getMethod("getResident", java.util.UUID.class)
                    .invoke(uni, player.getUniqueId());
            if (r == null) return false;
            Boolean k = (Boolean) r.getClass().getMethod("isKing").invoke(r);
            return k != null && k;
        } catch (Throwable t) { return false; }
    }

    /** Королевство, в котором состоит игрок. */
    public String residentNationName(Player player) {
        try {
            Class<?> u = Class.forName("com.palmergames.bukkit.towny.TownyUniverse");
            Object uni = u.getMethod("getInstance").invoke(null);
            Object r = u.getMethod("getResident", java.util.UUID.class)
                    .invoke(uni, player.getUniqueId());
            if (r == null) return null;
            Object t = callFirst(r, "getTownOrNull", "getTown");
            if (t == null) return null;
            Object n = callFirst(t, "getNationOrNull", "getNation");
            if (n == null) return null;
            return (String) n.getClass().getMethod("getName").invoke(n);
        } catch (Throwable t) { return null; }
    }

    /** Имя короля королевства (капитал). */
    public String kingName(String nationName) {
        try {
            Class<?> api = Class.forName("com.palmergames.bukkit.towny.TownyAPI");
            Object a = api.getMethod("getInstance").invoke(null);
            Object n = api.getMethod("getNation", String.class).invoke(a, nationName);
            if (n == null) return null;
            Object cap = callFirst(n, "getCapital");
            if (cap == null) return null;
            Object mayor = callFirst(cap, "getMayor");
            if (mayor == null) return null;
            return (String) mayor.getClass().getMethod("getName").invoke(mayor);
        } catch (Throwable t) { return null; }
    }

    /** UUID короля королевства. */
    public java.util.UUID kingUuid(String nationName) {
        try {
            Class<?> api = Class.forName("com.palmergames.bukkit.towny.TownyAPI");
            Object a = api.getMethod("getInstance").invoke(null);
            Object n = api.getMethod("getNation", String.class).invoke(a, nationName);
            if (n == null) return null;
            Object cap = callFirst(n, "getCapital");
            if (cap == null) return null;
            Object mayor = callFirst(cap, "getMayor");
            if (mayor == null) return null;
            return (java.util.UUID) mayor.getClass().getMethod("getUUID").invoke(mayor);
        } catch (Throwable t) { return null; }
    }

    /** Локация столицы королевства (спавн). */
    public Location capitalLocation(String nationName) {
        try {
            Class<?> api = Class.forName("com.palmergames.bukkit.towny.TownyAPI");
            Object a = api.getMethod("getInstance").invoke(null);
            Object n = api.getMethod("getNation", String.class).invoke(a, nationName);
            if (n == null) return null;
            Object cap = callFirst(n, "getCapital");
            if (cap == null) return null;
            Object spawn = cap.getClass().getMethod("getSpawn").invoke(cap);
            if (spawn instanceof Location loc) return loc;
            return null;
        } catch (Throwable t) { return null; }
    }

    public boolean depositNationBank(String nationName, double amount, String reason) {
        try {
            Class<?> api = Class.forName("com.palmergames.bukkit.towny.TownyAPI");
            Object a = api.getMethod("getInstance").invoke(null);
            Object n = api.getMethod("getNation", String.class).invoke(a, nationName);
            if (n == null) return false;
            Object acc = n.getClass().getMethod("getAccount").invoke(n);
            if (acc == null) return false;
            Method m = acc.getClass().getMethod("deposit", double.class, String.class);
            Object out = m.invoke(acc, amount, reason);
            return !(out instanceof Boolean b) || b;
        } catch (Throwable t) { return false; }
    }

    public boolean withdrawNationBank(String nationName, double amount, String reason) {
        try {
            Class<?> api = Class.forName("com.palmergames.bukkit.towny.TownyAPI");
            Object a = api.getMethod("getInstance").invoke(null);
            Object n = api.getMethod("getNation", String.class).invoke(a, nationName);
            if (n == null) return false;
            Object acc = n.getClass().getMethod("getAccount").invoke(n);
            if (acc == null) return false;
            Method m = acc.getClass().getMethod("withdraw", double.class, String.class);
            Object out = m.invoke(acc, amount, reason);
            return !(out instanceof Boolean b) || b;
        } catch (Throwable t) { return false; }
    }

    /** Текущий баланс банка королевства. */
    public double nationBalance(String nationName) {
        try {
            Class<?> api = Class.forName("com.palmergames.bukkit.towny.TownyAPI");
            Object a = api.getMethod("getInstance").invoke(null);
            Object n = api.getMethod("getNation", String.class).invoke(a, nationName);
            if (n == null) return 0.0;
            Object acc = n.getClass().getMethod("getAccount").invoke(n);
            if (acc == null) return 0.0;
            Object out = acc.getClass().getMethod("getHoldingBalance").invoke(acc);
            return out instanceof Number num ? num.doubleValue() : 0.0;
        } catch (Throwable t) { return 0.0; }
    }

    private Object callFirst(Object target, String... methods) {
        for (String name : methods) {
            try {
                Method m = target.getClass().getMethod(name);
                Object out = m.invoke(target);
                if (out != null) return out;
            } catch (Throwable ignored) {}
        }
        return null;
    }
}
