public class RegionTemplate {
    public RegionType type;

    public BiomeType mainBiome;
    public BiomeType secondaryBiome;
    public BiomeType waterBiome;
    public BiomeType borderBiome;

    public boolean hasWater;
    public boolean hasMainPath;

    public int entryCol;
    public int entryRow;

    public int exitCol;
    public int exitRow;

    public RegionTemplate(RegionType type) {
        this.type = type;
    }

    public static RegionTemplate createPlainsTemplate(int cols, int rows) {
        RegionTemplate t = new RegionTemplate(RegionType.PLAINS);

        t.mainBiome = BiomeType.GRASS;
        t.secondaryBiome = BiomeType.DIRT;
        t.waterBiome = BiomeType.WATER;
        t.borderBiome = BiomeType.MOUNTAIN;

        t.hasWater = true;
        t.hasMainPath = true;

        t.entryCol = 2;
        t.entryRow = rows / 2;

        t.exitCol = cols - 3;
        t.exitRow = rows / 2;

        return t;
    }

    public static RegionTemplate createCoastTemplate(int cols, int rows) {
        RegionTemplate t = new RegionTemplate(RegionType.COAST);

        t.mainBiome = BiomeType.GRASS;
        t.secondaryBiome = BiomeType.SAND;
        t.waterBiome = BiomeType.WATER;
        t.borderBiome = BiomeType.MOUNTAIN;

        t.hasWater = true;
        t.hasMainPath = true;

        t.entryCol = 2;
        t.entryRow = rows - 5;

        t.exitCol = cols - 3;
        t.exitRow = 4;

        return t;
    }

    public static RegionTemplate createSnowTemplate(int cols, int rows) {
        RegionTemplate t = new RegionTemplate(RegionType.SNOWFIELD);

        t.mainBiome = BiomeType.SNOW;
        t.secondaryBiome = BiomeType.DIRT;
        t.waterBiome = BiomeType.WATER;
        t.borderBiome = BiomeType.MOUNTAIN;

        t.hasWater = false;
        t.hasMainPath = true;

        t.entryCol = 2;
        t.entryRow = rows / 2;

        t.exitCol = cols - 3;
        t.exitRow = rows / 2;

        return t;
    }

    public static RegionTemplate createDesertTemplate(int cols, int rows) {
        RegionTemplate t = new RegionTemplate(RegionType.DESERT);

        t.mainBiome = BiomeType.SAND;
        t.secondaryBiome = BiomeType.DIRT;
        t.waterBiome = BiomeType.WATER;
        t.borderBiome = BiomeType.MOUNTAIN;

        t.hasWater = true;
        t.hasMainPath = true;

        t.entryCol = 2;
        t.entryRow = rows / 2;

        t.exitCol = cols - 3;
        t.exitRow = rows / 2;

        return t;
    }
}
