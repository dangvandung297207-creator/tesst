package com.animeki.network;

import com.animeki.AnimeKi;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Every packet used by the mod.
 *
 * <p>Client to server packets only ever carry <em>intent</em>: which button was pressed, which way
 * the player is pointing and (for flight) a raw movement request. Damage, positions, cooldowns, Ki
 * values and transformation state are always decided by the server and flow the other way.</p>
 */
public final class Payloads {
    private Payloads() {
    }

    /** Client to server: a gameplay button was pressed / released. */
    public record AbilityInput(byte action, float forward, float strafe, float vertical,
                              boolean sprint, boolean sneak, boolean jump) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<AbilityInput> TYPE = new CustomPacketPayload.Type<>(AnimeKi.id("ability_input"));
        public static final StreamCodec<RegistryFriendlyByteBuf, AbilityInput> CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeByte(payload.action());
                    buf.writeFloat(payload.forward());
                    buf.writeFloat(payload.strafe());
                    buf.writeFloat(payload.vertical());
                    buf.writeBoolean(payload.sprint());
                    buf.writeBoolean(payload.sneak());
                    buf.writeBoolean(payload.jump());
                },
                buf -> new AbilityInput(buf.readByte(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
                        buf.readBoolean(), buf.readBoolean(), buf.readBoolean()));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client to server: continuous flight movement request (only honoured while flying). */
    public record FlightInput(float forward, float strafe, float vertical, boolean boost) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<FlightInput> TYPE = new CustomPacketPayload.Type<>(AnimeKi.id("flight_input"));
        public static final StreamCodec<RegistryFriendlyByteBuf, FlightInput> CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeFloat(payload.forward());
                    buf.writeFloat(payload.strafe());
                    buf.writeFloat(payload.vertical());
                    buf.writeBoolean(payload.boost());
                },
                buf -> new FlightInput(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readBoolean()));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client to server: a martial arts strike was requested. */
    public record CombatInput(byte kind, boolean heavyModifier) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<CombatInput> TYPE = new CustomPacketPayload.Type<>(AnimeKi.id("combat_input"));
        public static final StreamCodec<RegistryFriendlyByteBuf, CombatInput> CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeByte(payload.kind());
                    buf.writeBoolean(payload.heavyModifier());
                },
                buf -> new CombatInput(buf.readByte(), buf.readBoolean()));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client to server: send a fresh state snapshot (after joining or respawning). */
    public record StateRequest(boolean resendEverything) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<StateRequest> TYPE = new CustomPacketPayload.Type<>(AnimeKi.id("state_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, StateRequest> CODEC = StreamCodec.of(
                (buf, payload) -> buf.writeBoolean(payload.resendEverything()),
                buf -> new StateRequest(buf.readBoolean()));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Server to client: the owning player's HUD state (Ki, charge, transformation, cooldowns). */
    public record KiState(double current, double max, boolean charging, boolean exhausted, int chargeTicks,
                          String transformationId, byte transformationPhase, float auraIntensity,
                          boolean flying, boolean boosting, int comboStep, int comboHits,
                          int[] cooldowns, String activeAbility, float abilityProgress, byte feedback,
                          String feedbackAbility) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<KiState> TYPE = new CustomPacketPayload.Type<>(AnimeKi.id("ki_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, KiState> CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeDouble(payload.current());
                    buf.writeDouble(payload.max());
                    buf.writeBoolean(payload.charging());
                    buf.writeBoolean(payload.exhausted());
                    buf.writeVarInt(payload.chargeTicks());
                    buf.writeUtf(payload.transformationId());
                    buf.writeByte(payload.transformationPhase());
                    buf.writeFloat(payload.auraIntensity());
                    buf.writeBoolean(payload.flying());
                    buf.writeBoolean(payload.boosting());
                    buf.writeByte(payload.comboStep());
                    buf.writeByte(payload.comboHits());
                    int[] cooldowns = payload.cooldowns();
                    buf.writeVarInt(cooldowns.length);
                    for (int value : cooldowns) {
                        buf.writeVarInt(value);
                    }
                    buf.writeUtf(payload.activeAbility());
                    buf.writeFloat(payload.abilityProgress());
                    buf.writeByte(payload.feedback());
                    buf.writeUtf(payload.feedbackAbility());
                },
                buf -> {
                    double current = buf.readDouble();
                    double max = buf.readDouble();
                    boolean charging = buf.readBoolean();
                    boolean exhausted = buf.readBoolean();
                    int chargeTicks = buf.readVarInt();
                    String transformationId = buf.readUtf();
                    byte phase = buf.readByte();
                    float aura = buf.readFloat();
                    boolean flying = buf.readBoolean();
                    boolean boosting = buf.readBoolean();
                    byte comboStep = buf.readByte();
                    byte comboHits = buf.readByte();
                    int[] cooldowns = new int[buf.readVarInt()];
                    for (int i = 0; i < cooldowns.length; i++) {
                        cooldowns[i] = buf.readVarInt();
                    }
                    String activeAbility = buf.readUtf();
                    float abilityProgress = buf.readFloat();
                    byte feedback = buf.readByte();
                    String feedbackAbility = buf.readUtf();
                    return new KiState(current, max, charging, exhausted, chargeTicks, transformationId, phase,
                            aura, flying, boosting, comboStep, comboHits, cooldowns, activeAbility,
                            abilityProgress, feedback, feedbackAbility);
                });

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Server to client: what other players (and the local player) should look like. */
    public record VisualState(int entityId, String transformationId, byte phase, float auraIntensity,
                              boolean charging, boolean flying, boolean boosting, int comboStep) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<VisualState> TYPE = new CustomPacketPayload.Type<>(AnimeKi.id("visual_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, VisualState> CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeVarInt(payload.entityId());
                    buf.writeUtf(payload.transformationId());
                    buf.writeByte(payload.phase());
                    buf.writeFloat(payload.auraIntensity());
                    buf.writeBoolean(payload.charging());
                    buf.writeBoolean(payload.flying());
                    buf.writeBoolean(payload.boosting());
                    buf.writeByte(payload.comboStep());
                },
                buf -> new VisualState(buf.readVarInt(), buf.readUtf(), buf.readByte(), buf.readFloat(),
                        buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readByte()));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Server to client: spawn a reusable impact effect. */
    public record Vfx(byte event, double x, double y, double z, float dx, float dy, float dz,
                      float scale, int color, int duration, int entityId) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<Vfx> TYPE = new CustomPacketPayload.Type<>(AnimeKi.id("vfx"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Vfx> CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeByte(payload.event());
                    buf.writeDouble(payload.x());
                    buf.writeDouble(payload.y());
                    buf.writeDouble(payload.z());
                    buf.writeFloat(payload.dx());
                    buf.writeFloat(payload.dy());
                    buf.writeFloat(payload.dz());
                    buf.writeFloat(payload.scale());
                    buf.writeInt(payload.color());
                    buf.writeVarInt(payload.duration());
                    buf.writeVarInt(payload.entityId());
                },
                buf -> new Vfx(buf.readByte(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
                        buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readInt(),
                        buf.readVarInt(), buf.readVarInt()));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Server to client: local camera feedback. */
    public record CameraEffect(byte type, float intensity, int duration, int seed) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<CameraEffect> TYPE = new CustomPacketPayload.Type<>(AnimeKi.id("camera_effect"));
        public static final StreamCodec<RegistryFriendlyByteBuf, CameraEffect> CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeByte(payload.type());
                    buf.writeFloat(payload.intensity());
                    buf.writeVarInt(payload.duration());
                    buf.writeVarInt(payload.seed());
                },
                buf -> new CameraEffect(buf.readByte(), buf.readFloat(), buf.readVarInt(), buf.readVarInt()));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Server to client: authoritative snapshot of the beams near the player, plus clash state. */
    public record BeamSync(List<BeamState> beams) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<BeamSync> TYPE = new CustomPacketPayload.Type<>(AnimeKi.id("beam_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, BeamSync> CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeVarInt(payload.beams().size());
                    for (BeamState beam : payload.beams()) {
                        beam.write(buf);
                    }
                },
                buf -> {
                    int count = buf.readVarInt();
                    List<BeamState> beams = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) {
                        beams.add(BeamState.read(buf));
                    }
                    return new BeamSync(beams);
                });

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        /** A single beam segment as the client should draw it. */
        public record BeamState(int id, int ownerId, String typeId, double originX, double originY, double originZ,
                                float dirX, float dirY, float dirZ, float length, float width, int coreColor,
                                int glowColor, float power, float clashRatio, double clashX, double clashY,
                                double clashZ, int clashPartner) {

            public void write(RegistryFriendlyByteBuf buf) {
                buf.writeVarInt(id);
                buf.writeVarInt(ownerId);
                buf.writeUtf(typeId);
                buf.writeDouble(originX);
                buf.writeDouble(originY);
                buf.writeDouble(originZ);
                buf.writeFloat(dirX);
                buf.writeFloat(dirY);
                buf.writeFloat(dirZ);
                buf.writeFloat(length);
                buf.writeFloat(width);
                buf.writeInt(coreColor);
                buf.writeInt(glowColor);
                buf.writeFloat(power);
                buf.writeFloat(clashRatio);
                buf.writeDouble(clashX);
                buf.writeDouble(clashY);
                buf.writeDouble(clashZ);
                buf.writeVarInt(clashPartner);
            }

            public static BeamState read(RegistryFriendlyByteBuf buf) {
                return new BeamState(buf.readVarInt(), buf.readVarInt(), buf.readUtf(), buf.readDouble(),
                        buf.readDouble(), buf.readDouble(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
                        buf.readFloat(), buf.readFloat(), buf.readInt(), buf.readInt(), buf.readFloat(),
                        buf.readFloat(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readVarInt());
            }
        }
    }

    /** Server to client: why an ability did not fire (HUD/sound feedback). */
    public record AbilityFeedback(byte reason, String abilityId, int cooldownTicks, double kiCost) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<AbilityFeedback> TYPE = new CustomPacketPayload.Type<>(AnimeKi.id("ability_feedback"));
        public static final StreamCodec<RegistryFriendlyByteBuf, AbilityFeedback> CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeByte(payload.reason());
                    buf.writeUtf(payload.abilityId());
                    buf.writeVarInt(payload.cooldownTicks());
                    buf.writeDouble(payload.kiCost());
                },
                buf -> new AbilityFeedback(buf.readByte(), buf.readUtf(), buf.readVarInt(), buf.readDouble()));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Server to client: boss health bar and phase information. */
    public record BossState(int entityId, String nameKey, float health, float maxHealth, int phase, boolean raging) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<BossState> TYPE = new CustomPacketPayload.Type<>(AnimeKi.id("boss_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, BossState> CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeVarInt(payload.entityId());
                    buf.writeUtf(payload.nameKey());
                    buf.writeFloat(payload.health());
                    buf.writeFloat(payload.maxHealth());
                    buf.writeByte(payload.phase());
                    buf.writeBoolean(payload.raging());
                },
                buf -> new BossState(buf.readVarInt(), buf.readUtf(), buf.readFloat(), buf.readFloat(),
                        buf.readByte(), buf.readBoolean()));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Server to client: authoritative movement parameters so the client can predict flight. */
    public record FlightParams(double speed, double boostMultiplier, double verticalSpeed, double acceleration,
                              double drag, double maxSpeed, boolean enabled) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<FlightParams> TYPE = new CustomPacketPayload.Type<>(AnimeKi.id("flight_params"));
        public static final StreamCodec<RegistryFriendlyByteBuf, FlightParams> CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeDouble(payload.speed());
                    buf.writeDouble(payload.boostMultiplier());
                    buf.writeDouble(payload.verticalSpeed());
                    buf.writeDouble(payload.acceleration());
                    buf.writeDouble(payload.drag());
                    buf.writeDouble(payload.maxSpeed());
                    buf.writeBoolean(payload.enabled());
                },
                buf -> new FlightParams(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
                        buf.readDouble(), buf.readDouble(), buf.readBoolean()));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
