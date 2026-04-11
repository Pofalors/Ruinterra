public class EnemyFactory {

    public static Enemy createEnemy(GamePanel gp, String enemyId) {
        if (enemyId == null) return null;

        switch (enemyId.trim().toLowerCase()) {
            case "goblin":
                return new Enemy_Goblin(gp);

            case "mushroom":
                return new Enemy_Mushroom(gp);

            case "skeleton":
                return new Enemy_Skeleton(gp);

            case "ashen_guardian":
                return new Enemy_AshenGuardian(gp);

            default:
                System.out.println("Unknown enemy id: " + enemyId);
                return null;
        }
    }
}
