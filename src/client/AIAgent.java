package client;

import client.model.Answer;

public interface AIAgent {
    Answer turn(World world);
}
