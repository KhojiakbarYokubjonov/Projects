import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import displays.Player;
import displays.GameOverDisplay;
import game.Robot;
import game.TimedMode;
import game.Chessboard;
import game.GameStatus;
import pieces.Rook;
import pieces.King;
import pieces.Piece;
import pieces.Queen;
import pieces.Bishop;
import pieces.Knight;

import java.io.File;
import java.net.Socket;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedWriter;

/**
 * The general UI for the Chess game.
 * 
 * @authors Ali Sartaz Khan & Jonathan Houge & Khojiakbar Yokubjonov & Julius Ramirez
 */
public class UI {

	private Canvas canvas;
	private Display display;
	private Shell shell;
	
	private BufferedWriter out;
	private BufferedReader in;
	
	Socket socket;

	Client client;
	Chessboard boardUI;
	GameStatus gameStatus;
	
	int gameOver = 0;
	boolean initialized = false;
	int SHELL_WIDTH_OFFSET = 20;
	int SHELL_HEIGHT_OFFSET = 50;
	public static int BOARD_COORD_OFFSET = 100;
	Piece selectedPiece; 
	public boolean whitesTurn; 
	public boolean yourTurn;
	
	// loading file
	protected Menu menuBar, fileMenu;
	protected MenuItem fileMenuHeader,fileSaveItem,fileExitItem;
	boolean loadOldGame;
	private String fileName;
	
	// timed mode
	private Composite upperComposite; private Composite middleComposite; private Composite lowerComposite;
	private TimedMode yourTimer; private TimedMode opponentsTimer;
	public boolean isOpponentConnected; String opponentsPreferedTime;
	
	String opponent; String username;
	private Robot robot;
	Player player;
	boolean isLocalGame = false;
	
	/**
	 * Constructor that assigns values.
	 * 
	 * in == null is used for local games.
	 * 
	 * @param client: client object
	 * @param in: input stream
	 * @param out: output stream
	 * @param socket: Socket object
	 */
	public UI(Client client, BufferedReader in, BufferedWriter out, Socket socket) {
		this.in = in; this.out = out;
		this.socket = socket; this.client = client;
		
		this.player = this.client.getPlayer();
		this.fileName = this.player.getFileName();
		this.username = this.player.getName();
		
		if (in == null || client.getPlayer().getColor().equals("White")) {
			this.whitesTurn = true;
			this.yourTurn = true; }
		
		else {
			this.whitesTurn = false;
			this.yourTurn = false; }
		
		isOpponentConnected = false;
	}
	
	
	/**
	 * Constructor when Client plays against a robot
	 * 
	 * @param client: Client Object
	 * @param robot: Robot opponent
	 */
	public UI(Client client, Robot robot) {
		this.robot = robot;
		this.client = client;
		this.player = this.client.getPlayer();
		this.fileName = this.player.getFileName();
		this.username = this.player.getName();
		if (this.player.getColor().equals("White")) {
			this.whitesTurn = true;
			this.yourTurn = true;
		}
		else {
			this.whitesTurn = false;
			this.yourTurn = false;
		}
		
		isOpponentConnected = true;
	}
	
	/**
	 * Constructor when Client plays local game
	 * 
	 * @param player: Player object from client
	 */
	public UI(Player player) {
		this.player = player;
		this.fileName = player.getFileName();
		this.username = player.getName();
		this.whitesTurn = true; //Newly added field
		this.yourTurn = true;
		isOpponentConnected = true;
		this.isLocalGame = true;
	}

