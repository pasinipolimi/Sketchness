//Singletone to access the gamebus from everywhere
package utils.gamebus;

/**
 *
 * @author Leyart
 */
public class GameBus {
    private static final MessageBus instance = new MessageBus();
 
    private GameBus() {}
 
    public static MessageBus getInstance() {
        return instance;
    }
}
