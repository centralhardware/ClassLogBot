package me.centralhardware.znatoki.telegram.statistic.validate;

import io.vavr.control.Either;
import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.entity.Service;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.ServicesMapper;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ServiceValidator implements Validator<Pair<String, UUID>, String>{

    private final ServicesMapper servicesMapper;

    @Override
    public Either<String, String> validate(Pair<String, UUID> value) {
        var services = servicesMapper.getServicesByOrganization(value.getRight());
        var servicesNames = services.stream().map(Service::getName).toList();
        if (servicesNames.contains(value.getLeft())){
            return Either.right(value.getLeft());
        } else {
          return Either.left("Выберите услугу из списка");
        }
    }
}
