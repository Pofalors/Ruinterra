import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


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

    private static class TilesetRange {
        public int firstGid;
        public String source;
        public String atlasName;

        public TilesetRange(int firstGid, String source, String atlasName) {
            this.firstGid = firstGid;
            this.source = source;
            this.atlasName = atlasName;
        }
    }

    public TileManager(GamePanel gp) {
        this.gp = gp;

        // loadTileData();

        tile = new Tile[fileNames.size()];
        // getTileImage();

        loadAtlases();
        // loadManifests();

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

    // public void getTileImage() {
    //     for (int i = 0; i < fileNames.size(); i++) {
    //         String fileName = fileNames.get(i);
    //         boolean collision = collisionStatus.get(i).equals("true");
    //         setup(i, fileName, collision);
    //     }
    // }

    public void loadAtlases() {
        loadAtlas("basic_terrain", "res/tilesets/basic_terrain.png");
        loadAtlas("cave", "res/tilesets/cave.png");
        loadAtlas("mountains", "res/tilesets/mountains.png");
        loadAtlas("mountains2", "res/tilesets/mountains2.png");
        loadAtlas("other", "res/tilesets/other.png");
        loadAtlas("Houses", "res/tilesets/Houses.png");
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

    // public void loadManifests() {
    //     loadManifest("basic_terrain", "res/tilesets/basic_terrain_manifest.txt");
    //     loadManifest("water", "res/tilesets/water_manifest.txt");
    // }

    // public void loadManifest(String atlasName, String filePath) {
    //     AtlasManifest manifest = AtlasManifestLoader.loadManifest(atlasName, filePath);
    //     if (manifest != null) {
    //         manifests.put(atlasName, manifest);
    //         System.out.println("Manifest loaded: " + atlasName);
    //     } else {
    //         System.out.println("Could not load manifest: " + atlasName);
    //     }
    // }

    public TilesetAtlas getAtlas(String atlasName) {
        return atlases.get(atlasName);
    }

    // public void setup(int index, String imageName, boolean collision) {
    //     try {
    //         tile[index] = new Tile();
    //         tile[index].image = ImageIO.read(new File("res/tiles/" + imageName));
    //         tile[index].collision = collision;
    //     } catch (IOException e) {
    //         e.printStackTrace();
    //     }
    // }

    // =========================================================
    // MAP LOADING
    // =========================================================
    private void loadAllMaps() {
        // =========================
        // LEGACY MAPS (τα παλιά σου)
        // =========================
        // addLegacyMap("Overworld", "res/maps/worldmap.txt");
        // addLegacyMap("Dungeon01", "res/maps/dungeon01.txt");
        // addLegacyMap("MerchantHouse", "res/maps/interior01.txt");
        // addLegacyMap("Town", "res/maps/town.txt");
        // addLegacyMap("BigHouse", "res/maps/interior01.txt");
        // addLegacyMap("SmallHouse", "res/maps/interior01.txt");


        // =========================
        //        NEW MAPS 
        // =========================
        // addTiledMapFromTMX("town_01", "res/maps/town_01.tmx");
        // addTiledMapFromTMX("fields_01", "res/maps/fields_01.tmx");
        // addTiledMapFromTMX("cave_01", "res/maps/cave_01.tmx");
        addTiledMapFromTMX("monastery_start", "res/maps/monastery_start.tmx");
        addTiledMapFromTMX("first_town", "res/maps/first_town.tmx");
        addTiledMapFromTMX("mountain_pass", "res/maps/mountain_pass.tmx");
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

    // public void addAdvancedMapFromFile(String metadataPath) {
    //     AdvancedMapData map = AdvancedMapLoader.loadAdvancedMap(metadataPath);
    //     if (map != null) {
    //         map.legacy = false;
    //         maps.add(map);
    //         System.out.println("Advanced map loaded: " + map.name + " (" + map.cols + "x" + map.rows + ")");
    //     }
    // }

    // public void addWaterManifestShowcaseMap() {
    //     AdvancedMapData map = new AdvancedMapData("WaterManifestShowcase", 36, 36);
    //     map.legacy = false;
    //     map.tilesetName = "water";

    //     MapLayer ground = new MapLayer("ground", 36, 36);
    //     ground.atlasName = "basic_terrain";

    //     MapLayer underground = new MapLayer("underground", 36, 36);
    //     underground.atlasName = "water";

    //     MapLayer collision = new MapLayer("collision", 36, 36);
    //     collision.atlasName = "";

    //     clearLayer(ground);
    //     clearLayer(underground);

    //     for (int row = 0; row < collision.rows; row++) {
    //         for (int col = 0; col < collision.cols; col++) {
    //             collision.tiles[row][col] = 0;
    //         }
    //     }

    //     // Βάση γης
    //     fillRectangleWithNamedTile(ground, "basic_terrain", "DIRT_FILL", 0, 0, map.cols, map.rows);

    //     // -------------------------------------------------
    //     // 1) GRASS/WATER family
    //     // -------------------------------------------------
    //     placeTerrain3x3(underground, "water", "GWATER", 2, 2);
    //     placeTerrain3x3(underground, "water", "WGRASS", 6, 2);

    //     // -------------------------------------------------
    //     // 2) SAND/WATER family
    //     // -------------------------------------------------
    //     placeTerrain3x3(underground, "water", "SWATER", 2, 6);
    //     placeTerrain3x3(underground, "water", "WSAND", 6, 6);

    //     // -------------------------------------------------
    //     // 3) SNOW/WATER family
    //     // -------------------------------------------------
    //     placeTerrain3x3(underground, "water", "SNWATER", 2, 10);
    //     placeTerrain3x3(underground, "water", "WSNOW", 6, 10);

    //     AtlasManifest waterManifest = getManifest("water");
    //     if (waterManifest == null) {
    //         System.out.println("No water manifest loaded.");
    //         return;
    //     }

    //     // -------------------------------------------------
    //     // 4) Waves row
    //     // -------------------------------------------------
    //     if (waterManifest.hasTile("WAVES_1")) setLayerTile(underground, 14, 2, waterManifest.getRequiredTileId("WAVES_1"));
    //     if (waterManifest.hasTile("WAVES_2")) setLayerTile(underground, 15, 2, waterManifest.getRequiredTileId("WAVES_2"));
    //     if (waterManifest.hasTile("WAVES_3")) setLayerTile(underground, 16, 2, waterManifest.getRequiredTileId("WAVES_3"));
    //     if (waterManifest.hasTile("WAVES_4")) setLayerTile(underground, 17, 2, waterManifest.getRequiredTileId("WAVES_4"));
    //     if (waterManifest.hasTile("WAVES_5")) setLayerTile(underground, 18, 2, waterManifest.getRequiredTileId("WAVES_5"));

    //     // Single water tile
    //     if (waterManifest.hasTile("WATER_TILE")) setLayerTile(underground, 20, 2, waterManifest.getRequiredTileId("WATER_TILE"));

    //     // Rocks
    //     if (waterManifest.hasTile("WATER_ROCKS_1")) setLayerTile(underground, 22, 2, waterManifest.getRequiredTileId("WATER_ROCKS_1"));
    //     if (waterManifest.hasTile("WATER_ROCKS_2")) setLayerTile(underground, 23, 2, waterManifest.getRequiredTileId("WATER_ROCKS_2"));

    //     // -------------------------------------------------
    //     // 5) Water rock path 2x2
    //     // -------------------------------------------------
    //     placeVariant2x2(underground, "water", "WATER_ROCK_PATH", 14, 6);

    //     // 6) Water snow path 2x2
    //     placeVariant2x2(underground, "water", "WATER_SNOW_PATH", 18, 6);

    //     // -------------------------------------------------
    //     // 7) Big waterfall 4x5
    //     // -------------------------------------------------
    //     placeExplicitBlock4x5(underground, "water",
    //             new String[][]{
    //                     {"WATERFALL_BIG_TLL", "WATERFALL_BIG_TL", "WATERFALL_BIG_TR", "WATERFALL_BIG_TRR"},
    //                     {"WATERFALL_BIG_MTLL", "WATERFALL_BIG_MTL", "WATERFALL_BIG_MTR", "WATERFALL_BIG_MTRR"},
    //                     {"WATERFALL_BIG_MLL", "WATERFALL_BIG_ML", "WATERFALL_BIG_MR", "WATERFALL_BIG_MRR"},
    //                     {"WATERFALL_BIG_MBLL", "WATERFALL_BIG_MBL", "WATERFALL_BIG_MBR", "WATERFALL_BIG_MBRR"},
    //                     {"WATERFALL_BIG_BLL", "WATERFALL_BIG_BL", "WATERFALL_BIG_BR", "WATERFALL_BIG_BRR"}
    //             }, 14, 12);

    //     // -------------------------------------------------
    //     // 8) Small waterfall 4x3
    //     // -------------------------------------------------
    //     placeExplicitBlock4x3(underground, "water",
    //             new String[][]{
    //                     {"WATERFALL_SMALL_TLL", "WATERFALL_SMALL_TL", "WATERFALL_SMALL_TR", "WATERFALL_SMALL_TRR"},
    //                     {"WATERFALL_SMALL_MLL", "WATERFALL_SMALL_ML", "WATERFALL_SMALL_MR", "WATERFALL_SMALL_MRR"},
    //                     {"WATERFALL_SMALL_BLL", "WATERFALL_SMALL_BL", "WATERFALL_SMALL_BR", "WATERFALL_SMALL_BRR"}
    //             }, 20, 12);

    //     // Border collision μόνο
    //     for (int col = 0; col < map.cols; col++) {
    //         collision.tiles[0][col] = 1;
    //         collision.tiles[map.rows - 1][col] = 1;
    //     }
    //     for (int row = 0; row < map.rows; row++) {
    //         collision.tiles[row][0] = 1;
    //         collision.tiles[row][map.cols - 1] = 1;
    //     }

    //     map.layers.add(ground);
    //     map.layers.add(underground);
    //     map.layers.add(collision);

    //     addAdvancedMap(map);

    //     System.out.println("Water manifest showcase map added.");
    // }

    // public void addGeneratedRegionMap() {
    //     int cols = 50;
    //     int rows = 50;

    //     AdvancedMapData map = new AdvancedMapData("GeneratedRegionMap", cols, rows);
    //     map.legacy = false;
    //     map.tilesetName = "basic_terrain";

    //     MapLayer ground = new MapLayer("ground", rows, cols);
    //     ground.atlasName = "basic_terrain";

    //     MapLayer underground = new MapLayer("underground", rows, cols);
    //     underground.atlasName = "basic_terrain";

    //     MapLayer waterunderground = new MapLayer("water_underground", rows, cols);
    //     waterunderground.atlasName = "water";

    //     MapLayer collision = new MapLayer("collision", rows, cols);
    //     collision.atlasName = "";

    //     clearLayer(ground);
    //     clearLayer(underground);
    //     clearLayer(waterunderground);

    //     for (int row = 0; row < rows; row++) {
    //         for (int col = 0; col < cols; col++) {
    //             collision.tiles[row][col] = 0;
    //         }
    //     }

    //     // 1. Βάση όλου του χάρτη = grass
    //     paintFilledRect(ground, "basic_terrain", "GRASS_FILL", 0, 0, cols, rows);

    //     // καθάρισε underground από terrain stamps
    //     clearLayer(underground);
    //     clearLayer(waterunderground);

    //     // 3. Αριστερή θάλασσα
    //     int seaCol = 1;
    //     int seaRow = 28;
    //     int seaWidth = 13;
    //     int seaHeight = 18;

    //     // 4. Παραλία γύρω από το νερό, 2 tiles πιο έξω
    //     paintBeachAroundWater(ground, seaCol, seaRow, seaWidth, seaHeight, 2);

    //     // 5. Νερό με sand-water shoreline
    //     paintWaterRect(waterunderground, "SWATER", seaCol, seaRow, seaWidth, seaHeight);

    //     // collision στη θάλασσα
    //     blockCollisionRect(collision, seaCol, seaRow, seaWidth, seaHeight);

    //     // 6. Κεντρικός δρόμος vertical
    //     paintRoadRect(ground, underground, 26, 3, 4, 40);

    //     // 7. Κεντρικός δρόμος horizontal προς σπίτι/yard
    //     paintRoadRect(ground, underground, 18, 16, 12, 4);

    //     // 8. Snow zone πάνω δεξιά
    //     paintSnowRect(ground, underground, 36, 1, 13, 11);

    //     // 9. House yard περιοχή
    //     paintFenceRect(underground, 20, 10, 10, 10);

    //     // 10. POI markers απλά για τώρα
    //     AtlasManifest basic = getManifest("basic_terrain");
    //     AtlasManifest water = getManifest("water");

    //     if (basic != null) {
    //         if (basic.hasVariant2x2("DIRT_PATH")) {
    //             paintVariantRectangle(underground, "basic_terrain", "DIRT_PATH", 22, 12, 2, 2); // house marker
    //         }
    //         if (basic.hasVariant2x2("FLOWER_PATH")) {
    //             paintVariantRectangle(underground, "basic_terrain", "FLOWER_PATH", 10, 20, 2, 2); // chest/flower marker
    //         }
    //         if (basic.hasVariant2x2("GRASS_PATH")) {
    //             paintVariantRectangle(underground, "basic_terrain", "GRASS_PATH", 27, 24, 2, 2); // sign marker
    //         }
    //         if (basic.hasVariant2x2("SAND_PATH")) {
    //             paintVariantRectangle(underground, "basic_terrain", "SAND_PATH", 42, 14, 2, 2); // cave marker near snow edge
    //         }
    //     }

    //     if (water != null && water.hasTile("WATER_ROCKS_1")) {
    //         setLayerTile(waterunderground, 8, 35, water.getRequiredTileId("WATER_ROCKS_1"));
    //     }

    //     // 10. Border collision
    //     for (int col = 0; col < cols; col++) {
    //         collision.tiles[0][col] = 1;
    //         collision.tiles[rows - 1][col] = 1;
    //     }
    //     for (int row = 0; row < rows; row++) {
    //         collision.tiles[row][0] = 1;
    //         collision.tiles[row][cols - 1] = 1;
    //     }

    //     map.layers.add(ground);
    //     map.layers.add(underground);
    //     map.layers.add(waterunderground);
    //     map.layers.add(collision);

    //     addAdvancedMap(map);

    //     System.out.println("Generated region map added.");
    // }

    // =========================================================
    // ADVANCED MAP PLACEHOLDER
    // =========================================================
    public void addAdvancedMap(AdvancedMapData map) {
        if (map != null) {
            map.legacy = false;
            maps.add(map);
        }
    }

    // public int[][] loadCSVLayer(String path, int cols, int rows) {
    //     int[][] map = new int[rows][cols];

    //     try (BufferedReader br = new BufferedReader(new FileReader(path))) {

    //         for (int row = 0; row < rows; row++) {
    //             String line = br.readLine();
    //             if (line == null) break;

    //             String[] numbers = line.split(",");

    //             for (int col = 0; col < cols; col++) {
    //                 int val = Integer.parseInt(numbers[col].trim());

    //                 // Tiled starts from 1, empty = 0
    //                 map[row][col] = (val == 0) ? -1 : val - 1;
    //             }
    //         }

    //     } catch (Exception e) {
    //         System.out.println("Failed to load CSV layer: " + path);
    //         e.printStackTrace();
    //     }

    //     return map;
    // }

    // public int[][] loadCSVLayerWithOffset(String path, int cols, int rows, int firstGid) {
    //     int[][] map = new int[rows][cols];

    //     try (BufferedReader br = new BufferedReader(new FileReader(path))) {
    //         for (int row = 0; row < rows; row++) {
    //             String line = br.readLine();
    //             if (line == null) break;

    //             String[] numbers = line.split(",");

    //             for (int col = 0; col < cols; col++) {
    //                 int val = Integer.parseInt(numbers[col].trim());

    //                 if (val == 0) {
    //                     map[row][col] = -1;
    //                 } else {
    //                     map[row][col] = val - firstGid;
    //                 }
    //             }
    //         }
    //     } catch (Exception e) {
    //         System.out.println("Failed to load CSV layer: " + path);
    //         e.printStackTrace();
    //     }

    //     return map;
    // }

    // public void addTiledMap(String name, int cols, int rows,
    //                         String groundPath,
    //                         String undergroundPath,
    //                         String waterPath,
    //                         String upperPath,
    //                         String collisionPath) {

    //     AdvancedMapData map = new AdvancedMapData(name, cols, rows);
    //     map.legacy = false;

    //     // ΒΑΛΕ ΕΔΩ ΤΑ σωστά firstgid από το Tiled tileset order
    //     int basicTerrainFirstGid = 1;
    //     int waterFirstGid = 529;   // παράδειγμα μόνο
    //     int otherFirstGid = 1444;  // παράδειγμα μόνο

    //     MapLayer ground = new MapLayer("ground", rows, cols);
    //     ground.atlasName = "basic_terrain";
    //     ground.tiles = loadCSVLayerWithOffset(groundPath, cols, rows, basicTerrainFirstGid);

    //     MapLayer underground = new MapLayer("underground", rows, cols);
    //     underground.atlasName = "basic_terrain";
    //     underground.tiles = loadCSVLayerWithOffset(undergroundPath, cols, rows, basicTerrainFirstGid);

    //     MapLayer water = new MapLayer("water", rows, cols);
    //     water.atlasName = "water";
    //     water.tiles = loadCSVLayerWithOffset(waterPath, cols, rows, waterFirstGid);

    //     MapLayer upper = new MapLayer("upper", rows, cols);
    //     upper.atlasName = "other";
    //     upper.tiles = loadCSVLayerWithOffset(upperPath, cols, rows, otherFirstGid);

    //     MapLayer collision = new MapLayer("collision", rows, cols);
    //     collision.atlasName = "";
    //     collision.tiles = loadCSVLayer(collisionPath, cols, rows);

    //     map.layers.add(ground);
    //     map.layers.add(underground);
    //     map.layers.add(water);
    //     map.layers.add(upper);
    //     map.layers.add(collision);

    //     addAdvancedMap(map);
    // }

    public void addTiledMapFromTMX(String name, String tmxPath) {
        try {
            File file = new File(tmxPath);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);
            doc.getDocumentElement().normalize();

            Element mapElement = doc.getDocumentElement();

            int cols = Integer.parseInt(mapElement.getAttribute("width"));
            int rows = Integer.parseInt(mapElement.getAttribute("height"));

            ArrayList<TilesetRange> ranges = getTilesetRanges(doc);

            AdvancedMapData map = new AdvancedMapData(name, cols, rows);
            map.legacy = false;

            MapLayer underground = loadTMXLayer(doc, "underground", "underground", rows, cols, ranges, false);
            MapLayer ground = loadTMXLayer(doc, "ground", "ground", rows, cols, ranges, false);
            MapLayer decor = loadTMXLayer(doc, "decor", "decor", rows, cols, ranges, false);
            MapLayer water = loadTMXLayer(doc, "water", "water", rows, cols, ranges, false);
            MapLayer collision = loadTMXLayer(doc, "collision", "collision", rows, cols, ranges, true);

            if (ground != null) map.layers.add(ground);
            if (underground != null) map.layers.add(underground);
            if (water != null) map.layers.add(water);
            if (decor != null) map.layers.add(decor);
            if (collision != null) map.layers.add(collision);
            map.objects.addAll(loadTMXObjects(doc, "spawns"));
            map.objects.addAll(loadTMXObjects(doc, "portals"));
            map.objects.addAll(loadTMXObjects(doc, "npcs"));
            map.objects.addAll(loadTMXObjects(doc, "chests"));
            map.objects.addAll(loadTMXObjects(doc, "encounters"));
            map.objects.addAll(loadTMXObjects(doc, "enemies"));
            map.objects.addAll(loadTMXObjects(doc, "story_triggers"));
            

            addAdvancedMap(map);

            System.out.println("TMX map loaded: " + name + " (" + cols + "x" + rows + ")");

        } catch (Exception e) {
            System.out.println("Failed to load TMX map: " + tmxPath);
            e.printStackTrace();
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
            LayerTile t = ground.tiles[row][col];

            if (t == null || t.isEmpty()) return 0;

            TilesetAtlas atlas = getAtlas(t.atlasName);
            if (atlas == null) return 0;

            return t.tileId;
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
            LayerTile t = collisionLayer.tiles[row][col];
            return t != null && !t.isEmpty();
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

    // public void setLayerTile(MapLayer layer, int col, int row, int tileId) {
    //     if (layer == null) return;
    //     if (row < 0 || row >= layer.rows || col < 0 || col >= layer.cols) return;
    //     layer.tiles[row][col] = tileId;
    // }

    // public void fillLayer(MapLayer layer, int tileId) {
    //     if (layer == null) return;

    //     for (int row = 0; row < layer.rows; row++) {
    //         for (int col = 0; col < layer.cols; col++) {
    //             layer.tiles[row][col] = tileId;
    //         }
    //     }
    // }

    // public void clearLayer(MapLayer layer) {
    //     if (layer == null) return;

    //     for (int row = 0; row < layer.rows; row++) {
    //         for (int col = 0; col < layer.cols; col++) {
    //             layer.tiles[row][col] = -1;
    //         }
    //     }
    // }

    // public void placeTerrain3x3(MapLayer layer, String atlasName, String prefix, int startCol, int startRow) {
    //     AtlasManifest manifest = getManifest(atlasName);
    //     if (manifest == null) return;
    //     if (!manifest.hasTerrain3x3(prefix)) return;

    //     setLayerTile(layer, startCol,     startRow,     manifest.getRequiredTileId(prefix + "_TL"));
    //     setLayerTile(layer, startCol + 1, startRow,     manifest.getRequiredTileId(prefix + "_T"));
    //     setLayerTile(layer, startCol + 2, startRow,     manifest.getRequiredTileId(prefix + "_TR"));

    //     setLayerTile(layer, startCol,     startRow + 1, manifest.getRequiredTileId(prefix + "_L"));
    //     setLayerTile(layer, startCol + 1, startRow + 1, manifest.getRequiredTileId(prefix + "_FILL"));
    //     setLayerTile(layer, startCol + 2, startRow + 1, manifest.getRequiredTileId(prefix + "_R"));

    //     setLayerTile(layer, startCol,     startRow + 2, manifest.getRequiredTileId(prefix + "_BL"));
    //     setLayerTile(layer, startCol + 1, startRow + 2, manifest.getRequiredTileId(prefix + "_B"));
    //     setLayerTile(layer, startCol + 2, startRow + 2, manifest.getRequiredTileId(prefix + "_BR"));
    // }

    // public void placeBottom3x3(MapLayer layer, String atlasName, String prefix, int startCol, int startRow) {
    //     AtlasManifest manifest = getManifest(atlasName);
    //     if (manifest == null) return;
    //     if (!manifest.hasBottom3x3(prefix)) return;

    //     setLayerTile(layer, startCol,     startRow,     manifest.getRequiredTileId(prefix + "_BTL"));
    //     setLayerTile(layer, startCol + 1, startRow,     manifest.getRequiredTileId(prefix + "_BT"));
    //     setLayerTile(layer, startCol + 2, startRow,     manifest.getRequiredTileId(prefix + "_BTR"));

    //     setLayerTile(layer, startCol,     startRow + 1, manifest.getRequiredTileId(prefix + "_BML"));
    //     setLayerTile(layer, startCol + 1, startRow + 1, manifest.getRequiredTileId(prefix + "_FILL"));
    //     setLayerTile(layer, startCol + 2, startRow + 1, manifest.getRequiredTileId(prefix + "_BMR"));

    //     setLayerTile(layer, startCol,     startRow + 2, manifest.getRequiredTileId(prefix + "_BBL"));
    //     setLayerTile(layer, startCol + 1, startRow + 2, manifest.getRequiredTileId(prefix + "_BB"));
    //     setLayerTile(layer, startCol + 2, startRow + 2, manifest.getRequiredTileId(prefix + "_BBR"));
    // }

    // public void placeTerrainFrame3x3(MapLayer layer, String atlasName, String prefix, int startCol, int startRow) {
    //     AtlasManifest manifest = getManifest(atlasName);
    //     if (manifest == null) return;
    //     if (!manifest.hasTerrain3x3(prefix)) return;

    //     setLayerTile(layer, startCol,     startRow,     manifest.getRequiredTileId(prefix + "_TL"));
    //     setLayerTile(layer, startCol + 1, startRow,     manifest.getRequiredTileId(prefix + "_T"));
    //     setLayerTile(layer, startCol + 2, startRow,     manifest.getRequiredTileId(prefix + "_TR"));

    //     setLayerTile(layer, startCol,     startRow + 1, manifest.getRequiredTileId(prefix + "_L"));
    //     setLayerTile(layer, startCol + 1, startRow + 1, -1); // ΠΟΛΥ ΣΗΜΑΝΤΙΚΟ
    //     setLayerTile(layer, startCol + 2, startRow + 1, manifest.getRequiredTileId(prefix + "_R"));

    //     setLayerTile(layer, startCol,     startRow + 2, manifest.getRequiredTileId(prefix + "_BL"));
    //     setLayerTile(layer, startCol + 1, startRow + 2, manifest.getRequiredTileId(prefix + "_B"));
    //     setLayerTile(layer, startCol + 2, startRow + 2, manifest.getRequiredTileId(prefix + "_BR"));
    // }

    // public void placeVariant2x2(MapLayer layer, String atlasName, String prefix, int startCol, int startRow) {
    //     AtlasManifest manifest = getManifest(atlasName);
    //     if (manifest == null) return;
    //     if (!manifest.hasVariant2x2(prefix)) return;

    //     setLayerTile(layer, startCol,     startRow,     manifest.getRequiredTileId(prefix + "_1"));
    //     setLayerTile(layer, startCol + 1, startRow,     manifest.getRequiredTileId(prefix + "_2"));
    //     setLayerTile(layer, startCol,     startRow + 1, manifest.getRequiredTileId(prefix + "_3"));
    //     setLayerTile(layer, startCol + 1, startRow + 1, manifest.getRequiredTileId(prefix + "_4"));
    // }

    // public void placeTerrainStack3x3(MapLayer groundLayer, MapLayer undergroundLayer,
    //                                 String atlasName,
    //                                 String bottomPrefix, String topPrefix,
    //                                 int startCol, int startRow) {
    //     placeBottom3x3(groundLayer, atlasName, bottomPrefix, startCol, startRow);
    //     placeTerrainFrame3x3(undergroundLayer, atlasName, topPrefix, startCol, startRow);
    // }

    // public void placePaintedWater3x3(MapLayer layer, String prefix, int startCol, int startRow) {
    //     placeTerrain3x3(layer, "water", prefix, startCol, startRow);
    // }

    // public void fillRectangleWithNamedTile(MapLayer layer, String atlasName, String tileName,
    //                                     int startCol, int startRow, int width, int height) {
    //     AtlasManifest manifest = getManifest(atlasName);
    //     if (manifest == null) return;
    //     if (!manifest.hasTile(tileName)) return;

    //     int tileId = manifest.getRequiredTileId(tileName);
    //     fillRectangleWithTile(layer, startCol, startRow, width, height, tileId);
    // }

    // public void fillRectangleWithTile(MapLayer layer, int startCol, int startRow, int width, int height, int tileId) {
    //     if (layer == null) return;

    //     for (int row = startRow; row < startRow + height; row++) {
    //         for (int col = startCol; col < startCol + width; col++) {
    //             setLayerTile(layer, col, row, tileId);
    //         }
    //     }
    // }

    // public void clearRectangle(MapLayer layer, int startCol, int startRow, int width, int height) {
    //     if (layer == null) return;

    //     for (int row = startRow; row < startRow + height; row++) {
    //         for (int col = startCol; col < startCol + width; col++) {
    //             setLayerTile(layer, col, row, -1);
    //         }
    //     }
    // }

    // public void placeTerrainRectangleFrame(MapLayer undergroundLayer, String atlasName, String prefix,
    //                                     int startCol, int startRow, int width, int height) {
    //     AtlasManifest manifest = getManifest(atlasName);
    //     if (manifest == null) return;
    //     if (!manifest.hasTerrain3x3(prefix)) return;
    //     if (width < 2 || height < 2) return;

    //     int tl = manifest.getRequiredTileId(prefix + "_TL");
    //     int t  = manifest.getRequiredTileId(prefix + "_T");
    //     int tr = manifest.getRequiredTileId(prefix + "_TR");
    //     int l  = manifest.getRequiredTileId(prefix + "_L");
    //     int r  = manifest.getRequiredTileId(prefix + "_R");
    //     int bl = manifest.getRequiredTileId(prefix + "_BL");
    //     int b  = manifest.getRequiredTileId(prefix + "_B");
    //     int br = manifest.getRequiredTileId(prefix + "_BR");

    //     // γωνίες
    //     setLayerTile(undergroundLayer, startCol, startRow, tl);
    //     setLayerTile(undergroundLayer, startCol + width - 1, startRow, tr);
    //     setLayerTile(undergroundLayer, startCol, startRow + height - 1, bl);
    //     setLayerTile(undergroundLayer, startCol + width - 1, startRow + height - 1, br);

    //     // πάνω / κάτω
    //     for (int col = startCol + 1; col < startCol + width - 1; col++) {
    //         setLayerTile(undergroundLayer, col, startRow, t);
    //         setLayerTile(undergroundLayer, col, startRow + height - 1, b);
    //     }

    //     // αριστερά / δεξιά
    //     for (int row = startRow + 1; row < startRow + height - 1; row++) {
    //         setLayerTile(undergroundLayer, startCol, row, l);
    //         setLayerTile(undergroundLayer, startCol + width - 1, row, r);
    //     }

    //     // κέντρο underground = κενό για να φαίνεται το κάτω layer
    //     for (int row = startRow + 1; row < startRow + height - 1; row++) {
    //         for (int col = startCol + 1; col < startCol + width - 1; col++) {
    //             setLayerTile(undergroundLayer, col, row, -1);
    //         }
    //     }
    // }

    // public void placeBottomTerrainRectangle(MapLayer groundLayer, String atlasName, String prefix,
    //                                         int startCol, int startRow, int width, int height) {
    //     AtlasManifest manifest = getManifest(atlasName);
    //     if (manifest == null) return;
    //     if (!manifest.hasTile(prefix + "_FILL")) return;

    //     int fillId = manifest.getRequiredTileId(prefix + "_FILL");
    //     fillRectangleWithTile(groundLayer, startCol, startRow, width, height, fillId);
    // }

    // public void paintTerrainRectangle(MapLayer groundLayer, MapLayer undergroundLayer,
    //                                 String atlasName,
    //                                 String bottomPrefix, String topPrefix,
    //                                 int startCol, int startRow, int width, int height) {
    //     if (width < 3 || height < 3) return;

    //     placeBottomTerrainRectangle(groundLayer, atlasName, bottomPrefix, startCol, startRow, width, height);
    //     placeTerrainRectangleFrame(undergroundLayer, atlasName, topPrefix, startCol, startRow, width, height);
    // }

    // public void placeExplicitBlock4x5(MapLayer layer, String atlasName, String[][] names, int startCol, int startRow) {
    //     AtlasManifest manifest = getManifest(atlasName);
    //     if (layer == null || manifest == null) return;

    //     for (int row = 0; row < 5; row++) {
    //         for (int col = 0; col < 4; col++) {
    //             String tileName = names[row][col];
    //             if (manifest.hasTile(tileName)) {
    //                 setLayerTile(layer, startCol + col, startRow + row, manifest.getRequiredTileId(tileName));
    //             }
    //         }
    //     }
    // }

    // public void placeExplicitBlock4x3(MapLayer layer, String atlasName, String[][] names, int startCol, int startRow) {
    //     AtlasManifest manifest = getManifest(atlasName);
    //     if (layer == null || manifest == null) return;

    //     for (int row = 0; row < 3; row++) {
    //         for (int col = 0; col < 4; col++) {
    //             String tileName = names[row][col];
    //             if (manifest.hasTile(tileName)) {
    //                 setLayerTile(layer, startCol + col, startRow + row, manifest.getRequiredTileId(tileName));
    //             }
    //         }
    //     }
    // }

    // public void paintVariantRectangle(MapLayer layer, String atlasName, String prefix,
    //                                 int startCol, int startRow, int width, int height) {
    //     AtlasManifest manifest = getManifest(atlasName);
    //     if (manifest == null) return;
    //     if (!manifest.hasVariant2x2(prefix)) return;
    //     if (width < 2 || height < 2) return;

    //     int t1 = manifest.getRequiredTileId(prefix + "_1");
    //     int t2 = manifest.getRequiredTileId(prefix + "_2");
    //     int t3 = manifest.getRequiredTileId(prefix + "_3");
    //     int t4 = manifest.getRequiredTileId(prefix + "_4");

    //     for (int row = 0; row < height; row++) {
    //         for (int col = 0; col < width; col++) {
    //             int targetCol = startCol + col;
    //             int targetRow = startRow + row;

    //             int modCol = col % 2;
    //             int modRow = row % 2;

    //             int tileId;
    //             if (modRow == 0 && modCol == 0) {
    //                 tileId = t1;
    //             } else if (modRow == 0 && modCol == 1) {
    //                 tileId = t2;
    //             } else if (modRow == 1 && modCol == 0) {
    //                 tileId = t3;
    //             } else {
    //                 tileId = t4;
    //             }

    //             setLayerTile(layer, targetCol, targetRow, tileId);
    //         }
    //     }
    // }

    // public void paintHorizontalPath(MapLayer layer, String atlasName, String prefix,
    //                                 int startCol, int row, int length) {
    //     AtlasManifest manifest = getManifest(atlasName);
    //     if (manifest == null) return;
    //     if (!manifest.hasVariant2x2(prefix)) return;
    //     if (length < 2) return;

    //     int t1 = manifest.getRequiredTileId(prefix + "_1");
    //     int t2 = manifest.getRequiredTileId(prefix + "_2");
    //     int t3 = manifest.getRequiredTileId(prefix + "_3");
    //     int t4 = manifest.getRequiredTileId(prefix + "_4");

    //     for (int col = 0; col < length; col++) {
    //         int targetCol = startCol + col;

    //         if (col % 2 == 0) {
    //             setLayerTile(layer, targetCol, row, t1);
    //             setLayerTile(layer, targetCol, row + 1, t3);
    //         } else {
    //             setLayerTile(layer, targetCol, row, t2);
    //             setLayerTile(layer, targetCol, row + 1, t4);
    //         }
    //     }
    // }

    // public void paintVerticalPath(MapLayer layer, String atlasName, String prefix,
    //                             int col, int startRow, int length) {
    //     AtlasManifest manifest = getManifest(atlasName);
    //     if (manifest == null) return;
    //     if (!manifest.hasVariant2x2(prefix)) return;
    //     if (length < 2) return;

    //     int t1 = manifest.getRequiredTileId(prefix + "_1");
    //     int t2 = manifest.getRequiredTileId(prefix + "_2");
    //     int t3 = manifest.getRequiredTileId(prefix + "_3");
    //     int t4 = manifest.getRequiredTileId(prefix + "_4");

    //     for (int row = 0; row < length; row++) {
    //         int targetRow = startRow + row;

    //         if (row % 2 == 0) {
    //             setLayerTile(layer, col, targetRow, t1);
    //             setLayerTile(layer, col + 1, targetRow, t2);
    //         } else {
    //             setLayerTile(layer, col, targetRow, t3);
    //             setLayerTile(layer, col + 1, targetRow, t4);
    //         }
    //     }
    // }

    // public void paintFilledRect(MapLayer layer, String atlasName, String tileName,
    //                             int startCol, int startRow, int width, int height) {
    //     fillRectangleWithNamedTile(layer, atlasName, tileName, startCol, startRow, width, height);
    // }

    // public void paintRoadRect(MapLayer ground, MapLayer underground,
    //                         int startCol, int startRow, int width, int height) {
    //     // εσωτερικό = DIRT_FILL
    //     // outline = GRASS_DIRT
    //     paintTerrainRectangle(ground, underground, "basic_terrain", "DIRT", "GRASS", startCol, startRow, width, height);
    // }

    // public void paintSnowRect(MapLayer ground, MapLayer underground,
    //                         int startCol, int startRow, int width, int height) {
    //     // Μόνο valid pairing: DIRT κάτω, SNOW πάνω
    //     paintTerrainRectangle(ground, underground, "basic_terrain", "DIRT", "SNOW", startCol, startRow, width, height);
    // }

    // public void paintGrassField(MapLayer ground, MapLayer underground,
    //                             int startCol, int startRow, int width, int height) {
    //     paintTerrainRectangle(ground, underground, "basic_terrain", "DIRT", "GRASS", startCol, startRow, width, height);
    // }

    // public void paintWaterRect(MapLayer waterunderground, String prefix,
    //                         int startCol, int startRow, int width, int height) {
    //     AtlasManifest waterManifest = getManifest("water");
    //     if (waterManifest == null) return;

    //     for (int row = startRow; row < startRow + height; row++) {
    //         for (int col = startCol; col < startCol + width; col++) {
    //             boolean up = row > startRow;
    //             boolean down = row < startRow + height - 1;
    //             boolean left = col > startCol;
    //             boolean right = col < startCol + width - 1;

    //             String tileName;

    //             if (!up && !left) tileName = prefix + "_TL";
    //             else if (!up && !right) tileName = prefix + "_TR";
    //             else if (!down && !left) tileName = prefix + "_BL";
    //             else if (!down && !right) tileName = prefix + "_BR";
    //             else if (!up) tileName = prefix + "_T";
    //             else if (!down) tileName = prefix + "_B";
    //             else if (!left) tileName = prefix + "_L";
    //             else if (!right) tileName = prefix + "_R";
    //             else tileName = prefix + "_FILL";

    //             if (waterManifest.hasTile(tileName)) {
    //                 setLayerTile(waterunderground, col, row, waterManifest.getRequiredTileId(tileName));
    //             }
    //         }
    //     }
    // }

    // public void paintBeachAroundWater(MapLayer ground,
    //                                 int waterCol, int waterRow, int waterWidth, int waterHeight,
    //                                 int thickness) {

    //     int sandCol = waterCol;
    //     int sandRow = Math.max(1, waterRow - thickness);
    //     int sandWidth = waterWidth + thickness;
    //     int sandHeight = waterHeight + (thickness * 2);

    //     // sand βάση κάτω από το νερό και γύρω του
    //     paintFilledRect(ground, "basic_terrain", "SAND_FILL", sandCol, sandRow, sandWidth, sandHeight);
    // }

    public void blockCollisionRect(MapLayer collision, int startCol, int startRow, int width, int height) {
        for (int row = startRow; row < startRow + height; row++) {
            for (int col = startCol; col < startCol + width; col++) {
                if (row >= 0 && row < collision.rows && col >= 0 && col < collision.cols) {
                    collision.tiles[row][col] = new LayerTile("collision", 0);
                }
            }
        }
    }

    // public void paintFenceRect(MapLayer underground, int startCol, int startRow, int width, int height) {
    //     AtlasManifest basic = getManifest("basic_terrain");
    //     if (basic == null || !basic.hasTile("DIRT_PATH_1")) return;

    //     int tl = basic.getRequiredTileId("DIRT_PATH_1");
    //     int tr = basic.getRequiredTileId("DIRT_PATH_2");
    //     int bl = basic.getRequiredTileId("DIRT_PATH_3");
    //     int br = basic.getRequiredTileId("DIRT_PATH_4");

    //     setLayerTile(underground, startCol, startRow, tl);
    //     setLayerTile(underground, startCol + width - 1, startRow, tr);
    //     setLayerTile(underground, startCol, startRow + height - 1, bl);
    //     setLayerTile(underground, startCol + width - 1, startRow + height - 1, br);
    // }

    // public void paintWaterBlockRectangle(MapLayer layer, String prefix,
    //                                     int startCol, int startRow, int blockCols, int blockRows) {
    //     for (int by = 0; by < blockRows; by++) {
    //         for (int bx = 0; bx < blockCols; bx++) {
    //             int col = startCol + (bx * 3);
    //             int row = startRow + (by * 3);
    //             placePaintedWater3x3(layer, prefix, col, row);
    //         }
    //     }
    // }

    private Element findLayerElement(Document doc, String layerName) {
        NodeList layers = doc.getElementsByTagName("layer");

        for (int i = 0; i < layers.getLength(); i++) {
            Element layer = (Element) layers.item(i);
            String name = layer.getAttribute("name");
            if (name.equalsIgnoreCase(layerName)) {
                return layer;
            }
        }

        return null;
    }

    private int findFirstGid(Document doc, String tsxFileName) {
        NodeList tilesets = doc.getElementsByTagName("tileset");

        for (int i = 0; i < tilesets.getLength(); i++) {
            Element ts = (Element) tilesets.item(i);
            String source = ts.getAttribute("source");

            if (source != null && source.endsWith(tsxFileName)) {
                return Integer.parseInt(ts.getAttribute("firstgid"));
            }
        }

        return -1;
    }

    private MapLayer loadTMXLayer(Document doc,
                                String tmxLayerName,
                                String internalLayerName,
                                int rows,
                                int cols,
                                ArrayList<TilesetRange> ranges,
                                boolean collisionMode) {

        Element layerElement = findLayerElement(doc, tmxLayerName);
        if (layerElement == null) {
            System.out.println("TMX layer not found: " + tmxLayerName);
            return null;
        }

        Element dataElement = (Element) layerElement.getElementsByTagName("data").item(0);
        String csv = dataElement.getTextContent().trim();

        String[] tokens = csv.split(",");

        MapLayer layer = new MapLayer(internalLayerName, rows, cols);

        int index = 0;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                if (index >= tokens.length) {
                    layer.tiles[row][col] = new LayerTile();
                    continue;
                }

                int gid = Integer.parseInt(tokens[index].trim());

                if (collisionMode) {
                    if (gid == 0) {
                        layer.tiles[row][col] = new LayerTile("", -1);
                    } else {
                        layer.tiles[row][col] = new LayerTile("collision", 1);
                    }
                } else {
                    if (gid == 0) {
                        layer.tiles[row][col] = new LayerTile("", -1);
                    } else {
                        TilesetRange range = findTilesetRangeForGid(ranges, gid);

                        if (range == null || range.atlasName.isEmpty()) {
                            layer.tiles[row][col] = new LayerTile("", -1);
                        } else {
                            int localId = gid - range.firstGid;
                            layer.tiles[row][col] = new LayerTile(range.atlasName, localId);
                        }
                    }
                }

                index++;
            }
        }

        return layer;
    }

    private String mapTsxSourceToAtlasName(String source) {
        if (source == null) return "";

        if (source.endsWith("basic_terrain.tsx")) return "basic_terrain";
        if (source.endsWith("water.tsx")) return "water";
        if (source.endsWith("decor.tsx")) return "other";
        if (source.endsWith("other.tsx")) return "other";
        if (source.endsWith("Houses.tsx")) return "Houses";
        if (source.endsWith("cave.tsx")) return "cave";
        if (source.endsWith("mountains.tsx")) return "mountains";
        if (source.endsWith("mountains2.tsx")) return "mountains2";
        if (source.endsWith("water_animated.tsx")) return "water_animated";

        return "";
    }

    private ArrayList<TilesetRange> getTilesetRanges(Document doc) {
        ArrayList<TilesetRange> ranges = new ArrayList<>();

        NodeList tilesets = doc.getElementsByTagName("tileset");

        for (int i = 0; i < tilesets.getLength(); i++) {
            Element ts = (Element) tilesets.item(i);

            int firstGid = Integer.parseInt(ts.getAttribute("firstgid"));
            String source = ts.getAttribute("source");
            String atlasName = mapTsxSourceToAtlasName(source);

            ranges.add(new TilesetRange(firstGid, source, atlasName));
        }

        ranges.sort((a, b) -> Integer.compare(a.firstGid, b.firstGid));
        return ranges;
    }

    private TilesetRange findTilesetRangeForGid(ArrayList<TilesetRange> ranges, int gid) {
        TilesetRange result = null;

        for (TilesetRange range : ranges) {
            if (gid >= range.firstGid) {
                result = range;
            } else {
                break;
            }
        }

        return result;
    }

    private Element findObjectGroupElement(Document doc, String layerName) {
        NodeList groups = doc.getElementsByTagName("objectgroup");

        for (int i = 0; i < groups.getLength(); i++) {
            Element group = (Element) groups.item(i);
            String name = group.getAttribute("name");
            if (name.equalsIgnoreCase(layerName)) {
                return group;
            }
        }

        return null;
    }

    private ArrayList<TiledObjectData> loadTMXObjects(Document doc, String objectGroupName) {
        ArrayList<TiledObjectData> result = new ArrayList<>();

        Element group = findObjectGroupElement(doc, objectGroupName);
        if (group == null) return result;

        NodeList objects = group.getElementsByTagName("object");

        for (int i = 0; i < objects.getLength(); i++) {
            Element obj = (Element) objects.item(i);

            TiledObjectData data = new TiledObjectData();
            data.layerName = objectGroupName;
            data.name = obj.getAttribute("name");
            data.type = obj.getAttribute("type");

            data.x = (int) Double.parseDouble(obj.getAttribute("x"));
            data.y = (int) Double.parseDouble(obj.getAttribute("y"));

            String w = obj.getAttribute("width");
            String h = obj.getAttribute("height");

            data.width = w.isEmpty() ? 0 : (int) Double.parseDouble(w);
            data.height = h.isEmpty() ? 0 : (int) Double.parseDouble(h);

            NodeList propsList = obj.getElementsByTagName("property");
            for (int p = 0; p < propsList.getLength(); p++) {
                Element prop = (Element) propsList.item(p);
                String key = prop.getAttribute("name");
                String value = prop.getAttribute("value");
                data.properties.put(key, value);
            }

            result.add(data);
        }

        return result;
    }

    public TiledObjectData findMapObjectByName(int mapIndex, String objectName) {
        AdvancedMapData map = getMap(mapIndex);
        if (map == null) return null;

        for (TiledObjectData obj : map.objects) {
            if (obj.name != null && obj.name.equalsIgnoreCase(objectName)) {
                return obj;
            }
        }

        return null;
    }

    public ArrayList<TiledObjectData> getMapObjectsByLayer(int mapIndex, String layerName) {
        ArrayList<TiledObjectData> result = new ArrayList<>();
        AdvancedMapData map = getMap(mapIndex);
        if (map == null) return result;

        for (TiledObjectData obj : map.objects) {
            if (obj.layerName != null && obj.layerName.equalsIgnoreCase(layerName)) {
                result.add(obj);
            }
        }

        return result;
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
        drawAtlasLayer(g2, currentMap.getLayer("underground"));
        drawAtlasLayer(g2, currentMap.getLayer("ground"));
        drawAtlasLayer(g2, currentMap.getLayer("decor"));
        drawAtlasLayer(g2, currentMap.getLayer("water"));
    }

    private void drawAtlasLayer(Graphics2D g2, MapLayer layer) {
        if (layer == null || !layer.visible) return;

        for (int row = 0; row < layer.rows; row++) {
            for (int col = 0; col < layer.cols; col++) {
                LayerTile layerTile = layer.tiles[row][col];
                if (layerTile == null || layerTile.isEmpty()) continue;

                TilesetAtlas atlas = getAtlas(layerTile.atlasName);
                if (atlas == null) continue;

                int worldX = col * gp.tileSize;
                int worldY = row * gp.tileSize;
                int screenX = worldX - gp.worldX;
                int screenY = worldY - gp.worldY;

                if (worldX + gp.tileSize > gp.worldX &&
                    worldX - gp.tileSize < gp.worldX + gp.screenWidth &&
                    worldY + gp.tileSize > gp.worldY &&
                    worldY - gp.tileSize < gp.worldY + gp.screenHeight) {

                    BufferedImage tileImage = atlas.getTileImage(layerTile.tileId);
                    if (tileImage != null) {
                        g2.drawImage(tileImage, screenX, screenY, gp.tileSize, gp.tileSize, null);
                    }
                }
            }
        }
    }
}