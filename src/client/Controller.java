package client;

import client.model.Answer;
import client.model.Game;
import client.model.dto.config.GameConfigMessage;
import client.model.dto.state.CurrentStateMessage;
import client.model.enums.Direction;
import com.google.gson.JsonObject;
import common.network.Json;
import common.network.data.Message;
import common.util.Log;

import java.util.function.Consumer;

/**
 * Main controller. Controls execution of the program, e.g. checks time limit of
 * the client, handles incoming messages, controls network operations, etc.
 * This is an internal implementation and you do not need to know anything about
 * this class.
 * Please do not change this class.
 */
public class Controller {
    // Logging tag
    private static final String TAG = "Controller";

    // File encoding for connection details
    private static String detailsEnc = "UTF-8";

    // Connection details
    private int port;
    private String host;
    private String token;
    private long retryDelay;

    // AI (participant's) class
    private AI ai;

    // Game model
    private Game game;

    // Client side network
    private client.Network network;

    // Terminator. Controller waits for this object to be notified. Then it will be terminated.
    private final Object terminator;

    private Consumer<Message> sender;

    /**
     * Constructor
     *
     * @param hostIP     host address
     * @param hostPort   host port
     * @param token      client token
     * @param retryDelay connection retry delay
     */
    public Controller(String hostIP, int hostPort, String token, long retryDelay) {
        this.terminator = new Object();
        this.host = hostIP;
        this.port = hostPort;
        this.token = token;
        this.retryDelay = retryDelay;
    }


    /**
     * Starts a client by connecting to the server and sending a token.
     */
    public void start() {
        try {
            network = new client.Network(this::handleMessage);
            sender = network::send;
            game = new Game();
            ai = new AI();

            network.setConnectionData(host, port, token);
            while (!network.isConnected()) {
                network.connect();
                Thread.sleep(retryDelay);
            }
            synchronized (terminator) {
                terminator.wait();
            }
            network.terminate();
        } catch (Exception e) {
            Log.e(TAG, "Can not start the client.", e);
            e.printStackTrace();
        }
    }

    /**
     * Handles incoming message. This method will be called from
     * {@link client.Network} when a new message is received.
     *
     * @param msg incoming message
     */
    private void handleMessage(Message msg) {
        Log.v(TAG, msg.type + " received.");
        switch (msg.type) {
            case "3":
                handleInitMessage(msg);
                break;
            case "4":
                handleTurnMessage(msg);
                break;
            default:
                Log.w(TAG, "Undefined message received: " + msg.type);
                break;
        }
        Log.v(TAG, msg.type + " handle finished.");
    }

    /**
     * Handles init message.
     *
     * @param msg init message
     */
    private void handleInitMessage(Message msg) {
        GameConfigMessage clientInitMessage = Json.GSON.fromJson(msg.getInfo(), GameConfigMessage.class);
        game.initGameConfig(clientInitMessage);
    }

    private void handleTurnMessage(Message msg) {
        Game newGame = new Game(game);
        CurrentStateMessage clientTurnMessage = Json.GSON.fromJson(msg.getInfo(), CurrentStateMessage.class);
        newGame.setCurrentState(clientTurnMessage);

        Message endMsg = new Message("6", new JsonObject());
        turn(newGame, endMsg);
    }

    private void turn(Game game, Message msg) {
        new Thread(() ->
        {
            try {
                sendResult(ai.turn(game));
            } catch (Exception e) {
                e.printStackTrace();
            }
            sendMessageToServer(msg);
        }).start();
    }

    private void sendResult(Answer answer) {
        chooseDirection(answer.getDirection());
        sendMessage(answer.getMessage(), answer.getMessageValue());
    }

    public void chooseDirection(Direction direction) {
        int directionNumber;
        if (direction == null)
            directionNumber = -1;
        else
            switch (direction) {
                case UP:
                    directionNumber = 2;
                    break;
                case DOWN:
                    directionNumber = 4;
                    break;
                case LEFT:
                    directionNumber = 3;
                    break;
                case RIGHT:
                    directionNumber = 1;
                    break;
                case CENTER:
                    directionNumber = 0;
                    break;
                default:
                    directionNumber = -1;
            }
        JsonObject answer = new JsonObject();
        answer.addProperty("direction", directionNumber);
        Message messageToSend = new Message("1", answer);
        sender.accept(messageToSend);
    }

    public void sendMessage(String message, int value) {
        if (message == null || message.length() > World.MAX_MESSAGE_LENGTH)
            return;
        JsonObject answer = new JsonObject();
        answer.addProperty("message", message);
        answer.addProperty("value", value);
        Message messageToSend = new Message("2", answer);
        sender.accept(messageToSend);
    }

    private void sendMessageToServer(Message message) {
        sender.accept(message);
    }

}