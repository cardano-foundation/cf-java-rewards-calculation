package org.cardanofoundation.rewards.util;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class CommonUtils {

  private CommonUtils() {}

  public static void setNullableValue(
      PreparedStatement ps, int parameterIndex, Object value, int sqlType) throws SQLException {
    if (value != null) {
      ps.setObject(parameterIndex, value, sqlType);
    } else {
      ps.setNull(parameterIndex, sqlType);
    }
  }
}
