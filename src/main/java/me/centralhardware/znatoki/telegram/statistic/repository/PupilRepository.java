package me.centralhardware.znatoki.telegram.statistic.repository;

import me.centralhardware.znatoki.telegram.statistic.entity.Pupil;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PupilRepository extends CrudRepository<Pupil, Integer> {

    List<Pupil> findAll();

    List<Pupil> findByOrganizationId(UUID organizationId);

    List<Pupil> findAllByNameAndSecondNameAndLastName(String name, String secondName, String lastName);

    List<Pupil> findByTelephone(String telephone);

    List<Pupil> findByTelephoneResponsible(String telephoneMother);

    @Query(value = """
        SELECT *
        FROM pupil
        WHERE lower(trim(concat(id, ' ', name, ' ', second_name, ' ', last_name))) = :fio
        ORDER BY date_of_record DESC
        LIMIT 1
    """, nativeQuery = true)
    Pupil findByFioAndId(@Param("fio") String fio);

    @Query(value = """
        SELECT *
        FROM pupil
        WHERE lower(trim(concat(name, ' ', second_name, ' ', last_name))) = :fio
        ORDER BY date_of_record DESC
        LIMIT 1
    """, nativeQuery = true)
    Pupil findByFio(@Param("fio") String fio);

}
