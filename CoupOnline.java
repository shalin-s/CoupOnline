import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.applet.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;

public class CoupOnline 
{
	
	private static final int startPortNum = 5000; //Can be changed
	
	private static final int UNKNOWN = 0;
	private static final int DUKE = 1;
	private static final int CAPTAIN = 2;
	private static final int AMBASSADOR = 3;
	private static final int ASSASSIN = 4;
	private static final int CONTESSA = 5;
	private static final int DEAD = -1;
	
	private static final int[] deckContents = {DUKE, DUKE, DUKE, CAPTAIN, CAPTAIN, CAPTAIN, AMBASSADOR, AMBASSADOR, AMBASSADOR, ASSASSIN, ASSASSIN, ASSASSIN, CONTESSA, CONTESSA, CONTESSA};
	
	private static final int ERROR = -999;
	private static final int REVEAL = 1001;
	private static final int HIDE = 1002;
	private static final int KILL = 1003;
	private static final int FAILED_CHALLENGE_EXCHANGE = 1004;
	private static final int TAKE_COIN = 1005;
	private static final int GIVE_COIN = 1006;
	private static final int CHAT = 2001;
	private static final int EVENT = 2002;
	
	private static final int AMBASSADOR_EXCHANGE = 1007;
	private static final int AMBASSADOR_CARD = 1008;
	private static final int KEEP = 1009;
	private static final int RETURN = 1010;
	
	
	private static boolean successWindowOn = false;
	private static boolean errorWindowOn = false;
	private static ObjectOutputStream[] outputs = new ObjectOutputStream[10]; //only if server
	private static ObjectInputStream[] inputs = new ObjectInputStream[10];
	
	private static ObjectOutputStream output; //only if client
	private static ObjectInputStream input;
	
	private static boolean connectionSetupDone = false;
	
	private static enum GameType {SERVER, CLIENT};
	private static GameType gameType = null;

	private static enum GameState {SETUP, ONGOING, OVER}
	private static GameState gameState = GameState.SETUP;
	
	//USEFUL DATA IF SERVER OR CLIENT
	private static final int MAX_PLAYERS = 6;
	private static final int MIN_PLAYERS = 1;
	private static int numPlayers = 0;
	private static String[] playerNames;
	private static String myName;
	private static int[] coins;
	
	//SERVER DATA	
	private static int[][] influences;
	private static LinkedList<Integer> deck = new LinkedList<Integer>();
	
	//CLIENT DATA
	private static String serverName;
	private static int myNumber;
	private static int[] myInfluences = new int[2];
	private static int[][] revealedInfluences;
	
	
	//GUI:
	
	private static JFrame gameTypeDeterminationWindow = new JFrame("Coup");
	private static JTextField nameTextField = new JTextField(14);
	
	private static JFrame connectionSetup = new JFrame("Coup");
	private static JLabel connectionSetupPrompt = new JLabel("Please enter IP address and port number of server.");
	private static JTextField IPAddressTextField = new JTextField(15);
	private static JTextField portTextField = new JTextField(6);
	private static JLabel connectionSetupError = new JLabel(" ");
	private static boolean connectionSetupInitialized = false;
	
	private static JFrame notificationWindow = new JFrame("Coup");
	private static boolean notificationWindowInitialized = false;
	private static JLabel playerListLabel = new JLabel();
	
	//MAIN WINDOW:
	
	private static JFrame window = new JFrame("Coup");
	private static JPanel playersPanel = new JPanel();
	private static JPanel myPanel = new JPanel();
	private static JPanel mainPanel = new JPanel();
	private static JPanel topPanel = new JPanel();
	private static JPanel coinPanel = new JPanel();
	private static JPanel ambassadorPanel = new JPanel();
	private static PlayerPanel[] playerPanels;
	private static JLabel[] influenceLabels = new JLabel[2];
	private static JPanel[] influencePanels;
	private static JPanel[] influenceActionPanels;
	private static JButton[] revealButtons;
	private static JButton[] hideButtons;
	private static JButton[] exchangeButtons;
	private static JButton[] killButtons;
	
	private static JButton ambassadorExchangeButton;
	private static int ambassadorCardsChosen = 0;
	private static int[] newCards = new int[2];
	
	//CHAT:
	
	private static JPanel chat = new JPanel();
	private static JTextField chatToSend;
	private static JTextArea chatDisplay = new JTextArea(0, 51);
	private static JScrollPane chatScrollPane = new JScrollPane(chatDisplay);

	private static String idToString (int id)
	{
		if (id == DUKE) return "DUKE";
		else if (id == CAPTAIN) return "CAPTAIN";
		else if (id == AMBASSADOR) return "AMBASSADOR";
		else if (id == ASSASSIN) return "ASSASSIN";
		else if (id == CONTESSA) return "CONTESSA";
		else if (id == DEAD) return "DEAD";
		else  return "UNKNOWN";
	}
	
	private static Font nonButtonFont (int size)
	{
		return (new Font (Font.DIALOG, Font.BOLD, size));
	}
	
	private static void writeIntServer(int client, int i)
	{
		try
		{
			outputs[client].writeObject(new Integer (i));
		}
		
		catch (Exception e)
		{
			exitProgram (e);
		}
	}
	
