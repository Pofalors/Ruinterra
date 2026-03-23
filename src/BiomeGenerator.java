import java.util.Random;

public class BiomeGenerator {
    private Random random = new Random();

    public BiomeMap generateFromTemplate(int cols, int rows, RegionTemplate template) {
        BiomeMap map = new BiomeMap(cols, rows);

        // 1. Βάση όλου του χάρτη
        map.fill(template.mainBiome);

        // 2. Περιμετρικό border
        for (int col = 0; col < cols; col++) {
            map.set(col, 0, template.borderBiome);
            map.set(col, rows - 1, template.borderBiome);
        }

        for (int row = 0; row < rows; row++) {
            map.set(0, row, template.borderBiome);
            map.set(cols - 1, row, template.borderBiome);
        }

        // 3. Secondary biome patches
        addPatch(map, template.secondaryBiome, 3, 3, 10, 6);
        addPatch(map, template.secondaryBiome, cols - 13, 3, 10, 6);

        if (rows > 16) {
            addPatch(map, template.secondaryBiome, 3, rows - 8, 10, 5);
        }

        // 4. Water patch
        if (template.hasWater) {
            int waterCol = cols - 13;
            int waterRow = rows - 8;

            if (template.type == RegionType.COAST) {
                waterCol = cols - 11;
                waterRow = rows / 2;
                addPatch(map, template.waterBiome, waterCol, waterRow, 8, rows / 2 - 2);
            } else {
                addPatch(map, template.waterBiome, waterCol, waterRow, 8, 5);
            }
        }

        // 5. Main path
        if (template.hasMainPath) {
            carveMainPath(map, template.entryCol, template.entryRow, template.exitCol, template.exitRow);
        }

        // 6. Small random sprinkles
        sprinkleBiome(map, template.secondaryBiome, 10);
        if (template.hasWater && template.type != RegionType.COAST) {
            sprinkleBiome(map, template.waterBiome, 4);
        }

        return map;
    }

    private void addPatch(BiomeMap map, BiomeType biome, int startCol, int startRow, int width, int height) {
        map.fillRect(startCol, startRow, width, height, biome);
    }

    private void sprinkleBiome(BiomeMap map, BiomeType biome, int count) {
        for (int i = 0; i < count; i++) {
            int col = 2 + random.nextInt(Math.max(1, map.cols - 4));
            int row = 2 + random.nextInt(Math.max(1, map.rows - 4));

            if (map.get(col, row) == BiomeType.GRASS || map.get(col, row) == BiomeType.DIRT || map.get(col, row) == BiomeType.SAND || map.get(col, row) == BiomeType.SNOW) {
                map.set(col, row, biome);
            }
        }
    }

    private void carveMainPath(BiomeMap map, int entryCol, int entryRow, int exitCol, int exitRow) {
        int col = entryCol;
        int row = entryRow;

        map.set(col, row, BiomeType.DIRT);

        while (col != exitCol) {
            if (col < exitCol) col++;
            else col--;

            carvePathCell(map, col, row);
        }

        while (row != exitRow) {
            if (row < exitRow) row++;
            else row--;

            carvePathCell(map, col, row);
        }
    }

    private void carvePathCell(BiomeMap map, int col, int row) {
        for (int r = row - 1; r <= row + 1; r++) {
            for (int c = col - 1; c <= col + 1; c++) {
                if (map.isValid(c, r) && map.get(c, r) != BiomeType.MOUNTAIN && map.get(c, r) != BiomeType.WATER) {
                    map.set(c, r, BiomeType.DIRT);
                }
            }
        }
    }
}
