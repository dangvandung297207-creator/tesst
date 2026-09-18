# Anime Ki Combat

An original, anime-inspired **Ki energy martial arts combat framework** for Minecraft **1.21.1** on
**NeoForge** (Java 21).

It is not "vanilla plus particles". Every system - Ki economy, melee combos, blasts, beams, power
clashes, flight, dash, vanish, cinematic transformations, auras, impacts, camera work, terrain
destruction and bosses - is a small service with data driven balance, so new fighters, skills,
transformations, bosses and Blender authored effects can be added without touching the core loop.

---

## 1. Requirements

| Component | Version |
| --- | --- |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.219 or newer |
| Java | 21 |

Works on the **dedicated server** as well as the client: the server is authoritative for every
gameplay value, and all rendering code lives behind a physical-client check.

## 2. Building and running

```bash
./gradlew build          # produces build/libs/animeki-<version>+1.21.1.jar
./gradlew runClient      # dev client
./gradlew runServer      # dev server (--nogui)
```

The mod metadata is generated from `src/main/templates/META-INF/neoforge.mods.toml` by the
`generateModMetadata` task, so the version numbers come from `gradle.properties`.

## 3. Controls (all rebindable, no vanilla conflicts)

| Action | Default key | Notes |
| --- | --- | --- |
| Ki Charge | `R` (hold) | Raises regeneration and the aura; cancels on movement, attack or damage |
| Ki Blast | `F` | Hold to charge a heavy blast, release to throw |
| Primary Skill | `V` | First team skill (beam, charged blast, ...) |
| Secondary Skill | `C` | Second team skill |
| Dash | `G` | Short burst of speed with afterimages and optional i-frames |
| Flight Toggle | `X` | Enables flight with jump ascend / sneak descend / WASD |
| Transform | `Z` | Cycles through the enabled forms, or drops out of the current one |
| Ultimate | `B` | Charges the ultimate and fires it when ready |

Vanilla attack and use are untouched: melee combos are driven from the server side attack event, so
`left click` keeps its normal meaning and never triggers a hidden Ki attack.

## 4. Command reference

```
/ki set <player> <amount>       # set the current Ki
/ki give <player> <amount>      # add Ki
/ki transform <player> <id>     # force a form (ascended, divine, celestial, transcendent) or "none"
/ki cooldown reset <player>     # clear every cooldown
/ki flight <player> <on|off>    # toggle flight
/ki debug [player]              # print Ki, flight, combo, ability, beam and destruction state
```

Requires permission level 2.

## 5. Feature tour

### Ki
A regenerating pool (default 1000) with a charge mode: charging triples regeneration, lights up the
aura, and is cancelled the moment you move, attack or get hit. Spending Ki blocks regeneration for a
short delay, running dry triggers an exhaustion state with weakened output, and every derived number
(output, defence, melee, flight, regen) flows through `KiMath` so transformations, charge and
exhaustion stack predictably.

### Melee combat
A configurable light combo (punch, punch, kick, heavy finisher) with per-step timing windows, plus
heavy attacks with knockback, shockwaves and optional splash damage. Air combat supports air punches
and kicks, knock-up, knock-down and a chase strike that closes the distance to a launch target.
Every step emits a reusable VFX event and a small camera punch, which is what makes the hits read.

### Abilities
Registered in `AbilityRegistry`, all balance from config: **Ki Blast**, **Charged Ki Blast**,
**Energy Beam**, **Power Clash**, **Dash Strike**, **Vanish**, **Ground Slam**, **Meteor Strike**,
**Ultimate Energy Sphere**. Each ability declares cost, cooldown, charge/cast time, range, damage,
animation id and how it reacts to being released early or interrupted.

### Energy beam and power clash
The beam is a **single segment object**, never a chain of entities: one clipped ray for terrain, one
bounding-box sweep for entities, damage gated per entity by an interval. When two beams from
different owners intersect, both are cut short at the contact point, their outputs are compared and
the clash point is pushed toward the weaker fighter. The loser's beam bursts, the winner punches
through, and equal fighters wrestle until somebody runs out of Ki or hits the duration cap. Any beam
type can take part - the clash logic only needs the `BeamType` data.

