public class TerrainAutoTiler {

    public static void buildBasicTerrain(BiomeMap biomeMap,
                                         MapLayer ground,
                                         MapLayer decor,
                                         TileManager tileM) {

        int rows = biomeMap.rows;
        int cols = biomeMap.cols;

        tileM.clearLayer(ground);
        tileM.clearLayer(decor);

        // base ground
        tileM.fillRectangleWithNamedTile(ground, "basic_terrain", "DIRT_FILL", 0, 0, cols, rows);

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {

                BiomeType biome = biomeMap.get(col, row);

                switch (biome) {
                    case GRASS:
                        paintCell(biomeMap, ground, decor, tileM, col, row, BiomeType.GRASS, "GRASS", "DIRT");
                        break;

                    case DIRT:
                        paintCell(biomeMap, ground, decor, tileM, col, row, BiomeType.DIRT, "DIRT", "GRASS");
                        break;

                    case SAND:
                        paintCell(biomeMap, ground, decor, tileM, col, row, BiomeType.SAND, "SAND", "DIRT");
                        break;

                    case SNOW:
                        paintCell(biomeMap, ground, decor, tileM, col, row, BiomeType.SNOW, "SNOW", "DIRT");
                        break;

                    case WATER:
                        paintCell(biomeMap, ground, decor, tileM, col, row, BiomeType.WATER, "GRASS", "DIRT");
                        break;

                    case MOUNTAIN:
                        tileM.setLayerTile(ground, col, row,
                                tileM.getManifest("basic_terrain").getRequiredTileId("DIRT_FILL"));
                        tileM.setLayerTile(decor, col, row, -1);
                        break;

                    default:
                        break;
                }
            }
        }
    }

    public static void buildWaterTerrain(BiomeMap biomeMap,
                                         MapLayer waterDecor,
                                         TileManager tileM) {

        tileM.clearLayer(waterDecor);

        int rows = biomeMap.rows;
        int cols = biomeMap.cols;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {

                if (biomeMap.get(col, row) != BiomeType.WATER) continue;

                boolean up    = biomeMap.get(col, row - 1) == BiomeType.WATER;
                boolean down  = biomeMap.get(col, row + 1) == BiomeType.WATER;
                boolean left  = biomeMap.get(col - 1, row) == BiomeType.WATER;
                boolean right = biomeMap.get(col + 1, row) == BiomeType.WATER;

                String tileName = resolveWater(up, down, left, right);

                int tileId = tileM.getAtlasTileId("water", tileName);
                if (tileId >= 0) {
                    tileM.setLayerTile(waterDecor, col, row, tileId);
                }
            }
        }
    }

    private static void paintCell(BiomeMap biomeMap,
                                 MapLayer ground,
                                 MapLayer decor,
                                 TileManager tileM,
                                 int col, int row,
                                 BiomeType type,
                                 String top,
                                 String bottom) {

        AtlasManifest m = tileM.getManifest("basic_terrain");

        boolean up    = biomeMap.get(col, row - 1) == type;
        boolean down  = biomeMap.get(col, row + 1) == type;
        boolean left  = biomeMap.get(col - 1, row) == type;
        boolean right = biomeMap.get(col + 1, row) == type;

        tileM.setLayerTile(ground, col, row,
                m.getRequiredTileId(bottom + "_FILL"));

        if (up && down && left && right) {
            if (m.hasTile(top + "_FILL")) {
                tileM.setLayerTile(decor, col, row,
                        m.getRequiredTileId(top + "_FILL"));
            }
            return;
        }

        String suffix = resolveBasic(up, down, left, right);
        String key = top + "_" + suffix;

        if (m.hasTile(key)) {
            tileM.setLayerTile(decor, col, row,
                    m.getRequiredTileId(key));
        }
    }

    private static String resolveBasic(boolean up, boolean down, boolean left, boolean right) {
        if (!up && !left) return "TL";
        if (!up && !right) return "TR";
        if (!down && !left) return "BL";
        if (!down && !right) return "BR";

        if (!up) return "T";
        if (!down) return "B";
        if (!left) return "L";
        if (!right) return "R";

        return "FILL";
    }

    private static String resolveWater(boolean up, boolean down, boolean left, boolean right) {
        if (!up && !left) return "GWATER_TL";
        if (!up && !right) return "GWATER_TR";
        if (!down && !left) return "GWATER_BL";
        if (!down && !right) return "GWATER_BR";

        if (!up) return "GWATER_T";
        if (!down) return "GWATER_B";
        if (!left) return "GWATER_L";
        if (!right) return "GWATER_R";

        return "GWATER_FILL";
    }
}