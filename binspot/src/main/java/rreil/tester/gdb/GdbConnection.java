package rreil.tester.gdb;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.Queue;

public class GdbConnection implements Runnable {
  private final InetAddress gdbAddress;
  private final int port;

  private Socket socket;
  private Thread worker;

  private final Queue<GdbPacket> messageQueue = new ArrayDeque<GdbPacket>();

  public GdbConnection (InetAddress gdbAddress, int port) {
    this.gdbAddress = gdbAddress;
    this.port = port;
  }

  public void connect () throws IOException {
    socket = new Socket(gdbAddress, port);
    worker = new Thread(this);
    worker.start();
  }

  @Override public void run () {
    try {
      InputStreamReader iSR = new InputStreamReader(socket.getInputStream());
      BufferedReader bR = new BufferedReader(iSR);

      StringBuilder text = new StringBuilder();
      while(true) {
        text.append((char)bR.read());

        while(true) {
          Tuple<Boolean, Tuple<Integer, Integer>> match = GdbPacket.match(text);
          if (match.x) {
            System.out.println("<== " + text);
            
            GdbPacket packet = new GdbPacket(text.substring(match.y.x + 1, match.y.y), text.substring(match.y.y + 1, match.y.y + 3));

            synchronized(messageQueue) {
              messageQueue.add(packet);
              messageQueue.notify();
            }

            text.delete(0, match.y.y + 3);
          } else
            break;
        }
      }
    } catch(IOException pleaseHandle) {
    }
  }

  public GdbPacket getNextPacket () throws InterruptedException {
    GdbPacket result;
    synchronized(messageQueue) {
      while(messageQueue.isEmpty())
        messageQueue.wait();
      result = messageQueue.poll();
    }
    return result;
  }
  
  public boolean acknowledgePacket(GdbPacket packet) throws IOException {
    boolean valid = packet.checksumValid();
    if(valid)
      send("+");
    else
      send("-");
    return valid;      
  }
  
  private void send(String data) throws IOException {
    System.out.println("==> " + data);
    
    OutputStreamWriter oSW = new OutputStreamWriter(socket.getOutputStream());
    BufferedWriter bW = new BufferedWriter(oSW);
    
    bW.write(data);
    bW.flush();
  }
  
  public void send(GdbPacket packet) throws IOException {
    send(packet.toString());
  }
}
