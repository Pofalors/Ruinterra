import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

public class Chest {

    public int worldX, worldY;
    public boolean opened = false;

    public String chestId;
    public String itemId;
    public int amount;

    public BufferedImage closedImage;
    public BufferedImage openedImage;

    public Chest(String chestId, int worldX, int worldY, String itemId, int amount) {
        this.chestId = chestId;
        this.worldX = worldX;
        this.worldY = worldY;
        this.itemId = itemId;
        this.amount = amount;

        try {
            closedImage = ImageIO.read(new File("res/items/chest_closed.png"));
            openedImage = ImageIO.read(new File("res/items/chest_opened.png"));
        } catch (Exception e) {
            System.out.println("Failed to load chest images.");
            e.printStackTrace();
        }
    }

    public void draw(Graphics2D g2, GamePanel gp) {
        int screenX = worldX - gp.worldX;
        int screenY = worldY - gp.worldY;

        BufferedImage img = opened ? openedImage : closedImage;

        if (img != null) {
            g2.drawImage(img, screenX, screenY, gp.tileSize, gp.tileSize, null);
        }
    }
}
