package integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.google.gson.Gson;
import controllers.PlayGame;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;
import models.GameBoard;
import models.Message;
import models.Player;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(OrderAnnotation.class)
public class GameTest {

  /**
   * Runs only once before the testing starts.
   */
  @BeforeAll
  public static void init() {
    // Start Server
    PlayGame.main(new String[0]);
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
    //int restStatus = response.getStatus();
    System.out.println("------Before Each:");
  }

  /**
   * This is a test case to evaluate the newgame endpoint.
   */
  @Test
  @Order(2)
  public void newGameTest() {

    // Create HTTP request and get response
    HttpResponse<String> response = Unirest.get("http://localhost:8080/newgame").asString();
    int restStatus = response.getStatus();

    // Check assert statement (New Game has started)
    assertEquals(restStatus, 200);
    System.out.println("Test New Game");
  }

  /**
   * This is a test case to evaluate the startgame endpoint.
   */
  @Test
  @Order(3)
  public void startGameTest() {

    // Create a POST request to startgame endpoint and get the body
    // Remember to use asString() only once for an endpoint call. Every time you
    // call asString(), a new request will be sent to the endpoint. Call it once and
    // then use the data in the object.
    HttpResponse<String> response = Unirest.post("http://localhost:8080/startgame").body("type=X")
        .asString();
    String responseBody = response.getBody();

    // --------------------------- JSONObject Parsing
    // ----------------------------------

    System.out.println("Start Game Response: " + responseBody);

    // Parse the response to JSON object
    JSONObject jsonObject = new JSONObject(responseBody);

    // Check if game started after player 1 joins: Game should not start at this
    // point
    assertEquals(false, jsonObject.get("gameStarted"));

    // ---------------------------- GSON Parsing -------------------------

    // GSON use to parse data to object
    Gson gson = new Gson();
    GameBoard gameBoard = gson.fromJson(jsonObject.toString(), GameBoard.class);
    Player player1 = gameBoard.getP1();

    // Check if player type is correct
    assertEquals('X', player1.getType());

    System.out.println("Test Start Game");
  }

  /**
   * This is a test case to evaluate the join game endpoint.
   */
  @Test
  @Order(4)
  public void joingameTest() {
    
    HttpResponse<String> response = Unirest.get("http://localhost:8080/joingame").asString();
    int restStatus = response.getStatus();
    
    // Check assert statement (New Game has started)
    assertEquals(restStatus, 200);
    
    HttpResponse<String> response2 = Unirest.post("http://localhost:8080/testgameboard").asString();
    String responseBody = response2.getBody();
    System.out.println("Join Game Response: " + responseBody);

    // Parse the response to JSON object
    JSONObject jsonObject = new JSONObject(responseBody);

    // Check if game started after player 2 joins: Game should start at this point
    assertEquals(true, jsonObject.get("gameStarted"));
    // Check if it is currently player 1's turn
    assertEquals(1, jsonObject.get("turn"));

    // ---------------------------- GSON Parsing -------------------------

    // GSON use to parse data to object
    Gson gson = new Gson();
    GameBoard gameBoard = gson.fromJson(jsonObject.toString(), GameBoard.class);
    Player player1 = gameBoard.getP1();
    Player player2 = gameBoard.getP2();

    // Check if player type is correct
    assertNotEquals(player1.getType(), player2.getType());

    System.out.println("Test Join Game");
  }

  /**
   * This is a test case to evaluate the test game board endpoint.
   */
  @Test
  public void testgameboardTest() {
    HttpResponse<String> response = Unirest.post("http://localhost:8080/testgameboard").asString();
    String responseBody = response.getBody();
    System.out.println("Current gameboard: " + responseBody);
  }
  
