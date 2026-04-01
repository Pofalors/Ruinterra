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

    public ArrayList<BattleEntity> getUpcomingTurns(int count) {
        ArrayList<BattleEntity> result = new ArrayList<>();

        if (turnOrder.isEmpty() || count <= 0) {
            return result;
        }

        int size = turnOrder.size();
        int startIndex = currentTurnIndex + 1;

        for (int i = 0; i < count; i++) {
            BattleEntity entity = turnOrder.get((startIndex + i) % size);
            if (entity != null && entity.isAlive()) {
                result.add(entity);
            }
        }

        return result;
    }

    public ArrayList<BattleEntity> getNextRoundPreview(int count) {
        ArrayList<BattleEntity> result = new ArrayList<>();
        ArrayList<BattleEntity> sorted = new ArrayList<>();

        for (BattleEntity entity : party) {
            if (entity != null && entity.isAlive()) {
                sorted.add(entity);
            }
        }

        for (BattleEntity entity : enemies) {
            if (entity != null && entity.isAlive()) {
                sorted.add(entity);
            }
        }

        Collections.sort(sorted, new Comparator<BattleEntity>() {
            @Override
            public int compare(BattleEntity e1, BattleEntity e2) {
                return Integer.compare(e2.speed, e1.speed);
            }
        });

        for (int i = 0; i < sorted.size() && i < count; i++) {
            result.add(sorted.get(i));
        }

        return result;
    }

    public ArrayList<BattleEntity> buildSpeedOrderSnapshot() {
        ArrayList<BattleEntity> result = new ArrayList<>();

        for (BattleEntity entity : party) {
            if (entity != null && entity.isAlive()) {
                result.add(entity);
            }
        }

        for (BattleEntity entity : enemies) {
            if (entity != null && entity.isAlive()) {
                result.add(entity);
            }
        }

        Collections.sort(result, new Comparator<BattleEntity>() {
            @Override
            public int compare(BattleEntity e1, BattleEntity e2) {
                return Integer.compare(e2.speed, e1.speed);
            }
        });

        return result;
    }

    public void startTurn(BattleEntity entity) {
        if (entity == null || !entity.isAlive()) return;

        entity.defending = false;
        entity.enterState(CombatState.READY);

        if (entity.playerRef != null && entity.playerRef.gp != null) {
            entity.playerRef.gp.sound.playBattleSE("NEXTTURN");

            if (entity.name.equals("Assassin")) {
                entity.playerRef.gp.sound.playBattleSE("TURNASSASSIN");
            } else if (entity.name.equals("Mage")) {
                entity.playerRef.gp.sound.playBattleSE("TURNMAGE");
            } else {
                // Όταν βάλεις hero turn voice:
                // entity.playerRef.gp.sound.playBattleSE("TURNHERO");
            }
        }
    }

    public void nextTurn() {
        if (turnOrder.isEmpty()) return;

        currentTurnIndex++;

        // Αν τελείωσε ο κύκλος, ξεκινά νέο round
        // και ξαναϋπολογίζουμε το order με βάση το speed
        if (currentTurnIndex >= turnOrder.size()) {
            startNewRoundBP();
            calculateTurnOrder();
            return;
        }

        BattleEntity current = getCurrentTurn();
        if (current != null && current.isAlive()) {
            startTurn(current);
        }
    }

    public void startNewRoundBP() {
        for (BattleEntity entity : party) {
            if (entity.isAlive()) {
                entity.gainBP();
            }
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
