public class Enemy_AshenGuardian extends Enemy {

    public Enemy_AshenGuardian(GamePanel gp) {
        super(gp);

        name = "Ashen Guardian";
        enemyId = "ashen_guardian";
        enemyType = "ashen_guardian";

        maxHp = 100;
        hp = 100;

        maxMp = 30;
        mp = 30;

        attack = 10;
        defense = 6;
        magicAttack = 14;
        magicDefense = 10;
        speed_stat = 7;

        exp = 140;
        goldReward = 100;

        battleBg = "ruins";
        groundType = "ruins";

        spriteSize = 128;

        solid = true;
        moving = false;

        try {
            loadAnimations("ashen_guardian", 128);

            if (anim != null && anim.getCurrentImage() != null) {
                currentImage = anim.getCurrentImage();
            }
        } catch (Exception e) {
            System.out.println("Failed to load Ashen Guardian animations.");
            e.printStackTrace();
        }
    }
}
