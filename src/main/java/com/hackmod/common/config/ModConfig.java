package com.hackmod.common.config;

import java.util.concurrent.atomic.AtomicBoolean;

public class ModConfig {

    // ── ESP ──────────────────────────────────────────────────────────────────
    public static final AtomicBoolean chestEsp     = new AtomicBoolean(false);
    public static final AtomicBoolean playerEsp    = new AtomicBoolean(false);
    public static volatile int        chestEspRadius = 12;
    public static volatile int        chestEspColor  = 0x00FF41;
    public static volatile int        playerEspColor = 0xFF3C00;

    // ── Visual ───────────────────────────────────────────────────────────────
    public static final AtomicBoolean fullbright       = new AtomicBoolean(false);
    public static final AtomicBoolean zoomEnabled      = new AtomicBoolean(false);
    public static volatile boolean    zoomActive       = false;
    public static volatile int        customFov        = 70;
    public static volatile boolean    customFovEnabled = false;

    // ── HUD ──────────────────────────────────────────────────────────────────
    public static final AtomicBoolean coordsHud  = new AtomicBoolean(false);
    public static final AtomicBoolean armorHud   = new AtomicBoolean(false);
    public static final AtomicBoolean biomeHud   = new AtomicBoolean(false);
    public static final AtomicBoolean clockHud   = new AtomicBoolean(false);

    // ── QoL ──────────────────────────────────────────────────────────────────
    public static final AtomicBoolean autoSprint = new AtomicBoolean(false);
    public static final AtomicBoolean noFall     = new AtomicBoolean(false);
    public static final AtomicBoolean armorAlert = new AtomicBoolean(false);

    // ── Seed & Structures ────────────────────────────────────────────────────
    public static final AtomicBoolean seedTracker    = new AtomicBoolean(false);
    public static volatile int structureScanRadius   = 100;
}
