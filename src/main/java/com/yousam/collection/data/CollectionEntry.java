package com.yousam.collection.data;

import net.minecraft.resources.Identifier;

public record CollectionEntry(
        Identifier id,
        Identifier itemId,
        int requiredCount,
        CollectionCategory category,
        int order,
        String componentKey,
        String componentValue
) {}