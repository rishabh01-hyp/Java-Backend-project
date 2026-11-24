package repository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Reusable, type-safe repository contract. */
public interface Repository<T, ID> {
    Optional<T> findById(ID id) throws SQLException;
    List<T> findAll() throws SQLException;
    void add(T entity) throws SQLException;
}
