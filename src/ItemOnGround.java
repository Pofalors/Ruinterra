import java.awt.image.BufferedImage;

public class ItemOnGround {
    public BufferedImage image;
    public String name;
    public int worldX, worldY;
    public Item item; // Το αντικείμενο που θα μπει στο inventory
    
    public ItemOnGround(String name, BufferedImage image, int worldX, int worldY, Item item) {
        this.name = name;
        this.image = image;
        this.worldX = worldX;
        this.worldY = worldY;
        this.item = item;
    }
}