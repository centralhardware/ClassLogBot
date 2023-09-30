package me.centralhardware.znatoki.telegram.statistic.typeHandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

import java.sql.*;

public abstract class JsonTypeHandler<T> implements TypeHandler<T> {

    protected abstract Class<T> getCLazz();

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void setParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException {
        try {
            ps.setString(i, mapper.writeValueAsString(parameter));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public T getResult(ResultSet rs, String columnName) throws SQLException {
        return deserialize(rs.getString(columnName));
    }

    @Override
    public T getResult(ResultSet rs, int columnIndex) throws SQLException {
        return deserialize(rs.getString(columnIndex));
    }

    @Override
    public T getResult(CallableStatement cs, int columnIndex) throws SQLException {
        return deserialize(cs.getString(columnIndex));
    }

    private T deserialize(String str){
        if (StringUtils.isBlank(str)) return null;

        try {
            return mapper.readValue(str, getCLazz());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
