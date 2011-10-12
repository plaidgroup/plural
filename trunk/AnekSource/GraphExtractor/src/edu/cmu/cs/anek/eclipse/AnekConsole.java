package edu.cmu.cs.anek.eclipse;

import java.io.OutputStream;

import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.MessageConsole;

/**
 * A fancy console, so we can see the output of the inference tool
 * inside of Eclipse.
 * <br>
 * अनेक<br>
 * Anek<br>
 * @author Nels E. Beckman
 *
 */
public final class AnekConsole {

    // Initialized by Activator on plugin start
    private static MessageConsole console = null;
    
    /**
     * Create a new console
     */
    public static void init() {
        console = new MessageConsole("अनेक (Anek) Console", null); 
        ConsolePlugin.getDefault().getConsoleManager().addConsoles(
                new IConsole[]{ console });
    }
    
    /**
     * Removes the console...
     */
    public static void end() {
        ConsolePlugin.getDefault().getConsoleManager().removeConsoles(
                new IConsole[]{ console });
        console = null;
    }
    
    /**
     * Activates the console and returns a new input stream
     * on that console.
     * @return
     */
    public static OutputStream outputStream() {
        if( console == null )
            throw new IllegalStateException("Console not itialized");
        
        console.activate();
        return console.newMessageStream();
    }
    
}
