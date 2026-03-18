import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class BattleScene {
    public BufferedImage background;
    public ArrayList<BattleEnemy> enemies = new ArrayList<>();
    public ArrayList<BattlePlayer> players = new ArrayList<>();

    // 0=entering, 1=battle, 2=victory, 3=defeat
    public int state = 0;
    public int timer = 0;

    public void reset() {
        enemies.clear();
        players.clear();
        state = 0;
        timer = 0;
        background = null;
    }
}
