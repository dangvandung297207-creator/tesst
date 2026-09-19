package com.megafishing.fish;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class FishAvailability {
    private final Set<String> seasons;
    private final Set<String> events;
    private final boolean requireActiveEvent;

    public FishAvailability(Set<String> seasons, Set<String> events, boolean requireActiveEvent) {
        this.seasons = seasons == null ? Collections.emptySet() : Collections.unmodifiableSet(lowered(seasons));
        this.events = events == null ? Collections.emptySet() : Collections.unmodifiableSet(lowered(events));
        this.requireActiveEvent = requireActiveEvent;
    }

    public boolean matches(String activeSeasonId, Set<String> activeEventIds) {
        boolean seasonOk = seasons.isEmpty() || (activeSeasonId != null && seasons.contains(activeSeasonId.toLowerCase()));
        Set<String> activeEvents = lowered(activeEventIds);
        boolean eventOk;
        if (events.isEmpty()) {
            eventOk = !requireActiveEvent || !activeEvents.isEmpty();
        } else {
            eventOk = activeEvents.stream().anyMatch(events::contains);
        }
        return seasonOk && eventOk;
    }

    public boolean isAlwaysAvailable() {
        return seasons.isEmpty() && events.isEmpty() && !requireActiveEvent;
    }

    public boolean isSeasonal() {
        return !seasons.isEmpty();
    }

    public boolean isEventLinked() {
        return requireActiveEvent || !events.isEmpty();
    }

    public Set<String> getSeasons() {
        return seasons;
    }

    public Set<String> getEvents() {
        return events;
    }

    public boolean isRequireActiveEvent() {
        return requireActiveEvent;
    }

    private Set<String> lowered(Set<String> values) {
        Set<String> lowered = new LinkedHashSet<>();
        if (values == null) {
            return lowered;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                lowered.add(value.toLowerCase());
            }
        }
        return lowered;
    }
}
