package com.megafishing.fishing;

import com.megafishing.MegaFishingPlugin;
import com.megafishing.util.Text;
import org.bukkit.configuration.ConfigurationSection;

import java.time.LocalDate;
import java.time.MonthDay;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FishingCalendarManager {
    private final MegaFishingPlugin plugin;
    private final Map<String, CalendarSeason> seasons = new LinkedHashMap<>();
    private final Map<String, SpecialEvent> events = new LinkedHashMap<>();
    private ZoneId zoneId = ZoneId.systemDefault();

    public FishingCalendarManager(MegaFishingPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        seasons.clear();
        events.clear();
        String timezone = plugin.getConfig().getString("calendar.timezone", "system");
        zoneId = resolveZoneId(timezone);
        loadSeasons();
        loadEvents();
    }

    public Snapshot snapshotNow() {
        return snapshot(LocalDate.now(zoneId));
    }

    public Snapshot snapshot(LocalDate date) {
        String seasonId = null;
        String seasonDisplayName = null;
        for (CalendarSeason season : seasons.values()) {
            if (season.matches(date)) {
                seasonId = season.id();
                seasonDisplayName = season.displayName();
                break;
            }
        }
        Set<String> activeEventIds = new LinkedHashSet<>();
        List<String> activeEventDisplayNames = new ArrayList<>();
        for (SpecialEvent event : events.values()) {
            if (event.matches(date)) {
                activeEventIds.add(event.id());
                activeEventDisplayNames.add(event.displayName());
            }
        }
        return new Snapshot(date, seasonId, seasonDisplayName, activeEventIds, activeEventDisplayNames);
    }

    public boolean isKnownSeason(String id) {
        return id != null && seasons.containsKey(id.toLowerCase());
    }

    public boolean isKnownEvent(String id) {
        return id != null && events.containsKey(id.toLowerCase());
    }

    public Collection<String> seasonIds() {
        return Collections.unmodifiableCollection(seasons.keySet());
    }

    public Collection<String> eventIds() {
        return Collections.unmodifiableCollection(events.keySet());
    }

    public ZoneId getZoneId() {
        return zoneId;
    }

    private void loadSeasons() {
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("calendar.seasons");
        if (root == null) {
            return;
        }
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            Set<Integer> months = new LinkedHashSet<>();
            for (int month : section.getIntegerList("months")) {
                if (month >= 1 && month <= 12) {
                    months.add(month);
                }
            }
            if (months.isEmpty()) {
                plugin.getLogger().warning("[MEGA-FISHING] Calendar season '" + id + "' has no valid months.");
                continue;
            }
            seasons.put(id.toLowerCase(), new CalendarSeason(
                    id.toLowerCase(),
                    section.getString("display-name", Text.plainEnum(id)),
                    months
            ));
        }
    }

    private void loadEvents() {
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("calendar.special-events");
        if (root == null) {
            return;
        }
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null || !section.getBoolean("enabled", true)) {
                continue;
            }
            MonthDay start = parseMonthDay(section.getString("start"));
            MonthDay end = parseMonthDay(section.getString("end"));
            if (start == null || end == null) {
                plugin.getLogger().warning("[MEGA-FISHING] Calendar event '" + id + "' has invalid start/end dates.");
                continue;
            }
            events.put(id.toLowerCase(), new SpecialEvent(
                    id.toLowerCase(),
                    section.getString("display-name", Text.plainEnum(id)),
                    start,
                    end
            ));
        }
    }

    private ZoneId resolveZoneId(String raw) {
        if (raw == null || raw.isBlank() || raw.equalsIgnoreCase("system")) {
            return ZoneId.systemDefault();
        }
        try {
            return ZoneId.of(raw);
        } catch (Exception exception) {
            plugin.getLogger().warning("[MEGA-FISHING] Invalid calendar timezone '" + raw + "'. Using system default.");
            return ZoneId.systemDefault();
        }
    }

    private MonthDay parseMonthDay(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            String[] split = raw.split("-");
            if (split.length != 2) {
                return null;
            }
            int month = Integer.parseInt(split[0]);
            int day = Integer.parseInt(split[1]);
            return MonthDay.of(month, day);
        } catch (Exception exception) {
            return null;
        }
    }

    public record Snapshot(LocalDate date, String seasonId, String seasonDisplayName,
                           Set<String> activeEventIds, List<String> activeEventDisplayNames) {
        public Snapshot {
            activeEventIds = activeEventIds == null ? Collections.emptySet() : Collections.unmodifiableSet(new LinkedHashSet<>(activeEventIds));
            activeEventDisplayNames = activeEventDisplayNames == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(activeEventDisplayNames));
        }

        public boolean hasSeason() {
            return seasonId != null && !seasonId.isBlank();
        }

        public boolean hasEvents() {
            return !activeEventIds.isEmpty();
        }
    }

    private record CalendarSeason(String id, String displayName, Set<Integer> months) {
        private CalendarSeason {
            months = Collections.unmodifiableSet(new LinkedHashSet<>(months));
        }

        private boolean matches(LocalDate date) {
            return date != null && months.contains(date.getMonthValue());
        }
    }

    private record SpecialEvent(String id, String displayName, MonthDay start, MonthDay end) {
        private boolean matches(LocalDate date) {
            if (date == null) {
                return false;
            }
            MonthDay current = MonthDay.from(date);
            if (start.compareTo(end) <= 0) {
                return current.compareTo(start) >= 0 && current.compareTo(end) <= 0;
            }
            return current.compareTo(start) >= 0 || current.compareTo(end) <= 0;
        }
    }
}
