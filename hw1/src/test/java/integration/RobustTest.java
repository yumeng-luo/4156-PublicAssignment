package integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.Gson;
import controllers.DataBase;
import controllers.PlayGame;
import java.sql.SQLException;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;
import models.GameBoard;
import models.Player;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;


class RobustTest {
  
  /**
   * Runs only once before the testing starts.
   */
  @BeforeAll
  public static void init() {
    // Start Server
    PlayGame.main(new String[0]);
    // clear previous db
    PlayGame.setC(DataBase.createConnection());
    DataBase.cleanTable(PlayGame.getC());
    try {
      PlayGame.getC().close();
      System.out.println("DB closed in before all... ");
    } catch (SQLException e) {
      e.printStackTrace();
    }

    System.out.println("=============Before All===========");
  }
  
  /**
   * This method starts a new game before every test run. It will run every time
   * before a test.
   */
  @BeforeEach
  public void startNewGame() {
    // Test if server is running. You need to have an endpoint /
    // If you do not wish to have this end point, it is okay to not have anything in
    // this method.
    Unirest.get("http://localhost:8080/").asString();
    // int restStatus = response.getStatus();
    System.out.println("------Before Each:");
  }
  
  /**
   * Each time new game starts, data baed tables must be cleaned.
   */
  @Test
  @Order(1)
  public void newGame() {
    Unirest.get("http://localhost:8080/newgame").asString();
    Unirest.post("http://localhost:8080/startgame").body("type=O").asString();
    // check game board changed
    HttpResponse<String> response = Unirest.post("http://localhost:8080/testgameboard").asString();
    String responseBody = response.getBody();
    JSONObject jsonObject = new JSONObject(responseBody);
    assertEquals(false, jsonObject.get("gameStarted"));
    assertEquals(0, jsonObject.get("winner"));
    assertEquals(false, jsonObject.get("isDraw"));
    assertEquals(1, jsonObject.get("turn"));
    Gson gson = new Gson();
    GameBoard gameBoard = gson.fromJson(jsonObject.toString(), GameBoard.class);
    Player p1 = gameBoard.getP1();
    assertEquals('O', p1.getType());
    char[][] boardState = gameBoard.getBoardState();
    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < 3; j++) {
        assertEquals(boardState[i][j], '\0');
      }
    }

    System.out.println("Test new game db");

  }

  /**
   * Each time new game starts, data base tables must be cleaned.
   */
  @Test
  @Order(2)
  public void newGameDB() {
    Unirest.get("http://localhost:8080/newgame").asString();
    Unirest.post("http://localhost:8080/startgame").body("type=O").asString();
    // check game board changed
    HttpResponse<String> response = Unirest.post("http://localhost:8080/testgameboard").asString();
    String responseBody = response.getBody();
    JSONObject jsonObject = new JSONObject(responseBody);
    assertEquals(false, jsonObject.get("gameStarted"));
    assertEquals(0, jsonObject.get("winner"));
    assertEquals(false, jsonObject.get("isDraw"));
    assertEquals(1, jsonObject.get("turn"));
    Gson gson = new Gson();
    GameBoard gameBoard = gson.fromJson(jsonObject.toString(), GameBoard.class);
    Player p1 = gameBoard.getP1();
    assertEquals('O', p1.getType());
    char[][] boardState = gameBoard.getBoardState();
    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < 3; j++) {
        assertEquals(boardState[i][j], '\0');
      }
    }

    System.out.println("Test new game db");

  }

  /**
   * Player 1 started, game crashed, reboot with player 1.
   */
  @Test
  @Order(3)
  public void p1DB() {
    Unirest.get("http://localhost:8080/newgame").asString();
    Unirest.post("http://localhost:8080/startgame").body("type=O").asString();

    // Interrupt game
    PlayGame.stop();

    PlayGame.main(new String[0]);
    Unirest.get("http://localhost:8080/").asString();
    System.out.println("Rebooted");

    // check game board
    HttpResponse<String> response = Unirest.post("http://localhost:8080/testgameboard").asString();
    String responseBody = response.getBody();
    JSONObject jsonObject = new JSONObject(responseBody);
    assertEquals(false, jsonObject.get("gameStarted"));
    assertEquals(0, jsonObject.get("winner"));
    assertEquals(false, jsonObject.get("isDraw"));
    assertEquals(1, jsonObject.get("turn"));
    Gson gson = new Gson();
    GameBoard gameBoard = gson.fromJson(jsonObject.toString(), GameBoard.class);
    Player p1 = gameBoard.getP1();
    assertEquals('O', p1.getType());
    char[][] boardState = gameBoard.getBoardState();
    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < 3; j++) {
        assertEquals(boardState[i][j], '\0');
      }
    }

    System.out.println("Test p1 start game db");

  }

  /**
   * Player 2 joined, game crashed, reboot with player 1 & 2.
   */
  @Test
  @Order(4)
  public void p2DB() {
    Unirest.get("http://localhost:8080/newgame").asString();
    Unirest.post("http://localhost:8080/startgame").body("type=O").asString();
    Unirest.get("http://localhost:8080/joingame").asString();

    // Interrupt game
    PlayGame.stop();

    PlayGame.main(new String[0]);
    Unirest.get("http://localhost:8080/").asString();
    System.out.println("Rebooted");

    // check game board
    HttpResponse<String> response = Unirest.post("http://localhost:8080/testgameboard").asString();
    String responseBody = response.getBody();
    JSONObject jsonObject = new JSONObject(responseBody);
    assertEquals(true, jsonObject.get("gameStarted"));
    assertEquals(0, jsonObject.get("winner"));
    assertEquals(false, jsonObject.get("isDraw"));
    assertEquals(1, jsonObject.get("turn"));
    Gson gson = new Gson();
    GameBoard gameBoard = gson.fromJson(jsonObject.toString(), GameBoard.class);
    Player p1 = gameBoard.getP1();
    assertEquals('O', p1.getType());
    Player p2 = gameBoard.getP2();
    assertEquals('X', p2.getType());
    char[][] boardState = gameBoard.getBoardState();
    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < 3; j++) {
        assertEquals(boardState[i][j], '\0');
      }
    }

    System.out.println("Test p2 start game db");

  }

  /**
   * Player 2 made invalid move, game crashed, reboot with player 1 & 2 and board
   * unchanged.
   */
  @Test
  @Order(5)
  public void invalidMoveDB() {
    Unirest.get("http://localhost:8080/newgame").asString();
    Unirest.post("http://localhost:8080/startgame").body("type=O").asString();
    Unirest.get("http://localhost:8080/joingame").asString();
    Unirest.post("http://localhost:8080/move/1").body("x=1&y=1").asString();
    Unirest.post("http://localhost:8080/move/2").body("x=1&y=1").asString();

    // Interrupt game
    PlayGame.stop();

    PlayGame.main(new String[0]);
    Unirest.get("http://localhost:8080/").asString();
    System.out.println("Rebooted");

    // check game board
    HttpResponse<String> response = Unirest.post("http://localhost:8080/testgameboard").asString();
    String responseBody = response.getBody();
    JSONObject jsonObject = new JSONObject(responseBody);
    assertEquals(true, jsonObject.get("gameStarted"));
    assertEquals(0, jsonObject.get("winner"));
    assertEquals(false, jsonObject.get("isDraw"));
    assertEquals(2, jsonObject.get("turn"));
    Gson gson = new Gson();
    GameBoard gameBoard = gson.fromJson(jsonObject.toString(), GameBoard.class);
    Player p1 = gameBoard.getP1();
    assertEquals('O', p1.getType());
    Player p2 = gameBoard.getP2();
    assertEquals('X', p2.getType());
    char[][] boardState = gameBoard.getBoardState();
    assertEquals('\0', boardState[0][0]);
    assertEquals('\0', boardState[0][1]);
    assertEquals('\0', boardState[0][2]);
    assertEquals('\0', boardState[1][0]);
    assertEquals('O', boardState[1][1]);
    assertEquals('\0', boardState[1][2]);
    assertEquals('\0', boardState[2][0]);
    assertEquals('\0', boardState[2][1]);
    assertEquals('\0', boardState[2][2]);

    System.out.println("Test invalid move db");

  }

  /**
   * Player 1 made a valid move, game crashed, reboot with player 1 & 2 and board
   * changed.
   */
  @Test
  @Order(6)
  public void validMoveDB() {
    Unirest.get("http://localhost:8080/newgame").asString();
    Unirest.post("http://localhost:8080/startgame").body("type=O").asString();
    Unirest.get("http://localhost:8080/joingame").asString();
    Unirest.post("http://localhost:8080/move/1").body("x=1&y=1").asString();

    // Interrupt game
    PlayGame.stop();

    PlayGame.main(new String[0]);
    Unirest.get("http://localhost:8080/").asString();
    System.out.println("Rebooted");

    // check game board
    HttpResponse<String> response = Unirest.post("http://localhost:8080/testgameboard").asString();
    String responseBody = response.getBody();
    JSONObject jsonObject = new JSONObject(responseBody);
    assertEquals(true, jsonObject.get("gameStarted"));
    assertEquals(0, jsonObject.get("winner"));
    assertEquals(false, jsonObject.get("isDraw"));
    assertEquals(2, jsonObject.get("turn"));
    Gson gson = new Gson();
    GameBoard gameBoard = gson.fromJson(jsonObject.toString(), GameBoard.class);
    Player p1 = gameBoard.getP1();
    assertEquals('O', p1.getType());
    Player p2 = gameBoard.getP2();
    assertEquals('X', p2.getType());
    char[][] boardState = gameBoard.getBoardState();
    assertEquals('\0', boardState[0][0]);
    assertEquals('\0', boardState[0][1]);
    assertEquals('\0', boardState[0][2]);
    assertEquals('\0', boardState[1][0]);
    assertEquals('O', boardState[1][1]);
    assertEquals('\0', boardState[1][2]);
    assertEquals('\0', boardState[2][0]);
    assertEquals('\0', boardState[2][1]);
    assertEquals('\0', boardState[2][2]);

    System.out.println("Test valid move db");

  }

  /**
   * Player 1 won, game crashed, reboot with player 1 as winner .
   */
  @Test
  @Order(7)
  public void winnerDB() {
    Unirest.get("http://localhost:8080/newgame").asString();
    Unirest.post("http://localhost:8080/startgame").body("type=O").asString();
    Unirest.get("http://localhost:8080/joingame").asString();
    Unirest.post("http://localhost:8080/move/1").body("x=1&y=1").asString();
    Unirest.post("http://localhost:8080/move/2").body("x=0&y=1").asString();
    Unirest.post("http://localhost:8080/move/1").body("x=0&y=0").asString();
    Unirest.post("http://localhost:8080/move/2").body("x=0&y=2").asString();
    Unirest.post("http://localhost:8080/move/1").body("x=2&y=2").asString();

    // Interrupt game
    PlayGame.stop();

    PlayGame.main(new String[0]);
    Unirest.get("http://localhost:8080/").asString();
    System.out.println("Rebooted");

    // check game board
    HttpResponse<String> response = Unirest.post("http://localhost:8080/testgameboard").asString();
    String responseBody = response.getBody();
    JSONObject jsonObject = new JSONObject(responseBody);
    assertEquals(false, jsonObject.get("gameStarted"));
    assertEquals(1, jsonObject.get("winner"));
    assertEquals(false, jsonObject.get("isDraw"));
    assertEquals(2, jsonObject.get("turn"));
    Gson gson = new Gson();
    GameBoard gameBoard = gson.fromJson(jsonObject.toString(), GameBoard.class);
    Player p1 = gameBoard.getP1();
    assertEquals('O', p1.getType());
    Player p2 = gameBoard.getP2();
    assertEquals('X', p2.getType());
    char[][] boardState = gameBoard.getBoardState();
    assertEquals('O', boardState[0][0]);
    assertEquals('X', boardState[0][1]);
    assertEquals('X', boardState[0][2]);
    assertEquals('\0', boardState[1][0]);
    assertEquals('O', boardState[1][1]);
    assertEquals('\0', boardState[1][2]);
    assertEquals('\0', boardState[2][0]);
    assertEquals('\0', boardState[2][1]);
    assertEquals('O', boardState[2][2]);

    System.out.println("Test win db");

  }

  /**
   * Game Draw, game crashed, reboot with draw .
   */
  @Test
  @Order(8)
  public void drawDB() {
    Unirest.get("http://localhost:8080/newgame").asString();
    Unirest.post("http://localhost:8080/startgame").body("type=O").asString();
    Unirest.get("http://localhost:8080/joingame").asString();
    Unirest.post("http://localhost:8080/move/1").body("x=1&y=1").asString();
    Unirest.post("http://localhost:8080/move/2").body("x=0&y=1").asString();
    Unirest.post("http://localhost:8080/move/1").body("x=2&y=1").asString();
    Unirest.post("http://localhost:8080/move/2").body("x=0&y=2").asString();
    Unirest.post("http://localhost:8080/move/1").body("x=0&y=0").asString();
    Unirest.post("http://localhost:8080/move/2").body("x=1&y=0").asString();
    Unirest.post("http://localhost:8080/move/1").body("x=1&y=2").asString();
    Unirest.post("http://localhost:8080/move/2").body("x=2&y=2").asString();
    Unirest.post("http://localhost:8080/move/1").body("x=2&y=0").asString();

    // Interrupt game
    PlayGame.stop();

    PlayGame.main(new String[0]);
    Unirest.get("http://localhost:8080/").asString();
    System.out.println("Rebooted");

    // check game board
    HttpResponse<String> response = Unirest.post("http://localhost:8080/testgameboard").asString();
    String responseBody = response.getBody();
    JSONObject jsonObject = new JSONObject(responseBody);
    assertEquals(false, jsonObject.get("gameStarted"));
    assertEquals(0, jsonObject.get("winner"));
    assertEquals(true, jsonObject.get("isDraw"));
    assertEquals(2, jsonObject.get("turn"));
    Gson gson = new Gson();
    GameBoard gameBoard = gson.fromJson(jsonObject.toString(), GameBoard.class);
    Player p1 = gameBoard.getP1();
    assertEquals('O', p1.getType());
    Player p2 = gameBoard.getP2();
    assertEquals('X', p2.getType());
    char[][] boardState = gameBoard.getBoardState();
    assertEquals('O', boardState[0][0]);
    assertEquals('X', boardState[0][1]);
    assertEquals('X', boardState[0][2]);
    assertEquals('X', boardState[1][0]);
    assertEquals('O', boardState[1][1]);
    assertEquals('O', boardState[1][2]);
    assertEquals('O', boardState[2][0]);
    assertEquals('O', boardState[2][1]);
    assertEquals('X', boardState[2][2]);

    System.out.println("Test draw db");

  }

  /**
   * Game created, game crashed, reboot with new gameboard .
   */
  @Test
  @Order(9)
  public void newDB() {
    Unirest.get("http://localhost:8080/newgame").asString();

    // Interrupt game
    PlayGame.stop();

    PlayGame.main(new String[0]);
    Unirest.get("http://localhost:8080/").asString();
    System.out.println("Rebooted");

    // check game board
    HttpResponse<String> response = Unirest.post("http://localhost:8080/testgameboard").asString();
    String responseBody = response.getBody();
    JSONObject jsonObject = new JSONObject(responseBody);
    assertEquals(false, jsonObject.get("gameStarted"));
    assertEquals(0, jsonObject.get("winner"));
    assertEquals(false, jsonObject.get("isDraw"));

    System.out.println("Test create game and crash db");

  }
  
  /**
   * This method runs only once after all the test cases have been executed.
   */
  @AfterAll
  public static void close() {
    // Stop Server
    PlayGame.stop();
    System.out.println("============After All============");
  }
}
