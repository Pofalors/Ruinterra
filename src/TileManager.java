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

        addAdvancedGeneratedTestMap();
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

    public void addAdvancedGeneratedTestMap() {
        AdvancedMapData map = new AdvancedMapData("GeneratedTerrainTest", 20, 15);
        map.legacy = false;
        map.tilesetName = "basic_terrain";

        MapLayer ground = new MapLayer("ground", 15, 20);
        MapLayer decor = new MapLayer("decor", 15, 20);
        MapLayer collision = new MapLayer("collision", 15, 20);

        clearLayer(ground);
        clearLayer(decor);

        for (int row = 0; row < collision.rows; row++) {
            for (int col = 0; col < collision.cols; col++) {
                collision.tiles[row][col] = 0;
            }
        }

        AtlasManifest manifest = getManifest("basic_terrain");
        if (manifest == null) {
            System.out.println("No manifest found for basic_terrain");
            return;
        }

        // Βάλε βασικό grass background αν υπάρχει
        //if (manifest.hasTile("GRASS_FILL")) {
            //fillLayer(ground, manifest.getRequiredTileId("GRASS_FILL"));
        //}

        paintTerrainRectangle(ground, decor, "basic_terrain", "DIRT", "GRASS", 1, 1, 6, 5);
        paintTerrainRectangle(ground, decor, "basic_terrain", "GRASS", "DIRT", 8, 1, 6, 5);
        paintTerrainRectangle(ground, decor, "basic_terrain", "DIRT", "SAND", 1, 8, 6, 5);
        paintTerrainRectangle(ground, decor, "basic_terrain", "DIRT", "SNOW", 8, 8, 6, 5);

        // 2x2 variants
        placeVariant2x2(decor, "basic_terrain", "GRASS_PATH", 2, 6);
        placeVariant2x2(decor, "basic_terrain", "FLOWER_PATH", 5, 6);
        placeVariant2x2(decor, "basic_terrain", "DIRT_PATH", 8, 6);
        placeVariant2x2(decor, "basic_terrain", "SNOW_PATH", 11, 6);
        placeVariant2x2(decor, "basic_terrain", "SAND_PATH", 14, 6);

        // Περιμετρικό collision wall
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

        System.out.println("Generated advanced terrain test map added.");
    }

    // =========================================================
    // ADVANCED MAP PLACEHOLDER (για αργότερα)
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
        TilesetAtlas atlas = getAtlas(currentMap.tilesetName);

        drawAtlasLayer(g2, currentMap.getLayer("ground"), atlas);
        drawAtlasLayer(g2, currentMap.getLayer("decor"), atlas);
    }

    private void drawAtlasLayer(Graphics2D g2, MapLayer layer, TilesetAtlas atlas) {
        if (layer == null || !layer.visible || atlas == null) return;

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