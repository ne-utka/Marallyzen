package neutka.marallys.marallyzen.client.emote;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.client.cutscene.editor.CutsceneRecorder;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Handler for playing Emotecraft emotes on NPCs using IPlayerEntity.emotecraft$playEmote().
 * Works only with entities that implement IPlayerEntity (Player, RemotePlayer via Emotecraft mixin).
 */
@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
public final class ClientEmoteHandler {
    private static final Set<String> LEGACY_EMOTE_LOGGED = new HashSet<>();

    /**
     * Loads an emote from Emotecraft's client-side registry by ID or name.
     * Uses UniversalEmoteSerializer.readData() to load with correct Animation type.
     * 
     * @param emoteId The ID or name of the emote (e.g., "Waving" or UUID string)
     * @return The loaded Animation object, or null if not found
     */
    public static Object loadEmoteFromRegistry(String emoteId) {
        // Marallyzen.LOGGER.info("ClientEmoteHandler: Attempting to load emote '{}'", emoteId);
        try {
            // First, try to find emote in EmoteHolder.list to get its resource name
            Class<?> emoteHolderClass = Class.forName("io.github.kosmx.emotes.main.EmoteHolder");
            java.lang.reflect.Field listField = emoteHolderClass.getField("list");
            Object emoteList = listField.get(null);
            java.lang.reflect.Method valuesMethod = emoteList.getClass().getMethod("values");
            java.util.Collection<?> emoteHolders = (java.util.Collection<?>) valuesMethod.invoke(emoteList);
            
            // Map of emote names to resource file names (built-in emotes)
            java.util.Map<String, String> nameToResource = new java.util.HashMap<>();
            nameToResource.put("waving", "waving.json");
            nameToResource.put("clap", "clap.json");
            nameToResource.put("crying", "crying.json");
            nameToResource.put("point", "point.json");
            nameToResource.put("over here", "here.json");
            nameToResource.put("here", "here.json");
            nameToResource.put("face palm", "palm.json");
            nameToResource.put("palm", "palm.json");
            nameToResource.put("back flip", "backflip.json");
            nameToResource.put("backflip", "backflip.json");
            nameToResource.put("roblox potion dance", "roblox_potion_dance.json");
            nameToResource.put("kazotsky kick", "kazotsky_kick.json");
            
            // If emoteId is a UUID, prefer EmoteHolder.list directly (new Animation type).
            String resourceName = null;
            UUID emoteUuid = null;
            
            try {
                emoteUuid = UUID.fromString(emoteId);
            } catch (IllegalArgumentException e) {
                // Not a UUID, search by name
            }

            if (emoteUuid != null) {
                Object emoteHolder = null;
                if (emoteList instanceof java.util.Map<?, ?> map) {
                    emoteHolder = map.get(emoteUuid);
                }
                if (emoteHolder == null) {
                    try {
                        java.lang.reflect.Method getMethod = emoteList.getClass().getMethod("get", Object.class);
                        emoteHolder = getMethod.invoke(emoteList, emoteUuid);
                    } catch (Exception ignored) {
                    }
                }
                if (emoteHolder != null) {
                    try {
                        java.lang.reflect.Field emoteField = emoteHolderClass.getField("emote");
                        Object animation = emoteField.get(emoteHolder);
                        if (animation != null) {
                            logLegacyEmote("emote_holder_uuid", emoteId, null, animation);
                            return animation;
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
            
            for (Object emoteHolder : emoteHolders) {
                try {
                    java.lang.reflect.Field nameField = emoteHolderClass.getField("name");
                    Object nameComponent = nameField.get(emoteHolder);
                    java.lang.reflect.Method getStringMethod = nameComponent.getClass().getMethod("getString");
                    String name = (String) getStringMethod.invoke(nameComponent);
                    
                    // Get UUID from emoteHolder
                    java.lang.reflect.Method getUuidMethod = emoteHolderClass.getMethod("getUuid");
                    UUID holderUuid = (UUID) getUuidMethod.invoke(emoteHolder);
                    
                    // Check if UUID matches
                    if (emoteUuid != null && holderUuid.equals(emoteUuid)) {
                        // Found by UUID - try to get resource name from fileName field
                        try {
                            java.lang.reflect.Field fileNameField = emoteHolderClass.getField("fileName");
                            Object fileNameComponent = fileNameField.get(emoteHolder);
                            if (fileNameComponent != null) {
                                String fileName = (String) getStringMethod.invoke(fileNameComponent);
                                resourceName = fileName;
                            }
                        } catch (Exception e) {
                            // Ignore
                        }
                        break;
                    }
                    
                    // Check if name matches
                    if (name.equalsIgnoreCase(emoteId) || name.toLowerCase().contains(emoteId.toLowerCase())) {
                        // Try to get resource name from fileName field
                        try {
                            java.lang.reflect.Field fileNameField = emoteHolderClass.getField("fileName");
                            Object fileNameComponent = fileNameField.get(emoteHolder);
                            if (fileNameComponent != null) {
                                String fileName = (String) getStringMethod.invoke(fileNameComponent);
                                resourceName = fileName;
                            } else {
                                // Try to match by name
                                String nameLower = name.toLowerCase();
                                resourceName = nameToResource.get(nameLower);
                            }
                        } catch (Exception e) {
                            // Try to match by name
                            String nameLower = name.toLowerCase();
                            resourceName = nameToResource.get(nameLower);
                        }
                        break;
                    }
                } catch (Exception e) {
                    continue;
                }
            }
            
            // If we found a resource name, load it using UniversalEmoteSerializer
            if (resourceName != null) {
                try {
                    // Get UniversalEmoteSerializer class
                    Class<?> serializerClass = Class.forName("io.github.kosmx.emotes.server.serializer.UniversalEmoteSerializer");
                    
                    // Get CommonData.MOD_ID for resource path
                    Class<?> commonDataClass = Class.forName("io.github.kosmx.emotes.common.CommonData");
                    java.lang.reflect.Field modIdField = commonDataClass.getField("MOD_ID");
                    String modId = (String) modIdField.get(null);
                    
                    // Load resource stream (built-in emotes)
                    String resourcePath = "assets/" + modId + "/emotes/" + resourceName;
                    InputStream stream = ClientEmoteHandler.class.getClassLoader().getResourceAsStream(resourcePath);
                    String sourceLabel = resourcePath;
                    
                    if (stream == null) {
                        // Try loading from external emotes folder (run/emotes)
                        try {
                            // Get InstanceService to get external emote directory
                            Class<?> instanceServiceClass = Class.forName("io.github.kosmx.emotes.server.services.InstanceService");
                            java.lang.reflect.Field instanceField = instanceServiceClass.getField("INSTANCE");
                            Object instanceService = instanceField.get(null);
                            java.lang.reflect.Method getExternalEmoteDirMethod = instanceServiceClass.getMethod("getExternalEmoteDir");
                            Object emoteDirPath = getExternalEmoteDirMethod.invoke(instanceService);
                            
                            // Convert Path to File
                            java.nio.file.Path emotePath = (java.nio.file.Path) emoteDirPath;
                            java.io.File emoteFile = new java.io.File(emotePath.toFile(), resourceName);
                            
                            if (emoteFile.exists() && emoteFile.isFile()) {
                                stream = new java.io.FileInputStream(emoteFile);
                                sourceLabel = emoteFile.getAbsolutePath();
                                Marallyzen.LOGGER.info(
                                    "ClientEmoteHandler: Loading emote '{}' from external file: {}",
                                    emoteId,
                                    emoteFile.getAbsolutePath()
                                );
                            } else {
                                // Try with exact emoteId as filename (for SPE_ prefixed emotes)
                                String emoteFileName = emoteId + ".json";
                                emoteFile = new java.io.File(emotePath.toFile(), emoteFileName);
                                if (emoteFile.exists() && emoteFile.isFile()) {
                                    stream = new java.io.FileInputStream(emoteFile);
                                    sourceLabel = emoteFile.getAbsolutePath();
                                    resourceName = emoteFileName;
                                    Marallyzen.LOGGER.info(
                                        "ClientEmoteHandler: Loading emote '{}' from external file: {}",
                                        emoteId,
                                        emoteFile.getAbsolutePath()
                                    );
                                }
                            }
                        } catch (Exception e) {
                            Marallyzen.LOGGER.debug("ClientEmoteHandler: Failed to load from external emotes folder: {}", e.getMessage());
                        }
                    }
                    
                    if (stream != null) {
                        try {
                            // Use UniversalEmoteSerializer.readData() to load with correct type
                            java.lang.reflect.Method readDataMethod = serializerClass.getMethod("readData", InputStream.class, String.class);
                            Object result = readDataMethod.invoke(null, stream, resourceName);
                            
                            Marallyzen.LOGGER.debug("ClientEmoteHandler: readData() returned type: {}", result != null ? result.getClass().getName() : "null");
                            
                            // Handle different return types - it should be Map<String, Animation>
                            java.util.Map<String, Object> emotes = null;
                            if (result instanceof java.util.Map) {
                                @SuppressWarnings("unchecked")
                                java.util.Map<String, Object> map = (java.util.Map<String, Object>) result;
                                emotes = map;
                            } else if (result instanceof java.util.Collection) {
                                // If it's a collection, convert to map
                                java.util.Collection<?> collection = (java.util.Collection<?>) result;
                                emotes = new java.util.HashMap<>();
                                int index = 0;
                                for (Object item : collection) {
                                    emotes.put("emote_" + index++, item);
                                }
                                Marallyzen.LOGGER.debug("ClientEmoteHandler: Converted Collection to Map with {} items", emotes.size());
                            } else {
                                Marallyzen.LOGGER.warn("ClientEmoteHandler: readData() returned unexpected type: {}", result != null ? result.getClass().getName() : "null");
                            }
                            
                            if (emotes != null && !emotes.isEmpty()) {
                                // Get first emote from map
                                Object animation = emotes.values().iterator().next();
                                String actualType = animation.getClass().getName();
                                logLegacyEmote(sourceLabel, emoteId, resourceName, animation);
                                Marallyzen.LOGGER.info("ClientEmoteHandler: Loaded emote '{}' from '{}' with type: {}", emoteId, resourceName, actualType);

                                // Verify it's the correct type
                                if (actualType.equals("com.zigythebird.playeranimcore.animation.Animation")) {
                                    return animation;
                                } else {
                                    Marallyzen.LOGGER.warn("ClientEmoteHandler: Loaded emote has wrong type: {} (expected: com.zigythebird.playeranimcore.animation.Animation)", actualType);
                                }
                            }
                        } finally {
                            stream.close();
                        }
                    }
                } catch (Exception e) {
                    Marallyzen.LOGGER.debug("ClientEmoteHandler: Failed to load from resource: {}", e.getMessage());
                }
            }
            
            Object modAssetEmote = loadFromModAssets(emoteId);
            if (modAssetEmote != null) {
                return modAssetEmote;
            }

            // Try to load directly from external emotes folder by emoteId
            // This is the PRIMARY method for custom emotes that aren't in EmoteHolder.list
            Marallyzen.LOGGER.info("ClientEmoteHandler: Emote '{}' not found in EmoteHolder.list, trying to load from external folder...", emoteId);
            if (emoteId != null && !emoteId.isEmpty()) {
                java.io.File emoteDir = null;
                
                // On client side, we cannot access server-side InstanceService
                // Use fallback path directly
                if (emoteDir == null || !emoteDir.exists() || !emoteDir.isDirectory()) {
                    // Try direct path: run/emotes (for dev environment)
                    // Current working directory is already "run/", so we need to use "emotes" directly
                    java.io.File currentDir = new java.io.File(".");
                    String currentPath = currentDir.getAbsolutePath();
                    Marallyzen.LOGGER.info("ClientEmoteHandler: Current working directory: {}", currentPath);
                    
                    // Try "emotes" if we're in run/ directory
                    emoteDir = new java.io.File("emotes");
                    if (!emoteDir.exists() || !emoteDir.isDirectory()) {
                        // Try "run/emotes" if we're in project root
                        emoteDir = new java.io.File("run/emotes");
                    }
                    if (!emoteDir.exists() || !emoteDir.isDirectory()) {
                        // Try absolute path: go up one level if we're in run/
                        if (currentPath.endsWith("run") || currentPath.endsWith("run\\")) {
                            emoteDir = new java.io.File(currentDir.getParentFile(), "run/emotes");
                        } else {
                            emoteDir = new java.io.File(currentDir, "run/emotes");
                        }
                    }
                    
                    Marallyzen.LOGGER.info("ClientEmoteHandler: Using fallback path: {}", emoteDir.getAbsolutePath());
                    Marallyzen.LOGGER.info("ClientEmoteHandler: Fallback directory exists: {}, isDirectory: {}", emoteDir.exists(), emoteDir.isDirectory());
                }
                
                try {
                    Marallyzen.LOGGER.info("ClientEmoteHandler: Checking emoteDir: null={}, exists={}, isDirectory={}", 
                        emoteDir == null, 
                        emoteDir != null && emoteDir.exists(), 
                        emoteDir != null && emoteDir.isDirectory());
                    
                    if (emoteDir != null && emoteDir.exists() && emoteDir.isDirectory()) {
                        Marallyzen.LOGGER.info("ClientEmoteHandler: Emote directory is valid: {}", emoteDir.getAbsolutePath());
                        
                        // Try exact filename first
                        String emoteFileName = emoteId + ".json";
                        java.io.File emoteFile = new java.io.File(emoteDir, emoteFileName);
                        
                        // Marallyzen.LOGGER.info("ClientEmoteHandler: Looking for emote file: {}", emoteFile.getAbsolutePath());
                        // Marallyzen.LOGGER.info("ClientEmoteHandler: File exists: {}, isFile: {}", emoteFile.exists(), emoteFile.isFile());
                        
                        // If exact file doesn't exist, try case-insensitive search
                        if (!emoteFile.exists() || !emoteFile.isFile()) {
                            // Marallyzen.LOGGER.debug("ClientEmoteHandler: Exact file not found, trying case-insensitive search...");
                            if (emoteDir.exists() && emoteDir.isDirectory()) {
                                java.io.File[] files = emoteDir.listFiles((dir, name) -> 
                                    name.equalsIgnoreCase(emoteFileName) && name.toLowerCase().endsWith(".json")
                                );
                                if (files != null && files.length > 0) {
                                    emoteFile = files[0];
                                    // Marallyzen.LOGGER.debug("ClientEmoteHandler: Found case-insensitive match: {}", emoteFile.getAbsolutePath());
                                }
                            }
                        }
                        
                        if (emoteFile.exists() && emoteFile.isFile()) {
                            // Marallyzen.LOGGER.info("ClientEmoteHandler: Loading emote from file: {}", emoteFile.getAbsolutePath());
                            
                            // Get UniversalEmoteSerializer class
                            Class<?> serializerClass = Class.forName("io.github.kosmx.emotes.server.serializer.UniversalEmoteSerializer");
                            
                            try (InputStream stream = new java.io.FileInputStream(emoteFile)) {
                                // Use UniversalEmoteSerializer.readData() to load with correct type
                                java.lang.reflect.Method readDataMethod = serializerClass.getMethod("readData", InputStream.class, String.class);
                                Object result = readDataMethod.invoke(null, stream, emoteFile.getName());
                                
                                // Marallyzen.LOGGER.debug("ClientEmoteHandler: readData() returned type: {}", result != null ? result.getClass().getName() : "null");
                                
                                // Handle different return types - it should be Map<String, Animation>
                                java.util.Map<String, Object> emotes = null;
                                if (result instanceof java.util.Map) {
                                    @SuppressWarnings("unchecked")
                                    java.util.Map<String, Object> map = (java.util.Map<String, Object>) result;
                                    emotes = map;
                                } else if (result instanceof java.util.Collection) {
                                    // If it's a collection, convert to map
                                    java.util.Collection<?> collection = (java.util.Collection<?>) result;
                                    emotes = new java.util.HashMap<>();
                                    int index = 0;
                                    for (Object item : collection) {
                                        emotes.put("emote_" + index++, item);
                                    }
                                    // Marallyzen.LOGGER.debug("ClientEmoteHandler: Converted Collection to Map with {} items", emotes.size());
                                } else {
                                    Marallyzen.LOGGER.warn("ClientEmoteHandler: readData() returned unexpected type: {}", result != null ? result.getClass().getName() : "null");
                                }
                                
                                if (emotes != null && !emotes.isEmpty()) {
                                    // Get first emote from map
                                    Object animation = emotes.values().iterator().next();
                                    String actualType = animation.getClass().getName();
                                    logLegacyEmote("external_folder", emoteId, emoteFile.getName(), animation);
                                    // Marallyzen.LOGGER.info("ClientEmoteHandler: Successfully loaded emote '{}' from external file '{}' with type: {}", emoteId, emoteFile.getAbsolutePath(), actualType);
                                    
                                    // Verify it's the correct type
                                    if (actualType.equals("com.zigythebird.playeranimcore.animation.Animation")) {
                                        return animation;
                                    } else {
                                        // Marallyzen.LOGGER.warn("ClientEmoteHandler: Loaded emote has wrong type: {} (expected: com.zigythebird.playeranimcore.animation.Animation)", actualType);
                                        // Still return it, as it might work with reflection
                                        return animation;
                                    }
                                } else {
                                    Marallyzen.LOGGER.warn("ClientEmoteHandler: File '{}' exists but contains no emotes", emoteFile.getAbsolutePath());
                                }
                            } catch (Exception e) {
                                Marallyzen.LOGGER.warn("ClientEmoteHandler: Failed to read emote file '{}': {}", emoteFile.getAbsolutePath(), e.getMessage());
                                e.printStackTrace();
                            }
                        } else {
                            // List available files for debugging
                            if (emoteDir.exists() && emoteDir.isDirectory()) {
                                java.io.File[] files = emoteDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
                                if (files != null && files.length > 0) {
                                    // Marallyzen.LOGGER.debug("ClientEmoteHandler: Available emote files in directory (first 10):");
                                    for (int i = 0; i < Math.min(10, files.length); i++) {
                                        // Marallyzen.LOGGER.debug("  - {}", files[i].getName());
                                    }
                                } else {
                                    // Marallyzen.LOGGER.debug("ClientEmoteHandler: No .json files found in directory");
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Marallyzen.LOGGER.warn("ClientEmoteHandler: Failed to load emote '{}' directly from external folder: {}", emoteId, e.getMessage());
                    e.printStackTrace();
                }
            }
            
            // Fallback: Try direct path to run/emotes (for dev environment)
            if (emoteId != null && !emoteId.isEmpty()) {
                try {
                    // Try direct path: run/emotes/emoteId.json
                    // Current working directory might be "run/", so try different paths
                    java.io.File currentDir = new java.io.File(".");
                    String currentPath = currentDir.getAbsolutePath();
                    
                    java.io.File directEmoteFile = null;
                    // Try "emotes" if we're in run/ directory
                    directEmoteFile = new java.io.File("emotes", emoteId + ".json");
                    if (!directEmoteFile.exists() || !directEmoteFile.isFile()) {
                        // Try "run/emotes" if we're in project root
                        directEmoteFile = new java.io.File("run/emotes", emoteId + ".json");
                    }
                    if (!directEmoteFile.exists() || !directEmoteFile.isFile()) {
                        // Try absolute path: go up one level if we're in run/
                        if (currentPath.endsWith("run") || currentPath.endsWith("run\\")) {
                            directEmoteFile = new java.io.File(currentDir.getParentFile(), "run/emotes/" + emoteId + ".json");
                        } else {
                            directEmoteFile = new java.io.File(currentDir, "run/emotes/" + emoteId + ".json");
                        }
                    }
                    
                    if (directEmoteFile.exists() && directEmoteFile.isFile()) {
                        Marallyzen.LOGGER.info(
                            "ClientEmoteHandler: Loading emote '{}' from direct path: {}",
                            emoteId,
                            directEmoteFile.getAbsolutePath()
                        );
                        
                        Class<?> serializerClass = Class.forName("io.github.kosmx.emotes.server.serializer.UniversalEmoteSerializer");
                        
                        try (InputStream stream = new java.io.FileInputStream(directEmoteFile)) {
                            java.lang.reflect.Method readDataMethod = serializerClass.getMethod("readData", InputStream.class, String.class);
                            Object result = readDataMethod.invoke(null, stream, directEmoteFile.getName());
                            
                            // Marallyzen.LOGGER.debug("ClientEmoteHandler: readData() returned type: {}", result != null ? result.getClass().getName() : "null");
                            
                            // Handle different return types - it should be Map<String, Animation>
                            java.util.Map<String, Object> emotes = null;
                            if (result instanceof java.util.Map) {
                                @SuppressWarnings("unchecked")
                                java.util.Map<String, Object> map = (java.util.Map<String, Object>) result;
                                emotes = map;
                            } else if (result instanceof java.util.Collection) {
                                // If it's a collection, convert to map
                                java.util.Collection<?> collection = (java.util.Collection<?>) result;
                                emotes = new java.util.HashMap<>();
                                int index = 0;
                                for (Object item : collection) {
                                    emotes.put("emote_" + index++, item);
                                }
                                // Marallyzen.LOGGER.debug("ClientEmoteHandler: Converted Collection to Map with {} items", emotes.size());
                            } else {
                                Marallyzen.LOGGER.warn("ClientEmoteHandler: readData() returned unexpected type: {}", result != null ? result.getClass().getName() : "null");
                            }
                            
                            if (emotes != null && !emotes.isEmpty()) {
                                Object animation = emotes.values().iterator().next();
                                String actualType = animation.getClass().getName();
                                logLegacyEmote("direct_path", emoteId, directEmoteFile.getName(), animation);
                                // Marallyzen.LOGGER.info("ClientEmoteHandler: Successfully loaded emote '{}' from direct path '{}' with type: {}", emoteId, directEmoteFile.getAbsolutePath(), actualType);
                                return animation;
                            }
                        }
                    } else {
                        // Marallyzen.LOGGER.debug("ClientEmoteHandler: Direct path file does not exist: {}", directEmoteFile.getAbsolutePath());
                    }
                } catch (Exception e) {
                    // Marallyzen.LOGGER.debug("ClientEmoteHandler: Failed to load from direct path: {}", e.getMessage());
                }
            }
            
            // Fallback: use EmoteHolder.list directly (may have wrong type due to classpath conflict)
            // Marallyzen.LOGGER.debug("ClientEmoteHandler: Falling back to EmoteHolder.list (may have type conflict)");
            
            // Try to get emote by UUID
            if (emoteUuid != null) {
                Object emoteHolder = null;
                if (emoteList instanceof java.util.Map<?, ?> map) {
                    emoteHolder = map.get(emoteUuid);
                }
                if (emoteHolder == null) {
                    try {
                        java.lang.reflect.Method getMethod = emoteList.getClass().getMethod("get", Object.class);
                        emoteHolder = getMethod.invoke(emoteList, emoteUuid);
                    } catch (Exception ignored) {
                    }
                }
                if (emoteHolder != null) {
                    java.lang.reflect.Field emoteField = emoteHolderClass.getField("emote");
                    Object animation = emoteField.get(emoteHolder);
                    if (animation != null) {
                        // Marallyzen.LOGGER.info("ClientEmoteHandler: Loaded emote '{}' from EmoteHolder.list by UUID (type: {})", emoteId, animation.getClass().getName());
                        return animation;
                    }
                }
            }
            
            // Search by name
            for (Object emoteHolder : emoteHolders) {
                try {
                    java.lang.reflect.Field nameField = emoteHolderClass.getField("name");
                    Object nameComponent = nameField.get(emoteHolder);
                    java.lang.reflect.Method getStringMethod = nameComponent.getClass().getMethod("getString");
                    String name = (String) getStringMethod.invoke(nameComponent);
                    
                    if (name.equalsIgnoreCase(emoteId) || name.toLowerCase().contains(emoteId.toLowerCase())) {
                        java.lang.reflect.Field emoteField = emoteHolderClass.getField("emote");
                        Object animation = emoteField.get(emoteHolder);
                        if (animation != null) {
                            logLegacyEmote("emote_holder_name", emoteId, name, animation);
                            Marallyzen.LOGGER.info("ClientEmoteHandler: Loaded emote '{}' from EmoteHolder.list by name (found: '{}', type: {})", 
                                emoteId, name, animation.getClass().getName());
                            return animation;
                        }
                    }
                } catch (Exception e) {
                    continue;
                }
            }
            
            Marallyzen.LOGGER.warn("ClientEmoteHandler: Emote '{}' not found in any location (EmoteHolder.list, external folder, or direct path)", emoteId);
            return null;
        } catch (ClassNotFoundException e) {
            Marallyzen.LOGGER.warn("ClientEmoteHandler: Emotecraft classes not found - Emotecraft is NOT installed: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("ClientEmoteHandler: Failed to load emote '{}': {}", emoteId, e.getMessage());
            Marallyzen.LOGGER.warn("ClientEmoteHandler: Exception details:", e);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Plays an emote on an NPC entity using IPlayerEntity.emotecraft$playEmote().
     * 
     * @param npc The entity to animate (must implement IPlayerEntity)
     * @param emote The Animation object loaded from registry
     */
    public static void playEmoteOnNpc(Entity npc, Object emote) {
        if (npc == null || emote == null) {
            return;
        }

        try {
            // Log entity type for debugging
            Marallyzen.LOGGER.debug("ClientEmoteHandler: Entity type: {}, class: {}", 
                    npc.getType().toString(), npc.getClass().getName());
            
            // Check if entity implements IPlayerEntity (via Emotecraft mixin)
            Class<?> iPlayerEntityClass = Class.forName("io.github.kosmx.emotes.main.mixinFunctions.IPlayerEntity");
            
            if (!iPlayerEntityClass.isInstance(npc)) {
                Marallyzen.LOGGER.warn("ClientEmoteHandler: Entity {} (type: {}) does not implement IPlayerEntity - Emotecraft mixin not applied", 
                        npc.getName().getString(), npc.getClass().getName());
                return;
            }
            
            // Cast to IPlayerEntity
            Object iPlayerEntity = iPlayerEntityClass.cast(npc);
            
            // Get Animation class from the emote object itself (it's already loaded)
            // This avoids ClassNotFoundException if the class is in a different classloader
            Class<?> animationClass = emote.getClass();
            String animationTypeName = animationClass.getName();
            
            Marallyzen.LOGGER.debug("ClientEmoteHandler: Animation type: {}", animationTypeName);
            
            // Check if we have the wrong type (old player-animation-lib)
            boolean isWrongType = animationTypeName.equals("dev.kosmx.playerAnim.core.data.KeyframeAnimation");
            if (isWrongType) {
                Marallyzen.LOGGER.warn("ClientEmoteHandler: Detected old Animation type: {}", animationTypeName);
                Marallyzen.LOGGER.warn("ClientEmoteHandler: This indicates a classpath conflict with old player-animation-lib");
                Marallyzen.LOGGER.warn("ClientEmoteHandler: Attempting to use reflection to bypass type checking...");
                try {
                    java.net.URL source = animationClass.getProtectionDomain()
                        .getCodeSource()
                        .getLocation();
                    Marallyzen.LOGGER.warn("ClientEmoteHandler: Old Animation code source: {}", source);
                } catch (Exception e) {
                    Marallyzen.LOGGER.warn("ClientEmoteHandler: Failed to resolve old Animation code source");
                }
            }
            
            // Try to find the method using reflection
            // The method signature is: emotecraft$playEmote(Animation, float, boolean)
            java.lang.reflect.Method playEmoteMethod = null;
            
            // Strategy 1: Try to find method with the correct Animation type
            try {
                Class<?> correctAnimationClass = Class.forName("com.zigythebird.playeranimcore.animation.Animation");
                playEmoteMethod = iPlayerEntityClass.getMethod(
                    "emotecraft$playEmote", 
                    correctAnimationClass,
                    float.class,
                    boolean.class
                );
                Marallyzen.LOGGER.debug("ClientEmoteHandler: Found method with correct Animation type");
            } catch (Exception e) {
                // Continue to next strategy
            }
            
            // Strategy 2: If wrong type, try to find method that accepts the old KeyframeAnimation type with int
            // Based on logs, the actual method signature is: (KeyframeAnimation, int, boolean)
            if (playEmoteMethod == null && isWrongType) {
                try {
                    Class<?> oldAnimationClass = Class.forName("dev.kosmx.playerAnim.core.data.KeyframeAnimation");
                    // Try with int first (as seen in logs)
                    try {
                        playEmoteMethod = iPlayerEntityClass.getMethod(
                            "emotecraft$playEmote", 
                            oldAnimationClass,
                            int.class,
                            boolean.class
                        );
                        Marallyzen.LOGGER.debug("ClientEmoteHandler: Found method that accepts old KeyframeAnimation type with int");
                    } catch (NoSuchMethodException e) {
                        // Try with float
                        playEmoteMethod = iPlayerEntityClass.getMethod(
                            "emotecraft$playEmote", 
                            oldAnimationClass,
                            float.class,
                            boolean.class
                        );
                        Marallyzen.LOGGER.debug("ClientEmoteHandler: Found method that accepts old KeyframeAnimation type with float");
                    }
                } catch (Exception e) {
                    // Continue to next strategy
                }
            }
            
            // Strategy 2b: Try to find method that accepts Object or any compatible type
            if (playEmoteMethod == null) {
                for (java.lang.reflect.Method method : iPlayerEntityClass.getMethods()) {
                    if (method.getName().equals("emotecraft$playEmote")) {
                        Class<?>[] paramTypes = method.getParameterTypes();
                        if (paramTypes.length == 3 && 
                            (paramTypes[1] == float.class || paramTypes[1] == int.class) &&
                            paramTypes[2] == boolean.class) {
                            // Check if first parameter can accept our animation object
                            // Try Object.class, or check if it's assignable
                            if (paramTypes[0] == Object.class || 
                                paramTypes[0].isAssignableFrom(animationClass) ||
                                animationClass.isAssignableFrom(paramTypes[0])) {
                                playEmoteMethod = method;
                                Marallyzen.LOGGER.debug("ClientEmoteHandler: Found compatible method: {} with param type: {}", 
                                    method.getName(), paramTypes[0].getName());
                                break;
                            }
                        }
                    }
                }
            }
            
            // Strategy 3: Try to find method with Object as first parameter (most permissive)
            if (playEmoteMethod == null) {
                try {
                    playEmoteMethod = iPlayerEntityClass.getMethod(
                        "emotecraft$playEmote", 
                        Object.class,
                        float.class,
                        boolean.class
                    );
                    Marallyzen.LOGGER.debug("ClientEmoteHandler: Found method with Object parameter");
                } catch (NoSuchMethodException e) {
                    // Continue to next strategy
                }
            }
            
            // Strategy 4: Use getDeclaredMethods to find the actual implementation (might be in mixin)
            if (playEmoteMethod == null) {
                // Get the actual implementation class (might be a mixin-generated class)
                Class<?> actualClass = iPlayerEntity.getClass();
                Marallyzen.LOGGER.debug("ClientEmoteHandler: Searching in actual class: {}", actualClass.getName());
                
                // Try getMethods first - look for method with old KeyframeAnimation and int
                for (java.lang.reflect.Method method : actualClass.getMethods()) {
                    if (method.getName().equals("emotecraft$playEmote")) {
                        Class<?>[] paramTypes = method.getParameterTypes();
                        Marallyzen.LOGGER.debug("ClientEmoteHandler: Found method via getMethods: {} with params: {}", 
                            method.getName(), java.util.Arrays.toString(paramTypes));
                        if (paramTypes.length == 3 && 
                            (paramTypes[1] == float.class || paramTypes[1] == int.class) &&
                            paramTypes[2] == boolean.class) {
                            // Accept any type for first parameter (old KeyframeAnimation or new Animation)
                            playEmoteMethod = method;
                            playEmoteMethod.setAccessible(true);
                            Marallyzen.LOGGER.debug("ClientEmoteHandler: Using method from getMethods with param type: {}", 
                                paramTypes[0].getName());
                            break;
                        }
                    }
                }
                
                // Try getDeclaredMethods (includes non-public methods from mixins)
                if (playEmoteMethod == null) {
                    for (java.lang.reflect.Method method : actualClass.getDeclaredMethods()) {
                        if (method.getName().equals("emotecraft$playEmote")) {
                            Class<?>[] paramTypes = method.getParameterTypes();
                            Marallyzen.LOGGER.debug("ClientEmoteHandler: Found method via getDeclaredMethods: {} with params: {}", 
                                method.getName(), java.util.Arrays.toString(paramTypes));
                            if (paramTypes.length == 3 && 
                                (paramTypes[1] == float.class || paramTypes[1] == int.class) &&
                                paramTypes[2] == boolean.class) {
                                // Accept any type for first parameter - this is our last resort
                                playEmoteMethod = method;
                                playEmoteMethod.setAccessible(true);
                                Marallyzen.LOGGER.debug("ClientEmoteHandler: Using method from getDeclaredMethods with param type: {}", 
                                    paramTypes[0].getName());
                                break;
                            }
                        }
                    }
                }
            }
            
            // Strategy 5: Try to find method that accepts the old KeyframeAnimation type directly
            // IMPORTANT: The method signature is (KeyframeAnimation, int, boolean) not (Animation, float, boolean)!
            if (playEmoteMethod == null && isWrongType) {
                try {
                    Class<?> oldAnimationClass = Class.forName("dev.kosmx.playerAnim.core.data.KeyframeAnimation");
                    // Try with int (as seen in logs)
                    try {
                        playEmoteMethod = iPlayerEntityClass.getMethod(
                            "emotecraft$playEmote", 
                            oldAnimationClass,
                            int.class,
                            boolean.class
                        );
                        Marallyzen.LOGGER.debug("ClientEmoteHandler: Found method that accepts old KeyframeAnimation type with int");
                    } catch (NoSuchMethodException e) {
                        // Try with float
                        playEmoteMethod = iPlayerEntityClass.getMethod(
                            "emotecraft$playEmote", 
                            oldAnimationClass,
                            float.class,
                            boolean.class
                        );
                        Marallyzen.LOGGER.debug("ClientEmoteHandler: Found method that accepts old KeyframeAnimation type with float");
                    }
                } catch (Exception e) {
                    // Try in actual class
                    Class<?> actualClass = iPlayerEntity.getClass();
                    try {
                        Class<?> oldAnimationClass = Class.forName("dev.kosmx.playerAnim.core.data.KeyframeAnimation");
                        // Try with int first (as seen in logs)
                        try {
                            playEmoteMethod = actualClass.getMethod(
                                "emotecraft$playEmote", 
                                oldAnimationClass,
                                int.class,
                                boolean.class
                            );
                            playEmoteMethod.setAccessible(true);
                            Marallyzen.LOGGER.debug("ClientEmoteHandler: Found method in actual class that accepts old KeyframeAnimation type with int");
                        } catch (NoSuchMethodException e2) {
                            // Try with float
                            playEmoteMethod = actualClass.getMethod(
                                "emotecraft$playEmote", 
                                oldAnimationClass,
                                float.class,
                                boolean.class
                            );
                            playEmoteMethod.setAccessible(true);
                            Marallyzen.LOGGER.debug("ClientEmoteHandler: Found method in actual class that accepts old KeyframeAnimation type with float");
                        }
                    } catch (Exception e3) {
                        // Continue
                    }
                }
            }
            
            // Strategy 6: Try to use EmotePlayer.triggerAnimation directly
            if (playEmoteMethod == null) {
                try {
                    // Get emotecraft$getEmote() to get EmotePlayer
                    java.lang.reflect.Method getEmoteMethod = iPlayerEntityClass.getMethod("emotecraft$getEmote");
                    Object emotePlayer = getEmoteMethod.invoke(iPlayerEntity);
                    
                    if (emotePlayer != null) {
                        Class<?> emotePlayerClass = emotePlayer.getClass();
                        Marallyzen.LOGGER.debug("ClientEmoteHandler: Found EmotePlayer: {}", emotePlayerClass.getName());
                        
                        // Try to find triggerAnimation method
                        for (java.lang.reflect.Method method : emotePlayerClass.getMethods()) {
                            if (method.getName().equals("triggerAnimation")) {
                                Class<?>[] paramTypes = method.getParameterTypes();
                                Marallyzen.LOGGER.debug("ClientEmoteHandler: Found triggerAnimation with params: {}", 
                                    java.util.Arrays.toString(paramTypes));
                                
                                // triggerAnimation takes RawAnimation and float
                                if (paramTypes.length == 2 && paramTypes[1] == float.class) {
                                    // Try to create RawAnimation using reflection
                                    try {
                                        Class<?> rawAnimationClass = Class.forName("com.zigythebird.playeranimcore.animation.RawAnimation");
                                        java.lang.reflect.Method beginMethod = rawAnimationClass.getMethod("begin");
                                        Object rawAnimation = beginMethod.invoke(null);
                                        
                                        // Try to call then() method
                                        java.lang.reflect.Method thenMethod = rawAnimationClass.getMethod("then", 
                                            Class.forName("com.zigythebird.playeranimcore.animation.Animation"),
                                            Class.forName("com.zigythebird.playeranimcore.animation.Animation$LoopType"));
                                        
                                        // Get LoopType.DEFAULT
                                        Class<?> loopTypeClass = Class.forName("com.zigythebird.playeranimcore.animation.Animation$LoopType");
                                        java.lang.reflect.Field defaultField = loopTypeClass.getField("DEFAULT");
                                        Object defaultLoopType = defaultField.get(null);
                                        
                                        // Try to cast or convert the animation
                                        // This might fail if types are incompatible
                                        Object finalRawAnimation = thenMethod.invoke(rawAnimation, emote, defaultLoopType);
                                        
                                        // Call triggerAnimation
                                        method.invoke(emotePlayer, finalRawAnimation, 0.0f);
                                        Marallyzen.LOGGER.info("ClientEmoteHandler: Successfully played emote via EmotePlayer.triggerAnimation()");
                                        return; // Success!
                                    } catch (Exception e) {
                                        Marallyzen.LOGGER.debug("ClientEmoteHandler: Failed to use EmotePlayer.triggerAnimation: {}", e.getMessage());
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Marallyzen.LOGGER.debug("ClientEmoteHandler: Failed to access EmotePlayer: {}", e.getMessage());
                }
            }
            
            // Strategy 7: Try using MethodHandle for type coercion
            if (playEmoteMethod == null) {
                try {
                    Class<?> actualClass = iPlayerEntity.getClass();
                    java.lang.invoke.MethodHandles.Lookup lookup = java.lang.invoke.MethodHandles.lookup();
                    
                    // Try to find any method with the right name and parameter count
                    for (java.lang.reflect.Method method : actualClass.getDeclaredMethods()) {
                        if (method.getName().equals("emotecraft$playEmote") && method.getParameterCount() == 3) {
                            method.setAccessible(true);
                            try {
                                java.lang.invoke.MethodHandle handle = lookup.unreflect(method);
                                // Try to call with type coercion
                                java.lang.invoke.MethodHandle adapted = handle.asType(
                                    java.lang.invoke.MethodType.methodType(void.class, Object.class, float.class, boolean.class)
                                );
                                adapted.invoke(iPlayerEntity, emote, 0.0f, true);
                                Marallyzen.LOGGER.info("ClientEmoteHandler: Successfully played emote via MethodHandle with type coercion");
                                return; // Success!
                            } catch (Throwable e) {
                                Marallyzen.LOGGER.debug("ClientEmoteHandler: MethodHandle failed: {}", e.getMessage());
                            }
                        }
                    }
                } catch (Exception e) {
                    Marallyzen.LOGGER.debug("ClientEmoteHandler: MethodHandle strategy failed: {}", e.getMessage());
                }
            }
            
            if (playEmoteMethod == null) {
                Marallyzen.LOGGER.error("ClientEmoteHandler: Could not find emotecraft$playEmote method for Animation type: {}", 
                        animationTypeName);
                // Log available methods for debugging - ALL methods, not just emote-related
                Class<?> actualClass = iPlayerEntity.getClass();
                Marallyzen.LOGGER.error("ClientEmoteHandler: Entity class: {}", actualClass.getName());
                Marallyzen.LOGGER.error("ClientEmoteHandler: Entity interfaces: {}", java.util.Arrays.toString(actualClass.getInterfaces()));
                
                Marallyzen.LOGGER.error("ClientEmoteHandler: ALL methods in actual class {} (first 50):", actualClass.getName());
                int count = 0;
                for (java.lang.reflect.Method m : actualClass.getMethods()) {
                    if (count++ < 50) {
                        Marallyzen.LOGGER.error("  - {} ({})", m.getName(), java.util.Arrays.toString(m.getParameterTypes()));
                    }
                }
                
                Marallyzen.LOGGER.error("ClientEmoteHandler: ALL declared methods in actual class {} (first 50):", actualClass.getName());
                count = 0;
                for (java.lang.reflect.Method m : actualClass.getDeclaredMethods()) {
                    if (count++ < 50) {
                        Marallyzen.LOGGER.error("  - {} ({}) [declared]", m.getName(), java.util.Arrays.toString(m.getParameterTypes()));
                    }
                }
                
                // Also check IPlayerEntity interface
                Marallyzen.LOGGER.error("ClientEmoteHandler: ALL methods in IPlayerEntity interface:");
                for (java.lang.reflect.Method m : iPlayerEntityClass.getMethods()) {
                    Marallyzen.LOGGER.error("  - {} ({})", m.getName(), java.util.Arrays.toString(m.getParameterTypes()));
                }
                
                Marallyzen.LOGGER.error("ClientEmoteHandler: All strategies failed. The old player-animation-lib conflict cannot be resolved.");
                Marallyzen.LOGGER.error("ClientEmoteHandler: Player Animator 2.0.1 is loaded as a dependency of Emotecraft (JarInJar).");
                Marallyzen.LOGGER.error("ClientEmoteHandler: This creates a classpath conflict. Consider updating Emotecraft or reporting this issue.");
                return;
            }
            
            // Log the method we're using
            Class<?>[] paramTypes = playEmoteMethod.getParameterTypes();
            Marallyzen.LOGGER.debug("ClientEmoteHandler: Using method: {} with parameter types: {}", 
                    playEmoteMethod.getName(), java.util.Arrays.toString(paramTypes));
            
            // Play emote: tick=0 (start immediately), forced=true (override current emote)
            // Use reflection to bypass type checking
            playEmoteMethod.setAccessible(true);
            
            // Check if second parameter is int or float
            Object tickValue;
            if (paramTypes.length > 1 && paramTypes[1] == int.class) {
                tickValue = 0; // Use int
            } else {
                tickValue = 0.0f; // Use float
            }
            
            playEmoteMethod.invoke(iPlayerEntity, emote, tickValue, true);
            
            Marallyzen.LOGGER.info("ClientEmoteHandler: Successfully played emote on {} via IPlayerEntity.emotecraft$playEmote()", 
                    npc.getName().getString());
        } catch (ClassNotFoundException e) {
            Marallyzen.LOGGER.warn("ClientEmoteHandler: Required class not found - Emotecraft is NOT installed: {}", e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("ClientEmoteHandler: Failed to play emote on {}: {}", 
                    npc.getName().getString(), e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Main handler method - loads emote and plays it on entity.
     * 
     * @param entityId The UUID of the entity to animate
     * @param emoteId The ID or name of the emote to play
     */
    public static void handle(UUID entityId, String emoteId) {
        handle(entityId, emoteId, true);
    }

    public static void handle(UUID entityId, String emoteId, boolean record) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        if (entityId == null || emoteId == null || emoteId.isEmpty()) {
            return;
        }

        // Find entity by UUID
        Entity entity = findEntity(mc, entityId);
        
        if (entity == null) {
            Marallyzen.LOGGER.warn("ClientEmoteHandler: Entity not found for UUID {} (emote={})", entityId, emoteId);
            return;
        }

        // Load emote from registry
        Object emote = loadEmoteFromRegistry(emoteId);
        if (emote == null) {
            Marallyzen.LOGGER.warn("ClientEmoteHandler: Could not load emote '{}'", emoteId);
            return;
        }

        if (record) {
            CutsceneRecorder recorder = CutsceneRecorder.getInstance();
            if (recorder != null && recorder.isRecording()) {
                recorder.recordEmoteEvent(entity, emoteId);
            }
        }

        // Play emote on entity
        playEmoteOnNpc(entity, emote);
    }

    public static void stop(UUID entityId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || entityId == null) {
            return;
        }
        Entity entity = findEntity(mc, entityId);
        if (entity == null) {
            Marallyzen.LOGGER.warn("ClientEmoteHandler: Entity not found for UUID {} (stop)", entityId);
            return;
        }
        try {
            java.lang.reflect.Method getEmote = entity.getClass().getMethod("emotecraft$getEmote");
            getEmote.setAccessible(true);
            Object emotePlayer = getEmote.invoke(entity);
            if (emotePlayer != null) {
                invokeStop(emotePlayer);
            }
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("ClientEmoteHandler: Failed to stop emote on {}: {}", entity.getName().getString(), e.getMessage());
        }
    }

    private static Entity findEntity(Minecraft mc, UUID entityId) {
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e.getUUID().equals(entityId)) {
                return e;
            }
        }
        return null;
    }

    private static void invokeStop(Object emotePlayer) {
        java.lang.reflect.Method[] methods = emotePlayer.getClass().getMethods();
        for (java.lang.reflect.Method method : methods) {
            String name = method.getName();
            if (!name.equals("stopEmote") && !name.equals("emotecraft$stopEmote") && !name.equals("stop")) {
                continue;
            }
            try {
                if (method.getParameterCount() == 0) {
                    method.setAccessible(true);
                    method.invoke(emotePlayer);
                    return;
                }
                if (method.getParameterCount() == 1 && method.getParameterTypes()[0] == boolean.class) {
                    method.setAccessible(true);
                    method.invoke(emotePlayer, true);
                    return;
                }
            } catch (Exception ignored) {
            }
        }
    }

    private static void logLegacyEmote(String source, String emoteId, String resourceName, Object animation) {
        if (animation == null) {
            return;
        }
        String typeName = animation.getClass().getName();
        if (!typeName.equals("dev.kosmx.playerAnim.core.data.KeyframeAnimation")) {
            return;
        }
        String key = source + "|" + (resourceName != null ? resourceName : emoteId);
        if (!LEGACY_EMOTE_LOGGED.add(key)) {
            return;
        }
        String holderName = null;
        String holderFile = null;
        if (emoteId != null) {
            try {
                UUID emoteUuid = UUID.fromString(emoteId);
                Class<?> emoteHolderClass = Class.forName("io.github.kosmx.emotes.main.EmoteHolder");
                java.lang.reflect.Field listField = emoteHolderClass.getField("list");
                Object emoteList = listField.get(null);
                Object emoteHolder = null;
                if (emoteList instanceof java.util.Map<?, ?> map) {
                    emoteHolder = map.get(emoteUuid);
                }
                if (emoteHolder == null) {
                    try {
                        java.lang.reflect.Method getMethod = emoteList.getClass().getMethod("get", Object.class);
                        emoteHolder = getMethod.invoke(emoteList, emoteUuid);
                    } catch (Exception ignored) {
                    }
                }
                if (emoteHolder != null) {
                    try {
                        java.lang.reflect.Field nameField = emoteHolderClass.getField("name");
                        Object nameComponent = nameField.get(emoteHolder);
                        if (nameComponent != null) {
                            java.lang.reflect.Method getString = nameComponent.getClass().getMethod("getString");
                            holderName = (String) getString.invoke(nameComponent);
                        }
                    } catch (Exception ignored) {
                    }
                    try {
                        java.lang.reflect.Field fileNameField = emoteHolderClass.getField("fileName");
                        Object fileNameComponent = fileNameField.get(emoteHolder);
                        if (fileNameComponent != null) {
                            java.lang.reflect.Method getString = fileNameComponent.getClass().getMethod("getString");
                            holderFile = (String) getString.invoke(fileNameComponent);
                        }
                    } catch (Exception ignored) {
                    }
                }
            } catch (IllegalArgumentException ignored) {
            } catch (Exception ignored) {
            }
        }
        Marallyzen.LOGGER.warn(
            "ClientEmoteHandler: Legacy emote detected (source={}, emoteId={}, resource={}, type={}, holderName={}, holderFile={})",
            source,
            emoteId,
            resourceName,
            typeName,
            holderName,
            holderFile
        );
    }

    private static Object loadFromModAssets(String emoteId) {
        if (emoteId == null || emoteId.isEmpty()) {
            return null;
        }
        String resourceName = resolveResourceName(emoteId);
        if (resourceName == null) {
            return null;
        }
        try {
            Class<?> serializerClass = Class.forName("io.github.kosmx.emotes.server.serializer.UniversalEmoteSerializer");
            String resourcePath = "assets/marallyzen/emotes/" + resourceName;
            InputStream stream = ClientEmoteHandler.class.getClassLoader().getResourceAsStream(resourcePath);
            if (stream == null) {
                return null;
            }
            try {
                java.lang.reflect.Method readDataMethod = serializerClass.getMethod("readData", InputStream.class, String.class);
                Object result = readDataMethod.invoke(null, stream, resourceName);

                java.util.Map<String, Object> emotes = null;
                if (result instanceof java.util.Map) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> map = (java.util.Map<String, Object>) result;
                    emotes = map;
                } else if (result instanceof java.util.Collection) {
                    java.util.Collection<?> collection = (java.util.Collection<?>) result;
                    emotes = new java.util.HashMap<>();
                    int index = 0;
                    for (Object item : collection) {
                        emotes.put("emote_" + index++, item);
                    }
                }

                if (emotes != null && !emotes.isEmpty()) {
                    Object animation = emotes.values().iterator().next();
                    if (animation != null) {
                        String typeName = animation.getClass().getName();
                        logLegacyEmote("mod_assets", emoteId, resourceName, animation);
                        if (!typeName.equals("dev.kosmx.playerAnim.core.data.KeyframeAnimation")) {
                            Marallyzen.LOGGER.info(
                                "ClientEmoteHandler: Mod asset emote '{}' loaded with type: {}",
                                resourceName,
                                typeName
                            );
                        }
                    }
                    return animation;
                }
            } finally {
                stream.close();
            }
        } catch (Exception e) {
            Marallyzen.LOGGER.debug("ClientEmoteHandler: Failed to load emote from mod assets: {}", e.getMessage());
        }
        return null;
    }

    private static String resolveResourceName(String emoteId) {
        String path = emoteId;
        try {
            ResourceLocation id = ResourceLocation.tryParse(emoteId);
            if (id != null) {
                path = id.getPath();
            }
        } catch (Exception ignored) {
        }
        if (path == null || path.isBlank()) {
            return null;
        }
        path = path.trim().toLowerCase().replace(' ', '_');
        if (!path.endsWith(".json")) {
            path += ".json";
        }
        return path;
    }
}
