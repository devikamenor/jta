/*
 * This file is part of "The Java Telnet Application".
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * "The Java Telnet Application" is distributed in the hope that it will be 
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this software; see the file COPYING.  If not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

package de.mud.jta.plugin;

import de.mud.jta.Plugin;
import de.mud.jta.FilterPlugin;
import de.mud.jta.PluginBus;
import de.mud.jta.event.ConfigurationListener;

import java.io.IOException;

import java.util.Vector;
import java.util.Properties;

/**
 * The script plugin takes a series of match and answer pairs to compare
 * the incoming data with the matches and if it succeeds writes the answers
 * back. It then moves on to the next match until all script elements have
 * matched.<P>
 * The script property Script.script should contain <B>|</B> separated strings
 * where each two represent a match and answer pair. A newline will be appended
 * to each answer!<P>
 * The script is very basic but is a very good example how to
 * write a plugin for <B>The Java<SUP>tm</SUP> Telnet Application</B>.
 * <P>
 * <B>Maintainer:</B> Matthias L. Jugel
 *
 * @version $Id$
 * @author Matthias L. Jugel, Marcus Mei�ner
 */
public class Script extends Plugin implements FilterPlugin {

  /** debugging level */
  private final static int debug = 0;

  /**
   * Create a new scripting plugin.
   */
  public Script(PluginBus bus) {
    super(bus);
    bus.registerPluginListener(new ConfigurationListener() {
      public void setConfiguration(Properties config) {
        Vector script = new Vector();
        if(config.containsKey("Script.script")) {
	  String s = config.getProperty("Script.script");
	  // check if the script is stored in a file
	  if(s.charAt(0) == '@') {
	    System.out.println("Script.script: @file not implemented yet");
	  }
	  // parse the script and set up  
	  if(debug > 0) 
	    System.err.println("Script: "+s);
	  String pair[] = null;
          int old = -1, idx = s.indexOf('|');
          while(idx >= 0) {
	    if(pair == null) {
	      pair = new String[2];
              pair[0] = s.substring(old + 1, idx);
	      if(debug > 0) System.out.print("match("+pair[0]+") -> ");
	    } else {
              pair[1] = s.substring(old + 1, idx)+"\n";
	      if(debug > 0) System.out.print(pair[1]);
	      script.addElement(pair);
	      pair = null;
	    }
            old = idx;
            idx = s.indexOf('|', old + 1);
          }
          if(pair != null) {
	    pair[1] = s.substring(old + 1)+"\n";
	    script.addElement(pair);
	    if(debug > 0) System.out.print(pair[1]);
	  } else
	    System.err.println("Script: unmatched pairs of script elements");
	  // set up the script
	  setup(script);
        }
      }
    });
  }

  /** holds the data source for input and output */
  protected FilterPlugin source;

  /**
   * Set the filter source where we can read data from and where to
   * write the script answer to.
   * @param plugin the filter plugin we use as source
   */
  public void setFilterSource(FilterPlugin plugin) {
    source = plugin;
  }

  /**
   * Read an array of bytes from the back end and put it through the
   * script parser to see if it matches. It will send the answer 
   * immediately to the filter source if a match occurs.
   * @param b the array where to read the bytes in
   * @return the amount of bytes actually read
   */
  public int read(byte[] b) throws IOException {
    int n = source.read(b);
    if(n > 0) match(b, n);
    return n;
  }

  public void write(byte[] b) throws IOException {
    source.write(b);
  }

  // =================================================================
  // the actual scripting code follows:
  // =================================================================

  private int matchPos;		// current position in the match
  private Vector script;	// the actual script
  private byte[] match;		// the current bytes to look for
  private boolean done = true;	// no script!

  /**
   * Setup the parser using the passed script. The script contains for
   * every element a two-element array of String where element zero
   * contains the match and element one the answer.
   * @param script the script
   */
  private void setup(Vector script) {
    this.script = script;
    if(debug > 0) 
      System.err.println("Script: script contains "+script.size()+" elements");
    match = ((String[])this.script.firstElement())[0].getBytes();
    reset();
    done = false;
  }

  /**
   * Try to match the byte array s against the most current script match.
   * It will write the answer immediatly  if the script matches and
   * will return instantly when all the script work is done.
   * @param s the array of bytes to match against
   * @param length the amount of bytes in the array
   */
  private void match(byte[] s, int length) throws IOException {
    for(int i = 0; !done && i < length; i++) {
      if(s[i] == match[matchPos])
       // the whole thing matched so, return the match answer 
       // and reset to use the next match
       if(++matchPos >= match.length)
         write(found());
    }
  }

  /**
   * This method is called when a script match was found and will
   * setup the next match to be used and return the answer for the
   * just found one.
   * @return the answer to the found match
   */
  private byte[] found() {
    if(debug > 0) System.err.println("Script: found '"+new String(match)+"'");
    // we have matched the string, so remember the answer ...
    byte[] answer = ((String[])script.firstElement())[1].getBytes();
    // remove the matched element
    script.removeElementAt(0);
    // set the next match if applicable
    if(!script.isEmpty()) {
      match = ((String[])script.firstElement())[0].getBytes();
      reset();
    } else
      done = true;
    return answer;
  }

  /**
   * Reset the match position counter.
   */
  private void reset() {
    matchPos = 0;
  }
}