public class Agent {

    State state;

    void do_action()
    {
        state.Handle();
    }
}
