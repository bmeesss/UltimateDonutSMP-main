package com.bx.ultimateDonutSmp.managers;

import com.bx.ultimateDonutSmp.UltimateDonutSmp;
import com.bx.ultimateDonutSmp.utils.LegacyMaterialSupport;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class FilterManager {

    private final UltimateDonutSmp plugin;
    private final LinkedHashMap<String, Set<Material>> categories = new LinkedHashMap<>();

    public FilterManager(UltimateDonutSmp plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        categories.clear();
        FileConfiguration config = plugin.getConfigManager().getFilter();
        if (config == null) return;

        for (String key : config.getKeys(false)) {
            List<String> matNames = config.getStringList(key);
            if (matNames == null) continue;
            Set<Material> materials = new LinkedHashSet<>();
            for (String name : matNames) {
                // filter.yml is written against modern (1.13+) material names; on 1.12.2 a bare
                // Material.matchMaterial silently drops most of them, which starves the Orders
                // catalog. Resolve through the central compatibility layer instead: legacy names
                // resolve natively, supported flattened names map to their 1.12.2 equivalent and
                // genuinely unsupported materials are skipped explicitly.
                LegacyMaterialSupport.Icon resolved = LegacyMaterialSupport.resolve(name);
                if (resolved != null) {
                    materials.add(resolved.material());
                }
            }
            categories.put(key, materials);
        }
    }

    public List<String> categoryNames() {
        return new ArrayList<>(categories.keySet());
    }

    public Set<Material> resolve(String category) {
        if (category == null) return Collections.emptySet();
        return categories.getOrDefault(category, Collections.emptySet());
    }
}
