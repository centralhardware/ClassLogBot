package me.centralhardware.znatoki.telegram.statistic.validate;

import io.vavr.control.Either;
import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.entity.postgres.Services;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.ServicesMapper;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RequiredArgsConstructor
public class ServiceValidatorTest {

    ServicesMapper servicesMapper = mock(ServicesMapper.class);

    ServiceValidator serviceValidator = new ServiceValidator(servicesMapper);

    @Test
    void validate_withValidService() {
        Pair<String, UUID> value = Pair.of("service1", UUID.randomUUID());
        Services validService = new Services();
        validService.setName("service1");

        when(servicesMapper.getServicesByOrganization(value.getRight())).thenReturn(List.of(validService));

        Either<String, String> result = serviceValidator.validate(value);

        assertTrue(result.isRight());
        assertEquals("service1", result.get());
    }

    @Test
    void validate_withInvalidService() {
        Pair<String, UUID> value = Pair.of("service2", UUID.randomUUID());
        Services validService = new Services();
        validService.setName("service1");

        when(servicesMapper.getServicesByOrganization(value.getRight())).thenReturn(List.of(validService));

        Either<String, String> result = serviceValidator.validate(value);

        assertTrue(result.isLeft());
        assertEquals("Выберите услугу из списка", result.getLeft());
    }
}