  /**
   * This is a test case to evaluate the move end point
   * Invalid move: player 1 try to start without player 2.
   */
  @Test
  public void move1before2() {
    
    // disregard previous tests and create new game
    Unirest.get("http://localhost:8080/newgame").asString();
    Unirest.post("http://localhost:8080/startgame").body("type=O").asString();
    HttpResponse<String> response = Unirest.post("http://localhost:8080/move/1").body("x=1&y=1").asString();
    String responseBody = response.getBody();

    // Parse the response to JSON object
    JSONObject jsonObject = new JSONObject(responseBody);
    // GSON use to parse data to object
    Gson gson = new Gson();
    Message currentMessage = gson.fromJson(jsonObject.toString(), Message.class);
    assertEquals(false, currentMessage.isMoveValidity());
    assertEquals(401, currentMessage.getCode());
    
    // check game board not changed
    HttpResponse<String> response2 = Unirest.post("http://localhost:8080/testgameboard").asString();
    responseBody = response2.getBody();
    jsonObject = new JSONObject(responseBody);
    assertEquals(false, jsonObject.get("gameStarted"));
    GameBoard gameBoard = gson.fromJson(jsonObject.toString(), GameBoard.class);
    char[][] boardState = gameBoard.getBoardState();
    assertEquals('\0', boardState[1][1]);
    
    System.out.println("Test Player 1 try to move before Player 2 joins");

  }
  
  /**
   * This is a test case to evaluate the move end point
   * Invalid move: player 1 try to start without start game.
   */
  @Test
  public void move1beforeStart() {
    
    // disregard previous tests and create new game
    Unirest.get("http://localhost:8080/newgame").asString();
    HttpResponse<String> response = Unirest.post("http://localhost:8080/move/1").body("x=0&y=2").asString();
    String responseBody = response.getBody();

    // Parse the response to JSON object
    JSONObject jsonObject = new JSONObject(responseBody);
    // GSON use to parse data to object
    Gson gson = new Gson();
    Message currentMessage = gson.fromJson(jsonObject.toString(), Message.class);
    assertEquals(false, currentMessage.isMoveValidity());
    assertEquals(401, currentMessage.getCode());
    
    // check game board not changed
    HttpResponse<String> response2 = Unirest.post("http://localhost:8080/testgameboard").asString();
    responseBody = response2.getBody();
    jsonObject = new JSONObject(responseBody);
    assertEquals(false, jsonObject.get("gameStarted"));
    
    System.out.println("Test Player 1 try to move before starting game");

  }
  
  /**
   * This is a test case to evaluate the move end point
   * Invalid move: player 2 try to start without creating game.
   */
  @Test
  @Order(1)
  public void move2beforeNew() {
    
    // disregard previous tests and create new game
    HttpResponse<String> response = Unirest.post("http://localhost:8080/move/2").body("x=0&y=2").asString();
    String responseBody = response.getBody();

    // Parse the response to JSON object
    JSONObject jsonObject = new JSONObject(responseBody);
    // GSON use to parse data to object
    Gson gson = new Gson();
    Message currentMessage = gson.fromJson(jsonObject.toString(), Message.class);
    assertEquals(false, currentMessage.isMoveValidity());
    assertEquals(401, currentMessage.getCode());
    System.out.println("Test Player 2 try to make a move beofore creating game");
  }

  /**
   * This is a test case to evaluate the move end point
   * Invalid move: player 2 try to make a move after game starts.
   */
  @Test
  public void move2afterStart() {
    
    // disregard previous tests and create new game
    Unirest.get("http://localhost:8080/newgame").asString();
    Unirest.post("http://localhost:8080/startgame").body("type=X").asString();
    Unirest.get("http://localhost:8080/joingame").asString();
    HttpResponse<String> response = Unirest.post("http://localhost:8080/move/2").body("x=2&y=2").asString();
    String responseBody = response.getBody();

    // Parse the response to JSON object
    JSONObject jsonObject = new JSONObject(responseBody);
    // GSON use to parse data to object
    Gson gson = new Gson();
    Message currentMessage = gson.fromJson(jsonObject.toString(), Message.class);
    assertEquals(false, currentMessage.isMoveValidity());
    assertEquals(405, currentMessage.getCode());
    
    // check game board not changed
    HttpResponse<String> response2 = Unirest.post("http://localhost:8080/testgameboard").asString();
    responseBody = response2.getBody();
    jsonObject = new JSONObject(responseBody);
    assertEquals(true, jsonObject.get("gameStarted"));
    assertEquals(1, jsonObject.get("turn"));
    GameBoard gameBoard = gson.fromJson(jsonObject.toString(), GameBoard.class);
    char[][] boardState = gameBoard.getBoardState();
    assertEquals('\0', boardState[2][2]);
    
    System.out.println("Test Player 2 try to make 1st move");
  }
  
