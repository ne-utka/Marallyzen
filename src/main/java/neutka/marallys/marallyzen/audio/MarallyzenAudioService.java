package neutka.marallys.marallyzen.audio;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLPaths;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.MarallyzenConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Supplier;

/**
 * Service for playing audio via PlasmoVoice API or fallback to vanilla sounds.
 * 
 * Audio files should be located in: config/marallyzen/audio/
 * Supported formats: ogg, wav, mp3
 */
public class MarallyzenAudioService {

    private static final Path AUDIO_BASE_DIR = resolveAudioBaseDir();
    
    /**
     * Plays NPC audio (positional, with radius).
     * 
     * @param level Server level
     * @param npcEntity NPC entity
     * @param audioFileName Audio file name (relative to audio base directory)
     * @param radius Sound radius
     * @param players List of players to play for (null = all nearby)
     * @return Duration in milliseconds, or -1 if unknown/failed
     */
    public static long playNpcAudio(
            ServerLevel level,
            Entity npcEntity,
            String audioFileName,
            float radius,
            List<ServerPlayer> players
    ) {
        // Increase radius for NPC audio (multiply by 2 for better range and volume)
        float enhancedRadius = radius * 2.0f;
        // If path already starts with "dialogues/", use it as-is, otherwise add "dialogues/"
        Path audioFile;
        if (audioFileName.startsWith("dialogues/")) {
            audioFile = AUDIO_BASE_DIR.resolve(audioFileName);
        } else {
            audioFile = AUDIO_BASE_DIR.resolve("dialogues").resolve(audioFileName);
        }
        
        if (!audioFile.toFile().exists()) {
            Marallyzen.LOGGER.warn("NPC audio file not found: {}", audioFile);
            return playFallbackSound(level, npcEntity.position(), players, () -> SoundEvents.VILLAGER_AMBIENT);
        }
        
        try {
            long duration = playAudioViaPlasmoVoice(
                    level,
                    npcEntity.position(),
                    audioFile,
                    enhancedRadius,
                    true, // positional
                    players
            );
            
            if (duration > 0) {
                return duration;
            }
            
            // Fallback to vanilla sound
            return playFallbackSound(level, npcEntity.position(), players, () -> SoundEvents.VILLAGER_AMBIENT);
        } catch (Exception e) {
            Marallyzen.LOGGER.error("Failed to play NPC audio: {}", audioFileName, e);
            return playFallbackSound(level, npcEntity.position(), players, () -> SoundEvents.VILLAGER_AMBIENT);
        }
    }
    
    /**
     * Plays global audio (non-positional, broadcast to all players).
     * 
     * @param level Server level
     * @param audioFileName Audio file name (relative to audio base directory)
     * @param players List of players to play for (null = all players)
     * @return Duration in milliseconds, or -1 if unknown/failed
     */
    public static long playGlobalAudio(
            ServerLevel level,
            String audioFileName,
            List<ServerPlayer> players
    ) {
        Path audioFile = AUDIO_BASE_DIR.resolve(audioFileName);
        
        if (!audioFile.toFile().exists()) {
            Marallyzen.LOGGER.warn("Global audio file not found: {}", audioFile);
            return playFallbackSound(level, null, players, () -> SoundEvents.AMBIENT_CAVE.value());
        }
        
        try {
            long duration = playAudioViaPlasmoVoice(
                    level,
                    null, // no position for global audio
                    audioFile,
                    0.0f, // no radius for global audio
                    false, // non-positional
                    players
            );
            
            if (duration > 0) {
                return duration;
            }
            
            // Fallback to vanilla sound
            return playFallbackSound(level, null, players, () -> SoundEvents.AMBIENT_CAVE.value());
        } catch (Exception e) {
            Marallyzen.LOGGER.error("Failed to play global audio: {}", audioFileName, e);
            return playFallbackSound(level, null, players, () -> SoundEvents.AMBIENT_CAVE.value());
        }
    }
    
