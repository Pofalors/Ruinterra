public class Enemy extends Entity {
    protected int hp, maxHp, attack, defense, exp; // protected για πρόσβαση από παιδιά
    
    public Enemy(GamePanel gp) {
        super(gp);
        solid = true;
        moving = false; // Οι εχθροί δεν κινούνται στο χάρτη
    }
    
    public void update() {
        // Μόνο animation
        animCounter++;
        if (animCounter > 10) {
            frame = (frame == 0) ? 1 : 0;
            animCounter = 0;
        }
        currentImage = (frame == 0) ? down1 : down2;
    }

    // Μέθοδος για επιβράβευση όταν πεθαίνει
    public void giveRewards(Entity player) {
        player.addExp(exp);
        // Τυχαίο gold
        int goldReward = (int)(Math.random() * 20) + 5;
        player.gold += goldReward;
        System.out.println("Πήρες " + goldReward + " gold!");
        
        // Τυχαίο item drop (θα το φτιάξουμε αργότερα)
    }
}