  /**
   * This is a test case to evaluate the move end point
   * Valid move: player 1 makes move after game starts.
   */
  @Test
  @Order(5)
  public void move1afterStart() {
    
    // check game started
    HttpResponse<String> response = Unirest.post("http://localhost:8080/testgameboard").asString();
    String responseBody = response.getBody();
    JSONObject jsonObject = new JSONObject(responseBody);
    assertEquals(true, jsonObject.get("gameStarted"));
    assertEquals(1, jsonObject.get("turn"));
    
    HttpResponse<String> response2 = Unirest.post("http://localhost:8080/move/1").body("x=1&y=2").asString();
    responseBody = response2.getBody();

    // Check move validity message
    jsonObject = new JSONObject(responseBody);
    Gson gson = new Gson();
    Message currentMessage = gson.fromJson(jsonObject.toString(), Message.class);
    assertEquals(true, currentMessage.isMoveValidity());
    assertEquals(100, currentMessage.getCode());

    // check game board changed
    HttpResponse<String> response3 = Unirest.post("http://localhost:8080/testgameboard").asString();
    responseBody = response3.getBody();
    jsonObject = new JSONObject(responseBody);
    assertEquals(true, jsonObject.get("gameStarted"));
    assertEquals(2, jsonObject.get("turn"));
    GameBoard gameBoard = gson.fromJson(jsonObject.toString(), GameBoard.class);
    char[][] boardState = gameBoard.getBoardState();
    assertEquals('X', boardState[1][2]);
    System.out.println("Test Player 1 makes a move");
    System.out.println("Current Game Board: " + responseBody);
  }

  /**
   * This is a test case to evaluate the move end point
   * Invalid move: player 2 makes 2 moves in their turn.
   */
  @Test
  @Order(6)
  public void move2TwoMoves() {
    
    // Player 1 has made a move, currently player 2's turn
    // check game started
    HttpResponse<String> response = Unirest.post("http://localhost:8080/testgameboard").asString();
    String responseBody = response.getBody();
    JSONObject jsonObject = new JSONObject(responseBody);
    assertEquals(true, jsonObject.get("gameStarted"));
    assertEquals(2, jsonObject.get("turn"));
    
    // make a move at x=1 y = 1
    HttpResponse<String> response2 = Unirest.post("http://localhost:8080/move/2").body("x=1&y=1").asString();
    responseBody = response2.getBody();
    // Check move validity message
    jsonObject = new JSONObject(responseBody);
    Gson gson = new Gson();
    Message currentMessage = gson.fromJson(jsonObject.toString(), Message.class);
    assertEquals(true, currentMessage.isMoveValidity());
    assertEquals(100, currentMessage.getCode());

    // check game board changed
    HttpResponse<String> response3 = Unirest.post("http://localhost:8080/testgameboard").asString();
    responseBody = response3.getBody();
    jsonObject = new JSONObject(responseBody);
    assertEquals(true, jsonObject.get("gameStarted"));
    assertEquals(1, jsonObject.get("turn"));
    GameBoard gameBoard = gson.fromJson(jsonObject.toString(), GameBoard.class);
    char[][] boardState = gameBoard.getBoardState();
    assertEquals('O', boardState[1][1]);
    
    // make a move at x=1 y=0
    HttpResponse<String> response4 = Unirest.post("http://localhost:8080/move/2").body("x=1&y=0").asString();
    responseBody = response4.getBody();
    // Check move validity message
    jsonObject = new JSONObject(responseBody);
    gson = new Gson();
    currentMessage = gson.fromJson(jsonObject.toString(), Message.class);
    assertEquals(false, currentMessage.isMoveValidity());
    assertEquals(405, currentMessage.getCode());
    
    // check game board changed
    HttpResponse<String> response5 = Unirest.post("http://localhost:8080/testgameboard").asString();
    responseBody = response5.getBody();
    jsonObject = new JSONObject(responseBody);
    assertEquals(true, jsonObject.get("gameStarted"));
    assertEquals(1, jsonObject.get("turn"));
    gameBoard = gson.fromJson(jsonObject.toString(), GameBoard.class);
    boardState = gameBoard.getBoardState();
    assertEquals('\0', boardState[1][0]);
    
    System.out.println("Test Player 2 making 2 moves");
  }
  
