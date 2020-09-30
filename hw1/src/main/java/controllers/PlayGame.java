package controllers;

import com.google.gson.Gson;
import io.javalin.Javalin;
import java.io.IOException;
import java.util.Queue;
import models.GameBoard;
import models.Message;
import models.Move;
import models.Player;
import org.eclipse.jetty.websocket.api.Session;

class PlayGame {

  private static final int PORT_NUMBER = 8080;

  private static Javalin app;

  private static GameBoard gb;
  private static Gson gson;

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

    // Test Echo Server
    app.post("/echo", ctx -> {
      ctx.result(ctx.body());
    });

    // new game
    app.get("/newgame", ctx -> {
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
      String result = gson.toJson(gb);
      ctx.result(result);

    });

    // player 2 join game
    app.get("/joingame", ctx -> {
      // Create player 2 and start game
      gb.setGameStarted(true);
      Player p2 = new Player();
      p2.setId(2);
      p2.setType(gb.getP1().getType() == 'X' ? 'O' : 'X');
      gb.setP2(p2);
      // redirect to game page
      ctx.redirect("/tictactoe.html?p=2");

      // update view for both players
      sendGameBoardToAllPlayers(gson.toJson(gb));
    });

    // player moves
    app.post("/move/:playerId", ctx -> {
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
      Message currentMessage = new Message();
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
            gb.setGameStarted(false);
          } else {
            gb.setWinner(currentPlayer);
            gb.setGameStarted(false);
          }
        }
      }
      // update view for both player
      String result = gson.toJson(currentMessage);
      ctx.result(result);
      sendGameBoardToAllPlayers(gson.toJson(gb));
      System.out.println(result);
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
  private static void sendGameBoardToAllPlayers(final String gameBoardJson) {
    Queue<Session> sessions = UiWebSocket.getSessions();
    for (Session sessionPlayer : sessions) {
      try {
        sessionPlayer.getRemote().sendString(gameBoardJson);
      } catch (IOException e) {
        // Add logger here
      }
    }
  }

  private static Message checkMoveValid(Move move, Message message) {
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
      message.setMessage("This tile is occupied");
      return message;
    }
    message.setCode(100);
    message.setMoveValidity(true);
    return message;
  }

  private static boolean checkGameEnd() {
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
