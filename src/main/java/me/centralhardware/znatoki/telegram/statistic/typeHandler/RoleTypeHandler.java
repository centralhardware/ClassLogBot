package me.centralhardware.znatoki.telegram.statistic.typeHandler;

import me.centralhardware.znatoki.telegram.statistic.entity.postgres.Role;

public class RoleTypeHandler extends EnumHandler<Role>{
    @Override
    public Class<Role> getClazz() {
        return Role.class;
    }
}
