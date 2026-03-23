import java.util.Random;

public class BiomeGenerator {
    private Random random = new Random();

    public BiomeMap generatePlainsTestMap(int cols, int rows) {
        BiomeMap map = new BiomeMap(cols, rows);

        // Βάση: όλο grass
        map.fill(BiomeType.GRASS);

        // Περιμετρικό border = VOID/MOUNTAIN placeholder
        for (int col = 0; col < cols; col++) {
            map.set(col, 0, BiomeType.MOUNTAIN);
            map.set(col, rows - 1, BiomeType.MOUNTAIN);
        }

        for (int row = 0; row < rows; row++) {
            map.set(0, row, BiomeType.MOUNTAIN);
            map.set(cols - 1, row, BiomeType.MOUNTAIN);
        }

        // Dirt περιοχή
        map.fillRect(3, 3, 10, 6, BiomeType.DIRT);

        // Sand περιοχή
        map.fillRect(cols - 13, 3, 10, 6, BiomeType.SAND);

        // Snow περιοχή
        map.fillRect(3, rows - 8, 10, 5, BiomeType.SNOW);

        // Water περιοχή
        map.fillRect(cols - 13, rows - 8, 8, 5, BiomeType.WATER);

        // Μικρές random παραλλαγές
        sprinkleBiome(map, BiomeType.DIRT, 8);
        sprinkleBiome(map, BiomeType.SAND, 6);
        sprinkleBiome(map, BiomeType.SNOW, 6);

        return map;
    }

    private void sprinkleBiome(BiomeMap map, BiomeType biome, int count) {
        for (int i = 0; i < count; i++) {
            int col = 2 + random.nextInt(Math.max(1, map.cols - 4));
            int row = 2 + random.nextInt(Math.max(1, map.rows - 4));

            if (map.get(col, row) == BiomeType.GRASS) {
                map.set(col, row, biome);
            }
        }
    }
}
