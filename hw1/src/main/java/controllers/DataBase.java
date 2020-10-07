package controllers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import models.GameBoard;
import models.Player;

public class DataBase {
  /**
   * Create database connection.
   * 
   * @return Connection
   */
  public static Connection createConnection() {
    Connection c = null;
    try {
      Class.forName("org.sqlite.JDBC");
      c = DriverManager.getConnection("jdbc:sqlite:board.db");

    } catch (Exception e) {

      System.err.println(e.getClass().getName() + ": " + e.getMessage());
      System.exit(0);

    }

    System.out.println("Opened database successfully");
    return c;
  }

  /**
   * Create new table.
   * 
   * @param c Connection object
   */
  public static void createTable(Connection c) {
    Statement stmt = null;
    System.out.println("--> Creating table... ");
    try {
      stmt = c.createStatement();
      String sql = "CREATE TABLE IF NOT EXISTS GameBoard " + "(P1_TYPE CHAR DEFAULT ' ',"
          + " P2_TYPE CHAR DEFAULT ' '," + " B_00 CHAR DEFAULT ' ', " + " B_01 CHAR DEFAULT ' ', "
          + " B_02 CHAR DEFAULT ' ', " + " B_10 CHAR DEFAULT ' ', " + " B_11 CHAR DEFAULT ' ', "
          + " B_12 CHAR DEFAULT ' ', " + " B_20 CHAR DEFAULT ' ', " + " B_21 CHAR DEFAULT ' ', "
          + " B_22 CHAR DEFAULT ' ', " + " START BOOL DEFAULT 0 , " + " TURN INT DEFAULT 0, "
          + " DRAW BOOL DEFAULT 0, " + " WINNER INT DEFAULT 0, " + "ID INT PRIMARY KEY NOT NULL )";
      stmt.executeUpdate(sql);
      System.out.println("Table created successfully");
    } catch (Exception e) {
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
    } finally {
      try {
        if (stmt != null) {
          stmt.close();
        }
      } catch (SQLException e) {
        System.err.println(e.getClass().getName() + ": " + e.getMessage());
      }
    }
  }

  /**
   * Clean data base.
   * 
   * @param c Connection object type char
   */
  public static void cleanTable(Connection c) {
    System.out.println("--> Cleaning table... ");
    Statement stmt = null;
    try {
      c.setAutoCommit(false);
      stmt = c.createStatement();
      String sql = "DROP TABLE GameBoard;";
      stmt.executeUpdate(sql);
      c.commit();
      System.out.println("Table dropped successfully");
    } catch (Exception e) {
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
    } finally {
      try {
        if (stmt != null) {
          stmt.close();
        }
      } catch (SQLException e) {
        System.err.println(e.getClass().getName() + ": " + e.getMessage());
      }
    }

  }

  /**
   * Insert player 1 into data base.
   * 
   * @param c Connection object
   */
  public static void insertP1(Connection c, char type) {
    Statement stmt = null;
    System.out.println("--> Inserting Player 1 to table... ");
    try {
      c.setAutoCommit(false);
      stmt = c.createStatement();
      String sql;
      if (type == 'X') {
        sql = "INSERT INTO GameBoard (P1_TYPE,START,TURN,DRAW,WINNER,ID,P2_TYPE,B_00,B_01,B_02"
            + ",B_10,B_11,B_12,B_20,B_21,B_22) " + " VALUES ("
            + "'X', 0, 1, 0, 0, 100, ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ' );";
      } else {
        sql = "INSERT INTO GameBoard (P1_TYPE,START,TURN,DRAW,WINNER,ID) " + " VALUES ("
            + "'O', 0, 1, 0, 0, 100 );";
      }

      stmt.executeUpdate(sql);
      c.commit();
      System.out.println("Player 1 added successfully");
    } catch (Exception e) {
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
    } finally {
      try {
        if (stmt != null) {
          stmt.close();
        }
      } catch (SQLException e) {
        System.err.println(e.getClass().getName() + ": " + e.getMessage());
      }
    }

  }

