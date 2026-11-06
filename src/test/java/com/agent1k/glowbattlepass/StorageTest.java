package com.agent1k.glowbattlepass;

import com.agent1k.glowbattlepass.domain.*;
import com.agent1k.glowbattlepass.storage.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

final class StorageTest {
    @TempDir Path directory;
    @Test void databaseRetainsBalancesAndPendingTransfersAcrossRestart() throws Exception {
        UUID id = UUID.randomUUID(), transferId = UUID.randomUUID(); Path file = directory.resolve("state.db");
        try (StateStore store = new StateStore(file)) {
            store.transaction(s -> {
                Profile p = new Profile(id, "Player"); p.balance(100); p.progress().put("daily:stone", 2L); s.profiles.put(id, p);
                s.transfers.put(transferId, new Transfer(transferId, id, Transfer.Direction.DELIVERY, "encoded-item", true)); return true;
            });
        }
        try (StateStore store = new StateStore(file)) {
            assertEquals(100, store.state().profile(id).balance());
            assertEquals(2L, store.state().profile(id).progress().get("daily:stone").longValue());
            assertTrue(store.state().transfers.get(transferId).active());
            assertEquals("encoded-item", store.state().transfers.get(transferId).item());
        }
    }
    @Test void failedTransactionRollsBackMemoryAndDisk() throws Exception {
        UUID id = UUID.randomUUID(); Path file = directory.resolve("rollback.db");
        try (StateStore store = new StateStore(file)) {
            store.transaction(s -> { Profile p = new Profile(id, "Player"); p.balance(10); s.profiles.put(id, p); return true; });
            assertThrows(IllegalArgumentException.class, () -> store.transaction(s -> { s.profile(id).balance(20); throw new IllegalArgumentException("rollback"); }));
            assertEquals(10, store.state().profile(id).balance());
        }
        try (StateStore store = new StateStore(file)) { assertEquals(10, store.state().profile(id).balance()); }
    }
}