  /**
   * This is a test case to evaluate the move end point
   * Invalid move: player 1 selects an occupied tile.
   */
  @Test
  @Order(7)
  public void move1Occupied() {
    
    // Player 2 has made a move, currently player 1's turn
    // check game started
    HttpResponse<String> response = Unirest.post("http://localhost:8080/testgameboard").asString();
    String responseBody = response.getBody();
    JSONObject jsonObject = new JSONObject(responseBody);
    assertEquals(true, jsonObject.get("gameStarted"));
    assertEquals(1, jsonObject.get("turn"));
    
    // make a move at x=1 y = 1
    HttpResponse<String> response2 = Unirest.post("http://localhost:8080/move/1").body("x=1&y=1").asString();
    responseBody = response2.getBody();
    // Check move validity message
    jsonObject = new JSONObject(responseBody);
    Gson gson = new Gson();
    Message currentMessage = gson.fromJson(jsonObject.toString(), Message.class);
    assertEquals(false, currentMessage.isMoveValidity());
    assertEquals(403, currentMessage.getCode());

    // check game board changed
    HttpResponse<String> response3 = Unirest.post("http://localhost:8080/testgameboard").asString();
    responseBody = response3.getBody();
    jsonObject = new JSONObject(responseBody);
    assertEquals(true, jsonObject.get("gameStarted"));
    assertEquals(1, jsonObject.get("turn"));
    GameBoard gameBoard = gson.fromJson(jsonObject.toString(), GameBoard.class);
    char[][] boardState = gameBoard.getBoardState();
    assertEquals('O', boardState[1][1]);
    
    System.out.println("Test Player 1 selecting occupied tile");
  }
  
  /**
   * This is a test case to evaluate the move end point
   * Valid move: player 1 selects a tile and wins.
   */
  @Test
  @Order(8)
  public void move2Win() {
    
    // Player 2 has made a move, currently player 1's turn
    // check game started
    HttpResponse<String> response = Unirest.post("http://localhost:8080/testgameboard").asString();
    String responseBody = response.getBody();
    JSONObject jsonObject = new JSONObject(responseBody);
    assertEquals(true, jsonObject.get("gameStarted"));
    assertEquals(1, jsonObject.get("turn"));
    
    // make moves till almost win
    Unirest.post("http://localhost:8080/move/1").body("x=2&y=2").asString();
    Unirest.post("http://localhost:8080/move/2").body("x=0&y=1").asString();
    Unirest.post("http://localhost:8080/move/1").body("x=0&y=0").asString();
    HttpResponse<String> response2 = Unirest.post("http://localhost:8080/move/2").body("x=2&y=1").asString();
    responseBody = response2.getBody();
    // Check move validity message
    jsonObject = new JSONObject(responseBody);
    Gson gson = new Gson();
    Message currentMessage = gson.fromJson(jsonObject.toString(), Message.class);
    assertEquals(true, currentMessage.isMoveValidity());
    assertEquals(100, currentMessage.getCode());

    // check game board changed
    HttpResponse<String> response3 = Unirest.post("http://localhost:8080/testgameboard").asString();
    responseBody = response3.getBody();
    jsonObject = new JSONObject(responseBody);
    assertEquals(false, jsonObject.get("gameStarted"));
    assertEquals(2, jsonObject.get("winner"));
    GameBoard gameBoard = gson.fromJson(jsonObject.toString(), GameBoard.class);
    char[][] boardState = gameBoard.getBoardState();
    assertEquals('O', boardState[2][1]);
    
    System.out.println("Test Player 2 winning game");
  }