	private static int readIntServer(int client)
	{
		
		try
		{
			int i = (Integer) inputs[client].readObject();
			return i;
		}
		
		catch (Exception e)
		{
			exitProgram (e);
		}
		
		return ERROR;
	}
	
	private static void writeIntClient(int i)
	{
		try
		{
			output.writeObject(new Integer (i));
		}
		
		catch (Exception e)
		{
			exitProgram (e);
		}
	}
	
	private static int readIntClient()
	{
		
		try
		{
			int i = (Integer) input.readObject();
			return i;
		}
		
		catch (Exception e) 
		{
			exitProgram (e);
		}
		
		return ERROR;
	}
	
	private static void sendEventLog (int playerNumber, String s) //ONLY FOR SERVER USE
	{
		for (int i = 0; i < numPlayers; i++)
		{
			writeIntServer(i, CHAT);
			try
			{
				outputs[i].writeObject("****" + playerNames[playerNumber] + " " + s + "****");
			}
			
			catch (Exception e)
			{
				exitProgram(e);
			}
		}
	}
	
	private static void exitProgram (Exception e)
	{	
		if (!errorWindowOn) 
		{
			e.printStackTrace();
			System.exit(0);
		}
		
		else if (gameState != GameState.OVER)
		{
			e.printStackTrace();
			
			JFrame errorWindow = new JFrame("Coup");
			errorWindow.setSize(500, 200);
			errorWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			errorWindow.setResizable(false);
			
			JPanel errorWindowTop = new JPanel();
			errorWindowTop.setLayout(new FlowLayout());
			JPanel errorWindowBottom = new JPanel();
			errorWindowBottom.setLayout(new FlowLayout());
			
			JLabel errorMessage = new JLabel();
			
			if (e instanceof SocketException)
			{
				errorMessage.setText("Fatal Error: Connection to other players broken. Exiting program.");
			}
			
			else
			{
				errorMessage.setText("Fatal Error. Exiting Program.");
			}
			
			JButton okButton = new JButton("OK");
			okButton.addActionListener (new ActionListener()
			{
				public void actionPerformed (ActionEvent ae)
				{
					System.exit(0);
				}
			});
			
			errorWindowTop.add(errorMessage);
			errorWindowBottom.add(okButton);
			
			errorWindow.add(errorWindowTop, BorderLayout.NORTH);
			errorWindow.add(errorWindowBottom, BorderLayout.SOUTH);
			
			if (window != null)
			{
				window.setVisible(false);
			}
			
			errorWindow.setVisible(true);
		}
		
		else
		{
			System.exit(0);
		}
	}
	
	private static void showSuccessWindow () //FOR DEBUG
	{
		if (!successWindowOn) return;
		JFrame successWindow = new JFrame ("Coup");
		successWindow.setLayout(new GridLayout (3, 1));
		if (gameType == GameType.SERVER) successWindow.add(new JLabel ("You are the server, " + myName + "."));
		else if (gameType == GameType.SERVER) successWindow.add(new JLabel ("Hooray! the connection to the server (" + playerNames [playerNames.length - 1] + ") worked, " + myName + "!"));
		successWindow.add(new JLabel ("Players: " + Arrays.toString(playerNames)));
		successWindow.add(new JLabel ("Identities: " + Arrays.toString(influences)));
		successWindow.add(new JLabel ("Close this window to end the program"));
		successWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		successWindow.setSize(600, 250);
		successWindow.setVisible(true);
	}
	
	private static void showGameTypeDetermination ()
	{
		gameTypeDeterminationWindow.setSize(300, 140);
		gameTypeDeterminationWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		gameTypeDeterminationWindow.setResizable(false);
		
		JLabel welcomeLabel = new JLabel("Welcome to Coup.");
		JPanel welcomePanel = new JPanel();
		welcomePanel.setLayout(new FlowLayout());
		
		JPanel gameTypeButtons = new JPanel();
		gameTypeButtons.setLayout(new FlowLayout());
		
		JButton serverButton = new JButton("Play as Server");
		serverButton.addActionListener(new ActionListener()
		{
			public void actionPerformed (ActionEvent e)
			{
				myName = nameTextField.getText().trim();
				gameType = GameType.SERVER;
				gameTypeDeterminationWindow.setVisible(false);
				initializeConnection();
			}
		});
		
		JButton clientButton = new JButton("Play as Client");
		clientButton.addActionListener(new ActionListener()
		{
			public void actionPerformed (ActionEvent e)
			{
				myName = nameTextField.getText().trim();
				gameType = GameType.CLIENT;
				gameTypeDeterminationWindow.setVisible(false);
				initializeConnection();
			}
		});

		welcomePanel.add(welcomeLabel);
		
		gameTypeButtons.add(serverButton);
		gameTypeButtons.add(clientButton);
		
		JPanel namePanel = new JPanel();
		namePanel.setLayout(new FlowLayout());
		namePanel.add(new JLabel ("Your Name: "));
		namePanel.add(nameTextField);
		
		
		gameTypeDeterminationWindow.add(welcomePanel, BorderLayout.NORTH);
		gameTypeDeterminationWindow.add(gameTypeButtons, BorderLayout.CENTER);
		gameTypeDeterminationWindow.add(namePanel, BorderLayout.SOUTH);

		gameTypeDeterminationWindow.setVisible(true);
	}
	
