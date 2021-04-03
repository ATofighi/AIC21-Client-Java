package client;

import client.model.Answer;
import client.model.enums.Direction;

public class UpAgent implements AIAgent {
    @Override
    public Answer turn(World world) {
        return new Answer(Direction.UP);
    }
}
