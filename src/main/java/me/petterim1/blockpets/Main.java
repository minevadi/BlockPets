package me.petterim1.blockpets;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.event.player.PlayerTeleportEvent;
import cn.nukkit.level.GlobalBlockPalette;
import cn.nukkit.level.Level;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.DoubleTag;
import cn.nukkit.nbt.tag.FloatTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import cn.wode490390.nukkit.blockpets.MetricsLite;
import java.util.NoSuchElementException;

/*

PPPPPPPPPPPPPPPPP                              tttt                            !!!
P::::::::::::::::P                          ttt:::t                           !!:!!
P::::::PPPPPP:::::P                         t:::::t                           !:::!
PP:::::P     P:::::P                        t:::::t                           !:::!
P::::P     P:::::P  eeeeeeeeeeee    ttttttt:::::ttttttt        ssssssssss     !:::!
P::::P     P:::::Pee::::::::::::ee  t:::::::::::::::::t      ss::::::::::s    !:::!
P::::PPPPPP:::::Pe::::::eeeee:::::eet:::::::::::::::::t    ss:::::::::::::s   !:::!
P:::::::::::::PPe::::::e     e:::::etttttt:::::::tttttt    s::::::ssss:::::s  !:::!
P::::PPPPPPPPP  e:::::::eeeee::::::e      t:::::t           s:::::s  ssssss   !:::!
P::::P          e:::::::::::::::::e       t:::::t             s::::::s        !:::!
P::::P          e::::::eeeeeeeeeee        t:::::t                s::::::s     !!:!!
P::::P          e:::::::e                 t:::::t      ttttt       sss:::::s   !!! 
PP::::::PP        e::::::::e                t::::::tttt:::::ts:::::ssss::::::s
P::::::::P         e::::::::eeeeeeee        tt::::::::::::::ts::::::::::::::s  !!!
P::::::::P          ee:::::::::::::e          tt:::::::::::tt s:::::::::::ss  !!:!!
PPPPPPPPPP            eeeeeeeeeeeeee            ttttttttttt    sssssssssss     !!!

*----------------------*
| Pets for Nukkit      |
| Created by PetteriM1 |
*----------------------*

*/
public class Main extends PluginBase implements Listener {

    private static final int CONFIG_VERSION = 2;

    private static Main instance;

    public static Main getInstance() {
        return instance;
    }

    private Config config;

    private String nameTagColor;

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.config = this.getConfig();

        if (this.config.getInt("configVersion") != CONFIG_VERSION) {
            switch (this.config.getInt("configVersion")) {
                case 1:
                    this.config.set("teleportPets", true);
                    this.config.set("configVersion", CONFIG_VERSION);
                    this.config.save();
                    this.config = this.getConfig();
                    this.getLogger().info("Config file updated.");
                    break;
                default:
                    this.getLogger().warning("Config file version is unknown. Unable to update.");
            }
        }

        this.nameTagColor = this.config.getString("nameTagColor").replace("§", "\u00A7").replace("&", "\u00A7");

        Entity.registerEntity("BlockPet", EntityBlockPet.class);
        this.getServer().getPluginManager().registerEvents(this, this);
        this.getServer().getCommandMap().register("bpet", new BlockPetCommand(this.getServer(), this, this.config));
        new MetricsLite(this);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        String name = p.getName();
        String[] block = this.config.getString("players." + name.toLowerCase()).split(":");
        if (block.length > 1) {
            int id;
            int damage;
            try {
                id = Integer.parseInt(block[0]);
                damage = Integer.parseInt(block[1]);
            } catch (NumberFormatException nfe) {
                this.getLogger().warning("An error occurred while reading the data: " + name);
                return;
            }
            EntityBlockPet blockPet = createBlockPet(p, id, damage);
            if (blockPet != null) {
                blockPet.setOwner(name);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        this.getServer().getLevels().values().forEach((level) -> {
            for (Entity entity : level.getEntities()) {
                if (entity instanceof EntityBlockPet) {
                    if (((EntityBlockPet) entity).getOwner() == p) {
                        entity.close();
                    }
                }
            }
        });
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent e) {
        if (!this.config.getBoolean("teleportPets")) {
            return;
        }
        Player p = e.getPlayer();
        this.getServer().getLevels().values().forEach((level) -> {
            for (Entity entity : level.getEntities()) {
                if (entity instanceof EntityBlockPet) {
                    if (((EntityBlockPet) entity).getOwner() == p) {
                        Level lvl = e.getTo().getLevel();
                        if (lvl != entity.getLevel()) {
                            entity.setLevel(lvl);
                        }
                        entity.teleport(e.getTo());
                    }
                }
            }
        });
    }

    public String getNameTagColor() {
        return this.nameTagColor;
    }

    public static EntityBlockPet createBlockPet(Player player, int id, int damage) {
        try {
            GlobalBlockPalette.getOrCreateRuntimeId(id, damage);
        } catch (NoSuchElementException nsee) {
            return null;
        }
        FullChunk chunk = player.getLevel().getChunk(player.getFloorX() >> 4, player.getFloorZ() >> 4);
        if (chunk != null) {
            CompoundTag nbt = new CompoundTag()
                    .putList(new ListTag<DoubleTag>("Pos")
                            .add(new DoubleTag("", player.getX() + 0.5))
                            .add(new DoubleTag("", player.getY()))
                            .add(new DoubleTag("", player.getZ() + 0.5)))
                    .putList(new ListTag<DoubleTag>("Motion")
                            .add(new DoubleTag("", 0))
                            .add(new DoubleTag("", 0))
                            .add(new DoubleTag("", 0)))
                    .putList(new ListTag<FloatTag>("Rotation")
                            .add(new FloatTag("", 0))
                            .add(new FloatTag("", 0)))
                    .putInt("TileID", id)
                    .putByte("Data", damage);
            return (EntityBlockPet) Entity.createEntity("BlockPet", chunk, nbt);
        }
        return null;
    }
}
