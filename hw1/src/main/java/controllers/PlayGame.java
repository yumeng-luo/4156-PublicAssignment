package controllers;

import com.google.gson.Gson;
import io.javalin.Javalin;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Queue;
import models.GameBoard;
import models.Message;
import models.Move;
import models.Player;
import org.eclipse.jetty.websocket.api.Session;

public class PlayGame {

  private static final int PORT_NUMBER = 8080;

  private static Javalin app;

  private static GameBoard gb;

  public static GameBoard getGb() {
    return gb;
  }

  public static void setGb(GameBoard gb) {
    PlayGame.gb = gb;
  }

  public static Connection getC() {
    return c;
  }

  public static void setC(Connection c) {
    PlayGame.c = c;
  }

  private static Gson gson;
  private static Connection c;

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

    c = DataBase.createConnection();
    DataBase.createTable(c);
    DataBase.retreiveTable(c);
    try {
      c.close();
      System.out.println("DB closed ... ");
    } catch (SQLException e) {
      e.printStackTrace();
    }

    // new game
    app.get("/newgame", ctx -> {
      c = DataBase.createConnection();
      gb = new GameBoard();
      DataBase.cleanTable(c);
      DataBase.createTable(c);
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

      c = DataBase.createConnection();
      // insert into db
      DataBase.cleanTable(c);
      DataBase.createTable(c);
      DataBase.insertP1(c, p1.getType());
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
      c = DataBase.createConnection();
      DataBase.retreiveTable(c);
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
      DataBase.updateDB(c);
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

      c = DataBase.createConnection();
      DataBase.retreiveTable(c);
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
          c = DataBase.createConnection();
          DataBase.updateDB(c);
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

  

  public static void stop() {
    app.stop();
  }
}
