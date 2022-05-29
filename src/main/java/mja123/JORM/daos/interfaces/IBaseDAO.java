package mja123.JORM.daos.interfaces;

import mja123.JORM.daos.mySqlImplementation.exceptions.ElementNotFoundException;

public interface IBaseDAO<T> {
  T getEntityByID(Long id) throws ElementNotFoundException;

  void saveEntity(T entity);

  void updateEntity(T entity) throws ElementNotFoundException;

  void removeEntity(Long id) throws ElementNotFoundException;
}
