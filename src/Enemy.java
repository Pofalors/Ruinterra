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

    // Μέθοδος για επιβράβευση όταν πεθαίνει - τώρα επιστρέφει array {exp, gold}
    public int[] giveRewards(Entity player) {
        // Τυχαίο EXP (10-50)
        int expReward = (int)(Math.random() * 40) + 10;
        // Τυχαίο gold (5-30)
        int goldReward = (int)(Math.random() * 25) + 5;
        
        // Δώσε τα rewards
        player.addExp(expReward);
        player.gold += goldReward;
        
        // Αποθήκευσε το μήνυμα για εμφάνιση
        if (gp != null) {
            gp.battleMessage = "Νίκη! +" + expReward + " EXP, +" + goldReward + " Gold!";
        }
        
        System.out.println("Πήρες " + expReward + " EXP και " + goldReward + " gold!");
        
        return new int[] {expReward, goldReward};
    }
}