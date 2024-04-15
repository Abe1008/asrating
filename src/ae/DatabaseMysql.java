/*
 * Copyright (c) 2018. Aleksey Eremin
 * 02.04.18 23:30
 */

package ae;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseMysql extends Database
{
  private String  f_host;
  private String  f_base;
  private String  f_user;
  private String  f_pass;

  public DatabaseMysql(String host, String base, String user, String pass)
  {
    this.f_host = host;
    this.f_base = base;
    this.f_user = user;
    this.f_pass = pass;
  }

  /**
   * Возвращает соединение к базе данных SQlite
   * @return соединение к БД
   */
  @Override
  public synchronized Connection getDbConnection()
  {
    if(f_connection == null) {
      try {
        String url = "jdbc:mysql://" + f_host + "/" + f_base;
        f_connection = DriverManager.getConnection (url, f_user, f_pass);
      } catch (SQLException e) {
        System.out.println(e.getMessage());
      }
    }
    return f_connection;
  }

} // end class
