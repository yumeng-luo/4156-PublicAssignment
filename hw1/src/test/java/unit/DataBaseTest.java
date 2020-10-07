package unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import controllers.DataBase;
import controllers.PlayGame;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import models.GameBoard;
import models.Player;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

class DataBaseTest {
  /*
   * Test createConnection()
   * 
   */
  @Test
  @Order(1)
  public void testCreateConnection() {
    System.out.println("========TESTING CREATE CONNECTION ========");
    Connection c = DataBase.createConnection();
    assertNotEquals(c, null);
    try {
      c.close();
    } catch (SQLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      assertEquals(1, 0);
    }
  }

  /*
   * Test createTable
   * 
   */
  @Test
  @Order(2)
  public void testCreateTable() {
    System.out.println("========TESTING CREATE TABLE ========");
    Connection c = DataBase.createConnection();
    DataBase.createTable(c);
    Statement stmt = null;

    try {
      c.setAutoCommit(false);
      stmt = c.createStatement();
      String sql = "SELECT * FROM GameBoard ";
      stmt.executeUpdate(sql);
      stmt.close();
      c.commit();
      c.close();
    } catch (Exception e) {
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
      assertEquals(1, 0);
    }

  }

  /*
   * Test cleanTable
   * 
   */
  @Test
  @Order(3)
  public void testCleanTable() {
    System.out.println("========TESTING CLEAN TABLE ========");
    Connection c = DataBase.createConnection();
    DataBase.createTable(c);
    DataBase.cleanTable(c);

    Statement stmt = null;

    try {
      c.setAutoCommit(false);
      stmt = c.createStatement();
      String sql = "SELECT * FROM GameBoard ";
      stmt.executeUpdate(sql);
      stmt.close();
      c.commit();
      c.close();
      assertEquals(1, 0);
    } catch (Exception e) {
      // correct
    }

  }

  /*
   * Test insertP1
   * 
   */
  @Test
  @Order(4)
  public void testInsertP1() {
    System.out.println("========TESTING INSERT PLAYER 1 ========");
    Connection c = DataBase.createConnection();
    DataBase.cleanTable(c);
    DataBase.createTable(c);
    DataBase.insertP1(c, 'O');
    Statement stmt = null;

    try {
      c.setAutoCommit(false);
      stmt = c.createStatement();
      String sql = "SELECT *" + "FROM GameBoard;";
      ResultSet rs = stmt.executeQuery(sql);
      while (rs.next()) {
        System.out.println("Has Content:" + rs.toString());
        String type1 = rs.getString("P1_TYPE");
        assertEquals(type1.charAt(0), 'O');
        int turn = rs.getInt("TURN");
        assertEquals(turn, 1);
        boolean start = rs.getBoolean("START");
        assertEquals(start, false);
      }

      stmt.close();
      c.commit();
      c.close();
    } catch (Exception e) {
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
      assertEquals(1, 0);
    }

  }

  /*
   * Test updateDB
   * 
   */
  @Test
  @Order(5)
  public void testUpdateDB() {
    System.out.println("========TESTING UPDATE TABLE ========");
    Connection c = DataBase.createConnection();
    DataBase.cleanTable(c);
    DataBase.createTable(c);
    DataBase.insertP1(c, 'X');

    PlayGame.setGb(new GameBoard());
    Player p1 = new Player();
    p1.setId(1);
    p1.setType('O');
    Player p2 = new Player();
    p2.setId(2);
    p2.setType('X');
    PlayGame.getGb().setP1(p1);
    PlayGame.getGb().setP2(p2);
    PlayGame.getGb().setTurn(1);
    PlayGame.getGb().setGameStarted(true);
    char[][] boardState = new char[3][3];
    boardState[0][1] = 'O';
    PlayGame.getGb().setBoardState(boardState);

    DataBase.updateDB(c);
    Statement stmt = null;

    try {
      c.setAutoCommit(false);
      stmt = c.createStatement();
      String sql = "SELECT * FROM GameBoard;";
      ResultSet rs = stmt.executeQuery(sql);
      while (rs.next()) {
        String type = rs.getString("P1_TYPE");
        assertEquals(type.charAt(0), 'O');
        type = rs.getString("P2_TYPE");
        assertEquals(type.charAt(0), 'X');

        String b00 = rs.getString("B_00");
        String b01 = rs.getString("B_01");
        String b02 = rs.getString("B_02");
        assertEquals(' ', b00.charAt(0));
        assertEquals(boardState[0][1], b01.charAt(0));
        assertEquals(' ', b02.charAt(0));
        String b10 = rs.getString("B_10");
        String b11 = rs.getString("B_11");
        String b12 = rs.getString("B_12");
        assertEquals(' ', b10.charAt(0));
        assertEquals(' ', b11.charAt(0));
        assertEquals(' ', b12.charAt(0));
        String b20 = rs.getString("B_20");
        String b21 = rs.getString("B_21");
        String b22 = rs.getString("B_22");
        assertEquals(' ', b20.charAt(0));
        assertEquals(' ', b21.charAt(0));
        assertEquals(' ', b22.charAt(0));
        boolean start = rs.getBoolean("START");
        assertEquals(start, true);
        int turn = rs.getInt("TURN");
        assertEquals(turn, 1);
        boolean isDraw = rs.getBoolean("DRAW");
        assertEquals(isDraw, false);
        int winner = rs.getInt("WINNER");
        assertEquals(winner, 0);
      }

      stmt.close();
      c.commit();
      c.close();
    } catch (Exception e) {
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
      assertEquals(1, 0);
    }

  }

  /*
   * Test retreiveTable
   * 
   */
  @Test
  @Order(6)
  public void testretreiveTable() {
    System.out.println("========TESTING RETREIVING TABLE ========");
    Connection c = DataBase.createConnection();
    DataBase.createTable(c);
    DataBase.insertP1(c, 'X');

    PlayGame.setGb(new GameBoard());
    Player p1 = new Player();
    p1.setId(1);
    p1.setType('O');
    Player p2 = new Player();
    p2.setId(2);
    p2.setType('X');
    PlayGame.getGb().setP1(p1);
    PlayGame.getGb().setP2(p2);
    PlayGame.getGb().setTurn(1);
    PlayGame.getGb().setGameStarted(true);
    char[][] boardState = new char[3][3];
    boardState[0][1] = 'O';
    PlayGame.getGb().setBoardState(boardState);

    DataBase.updateDB(c);

    PlayGame.setGb(new GameBoard());
    DataBase.retreiveTable(c);

    assertEquals(PlayGame.getGb().getP1().getType(), 'O');
    assertEquals(PlayGame.getGb().getP2().getType(), 'X');
    assertEquals(PlayGame.getGb().getTurn(), 1);
    assertEquals(PlayGame.getGb().getWinner(), 0);
    assertEquals(PlayGame.getGb().isDraw(), false);
    assertEquals(PlayGame.getGb().isGameStarted(), true);
    assertEquals(PlayGame.getGb().getBoardState()[0][1], 'O');

    try {
      c.close();
    } catch (SQLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      assertEquals(0, 1);
    }

  }

}
