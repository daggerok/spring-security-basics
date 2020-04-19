package daggerok;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.keygen.KeyGenerators;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@Log4j2
class CryptoTest {

  @Test
  void test() {
    var salt = KeyGenerators.string().generateKey();
    var encryptor = Encryptors.stronger("My super awesome password!", salt);

    var charset = StandardCharsets.UTF_8;
    var sourceBytes = "ololo trololo".getBytes(charset);
    var encryptedBytes = encryptor.encrypt(sourceBytes);
    var decryptedBytes = encryptor.decrypt(encryptedBytes);

    var expected = new String(sourceBytes);
    var actual = new String(decryptedBytes);
    assertThat(actual).isEqualTo(expected);
    log.info(actual);
  }
}