	private static void initializeConnection ()
	{	
		if (gameType == GameType.SERVER)
		{
			if (notificationWindowInitialized)
			{
				notificationWindow.setVisible(true);
				return;
			}
			
			String myAddress = "";
			try
			{
				myAddress = InetAddress.getLocalHost().getHostAddress();
			}
			catch (Exception e)
			{
				exitProgram(e);
			}
			
			notificationWindow.setSize(680, 250);
			notificationWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			notificationWindow.setResizable(false);
			notificationWindow.setLayout(new GridLayout(6,1));
			
			JLabel notification1 = new JLabel("Please inform clients to enter connection information as follows.");
			JLabel notification2 = new JLabel("Local IP address (for LAN play only, use external IP address instead for internet play): " + myAddress);
			JLabel notification3 = new JLabel("Port: " + startPortNum);
			JLabel notification4 = new JLabel("Press \"Done\" when you're ready to start the game.");
			
			notificationWindow.add(notification1);
			notificationWindow.add(notification2);
			notificationWindow.add(notification3);
			notificationWindow.add(notification4);
			
			JButton connectionSetupDoneButton = new JButton("Done");
			connectionSetupDoneButton.addActionListener(new ActionListener()
			{
				public void actionPerformed (ActionEvent e)
				{
					if (numPlayers < MIN_PLAYERS) return;
					
					connectionSetupDone = true;
					
					try
					{
						Socket socket = new Socket("localhost", startPortNum);
					}
					
					catch (Exception exception)
					{
						exitProgram (exception);
					}
					
					//notificationWindow.setVisible(false);
				}
			});
			
			playerListLabel.setText ("You are " + myName + ". Connected players: (none)");
			notificationWindow.add(playerListLabel);
			JPanel connectionSetupDonePanel = new JPanel();
			connectionSetupDonePanel.add (connectionSetupDoneButton);
			notificationWindow.add (connectionSetupDonePanel);

			notificationWindow.setVisible(true);
			notificationWindowInitialized = true;
			
			new WaiterForClientConnections().start();	
		}
		
		else if (gameType == GameType.CLIENT)
		{
			JButton backButton = new JButton("Back");
			backButton.addActionListener(new ActionListener()
			{
				public void actionPerformed (ActionEvent e)
				{
					connectionSetup.setVisible(false);
					notificationWindow.setVisible(false);
					gameType = null;
					showGameTypeDetermination();
				}
			});
			
			if (connectionSetupInitialized)
			{
				connectionSetup.setVisible(true);
				return;
			}
			
			connectionSetup.setSize(600, 250);
			connectionSetup.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			connectionSetup.setResizable(false);
			
			JPanel connectionSetupInput = new JPanel();
			connectionSetupInput.setLayout(new FlowLayout());
			
			JButton connectionSetupDoneButton = new JButton("Done");
			connectionSetupDoneButton.addActionListener(new ActionListener()
			{
				public void actionPerformed (ActionEvent e) //FIX THIS SOON
				{
					try
					{
						Socket socket = new Socket(IPAddressTextField.getText().trim(), Integer.parseInt(portTextField.getText().trim()));	
						input = new ObjectInputStream(socket.getInputStream());
						output = new ObjectOutputStream(socket.getOutputStream());
						
						serverName = (String) input.readObject();
						output.writeObject(myName); //make sure to initialize name soon
						
						String serverConfirmation = (String) input.readObject();
						System.out.println(serverConfirmation);
						
						//maybe have server verify it received the name
						connectionSetupError.setText("Connected to server. Waiting for other players.");
						new WaiterForGameStart().start();
						connectionSetupDoneButton.setEnabled (false);
						backButton.setEnabled (false);
					}
					
					catch (Exception exception)
					{
						connectionSetupError.setText("Error connecting to server. Please make sure the information you entered is correct and try again.");
						exception.printStackTrace();
						return;
					}
				}
			});
			
			connectionSetupInput.add(new JLabel("IP Address:"));
			connectionSetupInput.add(IPAddressTextField);
			connectionSetupInput.add(new JLabel("Port:"));
			connectionSetupInput.add(portTextField);
			connectionSetupInput.add(connectionSetupDoneButton);
			
			connectionSetup.add(connectionSetupPrompt, BorderLayout.NORTH);
			connectionSetup.add(connectionSetupInput, BorderLayout.CENTER);
			connectionSetup.add(connectionSetupError, BorderLayout.SOUTH);
			
			connectionSetup.setVisible(true);
			connectionSetupInitialized = true;
		}
	}
	
	private static class WaiterForGameStart extends Thread //FOR CLIENT
	{
		public void run ()
		{
			try
			{
				playerNames = (String[]) input.readObject();
				numPlayers = playerNames.length;
				myNumber = readIntClient();
				myInfluences[0] = readIntClient();
				myInfluences[1] = readIntClient();
				coins = new int[numPlayers];
				revealedInfluences = new int[numPlayers][2];
				for (int i = 0; i < coins.length; i++) coins[i] = 2;
				showSuccessWindow(); //DEBUG
				//connectionSetup.setVisible(false);
				startGame();
				//START GAME HERE
			}
			
			catch (Exception e)
			{
				exitProgram (e);
			}
		}
	}
	