    /**
     * Plays cutscene audio (positional or global).
     * 
     * @param level Server level
     * @param position Position for positional audio (can be null for global)
     * @param audioFileName Audio file name (relative to audio base directory)
     * @param radius Sound radius (for positional audio)
     * @param positional Whether audio should be positional
     * @param players List of players to play for (null = all players)
     * @return Duration in milliseconds, or -1 if unknown/failed
     */
    public static long playCutsceneAudio(
            ServerLevel level,
            Vec3 position,
            String audioFileName,
            float radius,
            boolean positional,
            List<ServerPlayer> players
    ) {
        Path audioFile = AUDIO_BASE_DIR.resolve("cutscenes").resolve(audioFileName);
        
        if (!audioFile.toFile().exists()) {
            Marallyzen.LOGGER.warn("Cutscene audio file not found: {}", audioFile);
            return playFallbackSound(level, position, players, () -> SoundEvents.AMBIENT_CAVE.value());
        }
        
        try {
            long duration = playAudioViaPlasmoVoice(
                    level,
                    position,
                    audioFile,
                    radius,
                    positional,
                    players
            );
            
            if (duration > 0) {
                return duration;
            }
            
            // Fallback to vanilla sound
            return playFallbackSound(level, position, players, () -> SoundEvents.AMBIENT_CAVE.value());
        } catch (Exception e) {
            Marallyzen.LOGGER.error("Failed to play cutscene audio: {}", audioFileName, e);
            return playFallbackSound(level, position, players, () -> SoundEvents.AMBIENT_CAVE.value());
        }
    }

    public static long playDictaphoneAudio(
            ServerLevel level,
            Vec3 position,
            String audioFileName,
            float radius,
            boolean positional,
            List<ServerPlayer> players
    ) {
        String resolvedPath = audioFileName;
        if (resolvedPath != null && !resolvedPath.contains("/")) {
            resolvedPath = "dictophone/" + resolvedPath;
        }
        Path audioFile = AUDIO_BASE_DIR.resolve(resolvedPath);

        if (!audioFile.toFile().exists()) {
            Marallyzen.LOGGER.warn("Dictaphone audio file not found: {}", audioFile);
            return playFallbackSound(level, position, players, () -> SoundEvents.AMBIENT_CAVE.value());
        }

        try {
            long duration = playAudioViaPlasmoVoice(
                    level,
                    position,
                    audioFile,
                    radius,
                    positional,
                    players
            );

            if (duration > 0) {
                return duration;
            }

            return playFallbackSound(level, position, players, () -> SoundEvents.AMBIENT_CAVE.value());
        } catch (Exception e) {
            Marallyzen.LOGGER.error("Failed to play dictaphone audio: {}", audioFileName, e);
            return playFallbackSound(level, position, players, () -> SoundEvents.AMBIENT_CAVE.value());
        }
    }

    public static long playRadioAudio(
            ServerLevel level,
            Vec3 position,
            String audioFileName,
            float radius,
            boolean positional,
            List<ServerPlayer> players
    ) {
        PlaybackHandle handle = playRadioAudioWithHandle(level, position, audioFileName, radius, positional, players);
        return handle.durationMs();
    }

    public static PlaybackHandle playRadioAudioWithHandle(
            ServerLevel level,
            Vec3 position,
            String audioFileName,
            float radius,
            boolean positional,
            List<ServerPlayer> players
    ) {
        String resolvedPath = audioFileName;
        if (resolvedPath != null && !resolvedPath.contains("/")) {
            resolvedPath = "radio/" + resolvedPath;
        }
        Path audioFile = AUDIO_BASE_DIR.resolve(resolvedPath);

        if (!audioFile.toFile().exists()) {
            Marallyzen.LOGGER.warn("Radio audio file not found: {}", audioFile);
            return new PlaybackHandle(-1, null);
        }

        try {
            PlaybackHandle handle = playAudioViaPlasmoVoiceWithHandle(
                    level,
                    position,
                    audioFile,
                    radius,
                    positional,
                    players
            );

            if (handle.durationMs() > 0) {
                return handle;
            }

            return new PlaybackHandle(-1, null);
        } catch (Exception e) {
            Marallyzen.LOGGER.error("Failed to play radio audio: {}", audioFileName, e);
            return new PlaybackHandle(-1, null);
        }
    }

    public static final class PlaybackHandle {
        private final long durationMs;
        private final Runnable stop;

        public PlaybackHandle(long durationMs, Runnable stop) {
            this.durationMs = durationMs;
            this.stop = stop;
        }

        public long durationMs() {
            return durationMs;
        }

        public void stop() {
            if (stop != null) {
                stop.run();
            }
        }
    }
    
