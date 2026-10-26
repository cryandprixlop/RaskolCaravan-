package ru.raskol.caravan.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.raskol.caravan.RaskolCaravan;
import ru.raskol.caravan.model.Caravan;
import ru.raskol.caravan.model.Offer;
import ru.raskol.caravan.service.CaravanService;
import ru.raskol.caravan.store.CaravanStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class CaravanCommand implements CommandExecutor, TabCompleter {

    private final RaskolCaravan plugin;
    private final CaravanStore store;
    private final CaravanService service;

    public CaravanCommand(RaskolCaravan plugin, CaravanStore store, CaravanService service) {
        this.plugin = plugin;
        this.store = store;
        this.service = service;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) { sendHelp(sender); return true; }
        switch (args[0].toLowerCase()) {
            case "offer"   -> handleOffer(sender, args);
            case "accept"  -> handleAccept(sender, args);
            case "reject"  -> handleReject(sender, args);
            case "list"    -> handleList(sender);
            case "offers"  -> handleOffers(sender);
            case "cancel"  -> handleCancel(sender, args);
            case "reload"  -> handleReload(sender);
            default        -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6=== RaskolTradeCaravan ===");
        sender.sendMessage("§e/tradecaravan offer <нация2> <сумма1> <сумма2> <процент1> <процент2> [стража]");
        sender.sendMessage("§e/tradecaravan accept <id> §7— принять оффер");
        sender.sendMessage("§e/tradecaravan reject <id> §7— отклонить оффер");
        sender.sendMessage("§e/tradecaravan offers §7— список входящих офферов");
        sender.sendMessage("§e/tradecaravan list §7— список активных караванов");
        sender.sendMessage("§e/tradecaravan cancel <id> §7— (админ) отменить караван");
        sender.sendMessage("§e/tradecaravan reload");
        sender.sendMessage("§7Сумма процентов должна быть = "
                + plugin.getConfig().getDouble("caravan.profit-percent", 10.0) + "%");
        sender.sendMessage("§7Базовое HP каравана: §e" + (int) plugin.getConfig().getDouble("caravan.base-hp", 500.0)
                + "§7, +§e" + (int) plugin.getConfig().getDouble("caravan.hp-per-guard", 200.0) + "§7 за стражника");
    }

    private boolean admin(CommandSender sender) {
        if (sender.hasPermission("caravan.admin")) return true;
        sender.sendMessage("§cНет прав.");
        return false;
    }

    private void handleOffer(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("§cТолько игроки."); return; }
        if (args.length < 6) {
            p.sendMessage("§c/tradecaravan offer <нация2> <сумма1> <сумма2> <процент1> <процент2> [стража]");
            return;
        }
        String target = args[1];
        double d1, d2, p1, p2;
        int guards = 0;
        try {
            d1 = Double.parseDouble(args[2]);
            d2 = Double.parseDouble(args[3]);
            p1 = Double.parseDouble(args[4]);
            p2 = Double.parseDouble(args[5]);
            if (args.length >= 7) guards = Integer.parseInt(args[6]);
        } catch (NumberFormatException e) {
            p.sendMessage("§cНеверный формат чисел.");
            return;
        }
        CaravanService.OfferResult r = service.createOffer(p, target, d1, d2, p1, p2, guards);
        p.sendMessage(r.message);
    }

    private void handleAccept(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("§cТолько игроки."); return; }
        if (args.length < 2) { p.sendMessage("§c/tradecaravan accept <id>"); return; }
        CaravanService.AcceptResult r = service.acceptOffer(p, args[1]);
        p.sendMessage(r.message);
    }

    private void handleReject(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("§cТолько игроки."); return; }
        if (args.length < 2) { p.sendMessage("§c/tradecaravan reject <id>"); return; }
        service.rejectOffer(p, args[1]);
    }

    private void handleList(CommandSender sender) {
        List<Caravan> all = store.caravans();
        if (all.isEmpty()) { sender.sendMessage("§7Активных караванов нет."); return; }
        sender.sendMessage("§6=== Активных караванов: " + all.size() + " ===");
        long now = System.currentTimeMillis() / 1000L;
        for (Caravan c : all) {
            long remain = Math.max(0, c.arriveAtEpoch - now);
            long h = remain / 3600;
            long m = (remain % 3600) / 60;
            sender.sendMessage("§e" + c.id + " §7" + c.nation1 + " → " + c.nation2
                    + " §7(вложено: §b" + plugin.getVaultHook().format(c.deposit1 + c.deposit2)
                    + "§7, стража: §e" + c.guards + "§7, HP: §c" + ((int) c.hp) + "/" + ((int) c.maxHp)
                    + "§7, прибудет через §e" + h + " ч " + m + " мин§7)");
        }
    }

    private void handleOffers(CommandSender sender) {
        if (!(sender instanceof Player p)) { sender.sendMessage("§cТолько игроки."); return; }
        String myNation = plugin.getTownyHook().residentNationName(p);
        if (myNation == null) { p.sendMessage("§cТы не состоишь в королевстве."); return; }
        List<Offer> mine = new ArrayList<>();
        long now = System.currentTimeMillis() / 1000L;
        for (Offer o : store.offers()) {
            if (o.toNation.equalsIgnoreCase(myNation) && o.expiresAtEpoch > now) mine.add(o);
        }
        if (mine.isEmpty()) { p.sendMessage("§7Входящих офферов нет."); return; }
        p.sendMessage("§6=== Входящих офферов: " + mine.size() + " ===");
        for (Offer o : mine) {
            long remain = o.expiresAtEpoch - now;
            p.sendMessage("§e" + o.id + " §7от §e" + o.fromKing + " (§e" + o.fromNation + "§7): "
                    + "вложение §b" + plugin.getVaultHook().format(o.deposit1 + o.deposit2)
                    + "§7, проценты §e" + o.percent1 + "/" + o.percent2
                    + "§7, стража §e" + o.guards + "§7, истекает через §e" + (remain / 60) + " мин");
            p.sendMessage("§aПринять: §e/tradecaravan accept " + o.id
                    + "  §cОтклонить: §e/tradecaravan reject " + o.id);
        }
    }

    private void handleCancel(CommandSender sender, String[] args) {
        if (!admin(sender)) return;
        if (args.length < 2) { sender.sendMessage("§c/tradecaravan cancel <id>"); return; }
        for (Caravan c : new ArrayList<>(store.caravans())) {
            if (c.id.equals(args[1])) {
                store.removeCaravan(c);
                sender.sendMessage("§cКараван §e" + c.id + " §cотменён админом.");
                return;
            }
        }
        sender.sendMessage("§cКараван не найден.");
    }

    private void handleReload(CommandSender sender) {
        if (!admin(sender)) return;
        plugin.reloadConfig();
        sender.sendMessage("§aКонфиг RaskolTradeCaravan перезагружен.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("offer", "accept", "reject", "offers",
                    "list", "cancel", "reload"), args[0]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> list, String prefix) {
        String lower = prefix.toLowerCase();
        List<String> out = new ArrayList<>();
        for (String s : list) if (s.toLowerCase().startsWith(lower)) out.add(s);
        return out;
    }
}
