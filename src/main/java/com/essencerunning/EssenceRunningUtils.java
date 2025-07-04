package com.essencerunning;

import com.google.common.collect.ArrayListMultimap;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuShouldLeftClick;
import net.runelite.api.widgets.Widget;
import net.runelite.client.util.Text;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EssenceRunningUtils {

    private static final String TRADING_WITH = "Trading with:<br>";

    public static void swap(final Client client,
                            final ArrayListMultimap<String, Integer> optionIndexes,
                            final String optionA,
                            final String optionB,
                            final String target,
                            final int index,
                            final boolean strict) {
        Menu menu = client.getMenu();
        final MenuEntry[] menuEntries = menu.getMenuEntries();
        final int thisIndex = findIndex(optionIndexes, menuEntries, index, optionB, target, strict);
        final int optionIdx = findIndex(optionIndexes, menuEntries, thisIndex, optionA, target, strict);

        if (thisIndex >= 0 && optionIdx >= 0) {
            swap(client, optionIndexes, menuEntries, optionIdx, thisIndex);
        }
    }

    private static int findIndex(final ArrayListMultimap<String, Integer> optionIndexes,
                                 final MenuEntry[] entries,
                                 final int limit,
                                 final String option,
                                 final String target,
                                 final boolean strict) {

        if (strict) {
            List<Integer> indexes = optionIndexes.get(option);

            // We want the last index which matches the target, as that is what is top-most on the menu
            for (int i = indexes.size() - 1; i >= 0; --i) {
                final int idx = indexes.get(i);
                MenuEntry entry = entries[idx];
                String entryTarget = Text.removeTags(entry.getTarget()).toLowerCase();

                // Limit to the last index which is prior to the current entry
                if (idx <= limit && entryTarget.equals(target)) {
                    return idx;
                }
            }
        } else {
            // Without strict matching we have to iterate all entries up to the current limit...
            for (int i = limit; i >= 0; i--) {
                final MenuEntry entry = entries[i];
                final String entryOption = Text.removeTags(entry.getOption()).toLowerCase();
                final String entryTarget = Text.removeTags(entry.getTarget()).toLowerCase();

                if (entryOption.contains(option.toLowerCase()) && entryTarget.equals(target)) {
                    return i;
                }
            }

        }

        return -1;
    }

    private static void swap(final Client client,
                             final ArrayListMultimap<String, Integer> optionIndexes,
                             final MenuEntry[] entries,
                             final int index1,
                             final int index2) {
        Menu menu = client.getMenu();

        final MenuEntry entry = entries[index1];
        entry.setType((entries[index2].getType()));
        entries[index1] = entries[index2];
        entries[index2] = entry;

        menu.setMenuEntries(entries);

        // Rebuild option indexes
        optionIndexes.clear();
        int idx = 0;
        for (MenuEntry menuEntry : entries) {
            final String option = Text.removeTags(menuEntry.getOption()).toLowerCase();
            optionIndexes.put(option, idx++);
        }
    }

    public static void swapBankOp(final Client client, final MenuEntryAdded menuEntryAdded) {

        // Deposit- op 2 is the current deposit amount 1/5/10/x
        if (menuEntryAdded.getType() == MenuAction.CC_OP.getId() && menuEntryAdded.getIdentifier() == 2
            && menuEntryAdded.getOption().startsWith("Deposit-")) {
            Menu menu = client.getMenu();
            final MenuEntry[] menuEntries = menu.getMenuEntries();

            // Find the extra menu option; they don't have fixed names, so check based on the menu identifier
            for (int i = menuEntries.length - 1; i >= 0; --i) {
                final MenuEntry entry = menuEntries[i];

                // The extra options are always option 9
                if (entry.getType() == MenuAction.CC_OP_LOW_PRIORITY && entry.getIdentifier() == 9
                    && !entry.getOption().equals("Empty")) { // exclude Runecraft pouch's "Empty" option

                    // we must also raise the priority of the op so it doesn't get sorted later
                    entry.setType(MenuAction.CC_OP);

                    menuEntries[i] = menuEntries[menuEntries.length - 1];
                    menuEntries[menuEntries.length - 1] = entry;

                    menu.setMenuEntries(menuEntries);
                    break;
                }
            }
        }
    }

    public static boolean itemEquipped(final Client client, final EquipmentInventorySlot slot) {
        final ItemContainer equipment = client.getItemContainer(InventoryID.WORN);
        if (equipment != null) {
            final Item[] item = equipment.getItems();
            return item.length > slot.getSlotIdx()
                && item[slot.getSlotIdx()] != null
                && item[slot.getSlotIdx()].getId() > -1;
        }
        return false;
    }

    public static void computeItemsTraded(final Client client, final EssenceRunningSession session) {
        final Widget tradingWith = client.getWidget(334, 30);
        final String tradingPartnerRsn = tradingWith.getText().replace(TRADING_WITH, "");
        final Widget partnerTrades = client.getWidget(334, 29);

        int pureEssenceTraded = 0;
        int bindingNecklaceTraded = 0;
        for (Widget widget : partnerTrades.getChildren()) {
            if (widget.getText().equals("Pure essence")) {
                pureEssenceTraded++;
            } else if (widget.getText().equals("Binding necklace")) {
                bindingNecklaceTraded++;
            }
        }
        session.updateRunnerStatistic(tradingPartnerRsn, pureEssenceTraded, bindingNecklaceTraded);
    }

    public static Map<Integer, String> getClanMessagesMap(final int size) {
        return new LinkedHashMap<Integer, String>(size) {
            @Override
            public boolean removeEldestEntry(Map.Entry<Integer, String> eldest) {
                return this.size() > size;
            }
        };
    }
}
