package me.petterim1.blockpets;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.data.CommandParamType;
import cn.nukkit.command.data.CommandParameter;
import cn.nukkit.entity.Entity;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.TextFormat;

public class BlockPetCommand extends Command {

    private final Server server;
    private final Main plugin;
    private final Config config;

    public BlockPetCommand(Server server, Main plugin, Config config) {
        super("bpet", "BlockPet command", "/bpet <add|remove> <player> [blockId] [blockData]");
        this.setPermission("blockpets.manage");
        this.commandParameters.clear();
        this.commandParameters.put("add", new CommandParameter[]{
                new CommandParameter("add", false, new String[]{"add"}),
                new CommandParameter("player", CommandParamType.TARGET, false),
                new CommandParameter("blockId", CommandParamType.INT, false),
                new CommandParameter("blockData", CommandParamType.INT, true)
        });
        this.commandParameters.put("remove", new CommandParameter[]{
                new CommandParameter("remove", false, new String[]{"remove"}),
                new CommandParameter("player", CommandParamType.TARGET, false)
        });
        this.server = server;
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!this.plugin.isEnabled()) {
            return false;
        }
        if (!this.testPermission(sender)) {
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(TextFormat.LIGHT_PURPLE + "* " + TextFormat.GREEN + "BlockPets " + TextFormat.LIGHT_PURPLE + "*");
            sender.sendMessage(TextFormat.GOLD + "/bpet add <player> <blockId> [blockData]");
            sender.sendMessage(TextFormat.GOLD + "/bpet remove <player>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "add":
                if (args.length < 3) {
                    sender.sendMessage(TextFormat.GOLD + "Usage: /bpet add <player> <blockId> [blockData]");
                    return true;
                }

                if (this.server.getPlayerExact(args[1]) == null) {
                    sender.sendMessage(TextFormat.LIGHT_PURPLE + ">> " + TextFormat.RED + "Unknown player");
                    return true;
                }

                if (this.config.getString("players." + args[1].toLowerCase()).contains(":")) {
                    sender.sendMessage(TextFormat.LIGHT_PURPLE + ">> " + TextFormat.RED + "This player already have a pet");
                    return true;
                }

                int id;
                try {
                    id = Integer.parseInt(args[2]);
                } catch (NumberFormatException nfe) {
                    sender.sendMessage(TextFormat.LIGHT_PURPLE + ">> " + TextFormat.RED + "Unable to parse block id");
                    return true;
                }

                int damage = 0;
                if (args.length > 3) {
                    try {
                        damage = Integer.parseInt(args[3]);
                    } catch (NumberFormatException ignore) {

                    }
                }

                Player player = this.server.getPlayer(args[1]);
                EntityBlockPet blockPet = Main.createBlockPet(player, id, damage);

                if (blockPet != null) {
                    blockPet.setOwner(player.getName());

                    this.config.set("players." + args[1].toLowerCase(), args[2] + ":" + damage);
                    this.config.save();

                    sender.sendMessage(TextFormat.LIGHT_PURPLE + ">> " + TextFormat.GREEN + "Pet added");
                    return true;
                }

                sender.sendMessage(TextFormat.LIGHT_PURPLE + ">> " + TextFormat.RED + "Unable to add pet");
                return true;
            case "remove":
                if (args.length < 2) {
                    sender.sendMessage(TextFormat.GOLD + "Usage: /bpet remove <player>");
                    return true;
                }

                if (this.server.getPlayerExact(args[1]) == null) {
                    sender.sendMessage(TextFormat.LIGHT_PURPLE + ">> " + TextFormat.RED + "Unknown player");
                    return true;
                }

                this.config.set("players." + args[1].toLowerCase(), "null");
                this.config.save();

                this.server.getLevels().values().forEach((level) -> {
                    for (Entity entity : level.getEntities()) {
                        if (entity instanceof EntityBlockPet) {
                            if (((EntityBlockPet) entity).getOwner() == this.server.getPlayer(args[1])) {
                                entity.close();
                            }
                        }
                    }
                });

                sender.sendMessage(TextFormat.LIGHT_PURPLE + ">> " + TextFormat.GREEN + "Pet removed");
                return true;
            default:
                sender.sendMessage(TextFormat.LIGHT_PURPLE + "* " + TextFormat.GREEN + "BlockPets " + TextFormat.LIGHT_PURPLE + "*");
                sender.sendMessage(TextFormat.GOLD + "/bpet add <player> <blockId> [blockData]");
                sender.sendMessage(TextFormat.GOLD + "/bpet remove <player>");
        }
        return true;
    }
}
