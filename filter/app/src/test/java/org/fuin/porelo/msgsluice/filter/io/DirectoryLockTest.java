package org.fuin.porelo.msgsluice.filter.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DirectoryLockTest {

    @TempDir Path tmp;

    @Test
    void aLockFileThatIsALinkIsRefusedAndWhatItPointsAtIsLeftAlone() throws Exception {
        final Path victim = Files.writeString(tmp.resolve("victim.txt"), "precious");
        final Path lock = Files.createSymbolicLink(tmp.resolve(".lock"), victim);

        assertThatThrownBy(() -> DirectoryLock.acquire(lock)).isInstanceOf(IOException.class);

        assertThat(victim).hasContent("precious");
    }

    @Test
    void aLockWhoseFileWasRemovedOrReplacedIsNoLongerHeld() throws Exception {
        // The harm: once the lock's file is gone, a second instance locks a new one, and both file
        // the same message - it is sent twice.
        final Path removed = tmp.resolve("removed.lock");
        final Path replaced = tmp.resolve("replaced.lock");
        try (DirectoryLock first = DirectoryLock.acquire(removed);
                DirectoryLock second = DirectoryLock.acquire(replaced)) {
            first.stillHeld();
            Files.delete(removed);
            Files.delete(replaced);
            Files.writeString(replaced, "pid 1");

            assertThatThrownBy(first::stillHeld).isInstanceOf(IOException.class);
            assertThatThrownBy(second::stillHeld).isInstanceOf(IOException.class);
        }
    }

    @Test
    void aHolderThatIsNotUtf8StillMeansAnotherInstance() throws Exception {
        final Path lock = tmp.resolve(".lock");
        try (DirectoryLock held = DirectoryLock.acquire(lock)) {
            Files.write(lock, new byte[] {(byte) 0xff, (byte) 0xfe, 'x'});

            assertThatThrownBy(() -> DirectoryLock.acquire(lock))
                    .isInstanceOf(DirectoryLock.LockedException.class)
                    .hasMessageContaining("Another instance");
        }
    }
}