	private static class WaiterForClientConnections extends Thread //FOR SERVER
	{
		private ArrayList <ObjectOutputStream> outs = new ArrayList<ObjectOutputStream>(10);
		private ArrayList <ObjectInputStream> ins = new ArrayList<ObjectInputStream>(10);
		private ArrayList <String> names = new ArrayList<String> (10);
		
		private class SP extends Thread
		{
			Socket s;
			public SP (Socket so)
			{
				s = so;
			}
			
			public void run ()
			{
				try
				{
					if (connectionSetupDone)
					{
						s.close(); //not sure about this one
						return;
					}
					/*
					if (numPlayers > MAX_PLAYERS)
					{
						s.close(); //not sure about this one
						return;
					}
					*/
					ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
					ObjectInputStream in = new ObjectInputStream(s.getInputStream());
					
					out.writeObject(myName);
					String n = (String) in.readObject();
					names.add(n);
					playerListLabel.setText ("You are " + myName + ". Connected players: " + names.toString());
					out.writeObject("I am " + myName + ". Your name has been received, " + n);
					outs.add(out);
					ins.add(in);
				}
				
				catch (Exception e)
				{
					exitProgram (e);
				}
			}
		}
		
		public void run ()
		{
			try
			{
				ServerSocket serverSocket = new ServerSocket(startPortNum);
				for (numPlayers = 0; !connectionSetupDone; numPlayers++)
				{
					Socket socket = serverSocket.accept();
					new SP(socket).start();
					
					if (connectionSetupDone)
					{
						break;
					}
				}
				
				outputs = outs.toArray(new ObjectOutputStream[outs.size()]); //make sure this works properly
				inputs = ins.toArray(new ObjectInputStream[ins.size()]);
				playerNames = new String[numPlayers];
				playerNames = names.toArray(playerNames);
				
				ArrayList<Integer> deckTemp = new ArrayList<Integer>(15);
				for (int i: deckContents) deckTemp.add(new Integer(i));
				while (!deckTemp.isEmpty())
				{
					int randomIndex = (int)(Math.random() * deckTemp.size());
					deck.add(deckTemp.remove(randomIndex));
				}
				
				influences = new int[numPlayers][2];
				
				for (int i = 0; i < numPlayers; i++)
				{
					influences[i][0] = deck.pop();
					influences[i][1] = deck.pop();
				}
				
				coins = new int[numPlayers];
				for (int i = 0; i < coins.length; i++) coins[i] = 2;
				
				for (int i = 0; i < outs.size(); i++)
				{
					
					outs.get(i).writeObject(playerNames); //make sure clients can receive this properly once all players are connected.
					outs.get(i).writeObject(new Integer(i));
					outs.get(i).writeObject(new Integer (influences[i][0]));
					outs.get(i).writeObject(new Integer (influences[i][1]));
				}
			}
			
			catch (Exception e)
			{
				exitProgram(e);
			}
			
			
			showSuccessWindow();
			startGame();
		}
	}
	
	private static class ServerListenerThread extends Thread
	{
		public final int clientNumber;
		
		public ServerListenerThread (int i)
		{
			clientNumber = i;
		}
		
