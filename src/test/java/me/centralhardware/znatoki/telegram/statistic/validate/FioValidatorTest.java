package me.centralhardware.znatoki.telegram.statistic.validate;

import io.vavr.control.Either;
import me.centralhardware.znatoki.telegram.statistic.entity.postgres.Client;
import me.centralhardware.znatoki.telegram.statistic.service.ClientService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

public class FioValidatorTest {
  @Test
  public void givenExistingFio_whenValidate_thenRight() {
    // Given
    ClientService mockClientService = Mockito.mock(ClientService.class);
    FioValidator fioValidator = new FioValidator(mockClientService);
    String fio = "Existing Fio";
    Client clientMock = Mockito.mock(Client.class);

    // When
    when(mockClientService.findByFioAndId(fio)).thenReturn(clientMock);
    Either<String, String> result = fioValidator.validate(fio);

    // Then
    assertTrue(result.isRight());
  }

  @Test
  public void givenNonExistingFio_whenValidate_thenLeft() {
    // Given
    ClientService mockClientService = Mockito.mock(ClientService.class);
    FioValidator fioValidator = new FioValidator(mockClientService);
    String fio = "Non-existing Fio";

    // When
    when(mockClientService.findByFioAndId(fio)).thenReturn(null);
    Either<String, String> result = fioValidator.validate(fio);

    // Then
    assertTrue(result.isLeft());
  }
}