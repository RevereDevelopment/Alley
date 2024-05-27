package me.emmy.alley.match.snapshot;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Remi
 * @project Alley
 * @date 5/26/2024
 */
@Getter
@Setter
public class SnapshotRepository {
    private final Map<UUID, Snapshot> snapshots = new HashMap<>();

}