### Flight, dash and vanish
Flight is a server driven mode with configurable speed, acceleration, drag, air braking, vertical
speed, boost multiplier and Ki drain - far faster than walking, and it never touches vanilla creative
flight. Dashing gives a short prep window, burst movement, optional invulnerability frames,
afterimages, a sonic boom past a speed threshold and a cooldown. Vanish teleports toward the aimed
target or the aim direction using a validated, collision checked destination, reappears with a burst
and opens a short post-vanish attack window.

### Transformations
A cinematic state machine (`CHARGING -> ENERGY_BUILDUP -> TRANSFORMATION -> SHOCKWAVE -> ACTIVE`)
with movement restriction, aura ramp, build-up sound, screen flash, camera shake, shockwave,
optional terrain scar and stat activation. Four original forms ship: **Ascended** (gold, aggressive),
**Divine** (crimson, Ki control and flight), **Celestial** (blue/white, huge Ki output and stronger
beams) and **Transcendent** (white/silver, dodge and melee focused). Multipliers, costs, drain,
duration, sequence speed and aura shapes are all configuration.

### Auras, impacts and camera
Auras are assembled from data (`AuraProfile`): outer shell, inner glow, energy flames, rotating
rings, floating sparks, ground ring and procedural lightning, with intensity/scale/colour/speed.
Impacts are reusable events (`LIGHT_HIT`, `HEAVY_HIT`, `GROUND_IMPACT`, `DASH_IMPACT`, `BEAM_IMPACT`,
`TRANSFORMATION_SHOCKWAVE`, `ULTIMATE_IMPACT`) that each resolve to geometry, particles, a sound and
a camera reaction - completely independent of the ability that fired them. The camera manager handles
shake, FOV punch, zoom, impulse, flash and vignette, always locally and always configurable
(including a full disable for motion sensitivity).

### Terrain destruction
Budgeted destruction: max radius, max blocks per call, allowed and protected blocks, cooldown,
debris VFX and an explicit "never touch bedrock or important structures" tag. Ground slams leave
craters, heavy punches chip the surface, ultimates carve a controlled area - and every change goes
through `DestructionService`, which is server side and rate limited.

### Ultimates and the boss
Meteor Strike charges with rapid Ki drain, launches the fighter, marks the target area, drops a
sphere and detonates it with a shockwave and terrain damage. A giant energy sphere is the second
ultimate. The **Void Titan** is a complete three phase boss (melee/dash/slam, then flight with Ki
volleys and beams, then a transformation with a faster, stronger aura and its own ultimate) driven by
its own controller and state machine, with a rage state under 18% health.

## 6. Configuration

Two files are generated on first run:

* `config/animeki-server.toml` - Ki, regeneration, charging, combat timing, every ability, beam and
  clash numbers, flight, dash, vanish, transformation multipliers, destruction limits, boss
  difficulty. Server owners control the balance; nothing is hardcoded.
* `config/animeki-client.toml` - VFX intensity and quality, aura/beam/trail toggles, vanilla particle
  overlap, camera shake scale and toggles, HUD layout and colours.

## 7. Multiplayer and anti-cheat notes

* The client only sends **intent**: which button was pressed, which way it aims, and raw movement
  input while flying. Damage, costs, cooldowns, teleport destinations, targeting and transformation
  state are decided on the server.
* Positions are never taken from packets. Vanish destinations are ray-checked and collision-checked on
  the server; dash direction comes from movement keys interpreted server side.
* Rates are capped (flight input only when changed, HUD snapshots at 4/40 tick intervals, beam
  snapshots every other tick, appearance broadcasts at 10/60 ticks), so a busy server is not
  flooded by small packets.
* Visual state for other players is broadcast separately from gameplay state, so a malicious client
  can at worst lie about its own animation - never about damage.

## 8. Architecture

