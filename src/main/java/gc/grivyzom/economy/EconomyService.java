package gc.grivyzom.economy;

import gc.grivyzom.GrvRTP;
import gc.grivyzom.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import net.milkbowl.vault.economy.Economy;

public class EconomyService {
    private final GrvRTP plugin;
    private final MessageUtil msgUtil;
    private Economy economy;
    private boolean economyEnabled;

    // Enum para tipos de moneda
    public enum CurrencyType {
        MONEY,
        EXPERIENCE,
        BOTH
    }

    public EconomyService(GrvRTP plugin, MessageUtil msgUtil) {
        this.plugin = plugin;
        this.msgUtil = msgUtil;
        this.economyEnabled = plugin.getConfig().getBoolean("economy.enabled", false);

        if (economyEnabled) {
            setupEconomy();
        }
    }

    private boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault no encontrado. Sistema de economía monetaria deshabilitado.");
            // Nota: XP no requiere Vault, así que parcialmente funcional
        } else {
            RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager()
                    .getRegistration(Economy.class);
            if (rsp == null) {
                plugin.getLogger().warning("Plugin de economía no encontrado. Sistema monetario deshabilitado.");
            } else {
                economy = rsp.getProvider();
                plugin.getLogger().info("Sistema de economía monetaria habilitado con " + economy.getName());
            }
        }
        return true;
    }

    public boolean isEnabled() {
        return economyEnabled;
    }

    public boolean isMoneyEnabled() {
        return economyEnabled && economy != null;
    }

    /**
     * Obtiene el tipo de moneda configurado para un comando
     */
    public CurrencyType getCurrencyType(String command) {
        String type = plugin.getConfig().getString("economy.commands." + command + ".currency-type", "money");
        try {
            return CurrencyType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Tipo de moneda inválido para " + command + ": " + type + ". Usando MONEY por defecto.");
            return CurrencyType.MONEY;
        }
    }

    /**
     * Calcula el coste en dinero basado en la distancia
     */
    public double calculateMoneyCost(String command, double distance, int maxRange) {
        if (!isMoneyEnabled()) return 0.0;

        String basePath = "economy.commands." + command + ".money.";
        double minCost = plugin.getConfig().getDouble(basePath + "min-cost", 10.0);
        double maxCost = plugin.getConfig().getDouble(basePath + "max-cost", 100.0);

        double ratio = Math.min(distance / maxRange, 1.0);
        double cost = minCost + (ratio * (maxCost - minCost));

        return Math.round(cost * 100.0) / 100.0;
    }

    /**
     * Calcula el coste en experiencia basado en la distancia
     */
    public int calculateExperienceCost(String command, double distance, int maxRange) {
        if (!isEnabled()) return 0;

        String basePath = "economy.commands." + command + ".experience.";
        int minCost = plugin.getConfig().getInt(basePath + "min-cost", 5);
        int maxCost = plugin.getConfig().getInt(basePath + "max-cost", 50);

        double ratio = Math.min(distance / maxRange, 1.0);
        int cost = (int) Math.round(minCost + (ratio * (maxCost - minCost)));

        return cost;
    }

    /**
     * Verifica si el jugador tiene suficiente dinero
     */
    public boolean hasEnoughMoney(Player player, double amount) {
        if (!isMoneyEnabled() || amount <= 0) return true;
        return economy.getBalance(player) >= amount;
    }

    /**
     * Verifica si el jugador tiene suficiente experiencia
     */
    public boolean hasEnoughExperience(Player player, int levels) {
        if (!isEnabled() || levels <= 0) return true;
        return player.getLevel() >= levels;
    }

    /**
     * Cobra dinero al jugador
     */
    public boolean chargeMoney(Player player, double amount) {
        if (!isMoneyEnabled() || amount <= 0) return true;

        if (!hasEnoughMoney(player, amount)) {
            player.sendMessage(msgUtil.format("econ-insufficient-money",
                    new String[][]{{"%amount%", String.format("%.2f", amount)}}));
            return false;
        }

        economy.withdrawPlayer(player, amount);
        player.sendMessage(msgUtil.format("econ-charged-money",
                new String[][]{{"%amount%", String.format("%.2f", amount)}}));
        return true;
    }

    /**
     * Cobra experiencia al jugador
     */
    public boolean chargeExperience(Player player, int levels) {
        if (!isEnabled() || levels <= 0) return true;

        if (!hasEnoughExperience(player, levels)) {
            player.sendMessage(msgUtil.format("econ-insufficient-exp",
                    new String[][]{{"%amount%", String.valueOf(levels)}}));
            return false;
        }

        player.setLevel(player.getLevel() - levels);
        player.sendMessage(msgUtil.format("econ-charged-exp",
                new String[][]{{"%amount%", String.valueOf(levels)}}));
        return true;
    }

    /**
     * Procesa el pago según el tipo de moneda configurado
     */
    public boolean processPayment(Player player, String command, double distance, int maxRange) {
        if (!isEnabled()) return true;

        CurrencyType currencyType = getCurrencyType(command);

        switch (currencyType) {
            case MONEY:
                double moneyCost = calculateMoneyCost(command, distance, maxRange);
                return chargeMoney(player, moneyCost);

            case EXPERIENCE:
                int expCost = calculateExperienceCost(command, distance, maxRange);
                return chargeExperience(player, expCost);

            case BOTH:
                double bothMoneyCost = calculateMoneyCost(command, distance, maxRange);
                int bothExpCost = calculateExperienceCost(command, distance, maxRange);

                // Verificar que tenga ambos recursos
                if (!hasEnoughMoney(player, bothMoneyCost)) {
                    player.sendMessage(msgUtil.format("econ-insufficient-money",
                            new String[][]{{"%amount%", String.format("%.2f", bothMoneyCost)}}));
                    return false;
                }
                if (!hasEnoughExperience(player, bothExpCost)) {
                    player.sendMessage(msgUtil.format("econ-insufficient-exp",
                            new String[][]{{"%amount%", String.valueOf(bothExpCost)}}));
                    return false;
                }

                // Cobrar ambos
                economy.withdrawPlayer(player, bothMoneyCost);
                player.setLevel(player.getLevel() - bothExpCost);

                player.sendMessage(msgUtil.format("econ-charged-both",
                        new String[][]{
                                {"%money%", String.format("%.2f", bothMoneyCost)},
                                {"%exp%", String.valueOf(bothExpCost)}
                        }));
                return true;

            default:
                return true;
        }
    }

    /**
     * Muestra información de coste al jugador antes del pago
     */
    public void showCostInfo(Player player, String command, double distance, int maxRange) {
        if (!isEnabled()) return;

        CurrencyType currencyType = getCurrencyType(command);

        switch (currencyType) {
            case MONEY:
                double moneyCost = calculateMoneyCost(command, distance, maxRange);
                player.sendMessage(msgUtil.format("econ-cost-info-money",
                        new String[][]{{"%amount%", String.format("%.2f", moneyCost)}}));
                break;

            case EXPERIENCE:
                int expCost = calculateExperienceCost(command, distance, maxRange);
                player.sendMessage(msgUtil.format("econ-cost-info-exp",
                        new String[][]{{"%amount%", String.valueOf(expCost)}}));
                break;

            case BOTH:
                double bothMoneyCost = calculateMoneyCost(command, distance, maxRange);
                int bothExpCost = calculateExperienceCost(command, distance, maxRange);
                player.sendMessage(msgUtil.format("econ-cost-info-both",
                        new String[][]{
                                {"%money%", String.format("%.2f", bothMoneyCost)},
                                {"%exp%", String.valueOf(bothExpCost)}
                        }));
                break;
        }
    }

    /**
     * Obtiene el balance del jugador
     */
    public double getBalance(Player player) {
        if (!isMoneyEnabled()) return 0.0;
        return economy.getBalance(player);
    }

    /**
     * Obtiene la experiencia del jugador
     */
    public int getExperienceLevel(Player player) {
        return player.getLevel();
    }

    /**
     * Recarga la configuración de economía
     */
    public void reload() {
        this.economyEnabled = plugin.getConfig().getBoolean("economy.enabled", false);
        if (economyEnabled && economy == null) {
            setupEconomy();
        }
    }
}