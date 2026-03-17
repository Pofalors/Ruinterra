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
        
        // Ταξινόμηση με βάση το speed_stat (από υψηλό σε χαμηλό)
        Collections.sort(turnOrder, new Comparator<BattleEntity>() {
            @Override
            public int compare(BattleEntity e1, BattleEntity e2) {
                return Integer.compare(e2.speed, e1.speed);
            }
        });
        
        currentTurnIndex = 0;
        battleEnded = false; // ΕΠΑΝΑΦΟΡΑ
    }

    public void calculateRandomTurnOrder() {
        turnOrder.clear();
        turnOrder.addAll(party);
        turnOrder.addAll(enemies);
        
        // Ανακάτεμα της σειράς
        Collections.shuffle(turnOrder);
        
        currentTurnIndex = 0;
        battleEnded = false;
        
        // Εξασφάλισε ότι στον πρώτο γύρο θα έχουμε 2 καλούς πριν τους κακούς
        if (!turnOrder.isEmpty()) {
            // Μέτρησε πόσοι καλοί και κακοί υπάρχουν
            int firstPlayerIndex = -1;
            int secondPlayerIndex = -1;
            int firstEnemyIndex = -1;
            
            // Βρες τις θέσεις
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
            
            // Αν ο πρώτος εχθρός είναι πριν από τον δεύτερο καλό, κάνε swap
            if (firstEnemyIndex != -1 && secondPlayerIndex != -1 && 
                firstEnemyIndex < secondPlayerIndex) {
                // Αντάλλαξε τον πρώτο εχθρό με τον δεύτερο καλό
                Collections.swap(turnOrder, firstEnemyIndex, secondPlayerIndex);
            }
            
            // Αν ο πρώτος εχθρός είναι πριν από τον πρώτο καλό (απίθανο, αλλά για σιγουριά)
            if (firstEnemyIndex != -1 && firstPlayerIndex != -1 && 
                firstEnemyIndex < firstPlayerIndex) {
                Collections.swap(turnOrder, firstEnemyIndex, firstPlayerIndex);
            }
        }
    }
    
    public BattleEntity getCurrentTurn() {
        if (turnOrder.isEmpty() || currentTurnIndex >= turnOrder.size()) {
            return null;
        }
        return turnOrder.get(currentTurnIndex);
    }
    
    public void nextTurn() {
        currentTurnIndex++;
        if (currentTurnIndex >= turnOrder.size()) {
            currentTurnIndex = 0;
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
        // Αφαίρεσε νεκρές οντότητες από το turnOrder
        turnOrder.removeIf(e -> !e.isAlive());
        
        // Αν η τρέχουσα οντότητα έχει πεθάνει ή ο δείκτης είναι εκτός ορίων, διόρθωσε
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