```
com.animeki
├── AnimeKi                 mod entry point
├── config/                 server + client ModConfigSpec trees
├── util/                   maths and targeting/raycast helpers
├── ki/                     PlayerKi data, KiMath (all derived values), KiService (server tick)
├── combat/                 ComboState, AttackKind, CombatService
├── ability/                framework (Ability, AbilityStats, AbilityContext, registry, service)
│   └── impl/               the nine shipped abilities
├── beam/                   BeamType/BeamTypes, BeamInstance, BeamService (collision + clashes)
├── flight/                 FlightState, FlightService, DashService
├── transformation/         Transformation, Transformations, AuraProfile, PlayerTransformation, service
├── entity/                 KiBlastEntity
├── boss/                   BossPhase, BossAbility, VoidTitanEntity, VoidTitanController
├── destruction/            DestructionProfile, DestructionService
├── vfx/                    VfxEvent, CameraEffectType, VfxDispatcher
├── network/                Payloads, AnimeKiNetwork, ServerPayloadHandler, ClientPayloadBridge
├── gameplay/               CommonGameplayEvents (all server hooks in one place)
├── command/                KiCommand
├── registry/               attachments, sounds, damage types, tags, entities, items
└── client/                 everything client only
    ├── input/  net/  hud/  camera/
    ├── render/             aura, beam, impact, world renderer, entity renderers
    └── vfx/                vanilla particle spawning
```

Resources follow the requested layout: `assets/animeki/{lang,models,textures,animations,shaders,vfx}`
and `data/animeki/{damage_type,tags/block}`.

## 9. Extending the mod

**A new ability** - extend `Ability`, fill `AbilityStats.builder()` from your config, implement
`onStart/onChargeTick/onRelease/onTick/onStop`, then register it in `AbilityRegistry.bootstrap()`.
Nothing else changes; keybinds, HUD pips, feedback and networking already work.

**A new transformation** - add a config section, then `Transformations.register(Transformation.builder(id)
    .displayKey(...).multipliers(...).cost(...).sequence(...).aura(AuraProfile.builder()...build()).build())`.
The aura renderer, HUD banner, stat application and drain all pick it up automatically.

**A new boss** - copy the `VoidTitanController` state machine shape: an entity with synched phase data
plus a controller that picks abilities on cooldowns. `BossAbility` already carries the shared move set
(melee combo, dash, slam, volley, beam, teleport, transformation, ultimate).

**A new impact effect** - add a `VfxEvent` constant, then a branch in `ImpactRenderer` (or just reuse an
existing shape). Abilities emit events, never draw.

**Blender / external assets** - `tools/` ships the pipeline description in
`assets/animeki/animations/README.md`: export the model, animation and VFX meshes from Blender, drop
them into `assets/animeki/{models,animations,textures}` and reference them by id in
`AuraProfile.mesh(...)` / the `assets/animeki/vfx/*.json` definitions. There is **no Blender runtime
dependency** - Blender is only used offline to author the files.

## 10. Performance

* Beams are one object per beam: no entity spam, one ray per tick, one bounding box query per tick.
* Destruction is budgeted (max radius/blocks per call, cooldown, server-side queue) and skips
  protected blocks; block updates never run per tick across an area.
* VFX are client side. Particle counts are capped and distance culled, and the custom geometry is
  emitted into a single additive render type with a handful of quads per effect.
* Syncing is rate limited and split (gameplay HUD state vs. appearance vs. beams).
* No per tick large-area scans: targeting uses ray/segment tests, cones and cached entity lists.

## 11. Repository tooling

* `tools/java_audit.py` - a dependency free cross reference auditor used because this development
  environment has no JDK: it verifies that every import resolves, every config property exists,
  every enum/registry constant exists and every cross class method call matches a declaration.
  Run it with `python3 tools/java_audit.py`.
* `tools/generate_textures.py` - regenerates every PNG (boss sheets, ability icons, logo) so no
  binary art has to be committed by hand.

## 12. Known limitations

* The mod ships no `.ogg` audio: `assets/animeki/sounds.json` maps every sound event to a vanilla
  fallback so the mod is audible out of the box, and a pack only has to drop real files into
  `assets/animeki/sounds/` (the loader prefers `animeki:<name>` when present).
* The boss animation set is deliberately minimal (vanilla humanoid animation plus procedural aura);
  the renderer is written so a Blender exported model and animation set can replace it.
* Balance values are sane defaults, not the result of a real play test - everything is exposed in
  the config for tuning.
