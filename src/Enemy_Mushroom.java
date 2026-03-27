import java.io.File;
import javax.imageio.ImageIO;

public class Enemy_Mushroom extends Enemy {
    public Enemy_Mushroom(GamePanel gp) {
        super(gp);
        try {
            down1 = ImageIO.read(new File("res/enemy/mushroom.png"));
            down2 = ImageIO.read(new File("res/enemy/mushroom.png"));
            currentImage = down1;
            
            // Stats
            level = 1;
            maxHp = 18;
            hp = maxHp;
            attack = 5;
            defense = 1;
            magicAttack = 3;
            magicDefense = 2;
            speed_stat = 3;
            exp = 12;

            name = "Mushroom";
            enemyType = "mushroom";
            spriteSize = 48;
            // Φόρτωσε animations (48x48)
            loadAnimations("mushroom", 128);
            
            // Fallback στην παλιά εικόνα αν αποτύχει
            try {
                if (anim == null) {
                    down1 = ImageIO.read(new File("res/enemy/mushroom.png"));
                    currentImage = down1;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
