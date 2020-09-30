package unit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.Gson;
import controllers.PlayGame;
import models.GameBoard;
import models.Message;
import models.Move;
import models.Player;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class MethodTest {

  private static Gson gson;

  /**
   * Runs only once before the testing starts.
   */
  @BeforeAll
  public static void init() {
    // Start Server
    PlayGame.main(new String[0]);
    System.out.println("=============Before All===========");
  }
  
  
  /*
   * Test sendGameBoardToAllPlayers
   * 
   */
  @Test
  public void testSendGB() {
    Player p1 = new Player();
    Player p2 = new Player();
    p1.setId(1);
    p2.setId(2);
    p1.setType('X');
    p2.setType('O');
    gson = new Gson();
    PlayGame.gb = new GameBoard();
    PlayGame.gb.setP1(p1);
    PlayGame.gb.setP2(p2);
    PlayGame.gb.setTurn(1);
    PlayGame.sendGameBoardToAllPlayers(gson.toJson(PlayGame.gb));

  }

  /*
   * Test Valid Move by player 1 on CheckMoveValid()
   * 
   */
  @Test

  public void testCheckMove_valid_p1() {

    // Set up board for valid moves
    gson = new Gson();
    Player p1 = new Player();
    p1.setId(1);
    p1.setType('O');
    Player p2 = new Player();
    p2.setId(2);
    p2.setType('X');
    PlayGame.gb.setP1(p1);
    PlayGame.gb.setP2(p2);
    PlayGame.gb.setTurn(1);
    PlayGame.gb.setGameStarted(true);
    PlayGame.gb.setBoardState(new char[3][3]);

    Move currentMove = new Move();
    currentMove.setMoveX(1);
    currentMove.setMoveY(1);
    currentMove.setPlayer(p1);
    Message currentMessage = new Message();

    currentMessage = PlayGame.checkMoveValid(currentMove, currentMessage);
    assertEquals(100, currentMessage.getCode());
    System.out.println("P1 Valid move message: " + currentMessage.getMessage());

  }

  /*
   * Test Valid Move by player 2 on CheckMoveValid()
   * 
   */
  @Test

  public void testCheckMove_valid_p2() {

    // Set up board for valid moves
    gson = new Gson();
    PlayGame.gb = new GameBoard();
    Player p1 = new Player();
    p1.setId(1);
    p1.setType('O');
    Player p2 = new Player();
    p2.setId(2);
    p2.setType('X');
    PlayGame.gb.setP1(p1);
    PlayGame.gb.setP2(p2);
    PlayGame.gb.setTurn(2);
    PlayGame.gb.setGameStarted(true);
    char[][] boardState = new char[3][3];
    boardState[1][1] = 'O';

    PlayGame.gb.setBoardState(boardState);
    Move currentMove = new Move();
    currentMove.setMoveX(0);
    currentMove.setMoveY(1);
    currentMove.setPlayer(p2);
    Message currentMessage = new Message();

    currentMessage = PlayGame.checkMoveValid(currentMove, currentMessage);
    assertEquals(100, currentMessage.getCode());
    assertEquals(true, currentMessage.isMoveValidity());
    System.out.println("P2 Valid move message: " + currentMessage.getMessage());

  }

  /*
   * Test Invalid Move on CheckMoveValid() player trying to select occupied tile
   */
  @Test

  public void testCheckMove_Occupied() {

    // Set up board for invalid moves
    gson = new Gson();
    PlayGame.gb = new GameBoard();
    Player p1 = new Player();
    p1.setId(1);
    p1.setType('O');
    Player p2 = new Player();
    p2.setId(2);
    p2.setType('X');
    PlayGame.gb.setP1(p1);
    PlayGame.gb.setP2(p2);
    PlayGame.gb.setTurn(2);
    PlayGame.gb.setGameStarted(true);
    char[][] boardState = new char[3][3];
    boardState[1][1] = 'O';
    PlayGame.gb.setBoardState(boardState);

    Move currentMove = new Move();
    currentMove.setMoveX(1);
    currentMove.setMoveY(1);
    currentMove.setPlayer(p2);
    Message currentMessage = new Message();

    currentMessage = PlayGame.checkMoveValid(currentMove, currentMessage);
    assertEquals(false, currentMessage.isMoveValidity());
    assertEquals(403, currentMessage.getCode());
    System.out.println("Invalid move message: " + currentMessage.getMessage());

  }

  /*
   * Test Invalid Move on CheckMoveValid() player trying move at someone else's
   * turn
   */
  @Test

  public void testCheckMoveWrongTurn() {

    // Set up board for invalid moves
    gson = new Gson();
    PlayGame.gb = new GameBoard();
    Player p1 = new Player();
    p1.setId(1);
    p1.setType('O');
    Player p2 = new Player();
    p2.setId(2);
    p2.setType('X');
    PlayGame.gb.setP1(p1);
    PlayGame.gb.setP2(p2);
    PlayGame.gb.setTurn(2);
    PlayGame.gb.setGameStarted(true);
    char[][] boardState = new char[3][3];
    boardState[1][1] = 'O';
    PlayGame.gb.setBoardState(boardState);

    Move currentMove = new Move();
    currentMove.setMoveX(2);
    currentMove.setMoveY(2);
    currentMove.setPlayer(p1);
    Message currentMessage = new Message();

    currentMessage = PlayGame.checkMoveValid(currentMove, currentMessage);
    assertEquals(405, currentMessage.getCode());
    assertEquals(false, currentMessage.isMoveValidity());
    System.out.println("Invalid move message: " + currentMessage.getMessage());

  }

  /*
   * Test on checkGameEnd when game has ended with a player winning
   * 
   */
  @Test

  public void testCheckGameEndTrue() {

    // Set up board for invalid moves
    Player p1 = new Player();
    p1.setId(1);
    p1.setType('O');
    Player p2 = new Player();
    p2.setId(2);
    p2.setType('X');
    PlayGame.gb = new GameBoard();
    PlayGame.gb.setP1(p1);
    PlayGame.gb.setP2(p2);
    PlayGame.gb.setTurn(1);
    PlayGame.gb.setGameStarted(true);
    
    // Check 8 different game end conditions
    char[][] boardState = { { 'O', 'X', '\0' }, { 'O', 'X', '\0' }, { 'O', '\0', '\0' } };
    PlayGame.gb.setBoardState(boardState);
    boolean gameEnd = PlayGame.checkGameEnd();
    assertEquals(true, gameEnd);
    
    char[][] boardState2 = { { '\0', 'O', 'X' }, { '\0', 'O', 'X' }, { '\0', 'O', '\0' } };
    PlayGame.gb.setBoardState(boardState2);
    gameEnd = PlayGame.checkGameEnd();
    assertEquals(true, gameEnd);
    
    char[][] boardState3 = { { 'X', '\0', 'O' }, { 'X', '\0', 'O' }, { '\0', '\0', 'O' } };
    PlayGame.gb.setBoardState(boardState3);
    gameEnd = PlayGame.checkGameEnd();
    assertEquals(true, gameEnd);
    
    char[][] boardState4 = { { 'O', 'O', 'O' }, { '\0', '\0', '\0'}, { 'X', 'X', '\0'  } };
    PlayGame.gb.setBoardState(boardState4);
    gameEnd = PlayGame.checkGameEnd();
    assertEquals(true, gameEnd);
    
    char[][] boardState5 = { { 'X', 'X', '\0' }, { 'O', 'O', 'O' }, { '\0', '\0', '\0' } };
    PlayGame.gb.setBoardState(boardState5);
    gameEnd = PlayGame.checkGameEnd();
    assertEquals(true, gameEnd);
    
    char[][] boardState6 = { { '\0', '\0', '\0' }, { 'X', 'X', '\0'  }, { 'O', 'O', 'O' } };
    PlayGame.gb.setBoardState(boardState6);
    gameEnd = PlayGame.checkGameEnd();
    assertEquals(true, gameEnd);
    
    char[][] boardState7 = { { 'O', '\0', '\0' }, { 'X', 'O', '\0'  }, { 'X', '\0', 'O' } };
    PlayGame.gb.setBoardState(boardState7);
    gameEnd = PlayGame.checkGameEnd();
    assertEquals(true, gameEnd);
    
    char[][] boardState8 = { { '\0', '\0', 'O' }, { 'X', 'O', '\0'  }, { 'O', '\0', 'X' } };
    PlayGame.gb.setBoardState(boardState8);
    gameEnd = PlayGame.checkGameEnd();
    assertEquals(true, gameEnd);
    
    char[][] boardState9 = { { '\0', '\0', 'O' }, { 'X', '\0', '\0'  }, { 'O', '\0', 'O' } };
    PlayGame.gb.setBoardState(boardState9);
    gameEnd = PlayGame.checkGameEnd();
    assertEquals(false, gameEnd);
    
    char[][] boardState10 = { { '\0', '\0', 'O' }, { 'X', '\0', '\0'  }, { 'O', 'X', 'O' } };
    PlayGame.gb.setBoardState(boardState10);
    gameEnd = PlayGame.checkGameEnd();
    assertEquals(false, gameEnd);

  }

  /*
   * Test on checkGameEnd when game has ended with a tie
   * 
   */
  @Test

  public void testCheckGameEndTie() {

    // Set up board for invalid moves
    gson = new Gson();
    PlayGame.gb = new GameBoard();
    Player p1 = new Player();
    p1.setId(1);
    p1.setType('O');
    Player p2 = new Player();
    p2.setId(2);
    p2.setType('X');
    PlayGame.gb.setP1(p1);
    PlayGame.gb.setP2(p2);
    PlayGame.gb.setTurn(2);
    PlayGame.gb.setGameStarted(true);
    char[][] boardState = { { 'O', 'X', 'O' }, { 'X', 'O', 'O' }, { 'X', 'O', 'X' } };
    PlayGame.gb.setBoardState(boardState);

    boolean gameEnd = PlayGame.checkGameEnd();
    assertEquals(true, gameEnd);

  }

  /*
   * Test on checkGameEnd when game has not ended
   * 
   */
  @Test

  public void testCheckGameEndFalse() {

    // Set up board for invalid moves
    gson = new Gson();
    PlayGame.gb = new GameBoard();
    Player p1 = new Player();
    p1.setId(1);
    p1.setType('O');
    Player p2 = new Player();
    p2.setId(2);
    p2.setType('X');
    PlayGame.gb.setP1(p1);
    PlayGame.gb.setP2(p2);
    PlayGame.gb.setTurn(1);
    PlayGame.gb.setGameStarted(true);
    char[][] boardState = new char[3][3];
    boardState[0][1] = 'O';
    PlayGame.gb.setBoardState(boardState);

    boolean gameEnd = PlayGame.checkGameEnd();
    assertEquals(false, gameEnd);
    
    boardState[0][1] = '\0';
    boardState[0][0] = 'O';
    boardState[1][2] = 'X';
    boardState[2][0] = 'O';
    PlayGame.gb.setBoardState(boardState);

    gameEnd = PlayGame.checkGameEnd();
    assertEquals(false, gameEnd);

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
