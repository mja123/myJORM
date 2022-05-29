package mja123.JORM.daos.mySqlImplementation;

import mja123.JORM.daos.mySqlImplementation.connectionPool.ConnectionPool;
import mja123.JORM.daos.mySqlImplementation.exceptions.ElementNotFoundException;
import mja123.JORM.daos.mySqlImplementation.exceptions.FullConnectionPoolException;
import mja123.JORM.daos.interfaces.IBaseDAO;
import mja123.JORM.daos.mySqlImplementation.exceptions.PrivateConstructorsException;
import mja123.JORM.daos.mySqlImplementation.utils.ReflectionUtil;
import mja123.JORM.daos.mySqlImplementation.utils.SqlUtil;
import mja123.JORM.daos.mySqlImplementation.utils.StringUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static mja123.JORM.daos.mySqlImplementation.enums.EAttributesEntities.*;
import static mja123.JORM.daos.mySqlImplementation.utils.StringUtil.attributeToColumn;

public class BaseDAO<T> implements IBaseDAO<T> {

  //TODO:
  /*
  - ADD REACTIVE COMPONENTS IN POOL CONNECTION
  - CREATE METHOD
   */

  private static final Logger LOGGER = LogManager.getLogger(BaseDAO.class);
  private final String TABLE_NAME;
  private Connection connection;
  private final String CLASS_NAME;
  private Class<T> instance;
  private final ReflectionUtil<T> REFLECTION;
  private Long id;
  private ConcurrentHashMap<String, String> classFields;
  private ConcurrentHashMap<String, Object> objectFields;
  private ConcurrentHashMap<String, String> declaredObjectFields;

  public BaseDAO(String tableName, String className) {
    this.TABLE_NAME = tableName;
    this.CLASS_NAME = className;
    classFields = new ConcurrentHashMap<>();
    objectFields = new ConcurrentHashMap<>();
    declaredObjectFields = new ConcurrentHashMap<>();
    this.REFLECTION = new ReflectionUtil<>(this.CLASS_NAME);
    this.setInstance();
  }

  @Override
  public T getEntityByID(Long id) throws ElementNotFoundException {
    setConnection();

    String select =
        "SELECT * FROM " + this.TABLE_NAME + " WHERE id = " + id + ";";

    T entityResult = null;
    try (PreparedStatement getEntity = connection.prepareStatement(select);
         ResultSet result = getEntity.executeQuery()){

      entityResult = this.parseResultSet(result);

    } catch (SQLException | PrivateConstructorsException throwables) {
      LOGGER.error(throwables);
    } finally {
       ConnectionPool.getInstance().goBackConnection(this.connection);
      this.objectFields.clear();
      this.declaredObjectFields.clear();
    }
    return entityResult;
  }
  @Override
  public void saveEntity(T entity) {
    this.setConnection();
    String create = "INSERT INTO " + this.TABLE_NAME + "(";

    if (classFields.isEmpty()) {
      REFLECTION.reflectionClass(this.classFields);
    }
    REFLECTION.reflectionFields(entity, classFields, objectFields);

    ArrayList<String> columnNames = attributeToColumn(objectFields);

    for (String column : columnNames) {
      //Compare if the current element is the last one.
      if (column.equals(columnNames.get(columnNames.size() - 1))) {
        create = create.concat(column + ") VALUES (");
      } else {
        create = create.concat(column + ", ");
      }
    }
    for (String values : columnNames) {

      if (values.equals(columnNames.get(columnNames.size() - 1))) {
        create = create.concat("?);");
      } else {
        create = create.concat("?,");
      }
    }

    try(PreparedStatement createEntity = this.connection.prepareStatement(create)) {
      this.setPlaceHolders(createEntity);

      System.out.println(createEntity);

      createEntity.execute();
    } catch (SQLException throwables) {

      LOGGER.error(throwables);
    } finally {

      ConnectionPool.getInstance().goBackConnection(this.connection);
      this.objectFields.clear();
      this.declaredObjectFields.clear();
    }
  }

  @Override
  public void updateEntity(T entity) throws ElementNotFoundException {
    this.setConnection();

    if (classFields.isEmpty()) {
      REFLECTION.reflectionClass(this.classFields);
    }

    this.objectFields = REFLECTION.reflectionFields(entity, classFields, objectFields);
    this.id = REFLECTION.getId();

    String update = "UPDATE " + this.TABLE_NAME + " SET ";

    int counter = 0;

    if(this.id == null) {
      throw new ElementNotFoundException("You have to send the record's id.");
    }

    ArrayList<String> formatedFields = attributeToColumn(objectFields);

    for (String field : formatedFields) {

      if (counter == formatedFields.size() - 1) {
        update = update.concat(field + " = ? WHERE id = " + this.id);
      } else {
        update = update.concat(field + " = ?, ");
      }
      counter++;
    }
    PreparedStatement updateRow = null;
    try {
      updateRow = this.connection.prepareStatement(update);
      setPlaceHolders(updateRow);
      System.out.println(updateRow);
      updateRow.executeUpdate();
    } catch (SQLException throwables) {
      LOGGER.error(throwables.getMessage());
    } finally {
      try {
        if (updateRow != null) {
          updateRow.close();
        }
      } catch (SQLException e) {
        LOGGER.error(e);
      }
      ConnectionPool.getInstance().goBackConnection(this.connection);
      this.declaredObjectFields.clear();
      this.objectFields.clear();
    }
  }

