package me.centralhardware.znatoki.telegram.statistic.typeHandler;

import me.centralhardware.znatoki.telegram.statistic.entity.postgres.Role;
import org.apache.ibatis.type.EnumTypeHandler;

public class RoleTypeHandler extends EnumTypeHandler<Role> {
    public RoleTypeHandler() {
        super(Role.class);
    }

}
