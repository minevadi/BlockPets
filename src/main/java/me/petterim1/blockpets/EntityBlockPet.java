package me.petterim1.blockpets;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockFence;
import cn.nukkit.block.BlockFenceGate;
import cn.nukkit.block.BlockLiquid;
import cn.nukkit.block.BlockSlab;
import cn.nukkit.block.BlockStairs;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityCreature;
import cn.nukkit.entity.data.IntEntityData;
import cn.nukkit.entity.passive.EntityAnimal;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.level.GlobalBlockPalette;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.NukkitMath;
import cn.nukkit.math.Vector2;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import java.util.concurrent.ThreadLocalRandom;

public class EntityBlockPet extends EntityCreature {

    public static final int NETWORK_ID = 66;

    protected String owner;
    protected Vector3 target;
    protected Entity followTarget;
    protected int stayTime = 0;
    protected int moveTime = 0;

    protected int blockId = 0;
    protected int damage = 0;

    public EntityBlockPet(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    @Override
    public int getNetworkId() {
        return NETWORK_ID;
    }

    @Override
    public float getWidth() {
        return 0.98f;
    }

    @Override
    public float getLength() {
        return 0.98f;
    }

    @Override
    public float getHeight() {
        return 0.98f;
    }

    @Override
    protected float getBaseOffset() {
        return 0.49f;
    }

    @Override
    public String getName() {
        return Main.getInstance().getNameTagColor() + this.owner + "'s pet";
    }

    @Override
    protected void initEntity() {
        super.initEntity();

        if (this.namedTag != null) {
            if (this.namedTag.contains("TileID")) {
                this.blockId = this.namedTag.getInt("TileID");
            }
            if (this.namedTag.contains("Data")) {
                this.damage = this.namedTag.getByte("Data");
            }

            if (this.namedTag.contains("Owner")) {
                this.setOwner(this.namedTag.getString("Owner"));
            }
            this.setNameTagVisible(true);
        }

        if (this.blockId == 0) {
            this.close();
            return;
        }

        this.setDataProperty(new IntEntityData(DATA_VARIANT, GlobalBlockPalette.getOrCreateRuntimeId(this.getBlock(), this.getDamage())));
    }

    @Override
    public void saveNBT() {
        super.saveNBT();
        this.namedTag.putInt("TileID", this.blockId);
        this.namedTag.putByte("Data", this.damage);

        this.namedTag.putString("Owner", this.owner);
    }

    @Override
    public boolean attack(EntityDamageEvent ev) {
        return false;
    }

    public Player getOwner() {
        return this.server.getPlayer(this.owner);
    }

    public void setOwner(String owner) {
        this.owner = owner;
        this.setNameTag(this.getName());
        this.saveNBT();
    }

    public boolean targetOption(EntityCreature creature, double distance) {
        if (creature instanceof Player) {
            Player player = (Player) creature;
            return player.isAlive() && !player.closed && player == this.getOwner() && distance < 300 && distance > 12;
        }

        return false;
    }

    protected void checkTarget() {
        if (this.followTarget != null && !this.followTarget.closed && this.followTarget.isAlive()) {
            return;
        }

        Vector3 target = this.target;
        if (!(target instanceof EntityCreature)
                || !this.targetOption((EntityCreature) target, this.distanceSquared(target))) {
            double near = Integer.MAX_VALUE;

            for (Entity entity : this.getLevel().getEntities()) {
                if (entity == this || !(entity instanceof EntityCreature) || entity instanceof EntityAnimal) {
                    continue;
                }

                EntityCreature creature = (EntityCreature) entity;

                double distance = this.distanceSquared(creature);
                if (distance > near || !this.targetOption(creature, distance)) {
                    continue;
                }
                near = distance;

                this.stayTime = 0;
                this.moveTime = 0;
                this.target = creature;
            }
        }

        if (this.target instanceof EntityCreature && !((Entity) this.target).closed
                && ((EntityCreature) this.target).isAlive()
                && this.targetOption((EntityCreature) this.target, this.distanceSquared(this.target))) {
            return;
        }

        int x;
        int z;
        if (this.stayTime > 0) {
            if (ThreadLocalRandom.current().nextInt(1, 100) > 5) {
                return;
            }
            x = ThreadLocalRandom.current().nextInt(10, 30);
            z = ThreadLocalRandom.current().nextInt(10, 30);
            this.target = this.add(ThreadLocalRandom.current().nextBoolean() ? x : -x, ThreadLocalRandom.current().nextInt(-20, 20) / 10, ThreadLocalRandom.current().nextBoolean() ? z : -z);
        } else if (ThreadLocalRandom.current().nextInt(1, 400) == 1) {
            x = ThreadLocalRandom.current().nextInt(10, 30);
            z = ThreadLocalRandom.current().nextInt(10, 30);
            this.stayTime = ThreadLocalRandom.current().nextInt(100, 200);
            this.target = this.add(ThreadLocalRandom.current().nextBoolean() ? x : -x, ThreadLocalRandom.current().nextInt(-20, 20) / 10, ThreadLocalRandom.current().nextBoolean() ? z : -z);
        } else if (this.moveTime <= 0 || this.target == null) {
            x = ThreadLocalRandom.current().nextInt(10, 30);
            z = ThreadLocalRandom.current().nextInt(10, 30);
            this.stayTime = 0;
            this.moveTime = ThreadLocalRandom.current().nextInt(60, 200);
            this.target = this.add(ThreadLocalRandom.current().nextBoolean() ? x : -x, 0, ThreadLocalRandom.current().nextBoolean() ? z : -z);
        }
    }

    protected boolean checkJump(double dx, double dz) {
        if (this.motionY == this.getGravity() * 2) {
            return this.level.getBlock(new Vector3(NukkitMath.floorDouble(this.x), (int) this.y,
                    NukkitMath.floorDouble(this.z))) instanceof BlockLiquid;
        } else {
            if (this.level.getBlock(new Vector3(NukkitMath.floorDouble(this.x), (int) (this.y + 0.8),
                    NukkitMath.floorDouble(this.z))) instanceof BlockLiquid) {
                this.motionY = this.getGravity() * 2;
                return true;
            }
        }

        if (!this.onGround || this.stayTime > 0) {
            return false;
        }

        Block that = this.getLevel().getBlock(new Vector3(NukkitMath.floorDouble(this.x + dx), (int) this.y, NukkitMath.floorDouble(this.z + dz)));
        if (this.getDirection() == null) {
            return false;
        }

        Block block = that.getSide(this.getHorizontalFacing());
        if (!block.canPassThrough() && block.up().canPassThrough()) {
            if (block instanceof BlockFence || block instanceof BlockFenceGate) {
                this.motionY = this.getGravity();
            } else if (this.motionY <= this.getGravity() * 4) {
                this.motionY = this.getGravity() * 4;
            } else if (block instanceof BlockSlab || block instanceof BlockStairs) {
                this.motionY = this.getGravity() * 4;
            } else if (this.motionY <= (this.getGravity() * 8)) {
                this.motionY = this.getGravity() * 8;
            } else {
                this.motionY += this.getGravity() * 0.25;
            }
            return true;
        }
        return false;
    }

    public Vector3 updateMove(int tickDiff) {
        if (this.followTarget != null && !this.followTarget.closed && this.followTarget.isAlive()) {
            double x = this.followTarget.x - this.x;
            double z = this.followTarget.z - this.z;

            double diff = Math.abs(x) + Math.abs(z);
            if (this.stayTime > 0 || this.distance(this.followTarget) <= (this.getWidth() + 0.0d) / 2 + 0.05) {
                this.motionX = 0;
                this.motionZ = 0;
            } else {
                if (this.isInsideOfWater()) {
                    this.motionX = 1.1 * 0.05 * (x / diff);
                    this.motionZ = 1.1 * 0.05 * (z / diff);
                } else {
                    this.motionX = 1.1 * 0.1 * (x / diff);
                    this.motionZ = 1.1 * 0.1 * (z / diff);
                }
            }
            this.yaw = Math.toDegrees(-Math.atan2(x / diff, z / diff));
            return this.followTarget;
        }

        Vector3 before = this.target;
        this.checkTarget();
        if (this.target instanceof EntityCreature || before != this.target) {
            double x = this.target.x - this.x;
            double z = this.target.z - this.z;

            double diff = Math.abs(x) + Math.abs(z);
            if (this.stayTime > 0 || this.distance(this.target) <= (this.getWidth() + 0.0d) / 2 + 0.05) {
                this.motionX = 0;
                this.motionZ = 0;
            } else {
                if (this.isInsideOfWater()) {
                    this.motionX = 1.1 * 0.05 * (x / diff);
                    this.motionZ = 1.1 * 0.05 * (z / diff);
                } else {
                    this.motionX = 1.1 * 0.15 * (x / diff);
                    this.motionZ = 1.1 * 0.15 * (z / diff);
                }
            }
            this.yaw = Math.toDegrees(-Math.atan2(x / diff, z / diff));
        }

        double dx = this.motionX * tickDiff;
        double dz = this.motionZ * tickDiff;
        boolean isJump = this.checkJump(dx, dz);
        if (this.stayTime > 0) {
            this.stayTime -= tickDiff;
            this.move(0, this.motionY * tickDiff, 0);
        } else {
            Vector2 be = new Vector2(this.x + dx, this.z + dz);
            this.move(dx, this.motionY * tickDiff, dz);
            Vector2 af = new Vector2(this.x, this.z);

            if ((be.x != af.x || be.y != af.y) && !isJump) {
                this.moveTime -= 90 * tickDiff;
            }
        }

        if (!isJump) {
            if (this.onGround) {
                this.motionY = 0;
            } else if (this.motionY > -this.getGravity() * 4) {
                if (!(this.level.getBlock(new Vector3(NukkitMath.floorDouble(this.x), (int) (this.y + 0.8),
                        NukkitMath.floorDouble(this.z))) instanceof BlockLiquid)) {
                    this.motionY -= this.getGravity() * 1;
                }
            } else {
                this.motionY -= this.getGravity() * tickDiff;
            }
        }

        this.updateMovement();

        return this.target;
    }

    @Override
    public boolean onUpdate(int currentTick) {
        if (this.closed) {
            return false;
        }

        if (!this.isAlive()) {
            this.close();
            return false;
        }

        int tickDiff = currentTick - this.lastUpdate;
        this.lastUpdate = currentTick;
        this.entityBaseTick(tickDiff);

        Vector3 target = this.updateMove(tickDiff);

        Player owner = this.getOwner();
        if (target instanceof Player) {
            if (this.distanceSquared(target) <= 100) {
                this.x = this.lastX;
                this.y = this.lastY;
                this.z = this.lastZ;
            } else if (owner != null && owner.isOnGround()) {
                this.teleport(target);
            }
        } else if (owner != null && this.distanceSquared(owner) > 100 && owner.isOnGround()) {
            this.teleport(owner);
        }

        return true;
    }

    @Override
    public boolean entityBaseTick(int tickDiff) {
        if (this.moveTime > 0) {
            this.moveTime -= tickDiff;
        }

        return true;
    }

    public int getBlock() {
        return this.blockId;
    }

    public int getDamage() {
        return this.damage;
    }
}
