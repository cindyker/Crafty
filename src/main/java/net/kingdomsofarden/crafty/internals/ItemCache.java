package net.kingdomsofarden.crafty.internals;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.*;

import net.kingdomsofarden.crafty.Crafty;
import net.kingdomsofarden.crafty.api.CraftyItem;


public class ItemCache {
    
    public class CacheRemovalListener implements RemovalListener<CacheKey, CraftyItem> {

        @Override
        public void onRemoval(RemovalNotification<CacheKey, CraftyItem> removed) {
            removed.getValue().updateItem();
        }

    }

    private LoadingCache<CacheKey, CraftyItem> cache;
    private Crafty plugin;
    
    public ItemCache(Crafty itemApiPlugin) {
        this.plugin = itemApiPlugin;
        this.cache = CacheBuilder.newBuilder()
                .concurrencyLevel(1)
                .expireAfterAccess(15,TimeUnit.MINUTES)
                .removalListener(new CacheRemovalListener())
                .build(new CacheLoader<CacheKey, CraftyItem>() {

                    @Override
                    public CraftyItem load(CacheKey obj) {
                        return new CraftyItem(obj, plugin); 
                    }
                });
    }
    
    public CraftyItem get(CacheKey key) throws ExecutionException {
        if (this.cache.asMap().containsKey(key)) {
            if (this.cache.get(key).getItem() != key.getItem()) {
                this.cache.invalidate(key);
            }
        }
        return this.cache.get(key);
        
    }
    
    public LoadingCache<CacheKey,CraftyItem> get() {
        return this.cache;
    }
}