		public void run()
		{
			ArrayList<Integer> choices = null;
			try
			{
				while (gameState != GameState.OVER)
				{
					int inputInteger = readIntServer(clientNumber); //initial message, then switch for appropriate follow-up info
					if (inputInteger == REVEAL)
					{
						int cardNumber = readIntServer(clientNumber);
						for (int i = 0; i < numPlayers; i++)
						{
							writeIntServer(i, REVEAL);
							writeIntServer(i, clientNumber);
							writeIntServer(i, cardNumber);
							writeIntServer(i, influences[clientNumber][cardNumber]);
						}
						
						sendEventLog (clientNumber, "has revealed card " + (cardNumber + 1) + ".");
					}
					
					else if (inputInteger == HIDE)
					{
						int cardNumber = readIntServer(clientNumber);
						for (int i = 0; i < numPlayers; i++)
						{
							writeIntServer(i, HIDE);
							writeIntServer(i, clientNumber);
							writeIntServer(i, cardNumber);
						}
						
						sendEventLog (clientNumber, "has hidden card " + (cardNumber + 1) + ".");
					}
					
					else if (inputInteger == TAKE_COIN)
					{					
						coins[clientNumber]++;
						for (int i = 0; i < numPlayers; i++)
						{
							writeIntServer(i, TAKE_COIN);
							writeIntServer(i, clientNumber);
						}
						
						sendEventLog (clientNumber, "has taken a coin.");
					}
					
					else if (inputInteger == GIVE_COIN)
					{
						coins[clientNumber]--;
						for (int i = 0; i < numPlayers; i++)
						{
							writeIntServer(i, GIVE_COIN);
							writeIntServer(i, clientNumber);
						}
						
						sendEventLog (clientNumber, "has returned a coin.");
					}
					
					else if (inputInteger == AMBASSADOR_EXCHANGE)
					{
						choices = new ArrayList<Integer>(4);
						//choices.add(influences[clientNumber][0]);
						//choices.add(influences[clientNumber][1]);
						choices.add(deck.pop());
						choices.add(deck.pop());
						
						writeIntServer(clientNumber, AMBASSADOR_EXCHANGE);
						for (Integer choice: choices)
						{
							writeIntServer(clientNumber, choice);
						}
						
						sendEventLog (clientNumber, "has started an ambassador exchange.");
						
						//influences[clientNumber][0] = readIntServer(clientNumber);
						//influences[clientNumber][1] = readIntServer(clientNumber);
						
						
					}
					
					else if (inputInteger == AMBASSADOR_CARD)
					{
						deck.addLast(readIntServer(clientNumber));
						deck.addLast(readIntServer(clientNumber));
						influences[clientNumber][0] = readIntServer(clientNumber);
						influences[clientNumber][1] = readIntServer(clientNumber);
						shuffleDeck();
						
						sendEventLog (clientNumber, "has finished an ambassador exchange.");
					}
					
					else if (inputInteger == FAILED_CHALLENGE_EXCHANGE)
					{
						int cardNumber = readIntServer(clientNumber);
						int newCard = deck.pop();
						writeIntServer(clientNumber, FAILED_CHALLENGE_EXCHANGE);
						writeIntServer(clientNumber, cardNumber);
						writeIntServer(clientNumber, newCard);
						int oldCard = influences[clientNumber][cardNumber];
						influences[clientNumber][cardNumber] = newCard;
						deck.addLast(oldCard);
						shuffleDeck();
						
						for (int i = 0; i < numPlayers; i++)
						{
							writeIntServer(i, HIDE);
							writeIntServer(i, clientNumber);
							writeIntServer(i, cardNumber);
						}
						
						sendEventLog (clientNumber, "has exchanged card " + (cardNumber + 1) + ".");
					}
					
					else if (inputInteger == KILL)
					{
						int cardNumber = readIntServer(clientNumber);
						deck.addLast(influences[clientNumber][cardNumber]);
						influences[clientNumber][cardNumber] = DEAD;
						shuffleDeck();
						
						for (int i = 0; i < numPlayers; i++)
						{
							writeIntServer(i, KILL);
							writeIntServer(i, clientNumber);
							writeIntServer(i, cardNumber);
						}
						
						sendEventLog (clientNumber, "has killed their card " + (cardNumber + 1) + ".");
					}
					
					else if (inputInteger == CHAT)
					{
						String chatMessage = (String)inputs[clientNumber].readObject();
						String editedMessage = playerNames[clientNumber].toUpperCase() + ": " + chatMessage;
						for (int i = 0; i < numPlayers; i++)
						{
							writeIntServer(i, CHAT);
							outputs[i].writeObject(editedMessage);
						}
					}
				}
			}
			
			catch (Exception e)
			{
				exitProgram (e);
			}
		}
		
		private class ServerReactionThread extends Thread
		{
			public void run()
			{
				try
				{
					
				}
				
				catch (Exception e)
				{
					exitProgram (e);
				}
			}
		
		}
	
	}
	
	private static void shuffleDeck ()
	{
		ArrayList<Integer> deckContents = new ArrayList<Integer>(15);
		for (int card: deck)
		{
			deckContents.add(card);
		}
		
		deck = new LinkedList<Integer>();
			
		while (!deckContents.isEmpty())
		{
			int randomIndex = (int) (Math.random() * deckContents.size()); //make sure this line works
			deck.push(deckContents.remove(randomIndex));
		}
	}
	
	private static class ClientListenerThread extends Thread
	{
		public void run()
		{
			try
			{
				while (gameState != GameState.OVER)
				{
					int inputInteger = readIntClient();
					if (inputInteger == REVEAL)
					{
						int player = readIntClient();
						int cardNumber = readIntClient();
						int identity = readIntClient();
						revealedInfluences[player][cardNumber] = identity;
						//new UpdateDisplayThread().start();
					}
					
					if (inputInteger == HIDE)
					{
						int player = readIntClient();
						int cardNumber = readIntClient();
						revealedInfluences[player][cardNumber] = UNKNOWN;
						//new UpdateDisplayThread().start();
					}
					
					if (inputInteger == KILL)
					{
						int player = readIntClient();
						int cardNumber = readIntClient();
						revealedInfluences[player][cardNumber] = DEAD;
						if (player == myNumber)
						{
							myInfluences[cardNumber] = DEAD;
						}
						//new UpdateDisplayThread().start();
					}
					
					else if (inputInteger == TAKE_COIN)
					{
						int player = readIntClient();
						coins[player]++;
					}
					
					else if (inputInteger == GIVE_COIN)
					{
						int player = readIntClient();
						coins[player]--;
					}
					
					else if (inputInteger == FAILED_CHALLENGE_EXCHANGE)
					{
						int cardNumber = readIntClient();
						int newCard = readIntClient();
						myInfluences[cardNumber] = newCard;
					}
					
					else if (inputInteger == AMBASSADOR_EXCHANGE)
					{
						ArrayList<Integer> choices = new ArrayList<Integer>(4);
						for (int i = 0; i < 2; i++)
						{
							if (revealedInfluences[myNumber][i] == UNKNOWN)
							{
								choices.add(myInfluences[i]);
							}
						}
						
						for (int i = 0; i < 2; i++)
						{
							int c = readIntClient();
							choices.add(new Integer(c));
						}
						
						showAmbassadorExchange (choices);
					}
					
					else if (inputInteger == CHAT)
					{
						String chatMessage = (String) input.readObject();
						addTextToChat(chatMessage);
						
						continue;
					}
					
					updateDisplay();

				}
			}
			
			catch (Exception e)
			{
				exitProgram (e);
			}
		}
	
	}
	
