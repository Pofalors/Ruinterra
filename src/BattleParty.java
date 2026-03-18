import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class BattleParty {
    public ArrayList<BattleEntity> party = new ArrayList<>();
    public ArrayList<BattleEntity> enemies = new ArrayList<>();
    public ArrayList<BattleEntity> turnOrder = new ArrayList<>();
    public int currentTurnIndex = 0;
    public boolean battleEnded = false;

    public void calculateTurnOrder() {
        turnOrder.clear();
        turnOrder.addAll(party);
        turnOrder.addAll(enemies);

        Collections.sort(turnOrder, new Comparator<BattleEntity>() {
            @Override
            public int compare(BattleEntity e1, BattleEntity e2) {
                return Integer.compare(e2.speed, e1.speed);
            }
        });

        currentTurnIndex = 0;
        battleEnded = false;

        if (!turnOrder.isEmpty()) {
            startTurn(turnOrder.get(currentTurnIndex));
        }
    }

    public void calculateRandomTurnOrder() {
        turnOrder.clear();
        turnOrder.addAll(party);
        turnOrder.addAll(enemies);

        Collections.shuffle(turnOrder);

        currentTurnIndex = 0;
        battleEnded = false;

        if (!turnOrder.isEmpty()) {
            int firstPlayerIndex = -1;
            int secondPlayerIndex = -1;
            int firstEnemyIndex = -1;

            for (int i = 0; i < turnOrder.size(); i++) {
                if (turnOrder.get(i).isPlayer) {
                    if (firstPlayerIndex == -1) {
                        firstPlayerIndex = i;
                    } else if (secondPlayerIndex == -1) {
                        secondPlayerIndex = i;
                    }
                } else {
                    if (firstEnemyIndex == -1) {
                        firstEnemyIndex = i;
                    }
                }
            }

            if (firstEnemyIndex != -1 && secondPlayerIndex != -1 &&
                firstEnemyIndex < secondPlayerIndex) {
                Collections.swap(turnOrder, firstEnemyIndex, secondPlayerIndex);
            }

            if (firstEnemyIndex != -1 && firstPlayerIndex != -1 &&
                firstEnemyIndex < firstPlayerIndex) {
                Collections.swap(turnOrder, firstEnemyIndex, firstPlayerIndex);
            }

            startTurn(turnOrder.get(currentTurnIndex));
        }
    }

    public BattleEntity getCurrentTurn() {
        if (turnOrder.isEmpty() || currentTurnIndex >= turnOrder.size()) {
            return null;
        }
        return turnOrder.get(currentTurnIndex);
    }

    public void startTurn(BattleEntity entity) {
        if (entity == null || !entity.isAlive()) return;

        entity.gainBP();
        entity.defending = false;
        entity.enterState(CombatState.READY);
    }

    public void nextTurn() {
        if (turnOrder.isEmpty()) return;

        currentTurnIndex++;
        if (currentTurnIndex >= turnOrder.size()) {
            currentTurnIndex = 0;
        }

        BattleEntity current = getCurrentTurn();
        if (current != null && current.isAlive()) {
            startTurn(current);
        }
    }

    public boolean isPlayerTurn() {
        BattleEntity current = getCurrentTurn();
        return current != null && party.contains(current);
    }

    public void removeDeadEntities() {
        party.removeIf(e -> !e.isAlive());
        enemies.removeIf(e -> !e.isAlive());

        if (party.isEmpty() || enemies.isEmpty()) {
            battleEnded = true;
        } else {
            updateTurnOrder();
        }
    }

    public void updateTurnOrder() {
        turnOrder.removeIf(e -> !e.isAlive());

        if (currentTurnIndex >= turnOrder.size()) {
            currentTurnIndex = 0;
        }

        if (turnOrder.isEmpty()) {
            battleEnded = true;
        }
    }

    public void syncPlayerHealth() {
        for (BattleEntity entity : party) {
            if (entity.isPlayer && entity.playerRef != null) {
                entity.playerRef.hp = entity.hp;
                entity.playerRef.mp = entity.mp;
            }
        }
    }
}