	/**
	 * Starts running the UI by setting up all the displays
	 * @return true or false [play again or not]
	 */
	public boolean start() {
		setup();
		
		gameStatus = new GameStatus(shell);
		loadOldGame = false;
		validateFileName();
		
		shell.addListener(SWT.Close, new Listener() {
			@Override
			public void handleEvent(Event event) {
				System.out.println("Closing the shell!");
				String fileToSaveGame = gameStatus.promptsFileWhileExiting();
				gameStatus.saveGame(boardUI.getBoard(), yourTurn, whitesTurn); }
		});
		
		canvas.addPaintListener(e -> {
			if (initialized == false) { // create the tiles and initial starting positions
				boardUI.createBoardData(e.gc);
				   
				if (!loadOldGame) {
					boardUI.setAllPieces();
					if (robot != null)
					robot.populatePiecesList(boardUI.getBoard()); }
				
				else {
					gameStatus.setFileName(fileName);
					gameStatus.loadGame(boardUI.getBoard());
					if (robot != null)
						robot.populatePiecesList(boardUI.getBoard()); }
				
				if (robot != null && !yourTurn) {
					robot.movePiece();
					yourTurn = true; }
				
				initialized = true;
			}
			
			if (initialized) { // board is already made
				boardUI.draw(e.gc); 
				if(whitesTurn) { System.out.println("It is currently whites turn!"); }
				else { System.out.println("It is currently blacks turn!"); }
			}
		});	

		canvas.addMouseListener(new MouseListener() {
			/**
			 * Called upon when mouse is pressed
			 * 
			 * @param mouse event
			 */
			public void mouseDown(MouseEvent e) {
				if(checkTimers()) // returns true if either of players run out of time
					return;
				
				if (!yourTurn) {
					return;}

				//Gather data, convert graphical coordinates into chessboard coordinates
				int coordinates[] = boardUI.getBoardIndex(e.x,e.y);
				
				if(coordinates == null) { return; }
				
				int xCoord = coordinates[0];
				int yCoord = coordinates[1];
				
				//Select a piece OR move piece
				if (selectedPiece == null) { // Selecting piece for first time
					selectedPiece = boardUI.selectPiece(xCoord, yCoord, whitesTurn);
					if (selectedPiece != null) {
						selectedPiece.setSelected();
						boardUI.highlightCoordinates(selectedPiece);
						canvas.redraw();
					}
				}
				
				else { // Determine if move being made or selecting new piece
					Piece possibleSelection;
					possibleSelection = boardUI.selectPiece(xCoord, yCoord,whitesTurn);
					
					if (possibleSelection != null) {// player has selected new piece
						if (boardUI.validMoveMade(xCoord,yCoord,selectedPiece,whitesTurn)) { //castling requires user to click rook 
							King k = (King) selectedPiece;
							k.castlingMoveMade = true;
							makeMove(xCoord,yCoord); }
						else {
							boardUI.unhighlightCoordinates();
							selectedPiece.SetNotSelected();
							selectedPiece = possibleSelection;
							selectedPiece.setSelected();
							boardUI.highlightCoordinates(selectedPiece);
							canvas.redraw(); }
					}
					
					else { // player may have moved onto empty space or onto enemy
						if(boardUI.validMoveMade(xCoord,yCoord,selectedPiece,whitesTurn)) {
							makeMove(xCoord,yCoord); }
						else { System.out.println("UI - INVALID MOVE MADE!"); }
					}
				}
			}

			/**
			 * Make piece move to a certain coordinate
			 * 
			 * @param xCoord: X coordinate
			 * @param yCoord: Y coordinate
			 */
			public void makeMove(int xCoord,int yCoord) {
				int xCoordBefore = selectedPiece.getX();
				int yCoordBefore = selectedPiece.getY();
				
				boardUI.unhighlightCoordinates();
				gameOver = boardUI.updateBoard(xCoord,yCoord,selectedPiece);
				
				if (in != null) { // don't want to communicate to server if local game
					try {
						if (boardUI.getPromotion()) { // pawn promotion
							String promotedPiece = boardUI.getTile(xCoord, yCoord).getPiece().getName();
							out.write("MOVE:"+xCoordBefore+"-"+yCoordBefore+"-"+xCoord+"-"+yCoord+"-PROMOTION:"+promotedPiece); }
						
						else { // normal move
							out.write("MOVE:"+xCoordBefore+"-"+yCoordBefore+"-"+xCoord+"-"+yCoord); }
						
						out.newLine();
						out.flush(); } 
					catch (IOException e1) { e1.printStackTrace(); } 
					yourTurn = !yourTurn;
				}
				
				else if (robot != null) {// Robot play
					robot.movePiece(); }
				
				else { whitesTurn = !whitesTurn; } // Local play
				
				canvas.redraw();
				selectedPiece.SetNotSelected();
				selectedPiece = null;
			}
			
			public void mouseUp(MouseEvent e) {} 
			public void mouseDoubleClick(MouseEvent e) {} 
		});

		canvas.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) { canvas.redraw(); } 
			public void keyReleased(KeyEvent e) {}
		});
		
		if (in != null) { // no need for server runnable if we're local
			Runnable runnable = new Runner();
			display.asyncExec(runnable); }
		
		shell.open(); 
		boolean isJustConnected = true;
		long start = System.currentTimeMillis();
		
		while (!shell.isDisposed() && gameOver == 0) 
				if (!display.readAndDispatch()) {
					long end = System.currentTimeMillis();
					
					// keeps track of the time for the game
					int diff = (int) ((end-start)/1000);
					if (diff == 1) {
						if (isOpponentConnected) { // the timers start when both players are connected
							if (isJustConnected) {
								if (opponentsPreferedTime != null) {
									if (!opponentsPreferedTime.contains("M")&& !opponentsPreferedTime.contains("S")) {
										opponentsTimer = new TimedMode(shell, upperComposite);
										//sets the timer for the opponent
										opponentsTimer.setTimeLimit(opponentsPreferedTime); 
										opponentsTimer.setPlayer(opponent);
									}
								}
								isJustConnected = false;
							}
							checkTimers();
							if(yourTurn && yourTimer != null) { yourTimer.update(); }
							else if(!yourTurn && opponentsTimer != null) {opponentsTimer.update();}
						}
						start = end;
					}
				}
	
		display.sleep();
		
		boolean again = false; // assume that client won't want to play again
		if (gameOver != 0) { // makes sure game over display doesn't pop up if client was closed
			again = new GameOverDisplay().start(display, gameOver); }
		
		display.dispose();
		
		if (again) { return true; }
		return false;
	}
	
	/**
	 * Setup all GUI components
	 */
	void setup() {
		display = new Display();
		
		shell = new Shell(display);
		shell.setSize(640+BOARD_COORD_OFFSET + SHELL_WIDTH_OFFSET, 800+BOARD_COORD_OFFSET/2 + SHELL_HEIGHT_OFFSET);
		
		shell.setLayout(new GridLayout());
		defineComposites();
		
		defineYourTimer(this.player.getPreferredTime());
		if (client != null) {
			shell.setText("Chess: " + client.getPlayer().getName() + " (" + client.getPlayer().getColor() + ")"); }
		else {
			shell.setText("Chess"); }
		
		canvas = new Canvas(middleComposite, SWT.FOCUSED);
		canvas.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		canvas.setSize(640+BOARD_COORD_OFFSET, 600+BOARD_COORD_OFFSET/2 + SHELL_HEIGHT_OFFSET);
//		canvas.setBounds(0, 0, 640, 640);
		
		createMenuBar();
		shell.setMenuBar(menuBar);
		boardUI = new Chessboard(canvas, shell, display);
		if (this.robot != null)
			robot.setBoard(boardUI);
	}
	

	/**
	 * Set connected to true
	 */
	public void setConnected() {this.isOpponentConnected = true;}
	
	/**
	 * Checks if timers are over
	 * @return true or false 
	 */
	private boolean checkTimers() {
		if (yourTimer != null) {
			if (yourTimer.isTimerOver()) {
				if(player.getColor().equals("White")) {gameOver = 2; return true; }
				else {gameOver = 1; return true;}
			}
		}
		if (opponentsTimer != null) {
			if(opponentsTimer.isTimerOver()) {
				if(player.getColor().equals("White")) {gameOver = 1; return true; }
				else {gameOver = 2; return true;}
			}
			
		}
		return false;
	}
	
	/**
	 * Define your own timer for the game
	 * 
	 * @param time: Time String
	 */
	private void defineYourTimer(String time) {
		if (time.contains("M") || time.contains("S") || isLocalGame) { return; }
		yourTimer = new TimedMode(shell, lowerComposite);
		yourTimer.setPlayer(username);
		yourTimer.setTimeLimit(time);
	}
	
	
	/**
	 * Defines composites for the shell
	 */
	private void defineComposites() {
		RowLayout rowLayout = new RowLayout();
		rowLayout.type = SWT.HORIZONTAL;
		rowLayout.marginLeft = 0;
		rowLayout.marginRight = 0;
		
		//composite to hold the opponent's timer
		upperComposite = new Composite(shell, SWT.NO_FOCUS);
		upperComposite.setLayoutData(opponentsTimer);
		upperComposite.setLayout(rowLayout);
		
		//composite to hold the canvas
		middleComposite = new Composite(shell, SWT.NO_FOCUS);
		middleComposite.setLayoutData(canvas);
		
		//composite to hold the player's timer
		lowerComposite = new Composite(shell, SWT.NO_FOCUS);
		lowerComposite.setLayoutData(yourTimer);
		lowerComposite.setLayout(rowLayout);
	}
	
	
	/**
	 * Checks if the entered game file exists.
	 * If not, it generates a new game instead.
	 */
	private void validateFileName(){
		if(fileName.equals("Enter file name")) {return;}
		String cwd = System.getProperty("user.dir") + "\\Saved Games\\" + fileName;
		File dir = new File(cwd);
		 if(dir.exists()) {
			 loadOldGame = true;
		 }else {
			 loadOldGame = false;
			 System.out.println("Sorry, the entered file could not be found!");
		 }
	}
	
	/**
	 * establishes the menu bar at the top of shell
	 * 
	 */
	protected void createMenuBar() {
		menuBar = new Menu(shell, SWT.BAR);
		createFileMenu(); //file ops
	}
	
	/**
	 * creates the file menu
	 */
	protected void createFileMenu() {
		fileMenuHeader = new MenuItem(menuBar, SWT.CASCADE);
		fileMenuHeader.setText("File");
		fileMenu = new Menu(shell, SWT.DROP_DOWN);
		fileMenuHeader.setMenu(fileMenu);
		createFileSaveItem();
		createFileExitItem();
	}
	
	/**
	 * Creates the file save menu.
	 */
	protected void createFileSaveItem() {
		fileSaveItem = new MenuItem(fileMenu, SWT.PUSH);
		fileSaveItem.setText("Save");
		fileSaveItem.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent event) {
				
				//prompts the user for a file name to save the game
				gameStatus.getFileName(); //prompts the player for a file name
				//saves the game inside the Saved Games folder
				gameStatus.saveGame(boardUI.getBoard(), yourTurn, whitesTurn);
			}

			public void widgetDefaultSelected(SelectionEvent event) {}
		});
	}
	
	/**
	 *  Creates file exit option. when selected, exits the program
	 */
	protected void createFileExitItem() {
		fileExitItem = new MenuItem(fileMenu, SWT.PUSH);
		fileExitItem.setText("Exit");
		fileExitItem.addSelectionListener(new SelectionListener() {
		public void widgetSelected(SelectionEvent event) {
			shell.close();
			display.dispose();
		}

		public void widgetDefaultSelected(SelectionEvent event) {
			shell.close();
			display.dispose();
		}
		});
	}
	
	/**
	 * Runner class
	 */
	class Runner implements Runnable {
		/**
		 * Run method
		 */
		public void run() {
			String msgFromOpponent;
			try {
				if (in.ready()) {
					try {
						msgFromOpponent = in.readLine();
						String[] list = msgFromOpponent.split("[:-]");
						if (msgFromOpponent.contains("MOVE")){

							int xBefore = Integer.parseInt(list[1]);
							int yBefore = Integer.parseInt(list[2]);
							int xAfter = Integer.parseInt(list[3]);
							int yAfter = Integer.parseInt(list[4]);
							
							String piecePromotion = "no";
							if (msgFromOpponent.contains("PROMOTION")) {
								//System.out.println("HELLO");
								piecePromotion = list[6]; }
							
							selectedPiece = boardUI.selectPiece(xBefore, yBefore, !whitesTurn);
							boardUI.validMoveMade(xAfter,yAfter,selectedPiece,whitesTurn); // necessary b/c pawn's validMove updates didEnPassant
							
							if (!piecePromotion.equals("no")) {
								boardUI.getTile(xBefore, yBefore).setPiece(null);
								
								if (piecePromotion.equals("QUEEN")) {
									boardUI.getTile(xAfter, yAfter).setPiece(new Queen(!whitesTurn, shell)); }
								else if (piecePromotion.equals("ROOK")) {
									boardUI.getTile(xAfter, yAfter).setPiece(new Rook(!whitesTurn, shell)); }
								else if (piecePromotion.equals("KNIGHT")) {
									boardUI.getTile(xAfter, yAfter).setPiece(new Knight(!whitesTurn, shell)); }
								else if (piecePromotion.equals("BISHOP")) {
									boardUI.getTile(xAfter, yAfter).setPiece(new Bishop(!whitesTurn, shell)); }
								
								gameOver = boardUI.updateBoard(xAfter,yAfter,boardUI.getTile(xAfter, yAfter).getPiece()); }
							else {
								gameOver = boardUI.updateBoard(xAfter,yAfter,selectedPiece); }
							
//							whitesTurn = !whitesTurn;
							yourTurn = !yourTurn;
							canvas.redraw();
							selectedPiece.SetNotSelected();
							selectedPiece = null;
							//System.out.println(msgFromOpponent);
						}else if(msgFromOpponent.contains("PLAYER")) {
							//takes in the time input by the opponent player
							opponent = list[3];
							opponentsPreferedTime = list[4];
							opponentsPreferedTime += (":" + list[5]);
							isOpponentConnected = true;}
					}
							
					 catch(IOException e) { client.close(); }
					
				}
			}
			catch(IOException ex) { System.out.println("The server did not respond (async)."); }				
            display.timerExec(0, this);
		}
	}
	
}