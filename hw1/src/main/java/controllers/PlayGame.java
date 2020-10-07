package controllers;

import com.google.gson.Gson;
import io.javalin.Javalin;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Queue;
import models.GameBoard;
import models.Message;
import models.Move;
import models.Player;
import org.eclipse.jetty.websocket.api.Session;

public class PlayGame {

  private static final int PORT_NUMBER = 8080;

  private static Javalin app;

  public static GameBoard gb;
  private static Gson gson;
  public static Connection c;

  /**
   * Main method of the application.
   * 
   * @param args Command line arguments
   */
  public static void main(final String[] args) {

    gson = new Gson(); // Initialize Gson

    app = Javalin.create(config -> {
      config.addStaticFiles("/public");
    }).start(PORT_NUMBER);

    c = createConnection();
    createTable(c);
    retreiveTable(c);
    try {
      c.close();
      System.out.println("DB closed ... ");
    } catch (SQLException e) {
      e.printStackTrace();
    }

    // new game
    app.get("/newgame", ctx -> {
      c = createConnection();
      gb = new GameBoard();
      cleanTable(c);
      createTable(c);
      try {
        c.close();
        System.out.println("DB closed ... ");
      } catch (SQLException e) {
        e.printStackTrace();
      }
      ctx.redirect("tictactoe.html");

    });

    // start game
    app.post("/startgame", ctx -> {
      String userType = ctx.body();
      gb = new GameBoard();
      // Create new player and set type as chosen
      Player p1 = new Player();
      p1.setId(1);
      p1.setType(userType.charAt(userType.length() - 1));
      gb.setP1(p1);
      gb.setTurn(1);
      gb.setBoardState(new char[3][3]);

      c = createConnection();
      // insert into db
      cleanTable(c);
      createTable(c);
      insertP1(c, p1.getType());
      try {
        c.close();
        System.out.println("DB closed ... ");
      } catch (SQLException e) {

        e.printStackTrace();
      }

      String result = gson.toJson(gb);
      ctx.result(result);

    });

    // player 2 join game
    app.get("/joingame", ctx -> {
      c = createConnection();
      retreiveTable(c);
      // Create player 2 and start game
      gb.setGameStarted(true);
      Player p2 = new Player();
      p2.setId(2);
      p2.setType(gb.getP1().getType() == 'X' ? 'O' : 'X');
      gb.setP2(p2);
      // redirect to game page
      ctx.redirect("/tictactoe.html?p=2");

      // uodate p2 in db
      // updatep2(c, p2.getType());
      updateDB(c);
      try {
        c.close();
        System.out.println("DB closed ... ");
      } catch (SQLException e) {
        e.printStackTrace();
      }
      // update view for both players
      sendGameBoardToAllPlayers(gson.toJson(gb));
    });

    // player moves
    app.post("/move/:playerId", ctx -> {

      c = createConnection();
      retreiveTable(c);
      try {
        c.close();

        System.out.println("DB closed ... ");
      } catch (SQLException e) {
        e.printStackTrace();
      }
      Message currentMessage = new Message();
      // check if game has started yet
      if (!gb.isGameStarted()) {
        currentMessage.setMoveValidity(false);
        currentMessage.setCode(401);
        currentMessage.setMessage("Wait for player to join");
      } else {
        // game started get move check valid and update
        // get player id , type and move
        Move currentMove = new Move();

        int currentPlayer = Integer.parseInt(ctx.pathParam("playerId"));
        if (currentPlayer == 1) {
          currentMove.setPlayer(gb.getP1());
        } else {
          currentMove.setPlayer(gb.getP2());
        }
        String playerMove = ctx.body();
        currentMove.setMoveX(Character.getNumericValue(playerMove.charAt(2)));
        currentMove.setMoveY(Character.getNumericValue(playerMove.charAt(6)));

        // check if player move valid
        currentMessage = checkMoveValid(currentMove, currentMessage);
        if (currentMessage.isMoveValidity()) {
          // make move and update board
          char[][] boardState = gb.getBoardState();
          boardState[currentMove.getMoveX()][currentMove.getMoveY()] = currentMove.getPlayer()
              .getType();
          gb.setBoardState(boardState);
          gb.setTurn(3 - gb.getTurn());

          // check if game ended
          if (checkGameEnd()) {
            if (gb.isDraw()) {
              gb.setWinner(0);
              gb.setGameStarted(false);
            } else {
              gb.setWinner(currentPlayer);
              gb.setGameStarted(false);
            }
          }
          // Make valid move in db and update draw, start, winner
          c = createConnection();
          updateDB(c);
          try {
            c.close();

            System.out.println("DB closed ... ");
          } catch (SQLException e) {
            e.printStackTrace();
          }
        }
      }
      // update view for both player
      String result = gson.toJson(currentMessage);
      ctx.result(result);
      sendGameBoardToAllPlayers(gson.toJson(gb));
      System.out.println(result);
    });

    // get game board for testing
    app.post("/testgameboard", ctx -> {
      String result = gson.toJson(gb);
      ctx.result(result);
    });

    // Web sockets - DO NOT DELETE or CHANGE
    app.ws("/gameboard", new UiWebSocket());

  }

