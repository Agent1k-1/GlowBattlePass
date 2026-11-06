package com.agent1k.glowbattlepass.storage;

import org.jetbrains.annotations.NotNull;
import java.nio.file.*;
import java.sql.*;
import java.util.function.Function;

public final class StateStore implements StateRepository {
    private final Connection connection;
    private final StateCodec codec = new StateCodec();
    private State state;
    public StateStore(@NotNull Path file) throws Exception {
        Files.createDirectories(file.getParent()); Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath());
        try (Statement q = connection.createStatement()) {
            q.execute("PRAGMA journal_mode=WAL"); q.execute("PRAGMA synchronous=FULL");
            q.execute("CREATE TABLE IF NOT EXISTS snapshot (id INTEGER PRIMARY KEY CHECK(id=1), data TEXT NOT NULL)");
        }
        state = new State();
        try (Statement q = connection.createStatement(); ResultSet rows = q.executeQuery("SELECT data FROM snapshot WHERE id=1")) { if (rows.next()) state = codec.decode(rows.getString(1)); }
    }
    public @NotNull State state() { return state; }
    @NotNull public <T> T transaction(@NotNull Function<State, T> operation) {
        String before = codec.encode(state);
        try {
            T result = java.util.Objects.requireNonNull(operation.apply(state));
            try (PreparedStatement q = connection.prepareStatement("INSERT INTO snapshot(id,data) VALUES(1,?) ON CONFLICT(id) DO UPDATE SET data=excluded.data")) { q.setString(1, codec.encode(state)); q.executeUpdate(); }
            return result;
        } catch (RuntimeException e) { state = codec.decode(before); throw e; }
        catch (SQLException e) { state = codec.decode(before); throw new IllegalStateException("Ошибка сохранения SQLite", e); }
    }
    @Override public void close() throws SQLException { connection.close(); }
}
