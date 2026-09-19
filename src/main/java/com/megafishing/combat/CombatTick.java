package com.megafishing.combat;

public record CombatTick(DirectionController.CounterResult counterResult, boolean obstructed, boolean tooFar, double tension, double damage) {
}
