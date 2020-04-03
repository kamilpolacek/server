import java.net.*;
import java.io.*;
import java.util.*;
import java.lang.*;

enum Direction {
  UP, RIGHT, DOWN, LEFT
}

abstract class Communication {
  int posX,posY;
  int x,y;
  int i,move,right,left,twice;
  Direction direction;
  boolean finish;
  boolean directionInit;

  public Communication() {
    posX = posY = 9999;
    x = y = 0;
    i = move = right = left = twice = 0;
    finish = false;
    directionInit = false;
  }

}
/**************************************************************************************************************************************************************************/
class ServerRunnable extends Communication implements Runnable {
  Socket socket;
  BufferedReader inputStream;
  PrintWriter outputStream;
  String end;
  String inputText;
  int serverHashCode;
  int stage;
  int timeToDisconnect;

  public ServerRunnable(Socket socket) {
      this.socket = socket;
      end = String.valueOf((char)'\u0007') + String.valueOf((char)'\u0008');
      serverHashCode = 0;
      stage = 0;
      inputText = "";
      timeToDisconnect = 1000;

      try{
        inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        outputStream = new PrintWriter(socket.getOutputStream());
      } catch (IOException e) {
        e.printStackTrace();
      }
  }
/***********************************************************************/
  public void run() {
    setTimeout(950);

    while(true) {
      getInputFromClient();
      if(finish)
        break;

      switch(stage) {
        case 0:
          System.out.println("state 0: sending conforming code");
          chooseStringToSendByCode(nameToConfirmCode(inputText));
          stage++;
          break;
        case 1:
          System.out.println("state 1: comparing hash codes + 2 initial moves ");
          chooseStringToSendByCode(compareHashCodes(inputText));
          stage++;
          break;
        case 2:
          System.out.println("state 2: navigation towards left-upper corner of pickupArea(-2,2) ");
          chooseStringToSendByCode(navigateTowardsPickUpArea(inputText));
          break;
        case 3:
          System.out.println("state 3: searching pickupArea");
          chooseStringToSendByCode(searchPickUpArea(inputText));
      }
    }

  }
/***********************************************************************/
  public void chooseStringToSendByCode(int code) {
    switch(code) {
      case 0:
        break;
      case 102:
        System.out.println("MOVE");
        sendStringToClient("102 MOVE"+end);
        break;
      case 103:
        System.out.println("TURN LEFT");
        sendStringToClient("103 TURN LEFT"+end);
        break;
      case 104:
        System.out.println("TURN RIGHT");
        sendStringToClient("104 TURN RIGHT"+end);
        break;
      case 105:
        System.out.println("GET MESSAGE");
        sendStringToClient("105 GET MESSAGE"+end);
        break;
      case 106:
        System.out.println("LOGOUT");
        sendStringToClient("106 LOGOUT"+end);
        closeSockets();
        break;
      case 201:
        System.out.println("OK");
        sendStringToClient("200 OK"+end);
        MoveTwice();                                 // <<<<-----------------------------------------------------------------------
        break;
      case 300:
        System.out.println("LOGIN FAILED");
        sendStringToClient("300 LOGIN FAILED"+end);
        closeSockets();
        break;
      case 301:
        System.out.println("SYNTAX ERROR");
        sendStringToClient("301 SYNTAX ERROR"+end);
        closeSockets();
        break;
      case 302:
        System.out.println("LOGIC ERROR");
        sendStringToClient("302 LOGIC ERROR"+end);
        closeSockets();
        break;
      default:
        System.out.println("CONFIRM CODE");
        sendStringToClient(code+end);
    }

  }
/***********************************************************************/
  public void sendStringToClient(String toSend) {
      outputStream.write(toSend);
      outputStream.flush();
      inputText = "";
  }
/***********************************************************************/
  public void setTimeout(int x) {
    try {
      socket.setSoTimeout(x);
    } catch (SocketException e) {
      e.printStackTrace();
    }
  }
/***********************************************************************/
  public void waitForBatteryToCharge() {
    System.out.println("Waiting for battery to charge!");
    setTimeout(4800);
    inputText = "";
    getInputFromClient();
    if(inputText.compareTo("FULL POWER") == 0) {
      setTimeout(950);
      inputText = "";
      getInputFromClient();
    }
    else
      chooseStringToSendByCode(302);

  }
/***********************************************************************/
  public void PickUpMessageIfInArea() {
    if(posX <= 2 && posX >= -2 && posY <= 2 && posY >= -2) {
      chooseStringToSendByCode(105);
      getInputFromClient();

      if( !inputText.isEmpty() ) {
        finish = true;
        chooseStringToSendByCode(106);
      }
    }

  }
/***********************************************************************/
  public void closeSockets(){
    try {
      finish = true;
      outputStream.close();
      socket.close();
      inputStream.close();
    } catch(IOException e) {
        e.printStackTrace();
    }
  }
/***********************************************************************/
  public int nameToConfirmCode(String name) {
    int size = name.length();
    int confirmCode = 0;

    if(size > 10)
      return 301;

    for(int i=0; i<size; i++) {
      serverHashCode += ((int)(name.charAt(i)))*1000;
    }

    serverHashCode = serverHashCode % 65536;
    confirmCode = (serverHashCode + 54621) % 65536;

    return confirmCode;
  }
/***********************************************************************/
  public int compareHashCodes(String clientsConfirmCode) {
    int size = clientsConfirmCode.length();
    String withoutSpaces = clientsConfirmCode.replaceAll("\\D+","");
    int confirmCode = Integer.parseInt(withoutSpaces);

    if(size > 5 || withoutSpaces != clientsConfirmCode )
      return 301;

    if((serverHashCode+45328) % 65536 == confirmCode)
      return 201;
    else
      return 300;
  }
/***********************************************************************/
  public int checkIfMoved() {
    int size = 0;

    if( x == posX && y == posY ) {
      chooseStringToSendByCode(102);
      getInputFromClient();
      size = inputText.length();

      for(int i=3; i<size; i++) {
        if(inputText.charAt(i) == ' ') {
          try {
            x = Integer.parseInt(inputText.substring(3,i));
            y = Integer.parseInt(inputText.substring(i+1,size));
          } catch(NumberFormatException e) {
            return 301;
          }
          break;
        }
      }
      checkIfMoved();
    }
    return 0;
  }
/***********************************************************************/
  public int extractPositions(String location) {
    int size=0;
    size = inputText.length();

    if(size > 10)
      return 301;

    for(int i=3; i<size; i++) {
      if(location.charAt(i) == ' ') {
        try {
          x = Integer.parseInt(location.substring(3,i));
          y = Integer.parseInt(location.substring(i+1,size));
        } catch(NumberFormatException e) {
          return 301;
        }
        break;
      }
    }

    if(checkIfMoved() == 301)
      return 301;

    if( !directionInit && posX != 9999 && posY != 9999 ) {
      directionInit = true;
      if(posX - x < 0)
        direction = direction.RIGHT;
      else if(posX - x > 0)
        direction = direction.LEFT;
      else if(posY - y < 0 )
        direction = direction.UP;
      else if(posY - y > 0 )
        direction = direction.DOWN;
      }

    posX = x;
    posY = y;
    PickUpMessageIfInArea();
    return 0;

  }
/***********************************************************************/
  public void MoveTwice() {
    chooseStringToSendByCode(102);
    getInputFromClient();
    if(extractPositions(inputText) == 301) {
      chooseStringToSendByCode(301);
    }
    if(finish) {
      return;
    }
    chooseStringToSendByCode(102);

  }
/***********************************************************************/
  public int navigateTowardsPickUpArea(String location) {

    if( extractPositions(location) == 301 )
      return 301;
    if(finish)
      return 0;

    if(posX != -2) {
      System.out.println("posX neni -2!!");
      if(posX < -2) {
        if(direction == direction.RIGHT) {
          return 102;
        }
        getRightDirection(direction.RIGHT);
      }
      if(posX > -2) {
        if(direction == direction.LEFT) {
          return 102;
        }
        getRightDirection(direction.LEFT);
      }
      return 0;
    }
    else if(posY != 2) {
      System.out.println("posY neni 2!!");
      if(posY < 2) {
        if(direction == direction.UP) {
          return 102;
        }
        getRightDirection(direction.UP);
      }
      if(posY > 2) {
        if(direction == direction.DOWN) {
          return 102;
        }
        getRightDirection(direction.DOWN);
      }
      return 0;
    }

    if(direction == direction.RIGHT)
      return 102;
    if(direction != direction.RIGHT)
      getRightDirection(direction.RIGHT);

    stage++;

    return 0;
  }
/***********************************************************************/
  public int searchPickUpArea(String location) {
    if(extractPositions(location) == 301) {
      chooseStringToSendByCode(301);
      return 0;
    }

    for(;i<23;i++) {
      if(move == 3){
        if(twice == 2) {
          move = 0;
          twice = 0;
          if(left == 2)
            right = 0;
        }
        else if(right < 2) {
          right++;
          twice++;
          left = 0;
          return 104;
        }
        else if(left < 2) {
          left++;
          twice++;
          return 103;
        }
      }
      else {
        move++;
        return 102;
      }
    }

    if(i == 23)
      return 106;

    return 0;

  }
/***********************************************************************/
  public void getRightDirection(Direction dir) {
    int directionChange = 0;
    while( direction != dir ) {
      switch (direction)
        {
        case UP:
            direction = direction.LEFT;
            break;
        case RIGHT:
            direction = direction.UP;
            break;
        case DOWN:
            direction = direction.RIGHT;
            break;
        case LEFT:
            direction = direction.DOWN;
            break;
        }

      directionChange++;
      if(finish)
        break;
      chooseStringToSendByCode(103);
    }

    for(int i=0; i<directionChange; i++) {
      getInputFromClient();
      inputText="";
    }
    chooseStringToSendByCode(102);

  }
/***********************************************************************/
  public void getInputFromClient() {
    int charr;
    while( true ) {
      if(finish) {
        break;
      }

      try {
        try {
          charr = (char)inputStream.read();
          inputText += (char)charr;
        } catch (SocketTimeoutException e) {
          if(inputText.endsWith(String.valueOf((char)'\u0007')))
            chooseStringToSendByCode(301);
          finish = true;
          closeSockets();
          break;
        }
      } catch(IOException e) {
        e.printStackTrace();
      }

      if(inputText.endsWith(end)) {
        if(!inputText.isEmpty())
          inputText = inputText.substring(0,inputText.length()-2);
        if(inputText.compareTo("RECHARGING") == 0)
          waitForBatteryToCharge();
        break;
      }
    }
  }

}
/**************************************************************************************************************************************************************************/
class Server {
  ServerSocket serverSocket;

  public Server() {
    try {
      serverSocket = new ServerSocket(6666);
      while(true) {
        Socket socket = serverSocket.accept();
        new Thread(new ServerRunnable(socket)).start();
      }
    }
    catch(IOException e) {
      e.printStackTrace();
    }
  }
}
/**************************************************************************************************************************************************************************/
public class Robot {
  public static void main(String[] args) throws IOException {
      new Server();
  }

}
