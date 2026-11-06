package com.agent1k.glowbattlepass.storage;

import org.jetbrains.annotations.NotNull;
import java.util.function.Function;

public interface StateRepository extends AutoCloseable {
    @NotNull State state();
    @NotNull <T> T transaction(@NotNull Function<State, T> operation);
}
