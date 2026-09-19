package com.megafishing.storage;

import com.megafishing.persistence.CaughtFishRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FishBag {
    private int capacity;
    private final List<CaughtFishRecord> fish = new ArrayList<>();

    public FishBag(int capacity) {
        this.capacity = capacity;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public boolean isFull() {
        return fish.size() >= capacity;
    }

    public int size() {
        return fish.size();
    }

    public int remaining() {
        return Math.max(0, capacity - fish.size());
    }

    public boolean add(CaughtFishRecord record) {
        if (isFull()) {
            return false;
        }
        return fish.add(record);
    }

    public List<CaughtFishRecord> contents() {
        return Collections.unmodifiableList(fish);
    }

    public List<CaughtFishRecord> snapshot() {
        return new ArrayList<>(fish);
    }

    public void replaceAll(List<CaughtFishRecord> values) {
        fish.clear();
        if (values != null) {
            fish.addAll(values);
        }
    }

    public void clear() {
        fish.clear();
    }
}
