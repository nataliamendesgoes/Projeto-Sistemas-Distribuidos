package Cliente;

import java.util.Scanner;

public class ClienteMain {
    public static void main(String[] args) {
        try (Scanner sc = new Scanner(System.in)) {
            System.out.print("IP do servidor: ");
            String ip = sc.nextLine().trim();
            System.out.print("Porta do servidor: ");
            int porta = Integer.parseInt(sc.nextLine().trim());

            ClienteCore cliente = new ClienteCore(ip, porta);
            cliente.start();
        }
    }
}

