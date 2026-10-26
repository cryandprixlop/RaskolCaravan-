package ru.raskol.caravan;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import ru.raskol.caravan.command.CaravanCommand;
import ru.raskol.caravan.hook.TownyHook;
import ru.raskol.caravan.hook.VaultHook;
import ru.raskol.caravan.listener.CaravanDamageListener;
import ru.raskol.caravan.service.CaravanService;
import ru.raskol.caravan.store.CaravanStore;
import ru.raskol.caravan.task.CaravanMoveTask;

public final class RaskolCaravan extends JavaPlugin {

    private CaravanStore store;
    private TownyHook townyHook;
    private VaultHook vaultHook;
    private CaravanService service;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        store = new CaravanStore(this);
        store.load();
        townyHook = new TownyHook(this);
        vaultHook = new VaultHook(this);
        service = new CaravanService(this, store, townyHook);

        PluginCommand cmd = getCommand("caravan");
        if (cmd != null) {
            CaravanCommand executor = new CaravanCommand(this, store, service);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }

        getServer().getPluginManager().registerEvents(
                new CaravanDamageListener(this), this);

        // Движение караванов каждые 5 секунд
        getServer().getScheduler().runTaskTimer(this,
                new CaravanMoveTask(this, store), 100L, 100L);

        getLogger().info("RaskolCaravan v" + getDescription().getVersion()
                + " включён. Активных караванов: " + store.caravans().size()
                + ", офферов в ожидании: " + store.offers().size()
                + ". Towny: " + (townyHook.isAvailable() ? "да" : "нет")
                + ". Vault: " + (vaultHook.isAvailable() ? "да" : "нет"));
    }

    @Override
    public void onDisable() {
        if (store != null) store.save();
        getLogger().info("RaskolCaravan выключен.");
    }

    public CaravanStore getStore() { return store; }
    public TownyHook getTownyHook() { return townyHook; }
    public VaultHook getVaultHook() { return vaultHook; }
    public CaravanService getCaravanService() { return service; }
}