  /**
   * Update Board in data base.
   * 
   * @param c Connection object
   */
  public static void updateDB(Connection c) {
    Statement stmt = null;
    System.out.println("--> Updating table... ");
    // get value from gb
    char type1 = PlayGame.getGb().getP1().getType();
    char type2 = PlayGame.getGb().getP2().getType();
    char[][] state = PlayGame.getGb().getBoardState().clone();

    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < 3; j++) {
        if (state[i][j] == '\0') {
          state[i][j] = ' ';
        }
      }
    }

    int turn = PlayGame.getGb().getTurn();
    int winner = PlayGame.getGb().getWinner();
    boolean start = PlayGame.getGb().isGameStarted();
    boolean draw = PlayGame.getGb().isDraw();

    try {
      c.setAutoCommit(false);
      stmt = c.createStatement();
      String sql = "UPDATE GameBoard set P1_TYPE = '" + Character.toString(type1) + "',P2_TYPE = '"
          + Character.toString(type2) + "',B_00 = '" + Character.toString(state[0][0])
          + "',B_01 = '" + Character.toString(state[0][1]) + "',B_02 = '"
          + Character.toString(state[0][2]) + "',B_10 = '" + Character.toString(state[1][0])
          + "',B_11 = '" + Character.toString(state[1][1]) + "',B_12 = '"
          + Character.toString(state[1][2]) + "',B_20 = '" + Character.toString(state[2][0])
          + "',B_21 = '" + Character.toString(state[2][1]) + "',B_22 = '"
          + Character.toString(state[2][2]) + "',START = " + Boolean.toString(start) + ",TURN = "
          + Integer.toString(turn) + ",DRAW = " + Boolean.toString(draw) + ",WINNER = "
          + Integer.toString(winner) + " where ID=100;";

      System.out.println(sql);
      stmt.executeUpdate(sql);
      c.commit();
      System.out.println("Board updated successfully");
    } catch (Exception e) {
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
    } finally {
      try {
        if (stmt != null) {
          stmt.close();
        }
      } catch (SQLException e) {
        System.err.println(e.getClass().getName() + ": " + e.getMessage());
      }
    }

    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < 3; j++) {
        if (state[i][j] == ' ') {
          state[i][j] = '\0';
        }
      }
    }

  }

  /**
   * Retrieve table from data base and update on gb.
   * 
   * @param c Connection object
   */
  public static void retreiveTable(Connection c) {
    GameBoard gb = new GameBoard();
    Statement stmt = null;
    System.out.println("--> Retreiving table... ");
    ResultSet rs = null;
    try {
      c.setAutoCommit(false);
      stmt = c.createStatement();
      String sql = "SELECT * FROM GameBoard";
      rs = stmt.executeQuery(sql);

      while (rs.next()) {
        System.out.println("--> Retreived INFO: " + rs.toString());
        Player p1 = new Player();
        p1.setId(1);
        String type = rs.getString("P1_TYPE");
        p1.setType(type.charAt(0));
        Player p2 = new Player();
        type = rs.getString("P2_TYPE");
        if (type.charAt(0) != ' ') {
          p2.setType(type.charAt(0));
          p2.setId(2);
          gb.setP2(p2);
        }
        

        char[][] boardState = new char[3][3];
        String b00 = rs.getString("B_00");
        String b01 = rs.getString("B_01");
        String b02 = rs.getString("B_02");
        boardState[0][0] = b00.charAt(0);
        boardState[0][1] = b01.charAt(0);
        boardState[0][2] = b02.charAt(0);
        String b10 = rs.getString("B_10");
        String b11 = rs.getString("B_11");
        String b12 = rs.getString("B_12");
        boardState[1][0] = b10.charAt(0);
        boardState[1][1] = b11.charAt(0);
        boardState[1][2] = b12.charAt(0);
        String b20 = rs.getString("B_20");
        String b21 = rs.getString("B_21");
        String b22 = rs.getString("B_22");
        boardState[2][0] = b20.charAt(0);
        boardState[2][1] = b21.charAt(0);
        boardState[2][2] = b22.charAt(0);

        gb.setP1(p1);

        for (int i = 0; i < 3; i++) {
          for (int j = 0; j < 3; j++) {
            if (boardState[i][j] == ' ') {
              boardState[i][j] = '\0';
            }
          }
        }

        gb.setBoardState(boardState);
        boolean start = rs.getBoolean("START");
        int turn = rs.getInt("TURN");
        boolean isDraw = rs.getBoolean("DRAW");
        int winner = rs.getInt("WINNER");
        gb.setGameStarted(start);
        gb.setTurn(turn);
        gb.setDraw(isDraw);
        gb.setWinner(winner);
        
        PlayGame.setGb(gb);

      }
      // c.close();
      System.out.println("Table updated successfully");
    } catch (Exception e) {
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
    } finally {
      try {
        if (stmt != null) {
          stmt.close();
        }
        if (rs != null) {
          rs.close();
        }
      } catch (SQLException e) {
        System.err.println(e.getClass().getName() + ": " + e.getMessage());
      }
    }

  }
}
