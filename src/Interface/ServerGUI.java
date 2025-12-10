package Interface;

import Servidor.Servidor;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerGUI extends JFrame {

    private JTextArea logArea;
    private JTextField txtPorta;
    private JButton btnIniciar;
    private boolean rodando = false;

    public static void main(String[] args) {
        // Configura o Look and Feel do sistema para ficar mais nativo/suave
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new ServerGUI().setVisible(true));
    }

    public ServerGUI() {
        setTitle("Servidor VoteFlix - Painel de Controle");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // Fundo geral escuro (Tema VoteFlix)
        getContentPane().setBackground(VoteFlixTheme.BG_DARK);
        setLayout(new BorderLayout(15, 15));
        
        // Adiciona uma margem interna geral
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(20, 20, 20, 20));

        // --- CABEÇALHO (Logo e Controles) ---
        JPanel panelTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
        panelTop.setOpaque(false); // Transparente para ver o fundo escuro

        JLabel lblLogo = new JLabel("SERVER LOGS");
        lblLogo.setFont(new Font("Arial", Font.BOLD, 28));
        lblLogo.setForeground(VoteFlixTheme.ORANGE_VOTEFLIX);
        
        JPanel panelConfig = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelConfig.setOpaque(false);
        
        JLabel lblPorta = new JLabel("Porta:");
        lblPorta.setFont(new Font("Arial", Font.BOLD, 14));
        lblPorta.setForeground(Color.WHITE);
        
        txtPorta = new JTextField("5000", 5);
        VoteFlixTheme.styleTextField(txtPorta); // Reutiliza o estilo do tema
        
        btnIniciar = new JButton("INICIAR SERVIÇO");
        VoteFlixTheme.stylePrimaryButton(btnIniciar);
        btnIniciar.setBackground(new Color(0, 150, 0)); // Verde para iniciar (diferente do padrão laranja)
        btnIniciar.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));

        panelConfig.add(lblPorta);
        panelConfig.add(txtPorta);
        panelConfig.add(btnIniciar);

        // Painel Norte que agrupa Logo e Config
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(lblLogo, BorderLayout.WEST);
        header.add(panelConfig, BorderLayout.EAST);
        
        add(header, BorderLayout.NORTH);

        // --- ÁREA DE LOG (Estilo "Papel" Limpo) ---
        logArea = new JTextArea();
        // Fundo Branco ou Cinza muito claro
        logArea.setBackground(new Color(245, 245, 245)); 
        // Texto Cinza Escuro (mais legível que preto puro)
        logArea.setForeground(new Color(50, 50, 50)); 
        // Fonte Monospaced moderna (Consolas, Menlo ou Monospaced padrão)
        logArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        logArea.setEditable(false);
        // Margem interna do texto
        logArea.setMargin(new Insets(10, 10, 10, 10)); 

        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(BorderFactory.createEmptyBorder()); // Remove borda dupla feia
        // Adiciona uma borda arredondada/sutil ao redor do scroll
        scroll.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100), 1));
        
        add(scroll, BorderLayout.CENTER);

        // --- RODAPÉ (Status) ---
        JLabel lblStatus = new JLabel("Status: Parado");
        lblStatus.setForeground(Color.GRAY);
        lblStatus.setFont(new Font("Arial", Font.ITALIC, 12));
        add(lblStatus, BorderLayout.SOUTH);

        // --- Lógica ---
        redirectSystemStreams();

        btnIniciar.addActionListener(e -> {
            iniciarServidor();
            lblStatus.setText("Status: Rodando na porta " + txtPorta.getText());
            lblStatus.setForeground(new Color(0, 200, 0)); // Verde
        });
    }

    private void iniciarServidor() {
        if (rodando) return;

        int porta;
        try {
            porta = Integer.parseInt(txtPorta.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Porta inválida.");
            return;
        }

        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(porta)) {
                rodando = true;
                System.out.println("--- SISTEMA VOTEFLIX INICIADO ---");
                System.out.println("Escutando na porta: " + porta);
                System.out.println("Aguardando conexões de clientes...");
                System.out.println("---------------------------------");
                
                SwingUtilities.invokeLater(() -> {
                    btnIniciar.setEnabled(false);
                    btnIniciar.setBackground(Color.GRAY);
                    btnIniciar.setText("RODANDO...");
                    txtPorta.setEditable(false);
                    txtPorta.setBackground(new Color(60, 60, 60)); // Escurece input desativado
                });

                while (true) {
                    Socket client = serverSocket.accept();
                    System.out.println(">> Nova conexão estabelecida: " + client.getInetAddress());
                    
                    Servidor handler = new Servidor(client);
                    new Thread(handler::run).start();
                }
            } catch (IOException e) {
                System.err.println("ERRO CRÍTICO NO SERVIDOR: " + e.getMessage());
            }
        }).start();
    }

    private void redirectSystemStreams() {
        OutputStream out = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                updateTextArea(String.valueOf((char) b));
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                updateTextArea(new String(b, off, len));
            }

            @Override
            public void write(byte[] b) throws IOException {
                write(b, 0, b.length);
            }
        };
        System.setOut(new PrintStream(out, true));
        System.setErr(new PrintStream(out, true));
    }

    private void updateTextArea(final String text) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(text);
            // Auto-scroll para o final
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
}