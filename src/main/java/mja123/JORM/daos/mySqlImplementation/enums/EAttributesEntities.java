package mja123.JORM.daos.mySqlImplementation.enums;

public enum EAttributesEntities {
  NAME("name"),
  EMAIL("email"),
  AGE("age"),
  YEARS_IN_DEGREE("year_in_degree"),
  ID("id"),
  CREATED_AT("created_at"),
  DELETED_AT("deleted_at"),
  PROFESSION("profession"),
  FACULTIES_ID("faculties_id"),
  YEAR("year"),
  BIANNUAL("biannual");


  private final String ATTRIBUTE;

  EAttributesEntities(String ATTRIBUTE) {
    this.ATTRIBUTE = ATTRIBUTE;
  }

  public String getATTRIBUTE() {
    return ATTRIBUTE;
  }
}
