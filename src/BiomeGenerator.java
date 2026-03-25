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
        placePointsOfInterest(map, template);
        connectPointsOfInterest(map, template);

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

    private void placePointsOfInterest(BiomeMap map, RegionTemplate template) {
        // Town κοντά στη δεξιά πλευρά, πάνω στο main land
        int townCol = map.cols - 8;
        int townRow = map.rows / 2;

        if (map.isValid(townCol, townRow)) {
            map.addPOI(PointOfInterestType.TOWN_ENTRY, townCol, townRow);
        }

        // Cave πάνω δεξιά
        int caveCol = map.cols - 7;
        int caveRow = 4;

        if (map.isValid(caveCol, caveRow)) {
            map.addPOI(PointOfInterestType.CAVE_ENTRY, caveCol, caveRow);
        }

        // Sign κοντά στο κέντρο του δρόμου
        int signCol = map.cols / 2;
        int signRow = map.rows / 2;

        if (map.isValid(signCol, signRow)) {
            map.addPOI(PointOfInterestType.SIGN_POST, signCol, signRow);
        }

        // Chest κάτω αριστερά αλλά όχι πάνω στο border
        int chestCol = 5;
        int chestRow = map.rows - 6;

        if (map.isValid(chestCol, chestRow)) {
            map.addPOI(PointOfInterestType.CHEST_SPOT, chestCol, chestRow);
        }

        // Lake marker στο πρώτο water cell
        for (int row = 1; row < map.rows - 1; row++) {
            for (int col = 1; col < map.cols - 1; col++) {
                if (map.get(col, row) == BiomeType.WATER) {
                    map.addPOI(PointOfInterestType.LAKE_MARKER, col, row);
                    return;
                }
            }
        }
    }

    private void connectPointsOfInterest(BiomeMap map, RegionTemplate template) {
        PointOfInterest town = findPOI(map, PointOfInterestType.TOWN_ENTRY);
        PointOfInterest cave = findPOI(map, PointOfInterestType.CAVE_ENTRY);
        PointOfInterest chest = findPOI(map, PointOfInterestType.CHEST_SPOT);

        // entry -> town
        if (town != null) {
            carveRoad(map, template.entryCol, template.entryRow, town.col, town.row);
        }

        // town -> cave
        if (town != null && cave != null) {
            carveRoad(map, town.col, town.row, cave.col, cave.row);
        }

        // town -> chest
        if (town != null && chest != null) {
            carveRoad(map, town.col, town.row, chest.col, chest.row);
        }
    }

    private PointOfInterest findPOI(BiomeMap map, PointOfInterestType type) {
        for (PointOfInterest poi : map.pointsOfInterest) {
            if (poi.type == type) return poi;
        }
        return null;
    }

    private void carveRoad(BiomeMap map, int startCol, int startRow, int endCol, int endRow) {
        int col = startCol;
        int row = startRow;

        while (col != endCol) {
            if (col < endCol) col++;
            else col--;
            carveRoadCell(map, col, row);
        }

        while (row != endRow) {
            if (row < endRow) row++;
            else row--;
            carveRoadCell(map, col, row);
        }
    }

    private void carveRoadCell(BiomeMap map, int col, int row) {
        for (int r = row - 1; r <= row + 1; r++) {
            for (int c = col - 1; c <= col + 1; c++) {
                if (!map.isValid(c, r)) continue;

                BiomeType current = map.get(c, r);
                if (current == BiomeType.MOUNTAIN || current == BiomeType.WATER) continue;

                map.set(c, r, BiomeType.DIRT);
            }
        }
    }
}