    /**
     * Plays audio via PlasmoVoice API 2.1.x using high-level API.
     * 
     * Steps:
     * 1. Get PlasmoVoiceServer instance via PlasmoVoiceServer.get()
     * 2. Get ServerWorld via server.getMinecraftServer().getWorld() (if available)
     * 3. Get Sources factory via server.getSources()
     * 4. Get Audio factory via server.getAudio()
     * 5. Create audio source: sources.createProximity(pos, radius) or sources.createBroadcast()
     * 6. Create audio stream: audio.createStream(path)
     * 7. Play stream: source.play(stream)
     * 
     * Uses only public API - no internal classes or methods.
     */
    private static long playAudioViaPlasmoVoice(
            ServerLevel level,
            Vec3 position,
            Path audioFile,
            float radius,
            boolean positional,
            List<ServerPlayer> players
    ) {
        PlaybackHandle handle = playAudioViaPlasmoVoiceWithHandle(level, position, audioFile, radius, positional, players);
        return handle.durationMs();
    }

    private static PlaybackHandle playAudioViaPlasmoVoiceWithHandle(
            ServerLevel level,
            Vec3 position,
            Path audioFile,
            float radius,
            boolean positional,
            List<ServerPlayer> players
    ) {
        try {
            // 1. Get PlasmoVoiceServer instance
            Object voiceServer = MarallyzenPlasmoVoiceAddon.getVoiceServer();
            Class<?> voiceServerClass = null;
            
            if (voiceServer == null) {
                try {
                    voiceServerClass = Class.forName("su.plo.voice.api.server.PlasmoVoiceServer");
                    voiceServer = voiceServerClass.getMethod("get").invoke(null);
                    if (voiceServer != null) {
                        MarallyzenPlasmoVoiceAddon.setVoiceServer(voiceServer);
                    }
                } catch (Exception e) {
                    Marallyzen.LOGGER.debug("PlasmoVoiceServer.get() failed: {}", e.getMessage());
                }
            } else {
                voiceServerClass = voiceServer.getClass();
            }
            
            if (voiceServer == null || voiceServerClass == null) {
                Marallyzen.LOGGER.warn("PlasmoVoice VoiceServer not available - using fallback");
                return new PlaybackHandle(-1, null);
            }
            
            // 2. Get ServerWorld (McServerWorld) from ServerLevel using McServerLib.getWorld(instance)
            Object serverWorld = null;
            try {
                // Get MinecraftServer (McServerLib) from PlasmoVoiceServer
                java.lang.reflect.Method getMinecraftServerMethod = voiceServerClass.getMethod("getMinecraftServer");
                Object minecraftServer = getMinecraftServerMethod.invoke(voiceServer);
                
                Marallyzen.LOGGER.debug("MinecraftServer (McServerLib) from PlasmoVoiceServer: {}", minecraftServer != null ? minecraftServer.getClass().getName() : "null");
                
                if (minecraftServer != null) {
                    // Method 1: Try getWorld(instance) - this is the standard way in PlasmoVoice
                    // According to MockServer.kt, McServerLib has getWorld(instance: Any): McServerWorld
                    try {
                        Class<?> mcServerLibClass = Class.forName("su.plo.slib.api.server.McServerLib");
                        if (mcServerLibClass.isInstance(minecraftServer)) {
                            java.lang.reflect.Method getWorldMethod = mcServerLibClass.getMethod("getWorld", Object.class);
                            serverWorld = getWorldMethod.invoke(minecraftServer, level);
                            if (serverWorld != null) {
                                Marallyzen.LOGGER.debug("Got ServerWorld via McServerLib.getWorld(ServerLevel)");
                            } else {
                                Marallyzen.LOGGER.debug("McServerLib.getWorld(ServerLevel) returned null");
                            }
                        }
                    } catch (Exception e) {
                        Marallyzen.LOGGER.debug("McServerLib.getWorld(instance) failed: {}", e.getMessage());
                    }
                    
                    // Method 2: Fallback - try getWorld(ResourceKey)
                    if (serverWorld == null) {
                        try {
                            Class<?> mcServerLibClass = Class.forName("su.plo.slib.api.server.McServerLib");
                            if (mcServerLibClass.isInstance(minecraftServer)) {
                                net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimensionKey = level.dimension();
                                java.lang.reflect.Method getWorldMethod = mcServerLibClass.getMethod("getWorld", net.minecraft.resources.ResourceKey.class);
                                serverWorld = getWorldMethod.invoke(minecraftServer, dimensionKey);
                                if (serverWorld != null) {
                                    Marallyzen.LOGGER.debug("Got ServerWorld via McServerLib.getWorld(ResourceKey)");
                                }
                            }
                        } catch (Exception e) {
                            Marallyzen.LOGGER.debug("McServerLib.getWorld(ResourceKey) failed: {}", e.getMessage());
                        }
                    }
                    
                    // Method 3: Fallback - try getWorlds() collection
                    if (serverWorld == null) {
                        try {
                            Class<?> mcServerLibClass = Class.forName("su.plo.slib.api.server.McServerLib");
                            if (mcServerLibClass.isInstance(minecraftServer)) {
                                java.lang.reflect.Method getWorldsMethod = mcServerLibClass.getMethod("getWorlds");
                                Object worlds = getWorldsMethod.invoke(minecraftServer);
                                if (worlds instanceof java.util.Collection) {
                                    java.util.Collection<?> worldsCollection = (java.util.Collection<?>) worlds;
                                    // Use first world as fallback (usually overworld)
                                    if (!worldsCollection.isEmpty()) {
                                        serverWorld = worldsCollection.iterator().next();
                                        Marallyzen.LOGGER.debug("Using first world from getWorlds() collection as fallback");
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Marallyzen.LOGGER.debug("getWorlds() method failed: {}", e.getMessage());
                        }
                    }
                    
                    if (serverWorld == null) {
                        Marallyzen.LOGGER.warn("Could not obtain ServerWorld from McServerLib - positional audio may not work correctly");
                    }
                } else {
                    Marallyzen.LOGGER.warn("MinecraftServer (McServerLib) from PlasmoVoiceServer is null");
                }
            } catch (Exception e) {
                Marallyzen.LOGGER.warn("Failed to get ServerWorld: {}", e.getMessage(), e);
            }
            
            // 3. Get SourceLineManager (PlasmoVoice API: server.getSourceLineManager())
            Object sourceLineManager = null;
            Class<?> sourceLineManagerClass = null;
            
            try {
                java.lang.reflect.Method getSourceLineManagerMethod = voiceServerClass.getMethod("getSourceLineManager");
                sourceLineManager = getSourceLineManagerMethod.invoke(voiceServer);
                if (sourceLineManager != null) {
                    sourceLineManagerClass = sourceLineManager.getClass();
                    Marallyzen.LOGGER.debug("Got SourceLineManager via getSourceLineManager()");
                }
            } catch (NoSuchMethodException e) {
                Marallyzen.LOGGER.error("PlasmoVoice getSourceLineManager() method not found - API version mismatch?");
                return new PlaybackHandle(-1, null);
            } catch (Exception e) {
                Marallyzen.LOGGER.error("Failed to get SourceLineManager: {}", e.getMessage(), e);
                return new PlaybackHandle(-1, null);
            }
            
            if (sourceLineManager == null || sourceLineManagerClass == null) {
                Marallyzen.LOGGER.error("PlasmoVoice SourceLineManager not available");
                return new PlaybackHandle(-1, null);
            }
            
            // 4. Get or create source line (use "proximity" line if available, otherwise create new one)
            Object sourceLine = null;
            Class<?> sourceLineClass = null;
            
            try {
                // Try to get existing "proximity" line
                java.lang.reflect.Method getLineByNameMethod = sourceLineManagerClass.getMethod("getLineByName", String.class);
                Object optionalLine = getLineByNameMethod.invoke(sourceLineManager, "proximity");
                
                // Optional handling - try to get value
                if (optionalLine != null) {
                    try {
                        java.lang.reflect.Method isPresentMethod = optionalLine.getClass().getMethod("isPresent");
                        boolean isPresent = (Boolean) isPresentMethod.invoke(optionalLine);
                        if (isPresent) {
                            java.lang.reflect.Method getMethod = optionalLine.getClass().getMethod("get");
                            sourceLine = getMethod.invoke(optionalLine);
                            if (sourceLine != null) {
                                sourceLineClass = sourceLine.getClass();
                                Marallyzen.LOGGER.debug("Got existing 'proximity' source line");
                            }
                        } else {
                            Marallyzen.LOGGER.debug("'proximity' source line not found (isPresent=false)");
                        }
                    } catch (Exception e) {
                        // Optional might be null or different structure
                        Marallyzen.LOGGER.debug("Could not extract value from Optional: {}", e.getMessage());
                    }
                } else {
                    Marallyzen.LOGGER.debug("getLineByName('proximity') returned null");
                }
            } catch (Exception e) {
                Marallyzen.LOGGER.debug("Could not get existing source line: {}", e.getMessage());
            }
            
            // If no existing line, create a new one
            if (sourceLine == null) {
                try {
                    // Get addon object - use voiceServer itself as addon object
                    // In PlasmoVoice, the addon object is typically the addon instance
                    // We can use voiceServer or create a simple object
                    Object addonObject = voiceServer; // Use voiceServer as addon object
                    
                    Marallyzen.LOGGER.debug("Creating new source line 'marallyzen_audio' with addon object: {}", addonObject != null ? addonObject.getClass().getName() : "null");
                    
                    // Create builder
                    java.lang.reflect.Method createBuilderMethod = sourceLineManagerClass.getMethod(
                        "createBuilder",
                        Object.class, String.class, String.class, String.class, int.class
                    );
                    Object builder = createBuilderMethod.invoke(
                        sourceLineManager,
                        addonObject,
                        "marallyzen_audio",
                        "marallyzen.audio",
                        "minecraft:textures/item/music_disc_11.png",
                        100
                    );
                    
                    if (builder == null) {
                        Marallyzen.LOGGER.error("createBuilder returned null");
                        return new PlaybackHandle(-1, null);
                    }
                    
                    // Build the line
                    java.lang.reflect.Method buildMethod = builder.getClass().getMethod("build");
                    sourceLine = buildMethod.invoke(builder);
                    if (sourceLine != null) {
                        sourceLineClass = sourceLine.getClass();
                        Marallyzen.LOGGER.debug("Created new source line 'marallyzen_audio'");
                    } else {
                        Marallyzen.LOGGER.error("build() returned null");
                        return new PlaybackHandle(-1, null);
                    }
                } catch (Exception e) {
                    Marallyzen.LOGGER.error("Failed to create source line: {}", e.getMessage(), e);
                    return new PlaybackHandle(-1, null);
                }
            }
            
            if (sourceLine == null || sourceLineClass == null) {
                Marallyzen.LOGGER.error("Failed to get or create source line");
                return new PlaybackHandle(-1, null);
            }
            
            // 5. Decode audio file to PCM samples
            short[] pcmSamples = null;
            long durationMs = -1;
            
            try {
                pcmSamples = AudioDecoder.decodeToPCM(audioFile);
                if (pcmSamples != null && pcmSamples.length > 0) {
                    applyPlasmoVoiceVolume(pcmSamples);
                    durationMs = AudioDecoder.getDurationMs(pcmSamples);
                    Marallyzen.LOGGER.debug("Decoded audio file to PCM: {} samples, duration: {}ms", pcmSamples.length, durationMs);
                }
            } catch (Exception e) {
                Marallyzen.LOGGER.error("Failed to decode audio file: {}", e.getMessage(), e);
                // Try AudioMetadata as fallback
                try {
                    durationMs = AudioMetadata.getDurationMs(audioFile.toString());
                    if (durationMs > 0) {
                        Marallyzen.LOGGER.debug("Got duration from AudioMetadata: {}ms", durationMs);
                    }
                } catch (Exception e2) {
                    Marallyzen.LOGGER.debug("Could not get duration from AudioMetadata: {}", e2.getMessage());
                }
                return new PlaybackHandle(-1, null);
            }
            
            // 6. Create audio source (proximity or static)
            Object audioSource = null;
            short distance = (short) Math.min(radius, Short.MAX_VALUE);
            
            if (positional && position != null && serverWorld != null) {
                // Create static (positional) source
                try {
                    Marallyzen.LOGGER.debug("Attempting to create static source at ({}, {}, {}) with radius {}", position.x, position.y, position.z, radius);
                    
                    // Get ServerPos3d class and create position
                    Class<?> serverPos3dClass = Class.forName("su.plo.slib.api.server.position.ServerPos3d");
                    Class<?> mcServerWorldClass = Class.forName("su.plo.slib.api.server.world.McServerWorld");
                    
                    Marallyzen.LOGGER.debug("ServerPos3d class: {}, McServerWorld class: {}", serverPos3dClass.getName(), mcServerWorldClass.getName());
                    Marallyzen.LOGGER.debug("serverWorld instance: {} (is instance of McServerWorld: {})", 
                        serverWorld.getClass().getName(), mcServerWorldClass.isInstance(serverWorld));
                    
                    // ServerPos3d(world, x, y, z) constructor (not static method of())
                    java.lang.reflect.Constructor<?> serverPosConstructor = serverPos3dClass.getConstructor(
                        mcServerWorldClass, double.class, double.class, double.class
                    );
                    Object serverPos = serverPosConstructor.newInstance(serverWorld, position.x, position.y, position.z);
                    
                    if (serverPos == null) {
                        Marallyzen.LOGGER.error("ServerPos3d constructor returned null");
                    } else {
                        Marallyzen.LOGGER.debug("Created ServerPos3d: {}", serverPos.getClass().getName());
                    }
                    
                    // Create static source: sourceLine.createStaticSource(ServerPos3d, stereo, decoderInfo)
                    // Try different method signatures
                    java.lang.reflect.Method createStaticSourceMethod = null;
                    try {
                        createStaticSourceMethod = sourceLineClass.getMethod(
                            "createStaticSource",
                            serverPos3dClass, boolean.class
                        );
                    } catch (NoSuchMethodException e) {
                        Marallyzen.LOGGER.debug("createStaticSource(ServerPos3d, boolean) not found, trying other signatures");
                        // Try with decoderInfo parameter
                        try {
                            Class<?> codecInfoClass = Class.forName("su.plo.voice.proto.data.audio.codec.CodecInfo");
                            createStaticSourceMethod = sourceLineClass.getMethod(
                                "createStaticSource",
                                serverPos3dClass, boolean.class, codecInfoClass
                            );
                            // Create default OpusDecoderInfo
                            Class<?> opusDecoderInfoClass = Class.forName("su.plo.voice.proto.data.audio.codec.opus.OpusDecoderInfo");
                            java.lang.reflect.Constructor<?> opusConstructor = opusDecoderInfoClass.getConstructor();
                            Object decoderInfo = opusConstructor.newInstance();
                            audioSource = createStaticSourceMethod.invoke(sourceLine, serverPos, false, decoderInfo);
                        } catch (Exception e2) {
                            Marallyzen.LOGGER.error("Failed to find/create static source method: {}", e2.getMessage(), e2);
                            throw e;
                        }
                    }
                    
                    if (createStaticSourceMethod != null && audioSource == null) {
                        audioSource = createStaticSourceMethod.invoke(sourceLine, serverPos, false);
                    }
                    
                    if (audioSource != null) {
                        Marallyzen.LOGGER.debug("Created static source at ({}, {}, {})", position.x, position.y, position.z);
                    } else {
                        Marallyzen.LOGGER.error("createStaticSource returned null");
                    }
                } catch (Exception e) {
                    Marallyzen.LOGGER.error("Failed to create static source: {}", e.getMessage(), e);
                    // Try entity source as fallback if we have an entity
                }
            }
            
            // If no positional source, we need to use a different approach
            // For broadcast/global audio, we can use a static source at player position or use entity source
            if (audioSource == null && players != null && !players.isEmpty()) {
                // Use first player's position for static source
                try {
                    ServerPlayer firstPlayer = players.get(0);
                    Vec3 playerPos = firstPlayer.position();
                    
                    Marallyzen.LOGGER.debug("Attempting to create static source at player position ({}, {}, {})", playerPos.x, playerPos.y, playerPos.z);
                    
                    if (serverWorld != null) {
                        Class<?> serverPos3dClass = Class.forName("su.plo.slib.api.server.position.ServerPos3d");
                        Class<?> mcServerWorldClass = Class.forName("su.plo.slib.api.server.world.McServerWorld");
                        java.lang.reflect.Constructor<?> serverPosConstructor = serverPos3dClass.getConstructor(
                            mcServerWorldClass, double.class, double.class, double.class
                        );
                        Object serverPos = serverPosConstructor.newInstance(serverWorld, playerPos.x, playerPos.y, playerPos.z);
                        
                        java.lang.reflect.Method createStaticSourceMethod = sourceLineClass.getMethod(
                            "createStaticSource",
                            serverPos3dClass, boolean.class
                        );
                        audioSource = createStaticSourceMethod.invoke(sourceLine, serverPos, false);
                        if (audioSource != null) {
                            Marallyzen.LOGGER.debug("Created static source at player position ({}, {}, {})", playerPos.x, playerPos.y, playerPos.z);
                        } else {
                            Marallyzen.LOGGER.error("createStaticSource at player position returned null");
                        }
                    } else {
                        Marallyzen.LOGGER.warn("serverWorld is null, cannot create static source at player position");
                    }
                } catch (Exception e) {
                    Marallyzen.LOGGER.error("Failed to create static source at player position: {}", e.getMessage(), e);
                }
            }
            
            if (audioSource == null) {
                Marallyzen.LOGGER.error("Failed to create audio source - positional={}, position={}, serverWorld={}, players={}", 
                    positional, position != null ? position.toString() : "null", 
                    serverWorld != null ? serverWorld.getClass().getName() : "null",
                    players != null ? players.size() : "null");
                return new PlaybackHandle(-1, null);
            }

            // Restrict playback to specific players when provided.
            if (players != null && !players.isEmpty()) {
                try {
                    java.lang.reflect.Method getPlayerManagerMethod = voiceServerClass.getMethod("getPlayerManager");
                    Object playerManager = getPlayerManagerMethod.invoke(voiceServer);
                    java.lang.reflect.Method getPlayerByInstanceMethod = playerManager.getClass().getMethod("getPlayerByInstance", Object.class);
                    java.util.Set<Object> allowedVoicePlayers = new java.util.HashSet<>();
                    for (ServerPlayer target : players) {
                        Object voicePlayer = getPlayerByInstanceMethod.invoke(playerManager, target);
                        if (voicePlayer != null) {
                            allowedVoicePlayers.add(voicePlayer);
                        }
                    }
                    if (!allowedVoicePlayers.isEmpty()) {
                        java.util.function.Predicate<Object> filter = allowedVoicePlayers::contains;
                        java.lang.reflect.Method addFilterMethod = audioSource.getClass().getMethod("addFilter", java.util.function.Predicate.class);
                        addFilterMethod.invoke(audioSource, filter);
                        Marallyzen.LOGGER.debug("Audio source restricted to {} player(s)", allowedVoicePlayers.size());
                    }
                } catch (Exception e) {
                    Marallyzen.LOGGER.warn("Failed to apply player filter to audio source: {}", e.getMessage());
                }
            }
            
            // 7. Create ArrayAudioFrameProvider and add PCM samples
            Object frameProvider = null;
            try {
                    Class<?> arrayFrameProviderClass = Class.forName("su.plo.voice.api.server.audio.provider.ArrayAudioFrameProvider");
                    // ArrayAudioFrameProvider constructor takes PlasmoBaseVoiceServer, not the concrete implementation
                    Class<?> plasmoBaseVoiceServerClass = Class.forName("su.plo.voice.api.server.PlasmoBaseVoiceServer");
                    java.lang.reflect.Constructor<?> constructor = arrayFrameProviderClass.getConstructor(
                        plasmoBaseVoiceServerClass, boolean.class
                    );
                    frameProvider = constructor.newInstance(voiceServer, false); // mono
                
                // Add samples to frame provider
                java.lang.reflect.Method addSamplesMethod = arrayFrameProviderClass.getMethod("addSamples", short[].class);
                addSamplesMethod.invoke(frameProvider, (Object) pcmSamples);
                
                Marallyzen.LOGGER.debug("Created ArrayAudioFrameProvider and added {} samples", pcmSamples.length);
            } catch (Exception e) {
                Marallyzen.LOGGER.error("Failed to create ArrayAudioFrameProvider: {}", e.getMessage(), e);
                return new PlaybackHandle(-1, null);
            }
            
            // 8. Create AudioSender and start playback
            try {
                Class<?> audioSourceClass = audioSource.getClass();
                Class<?> audioFrameProviderClass = Class.forName("su.plo.voice.api.server.audio.provider.AudioFrameProvider");
                java.lang.reflect.Method createAudioSenderMethod = audioSourceClass.getMethod(
                    "createAudioSender",
                    audioFrameProviderClass,
                    short.class
                );
                Object audioSender = createAudioSenderMethod.invoke(audioSource, frameProvider, distance);
                final Object playbackSource = audioSource;
                Runnable stopRunnable = () -> {
                    try {
                        java.lang.reflect.Method stopMethod = audioSender.getClass().getMethod("stop");
                        stopMethod.invoke(audioSender);
                    } catch (Exception ignore) {
                        // ignore
                    }
                    try {
                        java.lang.reflect.Method removeMethod = playbackSource.getClass().getMethod("remove");
                        removeMethod.invoke(playbackSource);
                    } catch (Exception ignore) {
                        // ignore
                    }
                };
                
                // Set onStop callback to remove source after playback completes
                // This ensures sources don't conflict when multiple audio files play sequentially
                try {
                    // AudioSender.onStop(Runnable) method
                    java.lang.reflect.Method onStopMethod = audioSender.getClass().getMethod("onStop", Runnable.class);
                    final Object onStopSource = audioSource;
                    final String fileName = audioFile.getFileName().toString();
                    onStopMethod.invoke(audioSender, (Runnable) () -> {
                        try {
                            // Remove the source after playback completes to prevent conflicts with next audio
                            java.lang.reflect.Method removeMethod = onStopSource.getClass().getMethod("remove");
                            removeMethod.invoke(onStopSource);
                            Marallyzen.LOGGER.debug("Removed audio source after playback completed: {}", fileName);
                        } catch (Exception e) {
                            Marallyzen.LOGGER.debug("Failed to remove audio source after playback: {}", e.getMessage());
                        }
                    });
                } catch (NoSuchMethodException e) {
                    // onStop might not be available, schedule removal after duration
                    if (durationMs > 0) {
                        final Object delayedSource = audioSource;
                        final String fileName = audioFile.getFileName().toString();
                        java.util.Timer timer = new java.util.Timer();
                        timer.schedule(new java.util.TimerTask() {
                            @Override
                            public void run() {
                                try {
                                    java.lang.reflect.Method removeMethod = delayedSource.getClass().getMethod("remove");
                                    removeMethod.invoke(delayedSource);
                                    Marallyzen.LOGGER.debug("Removed audio source after duration: {}", fileName);
                                } catch (Exception e) {
                                    Marallyzen.LOGGER.debug("Failed to remove audio source after duration: {}", e.getMessage());
                                }
                            }
                        }, durationMs + 100); // Add small delay to ensure playback completes
                    }
                }
                
                // Start the sender (it should start automatically, but check for start method)
                try {
                    java.lang.reflect.Method startMethod = audioSender.getClass().getMethod("start");
                    startMethod.invoke(audioSender);
                } catch (NoSuchMethodException e) {
                    // AudioSender might start automatically
                    Marallyzen.LOGGER.debug("AudioSender does not have start() method, assuming auto-start");
                }
                
                Marallyzen.LOGGER.info("Audio playback started via PlasmoVoice: {} (duration: {}ms)", 
                    audioFile.getFileName(), durationMs > 0 ? durationMs : "unknown");
                
                return new PlaybackHandle(durationMs > 0 ? durationMs : -1, stopRunnable);
            } catch (Exception e) {
                Marallyzen.LOGGER.error("Failed to create AudioSender: {}", e.getMessage(), e);
                return new PlaybackHandle(-1, null);
            }
        } catch (Exception e) {
            Marallyzen.LOGGER.error("Failed to play audio via PlasmoVoice - using fallback", e);
            return new PlaybackHandle(-1, null);
        }
    }
    
    /**
     * Fallback method to play vanilla Minecraft sound.
     */
    private static long playFallbackSound(
            ServerLevel level,
            Vec3 position,
            List<ServerPlayer> players,
            Supplier<SoundEvent> soundEvent
    ) {
        SoundEvent sound = soundEvent.get();
        List<ServerPlayer> targetPlayers = players;
        
        if (targetPlayers == null) {
            if (position != null) {
                // Find nearby players
                double range = 32.0;
                targetPlayers = level.getPlayers(p -> p.position().distanceTo(position) <= range);
            } else {
                targetPlayers = level.players();
            }
        }
        
        for (ServerPlayer player : targetPlayers) {
            if (position != null) {
                player.level().playSound(
                        null,
                        BlockPos.containing(position),
                        sound,
                        SoundSource.MASTER,
                        1.0f,
                        1.0f
                );
            } else {
                player.playNotifySound(sound, SoundSource.MASTER, 1.0f, 1.0f);
            }
        }
        
        // Return approximate duration for common sounds (1-2 seconds)
        return 1500;
    }

    private static void applyPlasmoVoiceVolume(short[] samples) {
        double volume = MarallyzenConfig.PLASMO_AUDIO_VOLUME.get();
        if (volume == 1.0 || samples == null || samples.length == 0) {
            return;
        }
        if (volume <= 0.0) {
            for (int i = 0; i < samples.length; i++) {
                samples[i] = 0;
            }
            return;
        }
        double clamped = Math.min(2.0, Math.max(0.0, volume));
        for (int i = 0; i < samples.length; i++) {
            int scaled = (int) Math.round(samples[i] * clamped);
            if (scaled > Short.MAX_VALUE) {
                scaled = Short.MAX_VALUE;
            } else if (scaled < Short.MIN_VALUE) {
                scaled = Short.MIN_VALUE;
            }
            samples[i] = (short) scaled;
        }
    }
    
    /**
     * Gets the base directory for audio files.
     */
    public static Path getAudioBaseDir() {
        return AUDIO_BASE_DIR;
    }

    private static Path resolveAudioBaseDir() {
        Path runConfig = FMLPaths.GAMEDIR.get().resolve("run").resolve("config");
        if (Files.exists(runConfig)) {
            return runConfig.resolve("marallyzen").resolve("audio");
        }
        return FMLPaths.CONFIGDIR.get().resolve("marallyzen").resolve("audio");
    }
}
