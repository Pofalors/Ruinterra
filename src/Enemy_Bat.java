import java.io.File;

import javax.imageio.ImageIO;

public class Enemy_Bat extends Enemy {
    public Enemy_Bat(GamePanel gp) {
        super(gp);
        try {
            down1 = ImageIO.read(new File("res/enemy/bat.png"));
            down2 = ImageIO.read(new File("res/enemy/bat.png"));
            currentImage = down1;
            
            // Stats
            level = 1;
            maxHp = 12;
            hp = maxHp;
            attack = 5;
            defense = 1;
            magicAttack = 2;
            magicDefense = 2;
            speed_stat = 6;
            exp = 8;
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
