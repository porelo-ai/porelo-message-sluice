package org.fuin.porelo.msgsluice.filter.io;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import org.junit.jupiter.api.Test;

/** The one signature Porelo writes, and everything that only looks like it. */
class SshSignatureTest {

    private static byte[] real() throws IOException {
        try (InputStream in =
                SshSignatureTest.class.getResourceAsStream("/signatures/porelo-ed25519.sig")) {
            return in.readAllBytes();
        }
    }

    /**
     * Made by ssh-keygen in the namespace the product had before it was renamed, which nothing
     * writes any more.
     */
    private static byte[] oldNamespace() throws IOException {
        try (InputStream in =
                SshSignatureTest.class.getResourceAsStream(
                        "/signatures/old-namespace-ed25519.sig")) {
            return in.readAllBytes();
        }
    }

    private static byte[] blob(final byte[] armored) {
        final String[] lines = new String(armored, StandardCharsets.US_ASCII).split("\n");
        return Base64.getDecoder()
                .decode(String.join("", Arrays.copyOfRange(lines, 1, lines.length - 1)));
    }

    private static byte[] armor(final byte[] blob) {
        final String b64 = Base64.getEncoder().encodeToString(blob);
        final StringBuilder out = new StringBuilder("-----BEGIN SSH SIGNATURE-----\n");
        for (int i = 0; i < b64.length(); i += 70) {
            out.append(b64, i, Math.min(b64.length(), i + 70)).append('\n');
        }
        return out.append("-----END SSH SIGNATURE-----\n")
                .toString()
                .getBytes(StandardCharsets.US_ASCII);
    }

    private static byte[] replace(final byte[] bytes, final String from, final String to) {
        final String s = new String(bytes, StandardCharsets.ISO_8859_1);
        assertThat(s).contains(from);
        return s.replace(from, to).getBytes(StandardCharsets.ISO_8859_1);
    }

    @Test
    void theSignaturePoreloWritesIsOne() throws Exception {
        assertThat(real()).hasSize(310);
        assertThat(SshSignature.isPorelos(real())).isTrue();
        assertThat(SshSignature.isPorelos(armor(blob(real())))).isTrue();
    }

    @Test
    void aSignatureInTheNamespaceBeforeTheRenameIsRefused() throws Exception {
        assertThat(oldNamespace())
                .as("a real signature, of the size ssh-keygen wrote it")
                .hasSize(306);
        assertThat(SshSignature.isPorelos(oldNamespace()))
                .as("signed in the namespace before the rename")
                .isFalse();
    }

    @Test
    void anythingBesideTheArmorIsRefused() throws Exception {
        final String text = new String(real(), StandardCharsets.US_ASCII);

        assertThat(SshSignature.isPorelos((text + "hidden").getBytes(StandardCharsets.US_ASCII)))
                .isFalse();
        assertThat(SshSignature.isPorelos((text + "\n").getBytes(StandardCharsets.US_ASCII)))
                .isFalse();
        assertThat(SshSignature.isPorelos(("x" + text).getBytes(StandardCharsets.US_ASCII)))
                .isFalse();
        assertThat(
                        SshSignature.isPorelos(
                                text.replace("\n", "\r\n").getBytes(StandardCharsets.US_ASCII)))
                .isFalse();
    }

    @Test
    void whatIsNotArmoredOrWrappedAsOpenSshWrapsItIsRefused() throws Exception {
        final String b64 = Base64.getEncoder().encodeToString(blob(real()));

        assertThat(SshSignature.isPorelos(new byte[0])).isFalse();
        assertThat(SshSignature.isPorelos(b64.getBytes(StandardCharsets.US_ASCII))).isFalse();
        assertThat(
                        SshSignature.isPorelos(
                                ("-----BEGIN SSH SIGNATURE-----\n"
                                                + b64
                                                + "\n-----END SSH SIGNATURE-----\n")
                                        .getBytes(StandardCharsets.US_ASCII)))
                .isFalse();
        assertThat(SshSignature.isPorelos("x".repeat(310).getBytes(StandardCharsets.US_ASCII)))
                .isFalse();
    }

    @Test
    void aBlobOtherThanAnEd25519SignatureInPorelosNamespaceIsRefused() throws Exception {
        final byte[] blob = blob(real());

        assertThat(SshSignature.isPorelos(armor(replace(blob, "SSHSIG", "SSHSIH")))).isFalse();
        assertThat(SshSignature.isPorelos(armor(replace(blob, "porelo-message", "porelo-massage"))))
                .isFalse();
        assertThat(SshSignature.isPorelos(armor(replace(blob, "sha512", "sha256")))).isFalse();
        assertThat(SshSignature.isPorelos(armor(replace(blob, "ssh-ed25519", "ssh-ed25518"))))
                .isFalse();
        final byte[] longer = Arrays.copyOf(blob, blob.length + 3);
        assertThat(SshSignature.isPorelos(armor(longer))).isFalse();
        assertThat(SshSignature.isPorelos(armor(Arrays.copyOf(blob, blob.length - 1)))).isFalse();
    }

    @Test
    void theLimitIsOneKibibyteEverywhere() {
        // doc/filter.md names the value, for whatever carries a signature beside a message.
        assertThat(SshSignature.MAX_BYTES).isEqualTo(1024);
    }
}
