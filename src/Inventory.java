import java.util.ArrayList;

public class Inventory {
    // Items αποθήκης (4x2 = 8 slots)
    public Item[] storage = new Item[8];

    // Key Items (4x2 = 8 slots)
    public Item[] keyItems = new Item[8];
    
    // Εξοπλισμός (3x3 = 9 slots) - με τις νέες ονομασίες
    public Item ring;        // Slot 0 - Πάνω αριστερά
    public Item helmet;      // Slot 1 - Πάνω μέση
    public Item necklace;    // Slot 2 - Πάνω δεξιά
    public Item sword;       // Slot 3 - Μέση αριστερά
    public Item chest;       // Slot 4 - Μέση μέση
    public Item shield;      // Slot 5 - Μέση δεξιά
    public Item gloves;      // Slot 6 - Κάτω αριστερά
    public Item belt;        // Slot 7 - Κάτω μέση
    public Item boots;       // Slot 8 - Κάτω δεξιά
    
    public int selectedStorageSlot = 0;
    public int selectedEquipSlot = 0;
    public int selectedKeyItemSlot = 0;
    public int inventoryMode = 0; // 0=Storage, 1=Equipment, 2=KeyItems
    // ===== OCTOPATH-STYLE MENU STATE =====
    public int menuSection = 0;         // 0=Items, 1=Equipment, 2=Status, 3=WorldMap, 4=Journal, 5=Save, 6=Options
    public int menuFocus = 0;           // 0=left command list, 1=center content
    public int selectedItemCategory = 0; // 0=Consumables, 1=Key Items
    public int selectedPartyMember = 0;  // για Status / Equipment αργότερα
    public boolean hideDetails = false;
    // ===== NEW ITEMS TAB STATE =====
    public int selectedConsumableIndex = 0;
    public int selectedEquipmentItemIndex = 0;
    public int selectedKeyItemIndex = 0;

    public boolean itemTargetSelectOpen = false;
    public int selectedItemTarget = 0; // 0=Hero, 1=partyMembers[0], 2=partyMembers[1]...
    // ===== NEW UNIFIED ITEMS TAB =====
    public int selectedItemsCategory = 0;   // 0=ALL, 1=POTIONS, 2=WEAPONS, 3=SHIELDS, 4=HELMETS, 5=BODY_ARMOR, 6=NECKLACES, 7=CONSUMABLES, 8=SCROLLS
    public int selectedItemsListIndex = 0;
    // ===== ITEMS USE FLOW =====
    public boolean itemUseTargetMode = false;
    public int itemUseTargetIndex = 0;
    // ===== STATUS FULLSCREEN WINDOW =====
    public boolean statusDetailOpen = false;
    public int selectedEquipmentListIndex = 0;
    // ===== STATUS EQUIP POPUP =====
    public boolean statusEquipPopupOpen = false;
    public int statusEquipPopupOption = 0; // 0=Yes, 1=No
    // ===== WORLD MAP =====
    public boolean worldMapOpenFromMenu = false;
    public int worldMapTransitionAlpha = 0;
    public boolean worldMapOpening = false;
    public boolean worldMapClosing = false;
    // ===== JOURNAL =====
    public boolean journalOpenFromMenu = false;
    
    public Inventory() {
        for (int i = 0; i < storage.length; i++) {
            storage[i] = null;
        }
        for (int i = 0; i < keyItems.length; i++) {
            keyItems[i] = null;
        }
    }
    
    public boolean addItem(Item item) {
        if (item.isKeyItem) {
            // Τα key items μπαίνουν στο keyItems array
            for (int i = 0; i < keyItems.length; i++) {
                if (keyItems[i] == null) {
                    keyItems[i] = item;
                    return true;
                }
            }
            return false; // Γεμάτο
        } else {
            // Κανονικά items στο storage
            for (int i = 0; i < storage.length; i++) {
                if (storage[i] == null) {
                    storage[i] = item;
                    return true;
                }
                else if (storage[i].name.equals(item.name) && storage[i].stackable) {
                    storage[i].amount += item.amount;
                    return true;
                }
            }
            return false;
        }
    }
    