	private static void updateDisplay ()
	{
		for (int i = 0; i < 2; i++)
		{
			influenceLabels[i].setText(idToString(myInfluences[i]));
		}
		
		for (int i = 0; i < numPlayers; i++)
		{
			playerPanels[i].update();
		}
	}
	
	private static class UpdateDisplayThread extends Thread
	{
		public void run ()
		{
			updateDisplay();
		}
	}
	
	private static class PlayerPanel extends JPanel
	{
		private int playerNumber;
		
		private JLabel nameLabel;
		private JLabel coinLabel;
		private JPanel iPanels[] = new JPanel[2];
		private JLabel[] iLabels = new JLabel[2];
		
		public PlayerPanel (int pn)
		{
			playerNumber = pn;
			this.setLayout(new GridLayout (2, 2));
			this.setBackground(Color.LIGHT_GRAY);
			this.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
			
			nameLabel = new JLabel (playerNames[playerNumber]);
			coinLabel = new JLabel();
			
			nameLabel.setHorizontalAlignment(SwingConstants.CENTER);
			coinLabel.setHorizontalAlignment(SwingConstants.CENTER);
			
			nameLabel.setFont(nonButtonFont(14));
			coinLabel.setFont(nonButtonFont(14));
			
			
			for (int i = 0; i < 2; i++)
			{
				iLabels[i] = new JLabel();
				iPanels[i] = new JPanel();
				iPanels[i].add(iLabels[i]);
				
				iLabels[i].setHorizontalAlignment(SwingConstants.CENTER);	
				iLabels[i].setVerticalAlignment(SwingConstants.CENTER);
				
				iLabels[i].setFont(nonButtonFont(16));
				iPanels[i].setLayout(new GridLayout (1, 1));
			}
			
			
			update();
			
			this.add(nameLabel);
			this.add(coinLabel);
			this.add(iPanels[0]);
			this.add(iPanels[1]);
			
		}
		
		public void update()
		{
			coinLabel.setText("Coins: " + coins[playerNumber]);
			for (int i = 0; i < 2; i++)
			{
				iLabels[i].setText(idToString(revealedInfluences[playerNumber][i]));
				if (revealedInfluences[playerNumber][i] == UNKNOWN || revealedInfluences[playerNumber][i] == DEAD)
				{
					iPanels[i].setBackground (Color.LIGHT_GRAY);
					iPanels[i].setBorder (BorderFactory.createEmptyBorder());
				}
				else
				{
					iPanels[i].setBackground (Color.WHITE);
					iPanels[i].setBorder (BorderFactory.createLineBorder(Color.BLACK, 1));
				}
			}
		}
	}
	
	private static void showPostRevealButtons (int cardNumber)
	{
		influenceActionPanels[cardNumber].setVisible(false);
		influenceActionPanels[cardNumber].removeAll();
		influenceActionPanels[cardNumber].setLayout(new GridLayout (3, 1)); //change back to 3x1 if kill button is put back
		influenceActionPanels[cardNumber].add (hideButtons[cardNumber]);
		influenceActionPanels[cardNumber].add (exchangeButtons[cardNumber]);
		influenceActionPanels[cardNumber].add (killButtons[cardNumber]);
		influenceActionPanels[cardNumber].setVisible(true);
	}
	
	private static void showRevealButton (int cardNumber)
	{
		influenceActionPanels[cardNumber].setVisible(false);
		influenceActionPanels[cardNumber].removeAll();
		influenceActionPanels[cardNumber].setLayout(new GridLayout (1, 1));
		influenceActionPanels[cardNumber].add (revealButtons[cardNumber]);
		influenceActionPanels[cardNumber].setVisible(true);
	}
	