  @Override
  public void removeEntity(Long id) throws ElementNotFoundException {
    setConnection();

    String remove =
        "DELETE FROM " + this.TABLE_NAME + " WHERE id = " + id + ";";

    try (PreparedStatement removeElement = connection.prepareStatement(remove)) {
      removeElement.executeUpdate();

    } catch (SQLException throwables) {
      LOGGER.error(throwables.getMessage());
    } finally {
       ConnectionPool.getInstance().goBackConnection(connection);
    }
  }

  private void setPlaceHolders(PreparedStatement query) throws SQLException {
    int counter = 1;
    Object value = null;
    declaredObjectFields = REFLECTION.declaredAttributesType(classFields, objectFields, declaredObjectFields);

    //Iterating for the datatype of the fields
    for (String identifier : declaredObjectFields.keySet()) {
      //Iterating for the identifiers of the fields
      for (String field : objectFields.keySet()) {
        //Set variable value with the value of the field.
        if (identifier.equals(field)) {
          //Search if the identifier is not someone that was used.
            value = objectFields.get(field);
            break;
          }
        }

      switch (declaredObjectFields.get(identifier)) {
        case "java.lang.Long" -> query.setLong(counter, (Long) value);
        case "java.lang.Integer" -> query.setInt(counter, (Integer) value);
        case "java.lang.String" -> query.setString(counter, (String) value);
        case "java.sql.Date" -> query.setDate(counter, (java.sql.Date) value);
        case "java.sql.Double" -> query.setDouble(counter, (Double) value);
        case "java.lang.Boolean" -> query.setBoolean(counter, (Boolean) value);
        case "java.lang.Float" -> query.setFloat(counter, (Float) value);
        default -> System.out.println("I don't know");
      }
      counter++;
    }
  }


  //In process
  private T parseResultSet(ResultSet result) throws PrivateConstructorsException, SQLException, ElementNotFoundException {

    T resultObject = REFLECTION.newEmptyObject();

   Method[] methods = resultObject.getClass().getDeclaredMethods();

    if (classFields.isEmpty()) {
      REFLECTION.reflectionClass(classFields);
    }

    //Fill objectFields with the column-value in the ResultSet.
    objectFields = SqlUtil.elementsInResultSet(result, classFields.size());
    //Parse the snake_case keys in db to camelCase for fields.
    StringUtil.columnsToAttributes(objectFields);
    //Cast the type of column's fields to their respective type in the object.
    REFLECTION.castValues(classFields, objectFields);

   for(Method method : methods) {
     //Search only setters
     if (!(method.getName().substring(0, 3).equals("set"))) {
       continue;
     }
     try {
       //In the setter, this separate the field's name and format it like field definition.
       String keyField = method.getName().substring(3, method.getName().length());
       keyField = keyField.substring(0, 1).toLowerCase(Locale.ROOT) + keyField.substring(1);

       //Set the parameter with the value of the attribute in the HashMap
       // (objectFields has the formatted result in the ResultSet)
       Object parameter = objectFields.get(keyField);
       //Invoke the setter with the parameter
       method.invoke(resultObject, parameter);
     } catch (IllegalAccessException | InvocationTargetException e) {
       LOGGER.error(e.getMessage());
     }
   }
   return instance.cast(resultObject);
  }

  private void setConnection() {
    try {
      this.connection = ConnectionPool.getInstance().getConnection();
    } catch (FullConnectionPoolException e) {
      LOGGER.error(e.getMessage());
      this.setConnection();
    }
  }

  private void setInstance() {
    try {
      instance = (Class) Class.forName(CLASS_NAME);
    } catch (ClassNotFoundException e) {
      LOGGER.error(e);
    }
  }
  // region Getters and setters
  public String getTABLE_NAME() {
    return TABLE_NAME;
  }

  public Connection getConnection() {
    this.setConnection();
    return connection;
  }

  public String getCLASS_NAME() {
    return CLASS_NAME;
  }

  public Class<T> getInstance() {
    return instance;
  }

  public void setInstance(Class<T> instance) {
    this.instance = instance;
  }

  public ConcurrentHashMap<String, String> getFields() {
    return classFields;
  }

  public void setFields(ConcurrentHashMap<String, String> fields) {
    this.classFields = fields;
  }
  // endregion


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BaseDAO<?> baseDAO = (BaseDAO<?>) o;
    return Objects.equals(TABLE_NAME, baseDAO.TABLE_NAME) && Objects.equals(connection, baseDAO.connection) && Objects.equals(CLASS_NAME, baseDAO.CLASS_NAME) && Objects.equals(instance, baseDAO.instance) && Objects.equals(classFields, baseDAO.classFields) && Objects.equals(objectFields, baseDAO.objectFields) && Objects.equals(declaredObjectFields, baseDAO.declaredObjectFields);
  }

  @Override
  public int hashCode() {
    return Objects.hash(TABLE_NAME, connection, CLASS_NAME, instance, classFields, objectFields, declaredObjectFields);
  }
}
