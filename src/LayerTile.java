public class LayerTile {
    public String atlasName;
    public int tileId;

    public LayerTile() {
        this.atlasName = "";
        this.tileId = -1;
    }

    public LayerTile(String atlasName, int tileId) {
        this.atlasName = atlasName;
        this.tileId = tileId;
    }

    public boolean isEmpty() {
        return tileId < 0 || atlasName == null || atlasName.isEmpty();
    }
}