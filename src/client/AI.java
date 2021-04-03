package client;


import client.model.Answer;
import client.model.Ant;
import client.model.enums.AntType;
import client.model.enums.Direction;

/**
 * You must put your code in this class {@link AI}.
 * This class has {@link #turn}, to do orders while game is running;
 */

public class AI {
    /**
     * this method is for participants' code
     *
     * @param world is your data for the game (read the documentation on {@link client.World})
     * the return value is a {@link client.model.Answer} which consists of Direction for your
     * next destination in map (the necessary parameter), the Message (not necessary) for your
     * chat message and the value (if there is any message) for your message value.
     */
    static private AIAgent agent = null;

    public Answer turn(World world) {
        if (agent == null) {
            if (world.getAnt().getType() == AntType.KARGAR) {
                agent = new KargarBFSAgent(world);
            } else {
                agent = new UpAgent();
            }
        }
        return agent.turn(world);
    }
}