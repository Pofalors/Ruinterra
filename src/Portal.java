public class Portal {
    public int worldX, worldY;
    public int sourceMap;
    public int targetMap;
    public int targetX, targetY;
    
    public Portal(int sourceMap, int worldX, int worldY, int targetMap, int targetX, int targetY) {
        this.sourceMap = sourceMap;
        this.worldX = worldX;
        this.worldY = worldY;
        this.targetMap = targetMap;
        this.targetX = targetX;
        this.targetY = targetY;
    }
}
