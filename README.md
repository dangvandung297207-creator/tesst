# Mega Fishing Tycoon

Paper 26.2 / Java 25 plugin that fully replaces vanilla fishing with a custom boss-fight fishing loop.

## Implemented MVP loop

- Vanilla Fishing Rod is the entry point
- Vanilla fishing events are intercepted and cancelled
- Custom cast into water only
- Bobber visual + bite timer
- Fish selection from island / zone config
- Live fish visual under water
- Tug-of-war with:
  - fish HP
  - tension system
  - direction countering
  - power window / danger window
- Active skill via configurable server-safe input
  - default: `SWAP_HANDS`
  - optional: `DROP`
- Exhausted -> capture -> fish bag
- Sell flow -> coins
- Rod progression
- Pets with sell multipliers, purchases, equip/unequip GUI, slot upgrades, and merchant hooks
- SQLite persistence
- Central tick loop for all active sessions
- Zone-based fishing environments with weaker normal waters and a dedicated Fishing World
- Config-driven world profiles, fishing zones, bite speed modifiers, rarity scaling, and large-fish distributions
- Fish model metadata with weight-reactive display scaling
- Phase 2 hardening: config validation, safer async profile loading, stronger bag GUI locking, debug session logging
- Phase 3 progression: rod shop GUI, bag upgrades, island unlock flow, merchant hooks
- Phase 4 combat polish: fish phase transitions, stronger state-based behavior, line snap effects, catch reveal polish
- Phase 5 merchant ecosystem: island broker GUI, pet keeper GUI, rod reclaim flow, richer progression access from NPCs
- Phase 6 onboarding/progression polish: starter tutorial replay, pet slot upgrades, and Pet Trainer merchant flow
- Phase 7 fishing world rework: world profiles, dedicated Fishing World zones, zone modifiers, and environment-locked fish generation

## Project structure

Main package:

```text
com.megafishing
```

Key packages:

- `fishing`
- `fish`
- `combat`
- `rod`
- `skill`
- `pet`
- `economy`
- `storage`
- `progression`
- `ui`
- `persistence`
- `visual`
- `anti`
- `command`

## Commands

- `/megafishing`
- `/megafishing reload`
- `/megafishing give <player> <rod>`
- `/megafishing stats`
- `/megafishing fish`
- `/megafishing bag`
- `/megafishing claimrod`
- `/megafishing sell`
- `/megafishing shop`
- `/megafishing bagupgrade`
- `/megafishing petslots`
- `/megafishing tutorial`
- `/megafishing islands`
- `/megafishing islands list`
- `/megafishing island unlock <id>`
- `/megafishing pet`
- `/megafishing pet list`
- `/megafishing pet buy <pet>`
- `/megafishing pet equip <pet>`
- `/megafishing pet unequip <pet>`
- `/megafishing pet give <player> <pet>`
- `/megafishing buyrod <rod>`
- `/megafishing debug`

## Config files

- `config.yml`
- `messages.yml`
- `skills.yml`
- `rods.yml`
- `fish.yml`
- `pets.yml`
- `islands.yml`
- `fishing-world.yml`
- `zones.yml`

## Build

```bash
mvn package
```

Output:

```text
target/mega-fishing-tycoon-1.1.1.jar
```

## Note

This Arena sandbox does not currently include `java`, `javac`, or `mvn`, so the code could not be compiled or runtime-tested inside the sandbox itself. The repository has been prepared for Maven-based Paper plugin builds on a normal Java 25 environment.
