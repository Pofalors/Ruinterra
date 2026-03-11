import java.io.File;
import javax.imageio.ImageIO;

public class Enemy_RedSlime extends Enemy {
    public Enemy_RedSlime(GamePanel gp) {
        super(gp);
        try {
            down1 = ImageIO.read(new File("res/enemy/red_slime.png"));
            down2 = ImageIO.read(new File("res/enemy/red_slime.png"));
            currentImage = down1;
            
            // Stats
            level = 2;
            maxHp = 25;
            hp = maxHp;
            attack = 7;
            defense = 2;
            magicAttack = 3;
            magicDefense = 2;
            speed_stat = 4;
            exp = 15;
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
