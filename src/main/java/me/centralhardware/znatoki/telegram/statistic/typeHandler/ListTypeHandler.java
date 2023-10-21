package me.centralhardware.znatoki.telegram.statistic.typeHandler;

import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public abstract class ListTypeHandler<T> implements TypeHandler<List<T>> {

    @Override
    public void setParameter(PreparedStatement ps, int i, List<T> parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter.stream().map(Object::toString).collect(Collectors.joining(":")));
    }

    @Override
    public List<T> getResult(ResultSet rs, String columnName) throws SQLException {
        return get(rs.getString(columnName));
    }

    @Override
    public List<T> getResult(ResultSet rs, int columnIndex) throws SQLException {
        return get(rs.getString(columnIndex));
    }

    @Override
    public List<T> getResult(CallableStatement cs, int columnIndex) throws SQLException {
        return get(cs.getString(columnIndex));
    }

    public List<T> get(String value){
        return Arrays.stream(value.split(":"))
                .map(this::convert)
                .collect(Collectors.toList());
    }

    protected abstract T convert(String val);


}
