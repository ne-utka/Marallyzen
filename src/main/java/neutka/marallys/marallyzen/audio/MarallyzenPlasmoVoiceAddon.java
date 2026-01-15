package neutka.marallys.marallyzen.audio;

import neutka.marallys.marallyzen.Marallyzen;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.Method;

import java.lang.reflect.Constructor;

/**
 * PlasmoVoice addon for Marallyzen.
 * Registers Marallyzen as a PlasmoVoice addon to access the API.
 * 
 * This class uses reflection to avoid compile-time dependency on PlasmoVoice.
 * According to PlasmoVoice documentation, we need to:
 * 1. Create a class annotated with @Addon
 * 2. Implement AddonInitializer
 * 3. Use @InjectPlasmoVoice to inject PlasmoVoiceServer
 * 4. Load the addon via PlasmoVoiceServer.getAddonsLoader().load(addon)
 * 
 * Since we use compileOnly dependency, we use reflection for all operations.
 * 
 * For NeoForge, the addon should be loaded in the mod constructor.
 */
public final class MarallyzenPlasmoVoiceAddon {
    
    private static Object voiceServer = null;
    private static boolean addonInitialized = false;
    private static Object addonInstance = null;
    
    /**
     * Creates and registers the PlasmoVoice addon.
     * Should be called during mod initialization if PlasmoVoice is available.
     * For NeoForge, this should be called in the mod constructor.
     */
    public static void registerAddon() {
        try {
            // Check if PlasmoVoice mod is loaded
            if (!net.neoforged.fml.ModList.get().isLoaded("plasmovoice")) {
                Marallyzen.LOGGER.debug("PlasmoVoice mod not loaded, skipping addon registration");
                return;
            }
            
            // Try to get PlasmoVoiceServer class
            Class<?> plasmoVoiceServerClass = Class.forName("su.plo.voice.api.server.PlasmoVoiceServer");
            
            // Try to get addons loader
            Object addonsLoader = plasmoVoiceServerClass.getMethod("getAddonsLoader").invoke(null);
            
            if (addonsLoader == null) {
                Marallyzen.LOGGER.warn("PlasmoVoice addons loader not available");
                return;
            }
            
            // Create addon instance - we'll create it as an anonymous class that implements AddonInitializer
            // We need to use reflection to create a class with @Addon annotation
            addonInstance = createAddonInstance();
            
            if (addonInstance == null) {
                Marallyzen.LOGGER.warn("Failed to create PlasmoVoice addon instance");
                return;
            }
            
            // Try to get voiceServer BEFORE loading addon (it might be available without addon registration)
            try {
                Class<?> plasmoVoiceServerClass2 = Class.forName("su.plo.voice.api.server.PlasmoVoiceServer");
                Object server = plasmoVoiceServerClass2.getMethod("get").invoke(null);
                if (server != null) {
                    voiceServer = server;
                    addonInitialized = true;
                    Marallyzen.LOGGER.info("PlasmoVoiceServer obtained directly via get() - addon registration may not be required");
                    // Don't load addon if we already have the server
                    return;
                }
            } catch (Exception e) {
                Marallyzen.LOGGER.debug("PlasmoVoiceServer.get() not available before addon registration: {}", e.getMessage());
            }
            
            // Load addon (only if we don't have the server yet)
            try {
                addonsLoader.getClass().getMethod("load", Object.class).invoke(addonsLoader, addonInstance);
                Marallyzen.LOGGER.info("PlasmoVoice addon registered successfully");
            } catch (Exception e) {
                Marallyzen.LOGGER.warn("Failed to load addon, but continuing - will try to get VoiceServer directly: {}", e.getMessage());
            }
            
            // Try to get voiceServer after addon registration
            // Note: onAddonInitialize will be called by PlasmoVoice, which will set voiceServer via setVoiceServer()
            // But we can also try to get it directly as a fallback
            try {
                Class<?> plasmoVoiceServerClass2 = Class.forName("su.plo.voice.api.server.PlasmoVoiceServer");
                Object server = plasmoVoiceServerClass2.getMethod("get").invoke(null);
                if (server != null) {
                    voiceServer = server;
                    addonInitialized = true;
                    Marallyzen.LOGGER.info("PlasmoVoiceServer obtained after addon registration (direct get())");
                } else {
                    Marallyzen.LOGGER.info("PlasmoVoiceServer.get() returned null - waiting for onAddonInitialize() to inject it");
                }
            } catch (Exception e) {
                Marallyzen.LOGGER.debug("PlasmoVoiceServer.get() not available after addon registration: {} - waiting for onAddonInitialize()", e.getMessage());
            }
            
            // Also try to get voiceServer from the addon instance via reflection (in case injection already happened)
            try {
                java.lang.reflect.Field voiceServerField = addonInstance.getClass().getDeclaredField("voiceServer");
                voiceServerField.setAccessible(true);
                Object injectedServer = voiceServerField.get(addonInstance);
                if (injectedServer != null) {
                    voiceServer = injectedServer;
                    addonInitialized = true;
                    Marallyzen.LOGGER.info("PlasmoVoiceServer obtained from addon instance via reflection");
                }
            } catch (Exception e) {
                Marallyzen.LOGGER.debug("Could not get voiceServer from addon instance: {} - will wait for onAddonInitialize()", e.getMessage());
            }
            
        } catch (ClassNotFoundException e) {
            Marallyzen.LOGGER.debug("PlasmoVoice API classes not found: {}", e.getMessage());
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("Failed to register PlasmoVoice addon: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Creates an addon instance that implements AddonInitializer.
     * Uses ASM to create a real class with @Addon annotation.
     */
    private static Object createAddonInstance() {
        try {
            Class<?> addonInitializerClass = Class.forName("su.plo.voice.api.addon.AddonInitializer");
            Class<?> addonAnnotationClass = Class.forName("su.plo.voice.api.addon.annotation.Addon");
            Class<?> injectAnnotationClass = Class.forName("su.plo.voice.api.addon.InjectPlasmoVoice");
            Class<?> plasmoVoiceServerClass = Class.forName("su.plo.voice.api.server.PlasmoVoiceServer");
            
            String className = "neutka/marallys/marallyzen/audio/MarallyzenPlasmoVoiceAddonImpl";
            String internalName = className.replace('/', '.');
            
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            
            // Create class: public final class MarallyzenPlasmoVoiceAddonImpl implements AddonInitializer
            cw.visit(Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, className, null, 
                    Type.getInternalName(Object.class), 
                    new String[] { Type.getInternalName(addonInitializerClass) });
            
            // Add @Addon annotation
            AnnotationVisitor addonAnnotation = cw.visitAnnotation(Type.getDescriptor(addonAnnotationClass), true);
            addonAnnotation.visit("id", "pv-addon-marallyzen");
            addonAnnotation.visit("name", "Marallyzen PlasmoVoice Addon");
            addonAnnotation.visit("version", "1.0.0");
            AnnotationVisitor authorsArray = addonAnnotation.visitArray("authors");
            authorsArray.visit(null, "Marallyzen");
            authorsArray.visitEnd();
            addonAnnotation.visitEnd();
            
            // Add field: @InjectPlasmoVoice private PlasmoVoiceServer voiceServer;
            FieldVisitor fieldVisitor = cw.visitField(Opcodes.ACC_PRIVATE, "voiceServer", 
                    Type.getDescriptor(plasmoVoiceServerClass), null, null);
            AnnotationVisitor injectAnnotation = fieldVisitor.visitAnnotation(Type.getDescriptor(injectAnnotationClass), true);
            injectAnnotation.visitEnd();
            fieldVisitor.visitEnd();
            
            // Default constructor
            MethodVisitor constructor = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
            constructor.visitCode();
            constructor.visitVarInsn(Opcodes.ALOAD, 0);
            constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(Object.class), "<init>", "()V", false);
            constructor.visitInsn(Opcodes.RETURN);
            constructor.visitMaxs(1, 1);
            constructor.visitEnd();
            
            // onAddonInitialize() method
            MethodVisitor initMethod = cw.visitMethod(Opcodes.ACC_PUBLIC, "onAddonInitialize", "()V", null, null);
            initMethod.visitCode();
            // Get voiceServer from injected field and set it via static method
            initMethod.visitVarInsn(Opcodes.ALOAD, 0);
            initMethod.visitFieldInsn(Opcodes.GETFIELD, className, "voiceServer", Type.getDescriptor(plasmoVoiceServerClass));
            initMethod.visitMethodInsn(Opcodes.INVOKESTATIC, 
                    Type.getInternalName(MarallyzenPlasmoVoiceAddon.class), 
                    "setVoiceServer", 
                    "(Ljava/lang/Object;)V", false);
            initMethod.visitInsn(Opcodes.RETURN);
            initMethod.visitMaxs(2, 1);
            initMethod.visitEnd();
            
            // onAddonShutdown() method
            MethodVisitor shutdownMethod = cw.visitMethod(Opcodes.ACC_PUBLIC, "onAddonShutdown", "()V", null, null);
            shutdownMethod.visitCode();
            shutdownMethod.visitInsn(Opcodes.ACONST_NULL);
            shutdownMethod.visitMethodInsn(Opcodes.INVOKESTATIC, 
                    Type.getInternalName(MarallyzenPlasmoVoiceAddon.class), 
                    "setVoiceServer", 
                    "(Ljava/lang/Object;)V", false);
            shutdownMethod.visitInsn(Opcodes.RETURN);
            shutdownMethod.visitMaxs(1, 1);
            shutdownMethod.visitEnd();
            
            cw.visitEnd();
            
            // Load the class
            byte[] bytecode = cw.toByteArray();
            DynamicClassLoader classLoader = new DynamicClassLoader(MarallyzenPlasmoVoiceAddon.class.getClassLoader());
            Class<?> addonClass = classLoader.defineClass(internalName, bytecode);
            
            // Create instance
            Constructor<?> addonConstructor = addonClass.getConstructor();
            Object instance = addonConstructor.newInstance();
            
            Marallyzen.LOGGER.info("Created PlasmoVoice addon class via ASM");
            return instance;
            
        } catch (Exception e) {
            Marallyzen.LOGGER.error("Failed to create addon instance via ASM", e);
            return null;
        }
    }
    
    /**
     * Dynamic class loader for generated addon class.
     */
    private static class DynamicClassLoader extends ClassLoader {
        public DynamicClassLoader(ClassLoader parent) {
            super(parent);
        }
        
        public Class<?> defineClass(String name, byte[] bytecode) {
            Class<?> clazz = super.defineClass(name, bytecode, 0, bytecode.length);
            resolveClass(clazz);
            return clazz;
        }
    }
    
    /**
     * Gets the PlasmoVoiceServer instance.
     * Returns null if addon is not initialized or server is not available.
     */
    public static Object getVoiceServer() {
        // If addon is initialized but voiceServer is null, try to get it again
        if (addonInitialized && voiceServer == null) {
            try {
                Class<?> plasmoVoiceServerClass = Class.forName("su.plo.voice.api.server.PlasmoVoiceServer");
                voiceServer = plasmoVoiceServerClass.getMethod("get").invoke(null);
                if (voiceServer != null) {
                    Marallyzen.LOGGER.info("PlasmoVoiceServer obtained via get()");
                }
            } catch (Exception e) {
                Marallyzen.LOGGER.debug("Failed to get PlasmoVoiceServer: {}", e.getMessage());
            }
        }
        return voiceServer;
    }
    
    /**
     * Checks if the addon is initialized.
     */
    public static boolean isAddonInitialized() {
        return addonInitialized;
    }
    
    /**
     * Sets the voice server instance (called via reflection from addon initialization).
     */
    public static void setVoiceServer(Object server) {
        voiceServer = server;
        addonInitialized = (server != null);
        if (server != null) {
            Marallyzen.LOGGER.info("PlasmoVoiceServer instance set via addon injection (from onAddonInitialize)");
        } else {
            Marallyzen.LOGGER.debug("PlasmoVoiceServer set to null (addon shutdown or not initialized)");
        }
    }
}

