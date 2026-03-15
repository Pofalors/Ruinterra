import java.io.File;
import javax.imageio.ImageIO;

public class Enemy_Goblin extends Enemy {
    public Enemy_Goblin(GamePanel gp) {
        super(gp);
        try {
            down1 = ImageIO.read(new File("res/enemy/goblin.png"));
            down2 = ImageIO.read(new File("res/enemy/goblin.png"));
            currentImage = down1;
            
            // Stats
            level = 2;
            maxHp = 25;
            hp = maxHp;
            attack = 7;
            defense = 2;
            magicAttack = 2;
            magicDefense = 2;
            speed_stat = 5;
            exp = 15;

            // Φόρτωσε animations (48x48)
            loadAnimations("goblin", 128);
            
            // Fallback
            try {
                if (anim == null) {
                    down1 = ImageIO.read(new File("res/enemy/goblin.png"));
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