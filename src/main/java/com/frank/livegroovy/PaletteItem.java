package com.frank.livegroovy;

public record PaletteItem(
    String id,
    String baseItem,
    String javaMaterial,
    int customModelData,
    String javaItemModel,
    String bedrockTexture,
    String bedrockIdentifier
) {}
