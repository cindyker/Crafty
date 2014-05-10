package net.kingdomsofarden.crafty.api.items;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.kingdomsofarden.crafty.internals.NBTUtil;

import org.bukkit.inventory.ItemStack;

public final class ModuleRegistrar {

    private Map<UUID, Class<? extends Module>> idToClassMap;
    private Map<UUID, String> idToNameMap;
    private Map<String, UUID> nameToIdMap;
    
    public ModuleRegistrar() {
        this.idToClassMap = new HashMap<UUID, Class<? extends Module>>();
        this.idToNameMap = new HashMap<UUID, String>();
        this.nameToIdMap = new HashMap<String, UUID>();
    }

    /**
     * Registers a module with this registrar, allowing for retrieval/saving of this data to an item
     * @param name The name of the module
     * @param id A {@link UUID} representing this module
     * @param moduleClazz The class of the Module to register
     * @return whether registration was successful
     */
    public boolean registerModule(String name, UUID id, Class<? extends Module> moduleClazz) {
        if (idToClassMap.containsKey(id)) {
            if (idToClassMap.get(id).getClass().getName().equals(moduleClazz.getName())) {
                UUID nameMapping = nameToIdMap.get(name);
                if (nameMapping != null && nameMapping.equals(id)) {
                    return true; // Duplicate registration of the same class, fail silently
                } else if(nameMapping == null) {
                    nameToIdMap.put(name, id); // Missing Name->ID mapping
                    idToNameMap.put(id, name);
                    return true; // ID->Class map exists and Name->ID Map now exists, return
                }
            }
            throw new UnsupportedOperationException("An attempt was made to register module " 
                    + moduleClazz.getName() + " with UUID " + id.toString() 
                    + " which duplicates a preexisting registration for " 
                    + idToClassMap.get(id).getClass().getName());
        }
        if (nameToIdMap.containsKey(name)) {
            if (!nameToIdMap.get(name).equals(id)) {
                throw new UnsupportedOperationException("An attempt was made to register module "
                        + moduleClazz.getName() + " with name " + name
                        + " which duplicates a preexisting registration for "
                        + nameToIdMap.get(id).getClass().getName());
            }
        }
        try {
            moduleClazz.getMethod("deserialize", String.class);
        } catch (NoSuchMethodException e) {
            throw new UnsupportedOperationException("An attempt was made to register module " 
                    + moduleClazz.getName() + " which does not implement the required method "
                    + "public static Module deserialize(String string) ");
        } catch (Exception e) {
            throw new RuntimeException("An unknown error occured when attempting to check for "
                    + "the presence of a deserialization method in "
                    + moduleClazz.getName() , e);
        }
        idToClassMap.put(id, moduleClazz);
        nameToIdMap.put(name, id);
        idToNameMap.put(id, name);
        return true;
    }
    
    /**
     * Creates a module instance from the provided item - slightly slower than loading a module
     * by UUID
     *  
     * @param name Name of the module to load
     * @param item The ItemStack to load the module's data from
     * @return The loaded module, or null if for some reason the module failed to load or does not exist
     */
    public Module getModule(String name, ItemStack item) {
        UUID id = nameToIdMap.get(name);
        return getModule(idToClassMap.get(id), name, id, item);
    }
    
    /**
     * Creates a module instance from the provided item 
     * @param id The UUID of the module to load
     * @param item The ItemStack to load the module's data from
     * @return The loaded module, or null if for some reason the module failed to load or does not exist
     */
    public Module getModule(UUID id, ItemStack item) {
        return getModule(idToClassMap.get(id), idToNameMap.get(id), id, item);
    }
    
    /**
     * Allows for faster lookup of the ID of a registered module by name compared to getting the whole module
     * @param name The name to look up
     * @return The ID of the module, or null if the module does not exist
     */
    public UUID getModuleUuid(String name) {
        return this.nameToIdMap.get(name);
    }
    
    /**
     * Allows for faster lookup of the name of a registered module by id compared to getting the whole module
     * @param id The id to look up
     * @return The name of the module, or null if the module does not exist
     */
    public String getModuleName(UUID id) {
        return this.idToNameMap.get(id);
    }
    
    private <T extends Module> T getModule(Class<? extends Module> clazz, String name, UUID id, ItemStack item) {
        if(clazz == null || name == null || id == null || item == null) {
            return null;
        }
        try {
            Method m = clazz.getMethod("deserialize", String.class);
            String data = NBTUtil.getData(id, item);
            @SuppressWarnings("unchecked")
            T mod = (T) m.invoke(null, data);
            if(mod == null) {
                return null;
            }
            Field uuidField = clazz.getField("identifier");
            uuidField.setAccessible(true);
            uuidField.set(mod, id);
            Field nameField = clazz.getField("name");
            nameField.setAccessible(true);
            nameField.set(mod, name);
            return mod;
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchFieldException e) {
            e.printStackTrace();
            return null;
        } 
    }
    
}