	private static void showAmbassadorExchange (ArrayList<Integer> choicesList)
	{
		if (choicesList.size() > 4) return;
		int[] choices = new int[choicesList.size()];
		for (int i = 0; i < choicesList.size(); i++)
		{
			choices[i] = Integer.valueOf(choicesList.get(i));
		}
		final int numChoices = choices.length;
		
		ambassadorPanel.setVisible(false);
		ambassadorPanel.removeAll();
		ambassadorPanel.setLayout (new GridLayout(2, 2));
		ambassadorCardsChosen = 0;
		newCards[0] = UNKNOWN;
		newCards[1] = UNKNOWN;
		
		for (int i = 0; i < choices.length; i++)
		{
			final int iFinal = i;
			JButton optionButton = new JButton ();
			optionButton.setText(idToString(choices[i]));
			optionButton.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					if (numChoices <= 3)
					{
						if (revealedInfluences[myNumber][0] == UNKNOWN)
						{
							myInfluences[0] = choices[iFinal];
						}
						
						else
						{
							myInfluences[1] = choices[iFinal];
						}
						
						choicesList.remove(new Integer(choices[iFinal]));
						
						writeIntClient(AMBASSADOR_CARD);
						for (Integer i: choicesList) writeIntClient(i); //always 2 cards
						writeIntClient(myInfluences[0]);
						writeIntClient(myInfluences[1]);
						
						updateDisplay();
						showAmbassadorButton();
						setRevealButtonsEnabled(true);
					}
					
					if (numChoices >= 4)
					{
						newCards[ambassadorCardsChosen] = choices[iFinal];
						ambassadorCardsChosen++;
						choicesList.remove(new Integer(choices[iFinal])); //maybe change to object typecast
						if (ambassadorCardsChosen >= 2)
						{
							myInfluences[0] = newCards[0];
							myInfluences[1] = newCards[1];
							writeIntClient(AMBASSADOR_CARD);
							for (Integer i: choicesList) writeIntClient(i); //always 2 cards
							writeIntClient(myInfluences[0]);
							writeIntClient(myInfluences[1]);
							
							updateDisplay();
							showAmbassadorButton();
							setRevealButtonsEnabled(true);
						}
						optionButton.setEnabled(false);
					}
				}
			});
			ambassadorPanel.add(optionButton);
		}
		
		setRevealButtonsEnabled(false);
		ambassadorPanel.setVisible(true);
	}
	
	private static void setRevealButtonsEnabled(boolean enabled)
	{
		for (int i = 0; i < 2; i++)
		{
			revealButtons[i].setEnabled(enabled);
			hideButtons[i].setEnabled(enabled);
			exchangeButtons[i].setEnabled(enabled);
			killButtons[i].setEnabled(enabled);
		}
	}
	
	private static void showAmbassadorButton ()
	{
		ambassadorPanel.setVisible(false);
		ambassadorPanel.removeAll();
		ambassadorPanel.setLayout (new GridLayout(1, 1));
		ambassadorPanel.add (ambassadorExchangeButton);
		ambassadorPanel.setVisible(true);
	}
	
	private static void startGame () //launch game
	{
		connectionSetup.setVisible(false);
		connectionSetup.dispose();
		notificationWindow.setVisible(false);
		notificationWindow.dispose();
		
		try
		{

			if (gameType == GameType.SERVER)
			{
				window.setSize(500, 200);
				window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				window.setResizable(false);
				window.add(new JLabel ("You are the server, " + myName + "."));
				window.add(new JLabel ("Players: " + Arrays.toString(playerNames)));
				window.add(new JLabel ("Close this window to end the program"));
				
				window.setVisible(true);
				for (int i = 0; i < numPlayers; i++)
				{
					new ServerListenerThread(i).start();
				}
				return;
				
			}
			
			window.setSize(960, 740);
			window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			window.setResizable(true);
			
			
			myPanel = new JPanel();
			playersPanel = new JPanel();
			
			//MYPANEL STUFF
			myPanel.setLayout(new GridLayout (3, 2));
			influencePanels = new JPanel[2];
			influenceActionPanels = new JPanel[2];
			revealButtons = new JButton[2];
			exchangeButtons = new JButton[2];
			hideButtons = new JButton[2];
			killButtons = new JButton[2];
			
			for (int i = 0; i < 2; i++)
			{
				final int iFinal = i;
				
				influencePanels[i] = new JPanel();
				influenceActionPanels[i] = new JPanel();
				
				influenceLabels[i] = new JLabel(idToString (myInfluences[i]));
				influenceLabels[i].setHorizontalAlignment (SwingConstants.CENTER);
				influenceLabels[i].setVerticalAlignment(SwingConstants.CENTER);
				influenceLabels[i].setFont(nonButtonFont(16));
				influencePanels[i].setLayout(new GridLayout (1, 1));
				influencePanels[i].setBackground(Color.WHITE);
				influencePanels[i].setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
				influencePanels[i].add(influenceLabels[i]);
				
				revealButtons[i] = new JButton();
				revealButtons[i].setText("Reveal " + (i + 1));
				revealButtons[i].addActionListener(new ActionListener()
				{
					public void actionPerformed (ActionEvent e)
					{
						writeIntClient(REVEAL);
						writeIntClient(iFinal);
						showPostRevealButtons(iFinal);						
					}
				});
				
				//influenceActionPanels[i].add(revealButtons[i]);
				
				hideButtons[i] = new JButton();
				hideButtons[i].addActionListener(new ActionListener()
				{
					public void actionPerformed (ActionEvent e)
					{
						if (revealedInfluences[myNumber][iFinal] == UNKNOWN) return;
						writeIntClient(HIDE);
						writeIntClient(iFinal);
						showRevealButton(iFinal);
						//reading new card given from server is taken care of in client listener thread
					}
				});
				hideButtons[i].setText("Hide " + (i + 1));
				
				exchangeButtons[i] = new JButton();
				exchangeButtons[i].addActionListener(new ActionListener()
				{
					public void actionPerformed (ActionEvent e)
					{
						if (revealedInfluences[myNumber][iFinal] == UNKNOWN) return;
						writeIntClient(FAILED_CHALLENGE_EXCHANGE);
						writeIntClient(iFinal);
						showRevealButton(iFinal);
						//reading new card given from server is taken care of in client listener thread
					}
				});
				exchangeButtons[i].setText("Exchange " + (i + 1));
				
				killButtons[i] = new JButton();
				killButtons[i].addActionListener(new ActionListener()
				{
					public void actionPerformed (ActionEvent e)
					{
						if (revealedInfluences[myNumber][iFinal] == UNKNOWN) return;
						writeIntClient(KILL);
						writeIntClient(iFinal);
						influenceActionPanels[iFinal].setVisible(false);
						//reading new card given from server is taken care of in client listener thread
					}
				});
				killButtons[i].setText("Kill " + (i + 1));
		
				showRevealButton(i);
			}
			
			ambassadorExchangeButton = new JButton ("Ambassador Exchange");
			ambassadorExchangeButton.addActionListener(new ActionListener()
			{
				public void actionPerformed (ActionEvent e)
				{
					writeIntClient(AMBASSADOR_EXCHANGE);
				}
			});
			
			showAmbassadorButton();
			
			coinPanel.setLayout(new GridLayout (2, 1));
			
			
			JButton takeCoinButton = new JButton("Take Coin");
			takeCoinButton.addActionListener(new ActionListener()
			{
				public void actionPerformed (ActionEvent e)
				{
					writeIntClient(TAKE_COIN);
				}
			});
			
			JButton giveCoinButton = new JButton("Give Coin");
			giveCoinButton.addActionListener(new ActionListener()
			{
				public void actionPerformed (ActionEvent e)
				{
					if (coins[myNumber] <= 0) return;
					writeIntClient(GIVE_COIN);
				}
			});
			
			coinPanel.add(takeCoinButton);
			coinPanel.add(giveCoinButton);
			
			
			myPanel.add(influencePanels[0]);
			myPanel.add(influencePanels[1]);
			myPanel.add(influenceActionPanels[0]);
			myPanel.add(influenceActionPanels[1]);
			myPanel.add(ambassadorPanel);
			myPanel.add(coinPanel);
			
			//PLAYERSPANEL STUFF
			playersPanel.setLayout(new GridLayout (numPlayers, 1)); //maybe change the confusing naming
			playerPanels = new PlayerPanel[numPlayers];
			for (int i = 0; i < playerPanels.length; i++)
			{
				playerPanels[i] = new PlayerPanel(i);
				playersPanel.add(playerPanels[i]);
			}
			
			
					
			if (gameType == GameType.CLIENT)
			{
				new ClientListenerThread().start();
			}
			
			mainPanel.setLayout (new GridLayout (1, 2));
			mainPanel.add(myPanel);
			mainPanel.add(playersPanel);			
			window.add(mainPanel, BorderLayout.CENTER);
			
			JLabel nameLabel = new JLabel("You are: " + myName);
			nameLabel.setFont(nonButtonFont(12));
			nameLabel.setHorizontalAlignment (SwingConstants.CENTER);
			window.add(nameLabel, BorderLayout.NORTH);
			
			initializeChat();
			window.add(chat, BorderLayout.SOUTH);
			
			window.setVisible (true);
			
		}
		
		catch (Exception e)
		{
			exitProgram (e);
		}
	}
	
	//Adds text to the text chat box, and then scrolls down to the bottom if the scroll bar was originally at the bottom.
	private static void addTextToChat (String text) 
	{
		boolean autoscroll = false;
		if (chatScrollPane.getVerticalScrollBar().getValue() + chatScrollPane.getVerticalScrollBar().getModel().getExtent() == chatScrollPane.getVerticalScrollBar().getMaximum())
		{
			autoscroll = true;
		}
		
		chatDisplay.setRows(chatDisplay.getRows() + 1);
		chatDisplay.setText(chatDisplay.getText() + text + "\n");
		
		if (autoscroll)
		{
			chatDisplay.setCaretPosition(chatDisplay.getText().length());
		}	
	}
	
	private static void initializeChat ()
	{
		chatToSend = new JTextField(50);
		JButton sendButton = new JButton("Send");
		ActionListener messageSender = new ActionListener()
		{
			public void actionPerformed (ActionEvent e)
			{
				try
				{
					if (chatToSend.getText().equals("")) return;
					writeIntClient(CHAT);
					output.writeObject(chatToSend.getText());
				}
				
				catch (Exception exception)
				{
					exitProgram(exception);
				}
				
				chatToSend.setText("");
			}
		};
		sendButton.addActionListener(messageSender);
		chatToSend.addActionListener(messageSender);
		
		JPanel chatControls = new JPanel();
		chatControls.setLayout(new FlowLayout());
		chatControls.add(chatToSend);
		chatControls.add(sendButton);
		
		chatDisplay.setEditable(false);
		DefaultCaret caret = (DefaultCaret) chatDisplay.getCaret();
		caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
		
		chat.setLayout(new BorderLayout());
		chatScrollPane.setPreferredSize(new Dimension(window.getWidth(), 100));
		JPanel chatTopPanel = new JPanel();
		chatTopPanel.setLayout(new FlowLayout());
		JLabel chatLabel = new JLabel("Text Chat:");
		chatLabel.setFont(nonButtonFont(12));
		chatTopPanel.add(chatLabel);
		chat.add(chatTopPanel, BorderLayout.NORTH);
		chat.add(chatScrollPane);
		chat.add(chatControls, BorderLayout.SOUTH);
	}
	
	//for quick text entry (useful for debugging)
	private static void autoSetup () 
	{
		int number = (int)(Math.random() * 1000);
		nameTextField.setText("P" + number);
		IPAddressTextField.setText("localhost");
		portTextField.setText("" + startPortNum);
	}
	
	public static void main (String[] args)
	{
		autoSetup();
		showGameTypeDetermination();
	}
}
