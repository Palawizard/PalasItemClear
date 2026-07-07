package net.palasitemclear.bin;

import java.util.List;

public final class RecoveryBinPagination {
    public static final int ITEMS_PER_PAGE = 45;
    public static final int CONTAINER_SIZE = 54;
    public static final int NAV_ROW_START = 45;
    public static final int SLOT_PREVIOUS = 48;
    public static final int SLOT_PAGE_INFO = 49;
    public static final int SLOT_NEXT = 50;

    private RecoveryBinPagination() {
    }

    public static int pageCount(int entryCount) {
        if (entryCount <= 0) {
            return 1;
        }

        return (entryCount + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE;
    }

    public static List<RecoveryBinEntry> slice(List<RecoveryBinEntry> entries, int page) {
        if (page < 0 || entries.isEmpty()) {
            return List.of();
        }

        int fromIndex = page * ITEMS_PER_PAGE;
        if (fromIndex >= entries.size()) {
            return List.of();
        }

        int toIndex = Math.min(fromIndex + ITEMS_PER_PAGE, entries.size());
        return entries.subList(fromIndex, toIndex);
    }

    public static boolean isNavigationSlot(int containerSlot) {
        return containerSlot >= NAV_ROW_START;
    }

    public static boolean isPreviousSlot(int containerSlot) {
        return containerSlot == SLOT_PREVIOUS;
    }

    public static boolean isNextSlot(int containerSlot) {
        return containerSlot == SLOT_NEXT;
    }
}
