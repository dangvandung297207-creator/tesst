# Omnitrix: Ultimate Alien Experience

A complete, immersive, high-polish Minecraft 1.21.1 NeoForge mod inspired by Ben 10.

Transform into 8 iconic alien lifeforms with distinct 3D models, smooth animations, cinematic multi-stage transformations, custom particle visual effects (VFX), unique combat abilities, and full multiplayer synchronization.

---

## 🌟 Key Features

### 1. The Omnitrix System
- **Bonding Mechanism:** Right-click the Omnitrix item to bind it with the player's DNA and open the interface.
- **Futuristic Radial Selector:** Dynamic wrist-mounted holographic selection wheel with smooth rotational physics, alien silhouettes, status stats, and audio cues (`[R]` key).
- **Transformation Timer & Recharge Cycle:** Configurable 60-second active alien duration followed by a 20-second cooldown recharge cycle with timeout warning audio beeps.
- **Safe Persistence & Multiplayer Synchronization:** Full NeoForge Data Attachment persistence across player death, respawn, dimension traversal, and server reconnects.

### 2. Cinematic Transformation Sequence
- **Multi-Stage Sequence:** 
  1. Arm raise and mechanical activation chimes.
  2. Concentrated green energy accumulation.
  3. Controlled cylindrical DNA particle spirals winding upward.
  4. Model swap accompanied by an energetic flash and shockwave burst.
- **Quick-Transform Mode:** Instant transformation option for high-intensity combat.
- **Reversion Sequence:** Red timeout warning beeps followed by a controlled red de-transformation smoke burst returning the player safely to human form.

### 3. Alien Roster (8 Playable Aliens)

1. **Heatblast (Pyronite):**
   - **Abilities:** Fire Punch, Charged Fireball Projectile, Ground Fire Slam.
   - **Ultimate:** Supernova Inferno Burst.
   - **Passives:** Total fire and lava immunity.

2. **XLR8 (Kineceleran):**
   - **Abilities:** Directional High-Speed Dash, Rapid Melee Rush Combo, Speed Vortex Spin.
   - **Ultimate:** Hyperdrive Overclock (extreme speed and agility).
   - **Passives:** Continuous hyper-speed sprint and high jumps.

3. **Four Arms (Tetramand):**
   - **Abilities:** Double Heavy Haymaker, Tremor Ground Slam, Colossal Leap Attack.
   - **Ultimate:** Quad Clapper Shockwave.
   - **Passives:** Massive strength buff, increased knockback resistance and heavy defense.

4. **Diamondhead (Petrosapien):**
   - **Abilities:** Sharp Crystal Shard Barrage, Defensive Crystal Wall creation, Diamond Spike Eruption.
   - **Ultimate:** Crystal Monolith Eruption.
   - **Passives:** Crystalline projectile deflection, heavy armor and fall damage resistance.

5. **Cannonbolt (Arburian Pelarota):**
   - **Abilities:** Rolling Ball Mode toggle, Ricochet Slam Dash, Heavy Shell Armor.
   - **Ultimate:** Wrecking Ball Meteor.
   - **Passives:** 65% physical damage reduction during roll and high rolling velocity.

6. **Upgrade (Galvanic Mechamorph):**
   - **Abilities:** Galvanic Optical Eye Beam, Liquid Intangibility Phase, Nanite Circuit Overload.
   - **Ultimate:** Technological Singularity Burst.
   - **Passives:** Night vision, underwater breathing, and techno-glow aura.

7. **Stinkfly (Lepidopterran):**
   - **Abilities:** Sticky Toxic Slime Spit, Wing Boost Launch, Toxic Slime Carpet.
   - **Ultimate:** Lepidopterran Acid Deluge.
   - **Passives:** Flapping 4-wing model animation and continuous slow falling glide.

8. **Wildmutt (Vulpimancer):**
   - **Abilities:** Vulpimancer Pounce & Bite, 360° Sonar Radar Pulse (scans living entities in 32 blocks), Feral Berserk Frenzy.
   - **Ultimate:** Primal Apex Savage Maul.
   - **Passives:** Full night vision, quadrupedal hunting animation, increased agility.

---

## 🎮 Controls & Keybindings

| Key | Action |
|-----|--------|
| **`R`** | Open Holographic Alien Selector Wheel |
| **`G`** | Slam Dial / Transform into Selected Alien |
| **`H`** | Revert to Human Form |
| **`Z`** | Alien Ability 1 |
| **`X`** | Alien Ability 2 (Hold to charge where applicable) |
| **`C`** | Alien Ability 3 |
| **`V`** | Alien Ultimate Ability (Requires 100% Ultimate Charge) |
| **Scroll / A & D** | Navigate Aliens in Selection Wheel |
| **Space / Enter** | Confirm Selection in Interface |

---

## 🛠️ Architecture & Mod Details

- **Target:** Minecraft Java Edition 1.21.1
- **Modloader:** NeoForge 21.1.256
- **Java:** Java 21 LTS
- **Networking:** NeoForge Versioned `PayloadRegistrar` with custom C2S and S2C packets (`OmnitrixSelectPayload`, `OmnitrixTransformPayload`, `OmnitrixRevertPayload`, `OmnitrixAbilityPayload`, `OmnitrixSyncPayload`, `OmnitrixVfxPayload`).
- **Rendering:** Intercepted `RenderPlayerEvent.Pre` with custom 3D `ModelPart` hierarchies for all 8 aliens, custom first-person arms via `RenderArmEvent`, and custom HUD overlay registered with `RegisterGuiLayersEvent`.
- **Data Storage:** NeoForge Data Attachments (`ModRegistry.OMNITRIX_DATA`) with `Codec` serialization and automatic `copyOnDeath()`.
