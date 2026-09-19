package com.megafishing.pet;

import com.megafishing.MegaFishingPlugin;
import com.megafishing.persistence.PlayerData;

public class PetManager {
    public enum PurchaseResult {
        SUCCESS,
        ALREADY_OWNED,
        UNKNOWN_PET,
        NOT_ENOUGH_COINS
    }

    public enum EquipResult {
        SUCCESS,
        ALREADY_EQUIPPED,
        NOT_OWNED,
        UNKNOWN_PET,
        MAX_SLOTS
    }

    private final MegaFishingPlugin plugin;
    private final PetRegistry registry = new PetRegistry();

    public PetManager(MegaFishingPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        registry.load(plugin.loadBundledConfiguration("pets.yml"), plugin.getLogger());
    }

    public PetRegistry getRegistry() {
        return registry;
    }

    public boolean grantPet(PlayerData data, String petId) {
        PetDefinition pet = registry.get(petId);
        if (pet == null) {
            return false;
        }
        return data.getOwnedPets().add(pet.getId());
    }

    public PurchaseResult purchase(PlayerData data, String petId) {
        PetDefinition pet = registry.get(petId);
        if (pet == null) {
            return PurchaseResult.UNKNOWN_PET;
        }
        if (data.getOwnedPets().contains(pet.getId())) {
            return PurchaseResult.ALREADY_OWNED;
        }
        if (!data.removeCoins(pet.getPrice())) {
            return PurchaseResult.NOT_ENOUGH_COINS;
        }
        data.getOwnedPets().add(pet.getId());
        return PurchaseResult.SUCCESS;
    }

    public EquipResult equip(PlayerData data, String petId) {
        PetDefinition pet = registry.get(petId);
        if (pet == null) {
            return EquipResult.UNKNOWN_PET;
        }
        if (!data.getOwnedPets().contains(pet.getId())) {
            return EquipResult.NOT_OWNED;
        }
        if (data.getEquippedPets().contains(pet.getId())) {
            return EquipResult.ALREADY_EQUIPPED;
        }
        if (data.getEquippedPets().size() >= data.getMaxPetSlots()) {
            return EquipResult.MAX_SLOTS;
        }
        data.getEquippedPets().add(pet.getId());
        return EquipResult.SUCCESS;
    }

    public boolean unequip(PlayerData data, String petId) {
        PetDefinition pet = registry.get(petId);
        if (pet == null) {
            return false;
        }
        return data.getEquippedPets().remove(pet.getId());
    }

    public boolean normalizeEquippedPets(PlayerData data) {
        boolean changed = data.getEquippedPets().removeIf(petId -> !data.getOwnedPets().contains(petId));
        while (data.getEquippedPets().size() > data.getMaxPetSlots()) {
            data.getEquippedPets().remove(data.getEquippedPets().size() - 1);
            changed = true;
        }
        return changed;
    }

    public boolean isEquipped(PlayerData data, String petId) {
        PetDefinition pet = registry.get(petId);
        return pet != null && data.getEquippedPets().contains(pet.getId());
    }

    public double getSellMultiplier(PlayerData data) {
        double multiplier = 1.0D;
        for (String petId : data.getEquippedPets()) {
            PetDefinition pet = registry.get(petId);
            if (pet != null) {
                multiplier *= pet.getSellMultiplier();
            }
        }
        return multiplier;
    }
}
