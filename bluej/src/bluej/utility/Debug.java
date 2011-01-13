/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011  Michael Kolling and John Rosenberg 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.utility;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import bluej.Config;

/**
 * Class to handle debugging messages.
 * 
 * @author Michael Kolling
 */
public class Debug
{
    private static final String eol = System.getProperty("line.separator");
    
    private static Writer debugStream = new Writer() {
        @Override
        public void write(char[] cbuf, int off, int len) throws IOException
        {
        }
        
        @Override
        public void flush() throws IOException
        {
        }
        
        @Override
        public void close() throws IOException
        {
        }
    };
    
    /**
     * Set the debug output stream. All debug messages go to the debug
     * output stream.
     */
    public static void setDebugStream(Writer debugStream)
    {
        Debug.debugStream = debugStream;
    }
    
    /**
     * Get the debug output stream.
     */
    public static Writer getDebugStream()
    {
        return debugStream;
    }
    
    /**
     * Write out a debug message. This may go to a terminal, or to
     * a debug file, depending on external debug settings.
     * 
     * @param msg The message to be written.
     */
    public static void message(String msg)
    {
        try {
            debugStream.write(msg);
            debugStream.write(eol);
            debugStream.flush();
        }
        catch (IOException ioe) {
            System.err.println("IOException writing debug log");
        }
    }
    
    /**
     * Write out a debug message to the debuglog file only.
     * 
     * @param msg The message to be written.
     */
    public static void log(String msg)
    {
        if (! Config.getPropString("bluej.debug").equals("true")) {
            message(msg);
        }
    }

    /**
     * Write out a BlueJ error message for debugging. Note, this does
     * not by itself provide a stack trace, so it's of only limited use -
     * it should be used only when what has gone wrong should be obvious.
     * 
     * <p>Use the variant which takes an exception as a parameter where
     * otherwise prudent.
     * 
     * @param error The error message.
     */
    public static void reportError(String error)
    {
        message("Internal error: " + error);
    }

    /**
     * Write out a BlueJ error message for debugging.
     * 
     * @param error The error message.
     */
    public static void reportError(String error, Throwable exc)
    {
        message("Internal error: " + error);
        message("Exception: " + exc);
        PrintWriter pwriter = new PrintWriter(debugStream);
        exc.printStackTrace(pwriter);
        pwriter.flush();
    }
    
    /**
     * Log an unexpected exception. Generally this should be used only if the
     * exception is probably harmless; otherwise, a message should also be
     * provided, stating what was being attempted when the exception occurred -
     * see {@link #reportError(String,Throwable)}.
     * 
     * @param error  The exception which occurred.
     */
    public static void reportError(Throwable error)
    {
        message("An unexpected exception occurred:");
        PrintWriter pwriter = new PrintWriter(debugStream);
        error.printStackTrace(pwriter);
        pwriter.flush();
    }
    
    /**
     * Log a stack trace with the given message.
     * @param msg  The message to precede the stack trace.
     */
    public static void printCallStack(String msg)
    {
        message(msg + "; call stack:");
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        // Miss out first line (getStackTrace()) and second line (printCallStack, us)
        // as that will always be the same and irrelevant:
        for (int i = 2; i < stack.length; i++) {
            message("  " + stack[i].toString());
        }
    }
}
