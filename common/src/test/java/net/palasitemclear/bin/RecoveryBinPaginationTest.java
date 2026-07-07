package net.palasitemclear.bin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class RecoveryBinPaginationTest {
    @Test
    void calculatesPageCount() {
        assertEquals(1, RecoveryBinPagination.pageCount(0));
        assertEquals(1, RecoveryBinPagination.pageCount(45));
        assertEquals(2, RecoveryBinPagination.pageCount(46));
        assertEquals(3, RecoveryBinPagination.pageCount(100));
    }

    @Test
    void slicesEntriesPerPage() {
        List<RecoveryBinEntry> entries = new ArrayList<>();
        for (int id = 1; id <= 46; id++) {
            entries.add(RecoveryBinEntry.testEntry(id, null, null, 100L));
        }

        assertEquals(45, RecoveryBinPagination.slice(entries, 0).size());
        assertEquals(1, RecoveryBinPagination.slice(entries, 1).size());
        assertEquals(46, RecoveryBinPagination.slice(entries, 1).get(0).id());
        assertEquals(0, RecoveryBinPagination.slice(entries, 2).size());
    }
}
