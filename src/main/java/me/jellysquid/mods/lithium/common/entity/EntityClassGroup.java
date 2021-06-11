package me.jellysquid.mods.lithium.common.entity;

import it.unimi.dsi.fastutil.objects.Reference2ByteOpenHashMap;
import lombok.extern.log4j.Log4j2;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.minecart.MinecartEntity;
import net.minecraft.entity.monster.ShulkerEntity;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * Class for grouping Entity classes by some property for use in TypeFilterableList
 * It is intended that an EntityClassGroup acts as if it was immutable, however we cannot predict which subclasses of
 * Entity might appear. Therefore we evaluate whether a class belongs to the class group when it is first seen.
 * Once a class was evaluated the result of it is cached and cannot be changed.
 *
 * @author 2No2Name
 */
 @Log4j2
public class EntityClassGroup {
    public static final EntityClassGroup BOAT_SHULKER_LIKE_COLLISION; //aka entities that other entities will do block-like collisions with when moving
    public static final EntityClassGroup MINECART_BOAT_LIKE_COLLISION; //aka entities that will attempt to collide with all other entities when moving

    static {
        //String remapped_method_30948 = FabricLoader.getInstance().getMappingResolver().mapMethodName("intermediary", "net.minecraft.class_1297", "method_30948", "()Z");
        String remapped_method_30948 = ObfuscationReflectionHelper.findMethod(Entity.class, "func_241845_aY").getName();
        BOAT_SHULKER_LIKE_COLLISION = new EntityClassGroup(
                (Class<?> entityClass) -> isMethodFromSuperclassOverwritten(entityClass, Entity.class, remapped_method_30948));


        //String remapped_method_30949 = FabricLoader.getInstance().getMappingResolver().mapMethodName("intermediary", "net.minecraft.class_1297", "method_30949", "(Lnet/minecraft/class_1297;)Z");
        String remapped_method_30949 = ObfuscationReflectionHelper.findMethod(Entity.class, "func_241849_j", Entity.class).getName();
        MINECART_BOAT_LIKE_COLLISION = new EntityClassGroup(
                (Class<?> entityClass) -> isMethodFromSuperclassOverwritten(entityClass, Entity.class, remapped_method_30949, Entity.class));

        //sanity check: in case intermediary mappings changed, we fail
        if ((!MINECART_BOAT_LIKE_COLLISION.contains(MinecartEntity.class))) {
            throw new AssertionError();
        }
        if ((!BOAT_SHULKER_LIKE_COLLISION.contains(ShulkerEntity.class))) {
            throw new AssertionError();
        }
        if ((MINECART_BOAT_LIKE_COLLISION.contains(ShulkerEntity.class))) {
            //should not throw an Error here, because another mod *could* add the method to ShulkerEntity. Wwarning when this sanity check fails.
            Logger.getLogger("Lithium EntityClassGroup").warning("Either chunk.entity_class_groups is broken or something else gave Shulkers the minecart-like collision behavior.");
        }
        BOAT_SHULKER_LIKE_COLLISION.clear();
        MINECART_BOAT_LIKE_COLLISION.clear();
    }

    private final Predicate<Class<?>> classFitEvaluator;
    private volatile Reference2ByteOpenHashMap<Class<?>> class2GroupContains;

    public EntityClassGroup(Predicate<Class<?>> classFitEvaluator) {
        this.class2GroupContains = new Reference2ByteOpenHashMap<>();
        Objects.requireNonNull(classFitEvaluator);
        this.classFitEvaluator = classFitEvaluator;
    }

    public void clear() {
        this.class2GroupContains = new Reference2ByteOpenHashMap<>();
    }

    public boolean contains(Class<?> entityClass) {
        byte contains = this.class2GroupContains.getOrDefault(entityClass, (byte) 2);
        if (contains != 2) {
            return contains == 1;
        } else {
            return this.testAndAddClass(entityClass);
        }
    }

    private boolean testAndAddClass(Class<?> entityClass) {
        byte contains;
        //synchronizing here to avoid multiple threads replacing the map at the same time, and therefore possibly undoing progress
        //it could also be fixed by using an AtomicReference's CAS, but we are writing very rarely (less than 150 times for the total game runtime in vanilla)
        synchronized (this) {
            //test the same condition again after synchronizing, as the collection might have been updated while this thread blocked
            contains = this.class2GroupContains.getOrDefault(entityClass, (byte) 2);
            if (contains != 2) {
                return contains == 1;
            }
            //construct new map instead of updating the old map to avoid thread safety problems
            //the map is not modified after publication
            Reference2ByteOpenHashMap<Class<?>> newMap = this.class2GroupContains.clone();
            contains = this.classFitEvaluator.test(entityClass) ? (byte) 1 : (byte) 0;
            newMap.put(entityClass, contains);
            //publish the new map in a volatile field, so that all threads reading after this write can also see all changes to the map done before the write
            this.class2GroupContains = newMap;
        }
        return contains == 1;
    }

    public static boolean isMethodFromSuperclassOverwritten(Class<?> clazz, Class<?> superclass, String methodName, Class<?>... methodArgs) {
        while (clazz != null && clazz != superclass && superclass.isAssignableFrom(clazz)) {
            try {
                clazz.getDeclaredMethod(methodName, methodArgs);
                return true;
            } catch (NoSuchMethodException e) {
                clazz = clazz.getSuperclass();
            } catch (Exception ex) {
                log.warn("Failed to load class={} while looking for superclass overrides on {}", clazz.getName(), superclass.getName());
                return false;
            }
        }
        return false;
    }
}