  /**
   * This is a test case to evaluate the move end point
   * Valid moves: player 1 player 2 selects valid tiles. 
   */
  @Test
  @Order(9)
  public void moveValid() {
    
    // Disregard previous state
    Unirest.get("http://localhost:8080/newgame").asString();
    Unirest.post("http://localhost:8080/startgame").body("type=O").asString();
    Unirest.get("http://localhost:8080/joingame").asString();
    
    // make moves till almost win
    Unirest.post("http://localhost:8080/move/1").body("x=1&y=1").asString();
    Unirest.post("http://localhost:8080/move/2").body("x=0&y=1").asString();
    Unirest.post("http://localhost:8080/move/1").body("x=2&y=1").asString();
    Unirest.post("http://localhost:8080/move/2").body("x=0&y=2").asString();
    Unirest.post("http://localhost:8080/move/1").body("x=0&y=0").asString(); 
    Unirest.post("http://localhost:8080/move/2").body("x=1&y=0").asString();
    Unirest.post("http://localhost:8080/move/1").body("x=1&y=2").asString();
   
    
    // check game board changed
    HttpResponse<String> response3 = Unirest.post("http://localhost:8080/testgameboard").asString();
    String responseBody = response3.getBody();
    JSONObject jsonObject = new JSONObject(responseBody);
    assertEquals(true, jsonObject.get("gameStarted"));
    assertEquals(2, jsonObject.get("turn"));
    Gson gson = new Gson();
    GameBoard gameBoard = gson.fromJson(jsonObject.toString(), GameBoard.class);
    char[][] boardState = gameBoard.getBoardState();
    assertEquals('O', boardState[1][1]);
    assertEquals('O', boardState[2][1]);
    assertEquals('O', boardState[0][0]);
    assertEquals('O', boardState[1][2]);
    assertEquals('X', boardState[0][1]);
    assertEquals('X', boardState[0][2]);
    assertEquals('X', boardState[1][0]);
    
    assertEquals('\0', boardState[2][0]);
    assertEquals('\0', boardState[2][2]);
    
    System.out.println("Test Player 1 & 2 making valid moves");
  }
  
  /**
   * This is a test case to evaluate the move end point
   * Valid moves: player 1 wins. 
   */
  @Test
  @Order(10)
  public void move1Win() {
    
    Unirest.post("http://localhost:8080/move/2").body("x=2&y=0").asString();
    // make a move and win
    HttpResponse<String> response = Unirest.post("http://localhost:8080/move/1").body("x=2&y=2").asString();
    String responseBody = response.getBody();
    // Check move validity message
    JSONObject jsonObject = new JSONObject(responseBody);
    Gson gson = new Gson();
    Message currentMessage = gson.fromJson(jsonObject.toString(), Message.class);
    assertEquals(true, currentMessage.isMoveValidity());
    assertEquals(100, currentMessage.getCode());

    // check game board changed
    HttpResponse<String> response3 = Unirest.post("http://localhost:8080/testgameboard").asString();
    responseBody = response3.getBody();
    jsonObject = new JSONObject(responseBody);
    assertEquals(false, jsonObject.get("gameStarted"));
    assertEquals(1, jsonObject.get("winner"));
    GameBoard gameBoard = gson.fromJson(jsonObject.toString(), GameBoard.class);
    char[][] boardState = gameBoard.getBoardState();
    assertEquals('O', boardState[2][2]);
    
    System.out.println("Test Player 1 winning game");
  }
  
  /**
   * This is a test case to evaluate the move end point.
   * Valid moves: Ties
   */
  @Test
  public void moveTie() {
    
    Unirest.get("http://localhost:8080/newgame").asString();
    Unirest.post("http://localhost:8080/startgame").body("type=O").asString();
    Unirest.get("http://localhost:8080/joingame").asString();
    
    // make moves till almost win
    Unirest.post("http://localhost:8080/move/1").body("x=1&y=1").asString();
    Unirest.post("http://localhost:8080/move/2").body("x=0&y=1").asString();
    Unirest.post("http://localhost:8080/move/1").body("x=2&y=1").asString();
    Unirest.post("http://localhost:8080/move/2").body("x=0&y=2").asString();
    Unirest.post("http://localhost:8080/move/1").body("x=0&y=0").asString(); 
    Unirest.post("http://localhost:8080/move/2").body("x=1&y=0").asString();
    Unirest.post("http://localhost:8080/move/1").body("x=1&y=2").asString();
    Unirest.post("http://localhost:8080/move/2").body("x=2&y=2").asString();
    Unirest.post("http://localhost:8080/move/1").body("x=2&y=0").asString();
    // check game board changed
    HttpResponse<String> response2 = Unirest.post("http://localhost:8080/testgameboard").asString();
    String responseBody = response2.getBody();
    JSONObject jsonObject = new JSONObject(responseBody);
    assertEquals(false, jsonObject.get("gameStarted"));
    assertEquals(0, jsonObject.get("winner"));
    assertEquals(true, jsonObject.get("isDraw"));
    
    System.out.println("Test Draw");
  }
  
  /**
   * This will run every time after a test has finished.
   */
  @AfterEach
  public void finishGame() {
    System.out.println("After Each--------");
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
