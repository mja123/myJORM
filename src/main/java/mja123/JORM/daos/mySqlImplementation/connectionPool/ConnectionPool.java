package mja123.JORM.daos.mySqlImplementation.connectionPool;

import mja123.JORM.daos.mySqlImplementation.exceptions.FullConnectionPoolException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ConnectionPool {
  private static final Logger LOGGER = LogManager.getLogger(ConnectionPool.class);
  private static ConnectionPool poolInstance = null;
  private final Integer MAX_CONNECTIONS = 5;
  private static Integer connectionsInUse;
  private final BlockingQueue<Connection> connections = new ArrayBlockingQueue<>(MAX_CONNECTIONS);

  private ConnectionPool() {
    connectionsInUse = 0;
  }

  public Connection getConnection() throws FullConnectionPoolException {

    // Look if I can create connections and if there aren't connections free in the collection.
    if ((connections.size() < this.MAX_CONNECTIONS)
        && ((connections.size() - connectionsInUse) == 0)) {
      Properties properties = new Properties();
      FileReader reader;
      try {
        reader = new FileReader(System.getenv("PROPERTIES"));
        properties.load(reader);

        connections.add(
            DriverManager.getConnection(
                properties.getProperty("URL"),
                properties.getProperty("USER"),
                properties.getProperty("PASSWORD")));

        connectionsInUse++;
        return connections.poll();
      } catch (IOException | SQLException e) {
        LOGGER.error(e.getMessage());
        return null;
      }
    } else if ((connections.size() - connectionsInUse) > 0) {
      connectionsInUse++;
      return connections.poll();
    } else {
      throw new FullConnectionPoolException("There aren't available connections to use.");
    }
  }

  public void goBackConnection(Connection connection) {
    connections.add(connection);
    connectionsInUse--;
  }

  public static ConnectionPool getInstance() {
    if (poolInstance == null) {
      poolInstance = new ConnectionPool();
    }
    return poolInstance;
  }
}
