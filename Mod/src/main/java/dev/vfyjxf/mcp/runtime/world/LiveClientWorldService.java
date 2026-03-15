package dev.vfyjxf.mcp.runtime.world;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelSummary;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class LiveClientWorldService implements WorldService {

    private static final long EXECUTION_TIMEOUT_SECONDS = 5L;
    private static final long WORLD_LOAD_TIMEOUT_SECONDS = 30L;
    private static final long POLL_INTERVAL_MILLIS = 100L;

    @Override
    public WorldListResult listWorlds() {
        var summaries = onClientThread(this::loadSummaries);
        return new WorldListResult(summaries.stream().map(this::descriptor).toList());
    }

    @Override
    public WorldCreateResult createWorld(WorldCreateRequest request) {
        if (!request.joinAfterCreate()) {
            throw new WorldServiceException("world_action_unavailable", "Creating a world without joining it is not supported");
        }
        if (request.name() == null || request.name().isBlank()) {
            throw new WorldServiceException("world_create_failed", "World name is required");
        }

        onClientThread(() -> {
            var minecraft = requireMinecraft();
            CreateWorldScreen.openFresh(minecraft, minecraft.screen);
            if (!(minecraft.screen instanceof CreateWorldScreen screen)) {
                throw new WorldServiceException("world_create_failed", "Create world screen did not open");
            }
            configure(screen, request);
            invokeCreate(screen);
            return null;
        });

        waitForWorld(request.name(), "world_create_failed");
        var created = findWorldByName(request.name());
        return new WorldCreateResult(created.getLevelId(), created.getLevelName(), true, true);
    }

    @Override
    public WorldJoinResult joinWorld(WorldJoinRequest request) {
        if ((request.id() == null || request.id().isBlank()) && (request.name() == null || request.name().isBlank())) {
            throw new WorldServiceException("world_not_found", "Either id or name is required");
        }

        var summary = resolveWorld(request);
        var currentName = onClientThread(() -> currentWorldName());
        if (currentName != null && currentName.equals(summary.getLevelName())) {
            throw new WorldServiceException("world_already_open", "The requested world is already open");
        }

        var failure = new AtomicReference<String>();
        onClientThread(() -> {
            requireMinecraft().createWorldOpenFlows().openWorld(summary.getLevelId(), () -> failure.compareAndSet(null, "World open flow reported failure"));
            return null;
        });

        waitForWorld(summary.getLevelName(), "world_join_failed", failure);
        return new WorldJoinResult(summary.getLevelId(), summary.getLevelName(), true);
    }

    private void configure(CreateWorldScreen screen, WorldCreateRequest request) {
        var uiState = screen.getUiState();
        uiState.setName(request.name());
        uiState.setAllowCommands(request.allowCheats());
        if (request.seed() != null && !request.seed().isBlank()) {
            uiState.setSeed(request.seed());
        }
        uiState.setGameMode(mapGameMode(request.gameMode()));
        uiState.setDifficulty(mapDifficulty(request.difficulty()));
        uiState.setBonusChest(request.bonusChest());
        uiState.setGenerateStructures(request.generateStructures());
        uiState.setWorldType(resolveWorldType(uiState, request.worldType()));
    }

    private WorldCreationUiState.SelectedGameMode mapGameMode(String gameMode) {
        var normalized = gameMode == null ? "survival" : gameMode.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "creative" -> WorldCreationUiState.SelectedGameMode.CREATIVE;
            case "hardcore" -> WorldCreationUiState.SelectedGameMode.HARDCORE;
            default -> WorldCreationUiState.SelectedGameMode.SURVIVAL;
        };
    }

    private Difficulty mapDifficulty(String difficulty) {
        var normalized = difficulty == null ? "normal" : difficulty.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "peaceful" -> Difficulty.PEACEFUL;
            case "easy" -> Difficulty.EASY;
            case "hard" -> Difficulty.HARD;
            default -> Difficulty.NORMAL;
        };
    }

    private WorldCreationUiState.WorldTypeEntry resolveWorldType(WorldCreationUiState uiState, String worldType) {
        var presetKey = mapWorldPreset(worldType);
        return availableWorldTypes(uiState).stream()
                .filter(entry -> entry.preset().unwrapKey().filter(presetKey::equals).isPresent())
                .findFirst()
                .orElseThrow(() -> new WorldServiceException("world_create_failed", "World type is unavailable: " + worldType));
    }

    private List<WorldCreationUiState.WorldTypeEntry> availableWorldTypes(WorldCreationUiState uiState) {
        return java.util.stream.Stream.concat(uiState.getNormalPresetList().stream(), uiState.getAltPresetList().stream())
                .distinct()
                .toList();
    }

    private ResourceKey<WorldPreset> mapWorldPreset(String worldType) {
        var normalized = worldType == null ? "default" : worldType.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "flat" -> WorldPresets.FLAT;
            case "large_biomes" -> WorldPresets.LARGE_BIOMES;
            case "amplified" -> WorldPresets.AMPLIFIED;
            default -> WorldPresets.NORMAL;
        };
    }

    private void invokeCreate(CreateWorldScreen screen) {
        try {
            Method onCreate = CreateWorldScreen.class.getDeclaredMethod("onCreate");
            onCreate.setAccessible(true);
            onCreate.invoke(screen);
        } catch (Exception exception) {
            throw new WorldServiceException("world_create_failed", exception.getMessage());
        }
    }

    private LevelSummary resolveWorld(WorldJoinRequest request) {
        var summaries = onClientThread(this::loadSummaries);
        if (request.id() != null && !request.id().isBlank()) {
            return summaries.stream()
                    .filter(summary -> request.id().equals(summary.getLevelId()))
                    .findFirst()
                    .orElseThrow(() -> new WorldServiceException("world_not_found", "World id not found: " + request.id()));
        }

        var matches = summaries.stream()
                .filter(summary -> request.name().equals(summary.getLevelName()))
                .toList();
        if (matches.isEmpty()) {
            throw new WorldServiceException("world_not_found", "World name not found: " + request.name());
        }
        if (matches.size() > 1) {
            throw new WorldServiceException("world_name_ambiguous", "Multiple worlds match name: " + request.name());
        }
        return matches.getFirst();
    }

    private LevelSummary findWorldByName(String name) {
        var summaries = onClientThread(this::loadSummaries);
        return summaries.stream()
                .filter(summary -> name.equals(summary.getLevelName()))
                .findFirst()
                .orElseThrow(() -> new WorldServiceException("world_not_found", "Created world not found: " + name));
    }

    private List<LevelSummary> loadSummaries() {
        try {
            var minecraft = requireMinecraft();
            var candidates = minecraft.getLevelSource().findLevelCandidates();
            var future = minecraft.getLevelSource().loadLevelSummaries(candidates);
            minecraft.managedBlock(future::isDone);
            return future.get();
        } catch (WorldServiceException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new WorldServiceException("world_action_unavailable", exception.getMessage());
        }
    }

    private WorldDescriptor descriptor(LevelSummary summary) {
        return new WorldDescriptor(
                summary.getLevelId(),
                summary.getLevelName(),
                summary.getLastPlayed(),
                summary.isHardcore() ? "hardcore" : summary.getGameMode().getName(),
                summary.isHardcore(),
                summary.hasCommands()
        );
    }

    private String currentWorldName() {
        var minecraft = requireMinecraft();
        if (!minecraft.hasSingleplayerServer() || minecraft.getSingleplayerServer() == null) {
            return null;
        }
        return minecraft.getSingleplayerServer().getWorldData().getLevelName();
    }

    private void waitForWorld(String expectedName, String errorCode) {
        waitForWorld(expectedName, errorCode, new AtomicReference<>());
    }

    private void waitForWorld(String expectedName, String errorCode, AtomicReference<String> failure) {
        var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(WORLD_LOAD_TIMEOUT_SECONDS);
        while (System.nanoTime() < deadline) {
            if (failure.get() != null) {
                throw new WorldServiceException(errorCode, failure.get());
            }
            var loaded = onClientThread(() -> expectedName.equals(currentWorldName()));
            if (loaded) {
                return;
            }
            try {
                Thread.sleep(POLL_INTERVAL_MILLIS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new WorldServiceException(errorCode, "Interrupted while waiting for world load");
            }
        }
        throw new WorldServiceException(errorCode, "Timed out waiting for world load");
    }

    private Minecraft requireMinecraft() {
        var minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            throw new WorldServiceException("world_action_unavailable", "Minecraft client is unavailable");
        }
        return minecraft;
    }

    private <T> T onClientThread(java.util.function.Supplier<T> action) {
        try {
            var minecraft = requireMinecraft();
            if (minecraft.isSameThread()) {
                return action.get();
            }
            var future = new CompletableFuture<T>();
            minecraft.execute(() -> {
                try {
                    future.complete(action.get());
                } catch (Throwable throwable) {
                    future.completeExceptionally(throwable);
                }
            });
            return future.get(EXECUTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (WorldServiceException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new WorldServiceException("world_action_unavailable", Objects.toString(exception.getMessage(), exception.getClass().getSimpleName()));
        }
    }
}
