package gc.grivyzom.util;

import gc.grivyzom.util.MessageUtil;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class EconomyService {

    private final FileConfiguration config;
    private final MessageUtil messageUtil;
    private Economy economy = null;
    private boolean vaultEnabled = false;

    public EconomyService(JavaPlugin plugin, FileConfiguration config, MessageUtil messageUtil) {
        this.config = config;
        this.messageUtil = messageUtil;
        setupEconomy(plugin);
    }

    /**
     * Configura la integración con Vault si está disponible
     */
    private void setupEconomy(JavaPlugin plugin) {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return;
        }

        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return;
        }

        economy = rsp.getProvider();
        vaultEnabled = economy != null;
    }

    /**
     * Verifica si el sistema de economía está habilitado
     */
    public boolean isEnabled() {
        return config.getBoolean("economy.enabled", false);
    }

    /**
     * Calcula el costo de un comando basado en la distancia
     */
    public EconomyCost calculateCost(String commandType, Location from, Location to) {
        if (!isEnabled()) {
            return new EconomyCost(0.0, 0, "none");
        }

        double distance = 0;
        if (from != null && to != null && from.getWorld().equals(to.getWorld())) {
            distance = from.distance(to);
        }

        String currencyType = config.getString("economy.commands." + commandType + ".currency-type", "experience");
        double maxRange = config.getDouble("max-range", 20000);

        // Calcular costos
        double moneyCost = 0.0;
        int expCost = 0;

        if (currencyType.equals("money") || currencyType.equals("both")) {
            double minMoney = config.getDouble("economy.commands." + commandType + ".money.min-cost", 0.0);
            double maxMoney = config.getDouble("economy.commands." + commandType + ".money.max-cost", 100.0);
            moneyCost = minMoney + (distance / maxRange) * (maxMoney - minMoney);
        }

        if (currencyType.equals("experience") || currencyType.equals("both")) {
            int minExp = config.getInt("economy.commands." + commandType + ".experience.min-cost", 0);
            int maxExp = config.getInt("economy.commands." + commandType + ".experience.max-cost", 30);
            expCost = (int) (minExp + (distance / maxRange) * (maxExp - minExp));
        }

        return new EconomyCost(moneyCost, expCost, currencyType);
    }

    /**
     * Calcula el costo de RTP basado en el rango
     */
    public EconomyCost calculateRTPCost(int range) {
        if (!isEnabled()) {
            return new EconomyCost(0.0, 0, "none");
        }

        String currencyType = config.getString("economy.commands.rtp.currency-type", "experience");
        double maxRange = config.getDouble("max-range", 20000);
        double rangeFactor = (double) range / maxRange;

        double moneyCost = 0.0;
        int expCost = 0;

        if (currencyType.equals("money") || currencyType.equals("both")) {
            double minMoney = config.getDouble("economy.commands.rtp.money.min-cost", 10.0);
            double maxMoney = config.getDouble("economy.commands.rtp.money.max-cost", 100.0);
            moneyCost = minMoney + rangeFactor * (maxMoney - minMoney);
        }

        if (currencyType.equals("experience") || currencyType.equals("both")) {
            int minExp = config.getInt("economy.commands.rtp.experience.min-cost", 5);
            int maxExp = config.getInt("economy.commands.rtp.experience.max-cost", 30);
            expCost = (int) (minExp + rangeFactor * (maxExp - minExp));
        }

        return new EconomyCost(moneyCost, expCost, currencyType);
    }

    /**
     * Verifica si el jugador puede pagar el costo
     */
    public boolean canAfford(Player player, EconomyCost cost) {
        if (!isEnabled() || cost.getCurrencyType().equals("none")) {
            return true;
        }

        boolean canAffordMoney = true;
        boolean canAffordExp = true;

        // Verificar dinero
        if (cost.getMoneyCost() > 0) {
            if (!vaultEnabled || economy == null) {
                return false; // No se puede usar dinero sin Vault
            }
            canAffordMoney = economy.has(player, cost.getMoneyCost());
        }

        // Verificar experiencia
        if (cost.getExpCost() > 0) {
            canAffordExp = player.getLevel() >= cost.getExpCost();
        }

        return canAffordMoney && canAffordExp;
    }

    /**
     * Cobra el costo al jugador
     */
    public boolean charge(Player player, EconomyCost cost) {
        if (!isEnabled() || cost.getCurrencyType().equals("none")) {
            return true;
        }

        if (!canAfford(player, cost)) {
            return false;
        }

        // Cobrar dinero
        if (cost.getMoneyCost() > 0 && vaultEnabled && economy != null) {
            economy.withdrawPlayer(player, cost.getMoneyCost());
        }

        // Cobrar experiencia
        if (cost.getExpCost() > 0) {
            player.setLevel(player.getLevel() - cost.getExpCost());
        }

        // Enviar mensaje de cobro
        sendChargeMessage(player, cost);

        return true;
    }

    /**
     * Muestra el costo al jugador sin cobrarlo
     */
    public void showCost(Player player, EconomyCost cost) {
        if (!isEnabled() || cost.getCurrencyType().equals("none")) {
            return;
        }

        String message = "";

        switch (cost.getCurrencyType()) {
            case "money" -> message = messageUtil.format("econ-cost-info-money",
                    new String[][]{{"%amount%", String.format("%.2f", cost.getMoneyCost())}});

            case "experience" -> message = messageUtil.format("econ-cost-info-exp",
                    new String[][]{{"%amount%", String.valueOf(cost.getExpCost())}});

            case "both" -> message = messageUtil.format("econ-cost-info-both",
                    new String[][]{
                            {"%money%", String.format("%.2f", cost.getMoneyCost())},
                            {"%exp%", String.valueOf(cost.getExpCost())}
                    });
        }

        if (!message.isEmpty()) {
            player.sendMessage(message);
        }
    }

    /**
     * Envía mensaje de insuficiencia de fondos
     */
    public void sendInsufficientFundsMessage(Player player, EconomyCost cost) {
        if (!isEnabled()) return;

        String message = "";

        // Verificar qué le falta específicamente
        boolean needsMoney = cost.getMoneyCost() > 0 &&
                (economy == null || !economy.has(player, cost.getMoneyCost()));
        boolean needsExp = cost.getExpCost() > 0 && player.getLevel() < cost.getExpCost();

        if (needsMoney && needsExp) {
            // Le faltan ambos
            message = messageUtil.format("econ-cost-info-both",
                    new String[][]{
                            {"%money%", String.format("%.2f", cost.getMoneyCost())},
                            {"%exp%", String.valueOf(cost.getExpCost())}
                    });
        } else if (needsMoney) {
            message = messageUtil.format("econ-insufficient-money",
                    new String[][]{{"%amount%", String.format("%.2f", cost.getMoneyCost())}});
        } else if (needsExp) {
            message = messageUtil.format("econ-insufficient-exp",
                    new String[][]{{"%amount%", String.valueOf(cost.getExpCost())}});
        }

        if (!message.isEmpty()) {
            player.sendMessage(message);
        }
    }

    /**
     * Envía mensaje de cobro exitoso
     */
    private void sendChargeMessage(Player player, EconomyCost cost) {
        String message = "";

        switch (cost.getCurrencyType()) {
            case "money" -> message = messageUtil.format("econ-charged-money",
                    new String[][]{{"%amount%", String.format("%.2f", cost.getMoneyCost())}});

            case "experience" -> message = messageUtil.format("econ-charged-exp",
                    new String[][]{{"%amount%", String.valueOf(cost.getExpCost())}});

            case "both" -> message = messageUtil.format("econ-charged-both",
                    new String[][]{
                            {"%money%", String.format("%.2f", cost.getMoneyCost())},
                            {"%exp%", String.valueOf(cost.getExpCost())}
                    });
        }

        if (!message.isEmpty()) {
            player.sendMessage(message);
        }
    }

    /**
     * Clase para representar un costo de economía
     */
    public static class EconomyCost {
        private final double moneyCost;
        private final int expCost;
        private final String currencyType;

        public EconomyCost(double moneyCost, int expCost, String currencyType) {
            this.moneyCost = Math.max(0, moneyCost);
            this.expCost = Math.max(0, expCost);
            this.currencyType = currencyType;
        }

        public double getMoneyCost() { return moneyCost; }
        public int getExpCost() { return expCost; }
        public String getCurrencyType() { return currencyType; }

        public boolean hasCost() {
            return moneyCost > 0 || expCost > 0;
        }
    }
}