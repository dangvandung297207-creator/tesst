package com.omnitrix.mod.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.omnitrix.mod.alien.AlienForm;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public class OmnitrixPlayerData {

    public static final Codec<OmnitrixPlayerData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.BOOL.fieldOf("hasOmnitrix").forGetter(OmnitrixPlayerData::hasOmnitrix),
                    Codec.STRING.fieldOf("currentForm").forGetter(d -> d.currentForm.getId()),
                    Codec.STRING.fieldOf("selectedForm").forGetter(d -> d.selectedForm.getId()),
                    Codec.INT.fieldOf("energy").forGetter(OmnitrixPlayerData::getEnergy),
                    Codec.INT.fieldOf("transformTicksLeft").forGetter(OmnitrixPlayerData::getTransformTicksLeft),
                    Codec.INT.fieldOf("rechargeCooldown").forGetter(OmnitrixPlayerData::getRechargeCooldown),
                    Codec.INT.fieldOf("transformSequenceTick").forGetter(OmnitrixPlayerData::getTransformSequenceTick),
                    Codec.BOOL.fieldOf("isTransforming").forGetter(OmnitrixPlayerData::isTransforming),
                    Codec.BOOL.fieldOf("isReverting").forGetter(OmnitrixPlayerData::isReverting),
                    Codec.INT.fieldOf("ultimateCharge").forGetter(OmnitrixPlayerData::getUltimateCharge),
                    Codec.INT.fieldOf("ability1Cooldown").forGetter(OmnitrixPlayerData::getAbility1Cooldown),
                    Codec.INT.fieldOf("ability2Cooldown").forGetter(OmnitrixPlayerData::getAbility2Cooldown),
                    Codec.INT.fieldOf("ability3Cooldown").forGetter(OmnitrixPlayerData::getAbility3Cooldown),
                    Codec.INT.fieldOf("cannonboltRolling").forGetter(OmnitrixPlayerData::getCannonboltRolling)
            ).apply(instance, OmnitrixPlayerData::new)
    );

    public static final StreamCodec<ByteBuf, OmnitrixPlayerData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, OmnitrixPlayerData::hasOmnitrix,
            ByteBufCodecs.STRING_UTF8, d -> d.currentForm.getId(),
            ByteBufCodecs.STRING_UTF8, d -> d.selectedForm.getId(),
            ByteBufCodecs.VAR_INT, OmnitrixPlayerData::getEnergy,
            ByteBufCodecs.VAR_INT, OmnitrixPlayerData::getTransformTicksLeft,
            ByteBufCodecs.VAR_INT, OmnitrixPlayerData::getRechargeCooldown,
            ByteBufCodecs.VAR_INT, OmnitrixPlayerData::getTransformSequenceTick,
            ByteBufCodecs.BOOL, OmnitrixPlayerData::isTransforming,
            ByteBufCodecs.BOOL, OmnitrixPlayerData::isReverting,
            ByteBufCodecs.VAR_INT, OmnitrixPlayerData::getUltimateCharge,
            ByteBufCodecs.VAR_INT, OmnitrixPlayerData::getAbility1Cooldown,
            ByteBufCodecs.VAR_INT, OmnitrixPlayerData::getAbility2Cooldown,
            ByteBufCodecs.VAR_INT, OmnitrixPlayerData::getAbility3Cooldown,
            ByteBufCodecs.VAR_INT, OmnitrixPlayerData::getCannonboltRolling,
            OmnitrixPlayerData::new
    );

    private boolean hasOmnitrix = true;
    private AlienForm currentForm = AlienForm.NONE;
    private AlienForm selectedForm = AlienForm.HEATBLAST;
    private int energy = 100;
    private int transformTicksLeft = 0;
    private int rechargeCooldown = 0;
    private int transformSequenceTick = 0;
    private boolean isTransforming = false;
    private boolean isReverting = false;
    private int ultimateCharge = 0;
    private int ability1Cooldown = 0;
    private int ability2Cooldown = 0;
    private int ability3Cooldown = 0;
    private int cannonboltRolling = 0; // 0 = not rolling, >0 = roll duration/speed tick

    public OmnitrixPlayerData() {}

    public OmnitrixPlayerData(boolean hasOmnitrix, String currentForm, String selectedForm, int energy,
                              int transformTicksLeft, int rechargeCooldown, int transformSequenceTick,
                              boolean isTransforming, boolean isReverting, int ultimateCharge,
                              int ability1Cooldown, int ability2Cooldown, int ability3Cooldown, int cannonboltRolling) {
        this.hasOmnitrix = hasOmnitrix;
        this.currentForm = AlienForm.fromId(currentForm);
        this.selectedForm = AlienForm.fromId(selectedForm);
        this.energy = energy;
        this.transformTicksLeft = transformTicksLeft;
        this.rechargeCooldown = rechargeCooldown;
        this.transformSequenceTick = transformSequenceTick;
        this.isTransforming = isTransforming;
        this.isReverting = isReverting;
        this.ultimateCharge = ultimateCharge;
        this.ability1Cooldown = ability1Cooldown;
        this.ability2Cooldown = ability2Cooldown;
        this.ability3Cooldown = ability3Cooldown;
        this.cannonboltRolling = cannonboltRolling;
    }

    public OmnitrixPlayerData copy() {
        return new OmnitrixPlayerData(
                this.hasOmnitrix,
                this.currentForm.getId(),
                this.selectedForm.getId(),
                this.energy,
                this.transformTicksLeft,
                this.rechargeCooldown,
                this.transformSequenceTick,
                this.isTransforming,
                this.isReverting,
                this.ultimateCharge,
                this.ability1Cooldown,
                this.ability2Cooldown,
                this.ability3Cooldown,
                this.cannonboltRolling
        );
    }

    // Getters and Setters
    public boolean hasOmnitrix() { return hasOmnitrix; }
    public void setHasOmnitrix(boolean hasOmnitrix) { this.hasOmnitrix = hasOmnitrix; }

    public AlienForm getCurrentForm() { return currentForm; }
    public void setCurrentForm(AlienForm currentForm) { this.currentForm = currentForm; }

    public AlienForm getSelectedForm() { return selectedForm; }
    public void setSelectedForm(AlienForm selectedForm) { this.selectedForm = selectedForm; }

    public int getEnergy() { return energy; }
    public void setEnergy(int energy) { this.energy = Math.max(0, Math.min(100, energy)); }

    public int getTransformTicksLeft() { return transformTicksLeft; }
    public void setTransformTicksLeft(int ticks) { this.transformTicksLeft = Math.max(0, ticks); }

    public int getRechargeCooldown() { return rechargeCooldown; }
    public void setRechargeCooldown(int cooldown) { this.rechargeCooldown = Math.max(0, cooldown); }

    public int getTransformSequenceTick() { return transformSequenceTick; }
    public void setTransformSequenceTick(int tick) { this.transformSequenceTick = tick; }

    public boolean isTransforming() { return isTransforming; }
    public void setTransforming(boolean transforming) { isTransforming = transforming; }

    public boolean isReverting() { return isReverting; }
    public void setReverting(boolean reverting) { isReverting = reverting; }

    public int getUltimateCharge() { return ultimateCharge; }
    public void setUltimateCharge(int charge) { this.ultimateCharge = Math.max(0, Math.min(100, charge)); }

    public int getAbility1Cooldown() { return ability1Cooldown; }
    public void setAbility1Cooldown(int cooldown) { this.ability1Cooldown = Math.max(0, cooldown); }

    public int getAbility2Cooldown() { return ability2Cooldown; }
    public void setAbility2Cooldown(int cooldown) { this.ability2Cooldown = Math.max(0, cooldown); }

    public int getAbility3Cooldown() { return ability3Cooldown; }
    public void setAbility3Cooldown(int cooldown) { this.ability3Cooldown = Math.max(0, cooldown); }

    public int getCannonboltRolling() { return cannonboltRolling; }
    public void setCannonboltRolling(int rolling) { this.cannonboltRolling = rolling; }

    public boolean isTransformed() {
        return currentForm != AlienForm.NONE;
    }

    public boolean isCoolingDown() {
        return rechargeCooldown > 0;
    }
}