  /**
   * Send message to all players.
   * 
   * @param gameBoardJson Gameboard JSON
   * @throws IOException Websocket message send IO Exception
   */
  public static void sendGameBoardToAllPlayers(final String gameBoardJson) {
    Queue<Session> sessions = UiWebSocket.getSessions();
    for (Session sessionPlayer : sessions) {
      try {
        sessionPlayer.getRemote().sendString(gameBoardJson);
      } catch (IOException e) {
        System.out.println("IO Exception");
      }
    }
  }

  /**
   * Check if current move is valid.
   * 
   * @param move Move, message Message
   * @return Message
   */
  public static Message checkMoveValid(Move move, Message message) {
    char[][] boardState = gb.getBoardState();
    // check if it's current player's turn
    if (gb.getTurn() != move.getPlayer().getId()) {
      message.setCode(405);
      message.setMoveValidity(false);
      message.setMessage("Not your turn");
      return message;
    }

    // check if move is occupied
    if (boardState[move.getMoveX()][move.getMoveY()] != '\0') {
      message.setCode(403);
      message.setMoveValidity(false);
      message.setMessage("This tile is occupied by "
          + Character.toString(boardState[move.getMoveX()][move.getMoveY()]));
      return message;
    }
    message.setCode(100);
    message.setMoveValidity(true);
    return message;
  }

  /**
   * Check if game has endded with tie or win.
   * 
   * @return boolean
   */
  public static boolean checkGameEnd() {
    char[][] boardState = gb.getBoardState();
    // one player wins
    if ((boardState[0][0] == boardState[0][1] && boardState[0][0] == boardState[0][2]
        && boardState[0][0] != '\0')
        || (boardState[1][0] == boardState[1][1] && boardState[1][0] == boardState[1][2]
            && boardState[1][0] != '\0')
        || (boardState[2][0] == boardState[2][1] && boardState[2][0] == boardState[2][2]
            && boardState[2][0] != '\0')
        || (boardState[0][0] == boardState[1][0] && boardState[0][0] == boardState[2][0]
            && boardState[0][0] != '\0')
        || (boardState[0][1] == boardState[1][1] && boardState[0][1] == boardState[2][1]
            && boardState[0][1] != '\0')
        || (boardState[0][2] == boardState[1][2] && boardState[0][2] == boardState[2][2]
            && boardState[0][2] != '\0')
        || (boardState[0][0] == boardState[1][1] && boardState[0][0] == boardState[2][2]
            && boardState[1][1] != '\0')
        || (boardState[0][2] == boardState[1][1] && boardState[0][2] == boardState[2][0]
            && boardState[1][1] != '\0')) {
      return true;
    }

    // no more empty spots
    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < 3; j++) {
        if (boardState[i][j] == '\0') {
          return false;
        }
      }
    }
    gb.setDraw(true);
    return true;
  }

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
      stmt.close();
      System.out.println("Table created successfully");
    } catch (Exception e) {
      System.err.println(e.getClass().getName() + ": " + e.getMessage());

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
      stmt.close();
      c.commit();
      System.out.println("Player 1 added successfully");
    } catch (Exception e) {
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
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
    char type1 = gb.getP1().getType();
    char type2 = gb.getP2().getType();
    char[][] state = gb.getBoardState().clone();

    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < 3; j++) {
        if (state[i][j] == '\0') {
          state[i][j] = ' ';
        }
      }
    }

    int turn = gb.getTurn();
    int winner = gb.getWinner();
    boolean start = gb.isGameStarted();
    boolean draw = gb.isDraw();

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
      stmt.close();
      c.commit();
      System.out.println("Board updated successfully");
    } catch (Exception e) {
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
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
    gb = new GameBoard();
    Statement stmt = null;
    System.out.println("--> Retreiving table... ");
    try {
      c.setAutoCommit(false);
      stmt = c.createStatement();
      String sql = "SELECT * FROM GameBoard";
      ResultSet rs = stmt.executeQuery(sql);

      while (rs.next()) {
        System.out.println("--> Retreived INFO: " + rs.toString());
        Player p1 = new Player();
        p1.setId(1);
        String type = rs.getString("P1_TYPE");
        p1.setType(type.charAt(0));
        Player p2 = new Player();
        p2.setId(2);
        type = rs.getString("P2_TYPE");
        p2.setType(type.charAt(0));

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
        gb.setP2(p2);

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

      }
      rs.close();
      stmt.close();
      // c.close();
      System.out.println("Table updated successfully");
    } catch (Exception e) {
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
    }

  }

  public static void stop() {
    app.stop();
  }
}