    public void removeFromStorage(int index) {
        if (index >= 0 && index < storage.length && storage[index] != null) {
            if (storage[index].stackable && storage[index].amount > 1) {
                storage[index].amount--;
            } else {
                storage[index] = null;
            }
        }
    }

    public void removeFromKeyItems(int index) {
        if (index >= 0 && index < keyItems.length && keyItems[index] != null) {
            keyItems[index] = null;
        }
    }
    
    public void equipItem(int storageIndex, int equipSlot) {
        if (storageIndex < 0 || storageIndex >= storage.length) return;
        if (storage[storageIndex] == null) return;
        
        Item itemToEquip = storage[storageIndex];
        
        // Αν το slot προορισμού έχει ήδη item, βάλτο πίσω στο storage
        Item oldItem = getEquipSlot(equipSlot);
        if (oldItem != null) {
            // Προσπάθησε να το βάλεις στο storage
            if (addItem(oldItem)) {
                // Αν προστέθηκε, προχώρα
            } else {
                // Αν το storage είναι γεμάτο, μην κάνεις τίποτα
                return;
            }
        }
        
        // Αφαίρεσε από storage
        if (itemToEquip.stackable && itemToEquip.amount > 1) {
            itemToEquip.amount--;
            Item equipCopy = new Item(itemToEquip.name);
            equipCopy.healAmount = itemToEquip.healAmount;
            equipCopy.attackBonus = itemToEquip.attackBonus;
            equipCopy.image = itemToEquip.image;
            equipCopy.stackable = false;
            equipCopy.amount = 1;
            setEquipSlot(equipSlot, equipCopy);
        } else {
            storage[storageIndex] = null;
            setEquipSlot(equipSlot, itemToEquip);
        }
    }
    
    public void unequipItem(int equipSlot) {
        Item item = getEquipSlot(equipSlot);
        if (item == null) return;
        
        if (addItem(item)) {
            setEquipSlot(equipSlot, null);
        }
    }
    
    private void setEquipSlot(int slot, Item item) {
        switch(slot) {
            case 0: ring = item; break;
            case 1: helmet = item; break;
            case 2: necklace = item; break;
            case 3: sword = item; break;
            case 4: chest = item; break;
            case 5: shield = item; break;
            case 6: gloves = item; break;
            case 7: belt = item; break;
            case 8: boots = item; break;
        }
    }
    
    public Item getEquipSlot(int slot) {
        switch(slot) {
            case 0: return ring;
            case 1: return helmet;
            case 2: return necklace;
            case 3: return sword;
            case 4: return chest;
            case 5: return shield;
            case 6: return gloves;
            case 7: return belt;
            case 8: return boots;
            default: return null;
        }
    }

    // Υπολογίζει το συνολικό attack από όλο τον εξοπλισμό
    public int getTotalAttack() {
        int total = 0;
        for (int i = 0; i < 9; i++) {
            Item item = getEquipSlot(i);
            if (item != null) {
                total += item.attackBonus;
            }
        }
        return total;
    }

    // Υπολογίζει το συνολικό defense από όλο τον εξοπλισμό
    public int getTotalDefense() {
        int total = 0;
        for (int i = 0; i < 9; i++) {
            Item item = getEquipSlot(i);
            if (item != null) {
                total += item.defenseBonus; // Θα προσθέσουμε αυτό το πεδίο
            }
        }
        return total;
    }

    // Υπολογίζει το συνολικό magic attack
    public int getTotalMagicAttack() {
        int total = 0;
        for (int i = 0; i < 9; i++) {
            Item item = getEquipSlot(i);
            if (item != null) {
                total += item.magicBonus;
            }
        }
        return total;
    }
}