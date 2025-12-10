package Servidor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class ServidorMain {

    public static void main(String[] args) {
        try (Scanner sc = new Scanner(System.in)) {
            System.out.println("=== Servidor JSON + JWT ===");
            System.out.print("Digite a porta TCP para o servidor (ex: 5000): ");
            int porta = Integer.parseInt(sc.nextLine().trim());

            try (ServerSocket serverSocket = new ServerSocket(porta)) {
                System.out.println("Servidor ouvindo na porta " + porta);
                while (true) {
                    Socket client = serverSocket.accept();
                    System.out.println("Conexão de: " + client.getRemoteSocketAddress());
                    Servidor handler = new Servidor(client);
                    new Thread(handler::run).start();
                }
            } catch (IOException e) {
                System.err.println("Erro ao criar ServerSocket: " + e.getMessage());
            }
        }
        
    }
}
