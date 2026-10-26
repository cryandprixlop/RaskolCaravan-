package ru.raskol.caravan.hook;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import ru.raskol.caravan.RaskolCaravan;

public final class VaultHook {
    private final RaskolCaravan plugin;
    private Economy economy;

    public VaultHook(RaskolCaravan plugin) {
        this.plugin = plugin;
        RegisteredServiceProvider<Economy> rsp = plugin.getServer()
                .getServicesManager().getRegistration(Economy.class);
        if (rsp != null) this.economy = rsp.getProvider();
    }

    public boolean isAvailable() { return economy != null; }
    public double balance(Player p) { return isAvailable() ? economy.getBalance(p) : 0.0; }
    public String format(double a) { return isAvailable() ? economy.format(a) : String.valueOf(a); }
}
