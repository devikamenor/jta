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
import de.mud.jta.VisualPlugin;
import de.mud.jta.PluginBus;
import de.mud.jta.event.ConfigurationListener;
import de.mud.jta.event.SocketListener;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;

import java.util.Hashtable;
import java.util.Properties;

import java.net.URL;

import java.awt.Graphics;
import java.awt.Panel;
import java.awt.CardLayout;
import java.awt.List;
import java.awt.Component;
import java.awt.Menu;
import java.awt.Dimension;

/**
 * <P>
 * <B>Maintainer:</B> Matthias L. Jugel
 *
 * @version $Id$
 * @author Matthias L. Jugel, Marcus Mei�ner
 */
public class MudConnector extends Plugin implements VisualPlugin {

  /** debugging level */
  private final static int debug = 0;

  protected URL listURL = null;
  protected Hashtable mudList = null;
  protected List mudListSelector = new List();
  protected Panel mudListPanel;
  protected ProgressBar progress;

  class ProgressBar extends Component {
    int max, current;
    Dimension size = new Dimension(250, 20);

    public void setMax(int max) {
      this.max = max;
    }

    public void paint(Graphics g) {
      int width = (int) (((float)current/(float)max) * getSize().width);
      g.fill3DRect(0, 0, getSize().width, getSize().height, false);
      g.setColor(getBackground());
      g.fill3DRect(0, 0, width, getSize().height, true);
      g.setColor(getForeground());
      g.setXORMode(getBackground());
      g.drawString("Loading mud list: "+(current * 100 / (max>0?max:1))+"%", 
                   getSize().width/2 - 60, 14);
    }

    public void adjust(int value) {
      if((current = value) > max)
        current = max;
      repaint();
    }

    public void setSize(int width, int height) {
      size = new Dimension(width, height);
    }

    public Dimension getPreferredSize() {
      return size;
    }

    public Dimension getMinimumSize() {
      return size;
    }
  }
      

  /**
   * Create the list plugin and get the url to the actual list.
   */
  public MudConnector(PluginBus bus) {
    super(bus);
    bus.registerPluginListener(new ConfigurationListener() {
      public void setConfiguration(Properties config) {
        String url = config.getProperty("MudConnector.listURL");
        if(url != null) {
          try {
            listURL = new URL(url);
          } catch(Exception e) {
            System.err.println("MudConnector: "+e);
          } 
        } else
          System.err.println("MudConnector: no listURL specified");
      }
    });

    bus.registerPluginListener(new SocketListener() {
      public void connect(String host, int port) { setup(); }
      public void disconnect() { setup(); }
    });
    mudListPanel = new Panel(new java.awt.BorderLayout());
    mudListPanel.add("Center", progress = new ProgressBar());
  }

  private void setup() {
    if(mudList == null && listURL != null) try {
      mudList = new Hashtable();
      BufferedReader r = 
        new BufferedReader(new InputStreamReader(listURL.openStream()));

      String line = r.readLine();
      int mudCount = 0;
      try {
        mudCount = Integer.parseInt(line);
      } catch(NumberFormatException nfe) {
        System.err.println("MudConnector: number of muds: "+nfe);
      }
      System.out.println("MudConnector: expecting "+mudCount+" mud entries");
      progress.setMax(mudCount);

      StreamTokenizer ts = new StreamTokenizer(r);
      ts.resetSyntax();
      ts.whitespaceChars(0, 9);
      ts.ordinaryChars(32, 255);
      ts.wordChars(32, 255);

      String name, host;
      Integer port;
      int token, counter = 0;

      while((token = ts.nextToken()) != ts.TT_EOF) {
        name = ts.sval; 

        if((token = ts.nextToken()) != ts.TT_EOF) {
	  if(token == ts.TT_EOL)
	    System.err.println("MudConnector: "+name+": unexpected EOL for host");
          host = ts.sval;
          port = new Integer(23);
          if((token = ts.nextToken()) != ts.TT_EOF) try {
	    if(token == ts.TT_EOL)
	      System.err.println("MudConnector: "+name+": unexpected EOL for port");
            port = new Integer(ts.sval);
          } catch(NumberFormatException nfe) {
            System.err.println("MudConnector: port for "+name+": "+nfe);
          }

          if(debug > 0) 
            System.err.println("MudConnector: "+name+" ["+host+","+port+"]");
          mudList.put(name, new Object[] { host, port });
          mudListSelector.add(name);
        }
	while(token != ts.TT_EOF && token != ts.TT_EOL)
	  token = ts.nextToken();
	progress.adjust(++counter);
      }
    } catch(Exception e) {
      System.err.println("MudConnector: error: "+e);
    }
    System.out.println("MudConnector: found "+mudList.size()+" entries");
  }

  public Component getPluginVisual() {
    return mudListPanel;
  }

  public Menu getPluginMenu() {
    return null;
  }
}