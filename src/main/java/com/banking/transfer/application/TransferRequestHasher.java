package com.banking.transfer.application;

import com.banking.transfer.api.dto.InternalTransferRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

/** Creates a stable fingerprint for detecting idempotency-key reuse with another payload. */
@Component
public class TransferRequestHasher {

  public String hash(InternalTransferRequest request) {
    String canonicalPayload =
        String.join(
            "|",
            request.fromAccount(),
            request.toAccount(),
            request.amount().stripTrailingZeros().toPlainString(),
            request.currency(),
            request.description() == null ? "" : request.description());
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256")
              .digest(canonicalPayload.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException exception) {
      // SHA-256 is mandatory in every Java runtime.
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }
}
