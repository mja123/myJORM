package mja123.JORM.daos.mySqlImplementation.utils;

import mja123.JORM.daos.mySqlImplementation.exceptions.ElementNotFoundException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

public class SqlUtil {

  public static ConcurrentHashMap<String, Object> elementsInResultSet(
      ResultSet result, int countOfFields) throws SQLException, ElementNotFoundException {

    ConcurrentHashMap<String, Object> fieldsResultSet = new ConcurrentHashMap<>();
    Boolean nullObject = false;
    // Fill a HashMap with the name of the columns in the row and the respective value.
    if (result.next()) {
      for (int i = 1; i < (countOfFields + 1); i++) {
        if (result.getObject(i) == null) {
          continue;
        }
        fieldsResultSet.put(result.getMetaData().getColumnLabel(i), result.getObject(i));
        nullObject = true;
      }
    }
    if (!nullObject) {
      throw new ElementNotFoundException("This record doesn't exist");
    }
    return fieldsResultSet;
  }
}