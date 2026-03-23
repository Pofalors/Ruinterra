import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;


public class TileManager {
    GamePanel gp;
    public Tile[] tile;

    ArrayList<String> fileNames = new ArrayList<>();
    ArrayList<String> collisionStatus = new ArrayList<>();

    // ΝΕΟ: όλα τα maps, legacy + advanced
    public ArrayList<AdvancedMapData> maps = new ArrayList<>();
    public Map<String, TilesetAtlas> atlases = new HashMap<>();
    public Map<String, AtlasManifest> manifests = new HashMap<>();

    public AtlasManifest getManifest(String atlasName) {
        return manifests.get(atlasName);
    }

    public TileManager(GamePanel gp) {
        this.gp = gp;

        loadTileData();

        tile = new Tile[fileNames.size()];
        getTileImage();

        loadAtlases();
        loadManifests();

        loadAllMaps();

        gp.maxMaps = maps.size();

        if (!maps.isEmpty()) {
            applyMapSizeToGamePanel(0);
        }
    }

    // =========================================================
    // TILE DATA
    // =========================================================
    private void loadTileData() {
        try {
            File file = new File("res/maps/tiledata.txt");
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);

            String line;
            while ((line = br.readLine()) != null) {
                fileNames.add(line);
                collisionStatus.add(br.readLine());
            }

            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void getTileImage() {
        for (int i = 0; i < fileNames.size(); i++) {
            String fileName = fileNames.get(i);
            boolean collision = collisionStatus.get(i).equals("true");
            setup(i, fileName, collision);
        }
    }

    public void loadAtlases() {
        loadAtlas("basic_terrain", "res/tilesets/basic_terrain.png");
        loadAtlas("cave", "res/tilesets/cave.png");
        loadAtlas("mountains", "res/tilesets/mountains.png");
        loadAtlas("mountains2", "res/tilesets/mountains2.png");
        loadAtlas("other", "res/tilesets/other.png");
        loadAtlas("water", "res/tilesets/water.png");
        loadAtlas("water_animated", "res/tilesets/water_animated.png");
    }

    public void loadAtlas(String atlasName, String filePath) {
        try {
            BufferedImage atlasImage = ImageIO.read(new File(filePath));
            TilesetAtlas atlas = new TilesetAtlas(atlasName, atlasImage, gp.originalTileSize);
            atlases.put(atlasName, atlas);
            System.out.println("Atlas loaded: " + atlasName + " (" + atlas.columns + "x" + atlas.rows + ")");
        } catch (IOException e) {
            System.out.println("Could not load atlas: " + atlasName + " from " + filePath);
            e.printStackTrace();
        }
    }

    public void loadManifests() {
        loadManifest("basic_terrain", "res/tilesets/basic_terrain_manifest.txt");
        loadManifest("water", "res/tilesets/water_manifest.txt");
    }

    public void loadManifest(String atlasName, String filePath) {
        AtlasManifest manifest = AtlasManifestLoader.loadManifest(atlasName, filePath);
        if (manifest != null) {
            manifests.put(atlasName, manifest);
            System.out.println("Manifest loaded: " + atlasName);
        } else {
            System.out.println("Could not load manifest: " + atlasName);
        }
    }

    public TilesetAtlas getAtlas(String atlasName) {
        return atlases.get(atlasName);
    }

    public void setup(int index, String imageName, boolean collision) {
        try {
            tile[index] = new Tile();
            tile[index].image = ImageIO.read(new File("res/tiles/" + imageName));
            tile[index].collision = collision;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // =========================================================
    // MAP LOADING
    // =========================================================
    private void loadAllMaps() {
        // =========================
        // LEGACY MAPS (τα παλιά σου)
        // =========================
        addLegacyMap("Overworld", "res/maps/worldmap.txt");
        addLegacyMap("Dungeon01", "res/maps/dungeon01.txt");
        addLegacyMap("MerchantHouse", "res/maps/interior01.txt");
        addLegacyMap("Town", "res/maps/town.txt");
        addLegacyMap("BigHouse", "res/maps/interior01.txt");
        addLegacyMap("SmallHouse", "res/maps/interior01.txt");

        // ======================================================
        // ΑΡΓΟΤΕΡΑ ΕΔΩ θα μπουν advanced maps / atlas maps
        // π.χ. addAdvancedMap(...)
        // ======================================================
        addAdvancedMapFromFile("res/maps/test_region_01.frmap");
        addAdvancedMapFromFile("res/maps/basic_terrain_index_test.frmap");

        addWaterManifestShowcaseMap();
        addGeneratedBiomeMap();
    }

    public void addLegacyMap(String name, String filePath) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(filePath));

            ArrayList<String[]> rowsList = new ArrayList<>();
            String line;

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                rowsList.add(line.split(" "));
            }

            br.close();

            if (rowsList.isEmpty()) {
                System.out.println("Κενός χάρτης: " + filePath);
                return;
            }

            int rows = rowsList.size();
            int cols = rowsList.get(0).length;

            AdvancedMapData map = new AdvancedMapData(name, cols, rows);
            map.legacy = true;

            for (int row = 0; row < rows; row++) {
                String[] numbers = rowsList.get(row);

                for (int col = 0; col < cols; col++) {
                    if (col < numbers.length) {
                        map.legacyTiles[row][col] = Integer.parseInt(numbers[col]);
                    } else {
                        map.legacyTiles[row][col] = 0;
                    }
                }
            }

            maps.add(map);
            System.out.println("Legacy map loaded: " + name + " (" + cols + "x" + rows + ")");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Σφάλμα φόρτωσης legacy χάρτη: " + filePath);
        }
    }

    public void addAdvancedMapFromFile(String metadataPath) {
        AdvancedMapData map = AdvancedMapLoader.loadAdvancedMap(metadataPath);
        if (map != null) {
            map.legacy = false;
            maps.add(map);
            System.out.println("Advanced map loaded: " + map.name + " (" + map.cols + "x" + map.rows + ")");
        }
    }

    public void addWaterManifestShowcaseMap() {
        AdvancedMapData map = new AdvancedMapData("WaterManifestShowcase", 36, 36);
        map.legacy = false;
        map.tilesetName = "water";

        MapLayer ground = new MapLayer("ground", 36, 36);
        ground.atlasName = "basic_terrain";

        MapLayer decor = new MapLayer("decor", 36, 36);
        decor.atlasName = "water";

        MapLayer collision = new MapLayer("collision", 36, 36);
        collision.atlasName = "";

        clearLayer(ground);
        clearLayer(decor);

        for (int row = 0; row < collision.rows; row++) {
            for (int col = 0; col < collision.cols; col++) {
                collision.tiles[row][col] = 0;
            }
        }

        // Βάση γης
        fillRectangleWithNamedTile(ground, "basic_terrain", "DIRT_FILL", 0, 0, map.cols, map.rows);

        // -------------------------------------------------
        // 1) GRASS/WATER family
        // -------------------------------------------------
        placeTerrain3x3(decor, "water", "GWATER", 2, 2);
        placeTerrain3x3(decor, "water", "WGRASS", 6, 2);

        // -------------------------------------------------
        // 2) SAND/WATER family
        // -------------------------------------------------
        placeTerrain3x3(decor, "water", "SWATER", 2, 6);
        placeTerrain3x3(decor, "water", "WSAND", 6, 6);

        // -------------------------------------------------
        // 3) SNOW/WATER family
        // -------------------------------------------------
        placeTerrain3x3(decor, "water", "SNWATER", 2, 10);
        placeTerrain3x3(decor, "water", "WSNOW", 6, 10);

        AtlasManifest waterManifest = getManifest("water");
        if (waterManifest == null) {
            System.out.println("No water manifest loaded.");
            return;
        }

        // -------------------------------------------------
        // 4) Waves row
        // -------------------------------------------------
        if (waterManifest.hasTile("WAVES_1")) setLayerTile(decor, 14, 2, waterManifest.getRequiredTileId("WAVES_1"));
        if (waterManifest.hasTile("WAVES_2")) setLayerTile(decor, 15, 2, waterManifest.getRequiredTileId("WAVES_2"));
        if (waterManifest.hasTile("WAVES_3")) setLayerTile(decor, 16, 2, waterManifest.getRequiredTileId("WAVES_3"));
        if (waterManifest.hasTile("WAVES_4")) setLayerTile(decor, 17, 2, waterManifest.getRequiredTileId("WAVES_4"));
        if (waterManifest.hasTile("WAVES_5")) setLayerTile(decor, 18, 2, waterManifest.getRequiredTileId("WAVES_5"));

        // Single water tile
        if (waterManifest.hasTile("WATER_TILE")) setLayerTile(decor, 20, 2, waterManifest.getRequiredTileId("WATER_TILE"));

        // Rocks
        if (waterManifest.hasTile("WATER_ROCKS_1")) setLayerTile(decor, 22, 2, waterManifest.getRequiredTileId("WATER_ROCKS_1"));
        if (waterManifest.hasTile("WATER_ROCKS_2")) setLayerTile(decor, 23, 2, waterManifest.getRequiredTileId("WATER_ROCKS_2"));

        // -------------------------------------------------
        // 5) Water rock path 2x2
        // -------------------------------------------------
        placeVariant2x2(decor, "water", "WATER_ROCK_PATH", 14, 6);

        // 6) Water snow path 2x2
        placeVariant2x2(decor, "water", "WATER_SNOW_PATH", 18, 6);

        // -------------------------------------------------
        // 7) Big waterfall 4x5
        // -------------------------------------------------
        placeExplicitBlock4x5(decor, "water",
                new String[][]{
                        {"WATERFALL_BIG_TLL", "WATERFALL_BIG_TL", "WATERFALL_BIG_TR", "WATERFALL_BIG_TRR"},
                        {"WATERFALL_BIG_MTLL", "WATERFALL_BIG_MTL", "WATERFALL_BIG_MTR", "WATERFALL_BIG_MTRR"},
                        {"WATERFALL_BIG_MLL", "WATERFALL_BIG_ML", "WATERFALL_BIG_MR", "WATERFALL_BIG_MRR"},
                        {"WATERFALL_BIG_MBLL", "WATERFALL_BIG_MBL", "WATERFALL_BIG_MBR", "WATERFALL_BIG_MBRR"},
                        {"WATERFALL_BIG_BLL", "WATERFALL_BIG_BL", "WATERFALL_BIG_BR", "WATERFALL_BIG_BRR"}
                }, 14, 12);

        // -------------------------------------------------
        // 8) Small waterfall 4x3
        // -------------------------------------------------
        placeExplicitBlock4x3(decor, "water",
                new String[][]{
                        {"WATERFALL_SMALL_TLL", "WATERFALL_SMALL_TL", "WATERFALL_SMALL_TR", "WATERFALL_SMALL_TRR"},
                        {"WATERFALL_SMALL_MLL", "WATERFALL_SMALL_ML", "WATERFALL_SMALL_MR", "WATERFALL_SMALL_MRR"},
                        {"WATERFALL_SMALL_BLL", "WATERFALL_SMALL_BL", "WATERFALL_SMALL_BR", "WATERFALL_SMALL_BRR"}
                }, 20, 12);

        // Border collision μόνο
        for (int col = 0; col < map.cols; col++) {
            collision.tiles[0][col] = 1;
            collision.tiles[map.rows - 1][col] = 1;
        }
        for (int row = 0; row < map.rows; row++) {
            collision.tiles[row][0] = 1;
            collision.tiles[row][map.cols - 1] = 1;
        }

        map.layers.add(ground);
        map.layers.add(decor);
        map.layers.add(collision);

        addAdvancedMap(map);

        System.out.println("Water manifest showcase map added.");
    }

    public void addGeneratedBiomeMap() {
        BiomeGenerator generator = new BiomeGenerator();
        BiomeMap biomeMap = generator.generatePlainsTestMap(36, 24);

        AdvancedMapData map = new AdvancedMapData("GeneratedBiomeMap", biomeMap.cols, biomeMap.rows);
        map.legacy = false;
        map.tilesetName = "basic_terrain";

        MapLayer ground = new MapLayer("ground", biomeMap.rows, biomeMap.cols);
        ground.atlasName = "basic_terrain";

        MapLayer decor = new MapLayer("decor", biomeMap.rows, biomeMap.cols);
        decor.atlasName = "basic_terrain";

        MapLayer waterDecor = new MapLayer("water_decor", biomeMap.rows, biomeMap.cols);
        waterDecor.atlasName = "water";

        MapLayer collision = new MapLayer("collision", biomeMap.rows, biomeMap.cols);
        collision.atlasName = "";

        clearLayer(ground);
        clearLayer(decor);
        clearLayer(waterDecor);

        for (int row = 0; row < collision.rows; row++) {
            for (int col = 0; col < collision.cols; col++) {
                collision.tiles[row][col] = 0;
            }
        }

        // Βάση όλου του χάρτη = dirt κάτω
        fillRectangleWithNamedTile(ground, "basic_terrain", "DIRT_FILL", 0, 0, biomeMap.cols, biomeMap.rows);

        // Πέρνα όλο το biome grid και χτίσε blocks
        for (int row = 1; row < biomeMap.rows - 2; row += 3) {
            for (int col = 1; col < biomeMap.cols - 2; col += 3) {
                BiomeType biome = biomeMap.get(col, row);

                switch (biome) {
                    case GRASS:
                        placeTerrainStack3x3(ground, decor, "basic_terrain", "DIRT", "GRASS", col, row);
                        break;

                    case DIRT:
                        placeTerrainStack3x3(ground, decor, "basic_terrain", "GRASS", "DIRT", col, row);
                        break;

                    case SAND:
                        placeTerrainStack3x3(ground, decor, "basic_terrain", "DIRT", "SAND", col, row);
                        break;

                    case SNOW:
                        placeTerrainStack3x3(ground, decor, "basic_terrain", "DIRT", "SNOW", col, row);
                        break;

                    case WATER:
                        placeTerrainStack3x3(ground, decor, "basic_terrain", "DIRT", "GRASS", col, row);
                        placePaintedWater3x3(waterDecor, "GWATER", col, row);

                        for (int r = row; r < row + 3; r++) {
                            for (int c = col; c < col + 3; c++) {
                                collision.tiles[r][c] = 1;
                            }
                        }
                        break;

                    case MOUNTAIN:
                        fillRectangleWithNamedTile(ground, "basic_terrain", "DIRT_FILL", col, row, 3, 3);

                        for (int r = row; r < row + 3; r++) {
                            for (int c = col; c < col + 3; c++) {
                                collision.tiles[r][c] = 1;
                            }
                        }
                        break;

                    default:
                        break;
                }
            }
        }

        // Border collision
        for (int col = 0; col < biomeMap.cols; col++) {
            collision.tiles[0][col] = 1;
            collision.tiles[biomeMap.rows - 1][col] = 1;
        }
        for (int row = 0; row < biomeMap.rows; row++) {
            collision.tiles[row][0] = 1;
            collision.tiles[row][biomeMap.cols - 1] = 1;
        }

        map.layers.add(ground);
        map.layers.add(decor);
        map.layers.add(waterDecor);
        map.layers.add(collision);

        addAdvancedMap(map);

        System.out.println("Generated biome map added.");
    }

    // =========================================================
    // ADVANCED MAP PLACEHOLDER
    // =========================================================
    public void addAdvancedMap(AdvancedMapData map) {
        if (map != null) {
            map.legacy = false;
            maps.add(map);
        }
    }

    // =========================================================
    // MAP HELPERS
    // =========================================================
    public AdvancedMapData getMap(int mapIndex) {
        if (mapIndex < 0 || mapIndex >= maps.size()) return null;
        return maps.get(mapIndex);
    }

    public AdvancedMapData getCurrentMapData() {
        return getMap(gp.currentMap);
    }

    public void applyMapSizeToGamePanel(int mapIndex) {
        AdvancedMapData map = getMap(mapIndex);
        if (map == null) return;

        gp.maxWorldCol = map.cols;
        gp.maxWorldRow = map.rows;
        gp.worldWidth = gp.tileSize * gp.maxWorldCol;
        gp.worldHeight = gp.tileSize * gp.maxWorldRow;
    }

    public boolean isValidTile(int mapIndex, int row, int col) {
        AdvancedMapData map = getMap(mapIndex);
        if (map == null) return false;

        return row >= 0 && row < map.rows && col >= 0 && col < map.cols;
    }

    public int getTileNum(int mapIndex, int row, int col) {
        AdvancedMapData map = getMap(mapIndex);
        if (map == null) return 0;
        if (!isValidTile(mapIndex, row, col)) return 0;

        if (map.legacy) {
            return map.legacyTiles[row][col];
        }

        // Advanced path - προς το παρόν ground layer fallback
        MapLayer ground = map.getLayer("ground");
        if (ground != null) {
            return ground.tiles[row][col];
        }

        return 0;
    }

    public boolean isTileCollision(int mapIndex, int row, int col) {
        AdvancedMapData map = getMap(mapIndex);
        if (map == null) return false;
        if (!isValidTile(mapIndex, row, col)) return false;

        if (map.legacy) {
            int tileNum = map.legacyTiles[row][col];

            if (tileNum < 0 || tileNum >= tile.length || tile[tileNum] == null) {
                return false;
            }

            return tile[tileNum].collision;
        }

        // Advanced maps: πρώτα δες collision layer
        MapLayer collisionLayer = map.getLayer("collision");
        if (collisionLayer != null) {
            return collisionLayer.tiles[row][col] == 1;
        }

        // fallback: αν δεν υπάρχει collision layer, πάρε από ground tile collision
        int tileNum = getTileNum(mapIndex, row, col);
        if (tileNum < 0 || tileNum >= tile.length || tile[tileNum] == null) {
            return false;
        }

        return tile[tileNum].collision;
    }

    public int getAtlasTileId(String atlasName, String tileName) {
        AtlasManifest manifest = getManifest(atlasName);
        if (manifest == null) return -1;
        return manifest.getTileId(tileName);
    }

    public BufferedImage getAtlasTileImageByName(String atlasName, String tileName) {
        TilesetAtlas atlas = getAtlas(atlasName);
        AtlasManifest manifest = getManifest(atlasName);

        if (atlas == null || manifest == null) return null;

        int tileId = manifest.getTileId(tileName);
        if (tileId < 0) return null;

        return atlas.getTileImage(tileId);
    }

    public void setLayerTile(MapLayer layer, int col, int row, int tileId) {
        if (layer == null) return;
        if (row < 0 || row >= layer.rows || col < 0 || col >= layer.cols) return;
        layer.tiles[row][col] = tileId;
    }

    public void fillLayer(MapLayer layer, int tileId) {
        if (layer == null) return;

        for (int row = 0; row < layer.rows; row++) {
            for (int col = 0; col < layer.cols; col++) {
                layer.tiles[row][col] = tileId;
            }
        }
    }

    public void clearLayer(MapLayer layer) {
        if (layer == null) return;

        for (int row = 0; row < layer.rows; row++) {
            for (int col = 0; col < layer.cols; col++) {
                layer.tiles[row][col] = -1;
            }
        }
    }

    public void placeTerrain3x3(MapLayer layer, String atlasName, String prefix, int startCol, int startRow) {
        AtlasManifest manifest = getManifest(atlasName);
        if (manifest == null) return;
        if (!manifest.hasTerrain3x3(prefix)) return;

        setLayerTile(layer, startCol,     startRow,     manifest.getRequiredTileId(prefix + "_TL"));
        setLayerTile(layer, startCol + 1, startRow,     manifest.getRequiredTileId(prefix + "_T"));
        setLayerTile(layer, startCol + 2, startRow,     manifest.getRequiredTileId(prefix + "_TR"));

        setLayerTile(layer, startCol,     startRow + 1, manifest.getRequiredTileId(prefix + "_L"));
        setLayerTile(layer, startCol + 1, startRow + 1, manifest.getRequiredTileId(prefix + "_FILL"));
        setLayerTile(layer, startCol + 2, startRow + 1, manifest.getRequiredTileId(prefix + "_R"));

        setLayerTile(layer, startCol,     startRow + 2, manifest.getRequiredTileId(prefix + "_BL"));
        setLayerTile(layer, startCol + 1, startRow + 2, manifest.getRequiredTileId(prefix + "_B"));
        setLayerTile(layer, startCol + 2, startRow + 2, manifest.getRequiredTileId(prefix + "_BR"));
    }

    public void placeBottom3x3(MapLayer layer, String atlasName, String prefix, int startCol, int startRow) {
        AtlasManifest manifest = getManifest(atlasName);
        if (manifest == null) return;
        if (!manifest.hasBottom3x3(prefix)) return;

        setLayerTile(layer, startCol,     startRow,     manifest.getRequiredTileId(prefix + "_BTL"));
        setLayerTile(layer, startCol + 1, startRow,     manifest.getRequiredTileId(prefix + "_BT"));
        setLayerTile(layer, startCol + 2, startRow,     manifest.getRequiredTileId(prefix + "_BTR"));

        setLayerTile(layer, startCol,     startRow + 1, manifest.getRequiredTileId(prefix + "_BML"));
        setLayerTile(layer, startCol + 1, startRow + 1, manifest.getRequiredTileId(prefix + "_FILL"));
        setLayerTile(layer, startCol + 2, startRow + 1, manifest.getRequiredTileId(prefix + "_BMR"));

        setLayerTile(layer, startCol,     startRow + 2, manifest.getRequiredTileId(prefix + "_BBL"));
        setLayerTile(layer, startCol + 1, startRow + 2, manifest.getRequiredTileId(prefix + "_BB"));
        setLayerTile(layer, startCol + 2, startRow + 2, manifest.getRequiredTileId(prefix + "_BBR"));
    }

    public void placeTerrainFrame3x3(MapLayer layer, String atlasName, String prefix, int startCol, int startRow) {
        AtlasManifest manifest = getManifest(atlasName);
        if (manifest == null) return;
        if (!manifest.hasTerrain3x3(prefix)) return;

        setLayerTile(layer, startCol,     startRow,     manifest.getRequiredTileId(prefix + "_TL"));
        setLayerTile(layer, startCol + 1, startRow,     manifest.getRequiredTileId(prefix + "_T"));
        setLayerTile(layer, startCol + 2, startRow,     manifest.getRequiredTileId(prefix + "_TR"));

        setLayerTile(layer, startCol,     startRow + 1, manifest.getRequiredTileId(prefix + "_L"));
        setLayerTile(layer, startCol + 1, startRow + 1, -1); // ΠΟΛΥ ΣΗΜΑΝΤΙΚΟ
        setLayerTile(layer, startCol + 2, startRow + 1, manifest.getRequiredTileId(prefix + "_R"));

        setLayerTile(layer, startCol,     startRow + 2, manifest.getRequiredTileId(prefix + "_BL"));
        setLayerTile(layer, startCol + 1, startRow + 2, manifest.getRequiredTileId(prefix + "_B"));
        setLayerTile(layer, startCol + 2, startRow + 2, manifest.getRequiredTileId(prefix + "_BR"));
    }

    public void placeVariant2x2(MapLayer layer, String atlasName, String prefix, int startCol, int startRow) {
        AtlasManifest manifest = getManifest(atlasName);
        if (manifest == null) return;
        if (!manifest.hasVariant2x2(prefix)) return;

        setLayerTile(layer, startCol,     startRow,     manifest.getRequiredTileId(prefix + "_1"));
        setLayerTile(layer, startCol + 1, startRow,     manifest.getRequiredTileId(prefix + "_2"));
        setLayerTile(layer, startCol,     startRow + 1, manifest.getRequiredTileId(prefix + "_3"));
        setLayerTile(layer, startCol + 1, startRow + 1, manifest.getRequiredTileId(prefix + "_4"));
    }

    public void placeTerrainStack3x3(MapLayer groundLayer, MapLayer decorLayer,
                                    String atlasName,
                                    String bottomPrefix, String topPrefix,
                                    int startCol, int startRow) {
        placeBottom3x3(groundLayer, atlasName, bottomPrefix, startCol, startRow);
        placeTerrainFrame3x3(decorLayer, atlasName, topPrefix, startCol, startRow);
    }

    public void placePaintedWater3x3(MapLayer layer, String prefix, int startCol, int startRow) {
        placeTerrain3x3(layer, "water", prefix, startCol, startRow);
    }

    public void fillRectangleWithNamedTile(MapLayer layer, String atlasName, String tileName,
                                        int startCol, int startRow, int width, int height) {
        AtlasManifest manifest = getManifest(atlasName);
        if (manifest == null) return;
        if (!manifest.hasTile(tileName)) return;

        int tileId = manifest.getRequiredTileId(tileName);
        fillRectangleWithTile(layer, startCol, startRow, width, height, tileId);
    }

    public void fillRectangleWithTile(MapLayer layer, int startCol, int startRow, int width, int height, int tileId) {
        if (layer == null) return;

        for (int row = startRow; row < startRow + height; row++) {
            for (int col = startCol; col < startCol + width; col++) {
                setLayerTile(layer, col, row, tileId);
            }
        }
    }

    public void clearRectangle(MapLayer layer, int startCol, int startRow, int width, int height) {
        if (layer == null) return;

        for (int row = startRow; row < startRow + height; row++) {
            for (int col = startCol; col < startCol + width; col++) {
                setLayerTile(layer, col, row, -1);
            }
        }
    }

    public void placeTerrainRectangleFrame(MapLayer decorLayer, String atlasName, String prefix,
                                        int startCol, int startRow, int width, int height) {
        AtlasManifest manifest = getManifest(atlasName);
        if (manifest == null) return;
        if (!manifest.hasTerrain3x3(prefix)) return;
        if (width < 2 || height < 2) return;

        int tl = manifest.getRequiredTileId(prefix + "_TL");
        int t  = manifest.getRequiredTileId(prefix + "_T");
        int tr = manifest.getRequiredTileId(prefix + "_TR");
        int l  = manifest.getRequiredTileId(prefix + "_L");
        int r  = manifest.getRequiredTileId(prefix + "_R");
        int bl = manifest.getRequiredTileId(prefix + "_BL");
        int b  = manifest.getRequiredTileId(prefix + "_B");
        int br = manifest.getRequiredTileId(prefix + "_BR");

        // γωνίες
        setLayerTile(decorLayer, startCol, startRow, tl);
        setLayerTile(decorLayer, startCol + width - 1, startRow, tr);
        setLayerTile(decorLayer, startCol, startRow + height - 1, bl);
        setLayerTile(decorLayer, startCol + width - 1, startRow + height - 1, br);

        // πάνω / κάτω
        for (int col = startCol + 1; col < startCol + width - 1; col++) {
            setLayerTile(decorLayer, col, startRow, t);
            setLayerTile(decorLayer, col, startRow + height - 1, b);
        }

        // αριστερά / δεξιά
        for (int row = startRow + 1; row < startRow + height - 1; row++) {
            setLayerTile(decorLayer, startCol, row, l);
            setLayerTile(decorLayer, startCol + width - 1, row, r);
        }

        // κέντρο decor = κενό για να φαίνεται το κάτω layer
        for (int row = startRow + 1; row < startRow + height - 1; row++) {
            for (int col = startCol + 1; col < startCol + width - 1; col++) {
                setLayerTile(decorLayer, col, row, -1);
            }
        }
    }

    public void placeBottomTerrainRectangle(MapLayer groundLayer, String atlasName, String prefix,
                                            int startCol, int startRow, int width, int height) {
        AtlasManifest manifest = getManifest(atlasName);
        if (manifest == null) return;
        if (!manifest.hasTile(prefix + "_FILL")) return;

        int fillId = manifest.getRequiredTileId(prefix + "_FILL");
        fillRectangleWithTile(groundLayer, startCol, startRow, width, height, fillId);
    }

    public void paintTerrainRectangle(MapLayer groundLayer, MapLayer decorLayer,
                                    String atlasName,
                                    String bottomPrefix, String topPrefix,
                                    int startCol, int startRow, int width, int height) {
        if (width < 3 || height < 3) return;

        placeBottomTerrainRectangle(groundLayer, atlasName, bottomPrefix, startCol, startRow, width, height);
        placeTerrainRectangleFrame(decorLayer, atlasName, topPrefix, startCol, startRow, width, height);
    }

    public void placeExplicitBlock4x5(MapLayer layer, String atlasName, String[][] names, int startCol, int startRow) {
        AtlasManifest manifest = getManifest(atlasName);
        if (layer == null || manifest == null) return;

        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 4; col++) {
                String tileName = names[row][col];
                if (manifest.hasTile(tileName)) {
                    setLayerTile(layer, startCol + col, startRow + row, manifest.getRequiredTileId(tileName));
                }
            }
        }
    }

    public void placeExplicitBlock4x3(MapLayer layer, String atlasName, String[][] names, int startCol, int startRow) {
        AtlasManifest manifest = getManifest(atlasName);
        if (layer == null || manifest == null) return;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 4; col++) {
                String tileName = names[row][col];
                if (manifest.hasTile(tileName)) {
                    setLayerTile(layer, startCol + col, startRow + row, manifest.getRequiredTileId(tileName));
                }
            }
        }
    }

    public void paintVariantRectangle(MapLayer layer, String atlasName, String prefix,
                                    int startCol, int startRow, int width, int height) {
        AtlasManifest manifest = getManifest(atlasName);
        if (manifest == null) return;
        if (!manifest.hasVariant2x2(prefix)) return;
        if (width < 2 || height < 2) return;

        int t1 = manifest.getRequiredTileId(prefix + "_1");
        int t2 = manifest.getRequiredTileId(prefix + "_2");
        int t3 = manifest.getRequiredTileId(prefix + "_3");
        int t4 = manifest.getRequiredTileId(prefix + "_4");

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                int targetCol = startCol + col;
                int targetRow = startRow + row;

                int modCol = col % 2;
                int modRow = row % 2;

                int tileId;
                if (modRow == 0 && modCol == 0) {
                    tileId = t1;
                } else if (modRow == 0 && modCol == 1) {
                    tileId = t2;
                } else if (modRow == 1 && modCol == 0) {
                    tileId = t3;
                } else {
                    tileId = t4;
                }

                setLayerTile(layer, targetCol, targetRow, tileId);
            }
        }
    }

    public void paintHorizontalPath(MapLayer layer, String atlasName, String prefix,
                                    int startCol, int row, int length) {
        AtlasManifest manifest = getManifest(atlasName);
        if (manifest == null) return;
        if (!manifest.hasVariant2x2(prefix)) return;
        if (length < 2) return;

        int t1 = manifest.getRequiredTileId(prefix + "_1");
        int t2 = manifest.getRequiredTileId(prefix + "_2");
        int t3 = manifest.getRequiredTileId(prefix + "_3");
        int t4 = manifest.getRequiredTileId(prefix + "_4");

        for (int col = 0; col < length; col++) {
            int targetCol = startCol + col;

            if (col % 2 == 0) {
                setLayerTile(layer, targetCol, row, t1);
                setLayerTile(layer, targetCol, row + 1, t3);
            } else {
                setLayerTile(layer, targetCol, row, t2);
                setLayerTile(layer, targetCol, row + 1, t4);
            }
        }
    }

    public void paintVerticalPath(MapLayer layer, String atlasName, String prefix,
                                int col, int startRow, int length) {
        AtlasManifest manifest = getManifest(atlasName);
        if (manifest == null) return;
        if (!manifest.hasVariant2x2(prefix)) return;
        if (length < 2) return;

        int t1 = manifest.getRequiredTileId(prefix + "_1");
        int t2 = manifest.getRequiredTileId(prefix + "_2");
        int t3 = manifest.getRequiredTileId(prefix + "_3");
        int t4 = manifest.getRequiredTileId(prefix + "_4");

        for (int row = 0; row < length; row++) {
            int targetRow = startRow + row;

            if (row % 2 == 0) {
                setLayerTile(layer, col, targetRow, t1);
                setLayerTile(layer, col + 1, targetRow, t2);
            } else {
                setLayerTile(layer, col, targetRow, t3);
                setLayerTile(layer, col + 1, targetRow, t4);
            }
        }
    }

    public void paintWaterBlockRectangle(MapLayer layer, String prefix,
                                        int startCol, int startRow, int blockCols, int blockRows) {
        for (int by = 0; by < blockRows; by++) {
            for (int bx = 0; bx < blockCols; bx++) {
                int col = startCol + (bx * 3);
                int row = startRow + (by * 3);
                placePaintedWater3x3(layer, prefix, col, row);
            }
        }
    }

    // =========================================================
    // DRAW
    // =========================================================
    public void draw(Graphics2D g2) {
        AdvancedMapData currentMap = getCurrentMapData();
        if (currentMap == null) return;

        if (currentMap.legacy) {
            drawLegacyMap(g2, currentMap);
        } else {
            drawAdvancedMap(g2, currentMap);
        }
    }

    private void drawLegacyMap(Graphics2D g2, AdvancedMapData currentMap) {
        int worldCol = 0;
        int worldRow = 0;

        while (worldCol < currentMap.cols && worldRow < currentMap.rows) {
            int tileNum = currentMap.legacyTiles[worldRow][worldCol];

            int worldX = worldCol * gp.tileSize;
            int worldY = worldRow * gp.tileSize;
            int screenX = worldX - gp.worldX;
            int screenY = worldY - gp.worldY;

            if (worldX + gp.tileSize > gp.worldX &&
                worldX - gp.tileSize < gp.worldX + gp.screenWidth &&
                worldY + gp.tileSize > gp.worldY &&
                worldY - gp.tileSize < gp.worldY + gp.screenHeight) {

                if (tileNum >= 0 && tileNum < tile.length && tile[tileNum] != null) {
                    g2.drawImage(tile[tileNum].image, screenX, screenY, gp.tileSize, gp.tileSize, null);
                }
            }

            worldCol++;
            if (worldCol == currentMap.cols) {
                worldCol = 0;
                worldRow++;
            }
        }
    }

    private void drawAdvancedMap(Graphics2D g2, AdvancedMapData currentMap) {
        drawAtlasLayer(g2, currentMap.getLayer("ground"));
        drawAtlasLayer(g2, currentMap.getLayer("decor"));
        drawAtlasLayer(g2, currentMap.getLayer("water_decor"));
    }

    private void drawAtlasLayer(Graphics2D g2, MapLayer layer) {
        if (layer == null || !layer.visible) return;

        TilesetAtlas atlas = getAtlas(layer.atlasName);
        if (atlas == null) return;

        for (int row = 0; row < layer.rows; row++) {
            for (int col = 0; col < layer.cols; col++) {
                int tileNum = layer.tiles[row][col];
                if (tileNum < 0) continue;

                int worldX = col * gp.tileSize;
                int worldY = row * gp.tileSize;
                int screenX = worldX - gp.worldX;
                int screenY = worldY - gp.worldY;

                if (worldX + gp.tileSize > gp.worldX &&
                    worldX - gp.tileSize < gp.worldX + gp.screenWidth &&
                    worldY + gp.tileSize > gp.worldY &&
                    worldY - gp.tileSize < gp.worldY + gp.screenHeight) {

                    BufferedImage tileImage = atlas.getTileImage(tileNum);
                    if (tileImage != null) {
                        g2.drawImage(tileImage, screenX, screenY, gp.tileSize, gp.tileSize, null);
                    }
                }
            }
        }
    }
}