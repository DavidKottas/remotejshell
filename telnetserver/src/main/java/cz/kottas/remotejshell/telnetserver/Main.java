package cz.kottas.remotejshell.telnetserver;

public class Main {
    public static void main(String[] args) throws Exception {
        new TcpServer(9876, null);
    }

}
