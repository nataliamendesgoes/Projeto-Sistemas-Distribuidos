package Interface;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ClientGUI extends JFrame {

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String token;

    private Gson gsonNetwork = new Gson(); 
    private Gson gsonDisplay = new GsonBuilder().setPrettyPrinting().create();


    private JPanel mainPanel;      
    private CardLayout cardLayout;
    

    private JPanel sidebarPanel;   
    private JPanel contentPanel;   
    private JPanel sidebarContent;

    private enum Tab { FILME, REVIEW, PERFIL }
    private Tab currentTab = Tab.FILME;

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}
        SwingUtilities.invokeLater(() -> new ClientGUI().setVisible(true));
    }

    public ClientGUI() {
        setTitle("VoteFlix");
        setSize(1100, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        getContentPane().setBackground(VoteFlixTheme.BG_DARK);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        
        mainPanel.add(createLoginScreen(), "LOGIN");
        mainPanel.add(createDashboardScreen(), "DASHBOARD");

        add(mainPanel);
        
        cardLayout.show(mainPanel, "LOGIN");
    }

    private JPanel createLoginScreen() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        JPanel header = new JPanel(new FlowLayout(FlowLayout.RIGHT, 30, 20));
        header.setBackground(Color.BLACK);
        header.setPreferredSize(new Dimension(1000, 100));
        
        JLabel lblLogo = new JLabel("<html><font color='#E65100'>VOTE</font><font color='white'>FLIX</font></html>");
        lblLogo.setFont(new Font("Arial", Font.BOLD, 40));
        header.add(lblLogo);
        
        panel.add(header, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new CardLayout());
        centerPanel.setOpaque(false);
        
        centerPanel.add(createFormConexao(), "CONEXAO");
        centerPanel.add(createFormLogin(), "LOGIN_FORM");
        
        panel.add(centerPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createFormConexao() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridy = 0; gbc.gridx = 0; gbc.gridwidth = 1;
        addLabelAndInput(form, "IP", gbc, 0, 0);
        ((JTextField)form.getComponent(1)).setText("127.0.0.1");
        
        gbc.gridx = 1;
        addLabelAndInput(form, "PORTA SERVIDOR", gbc, 0, 1);
        ((JTextField)form.getComponent(3)).setText("5000");
        
        JButton btnEntrar = new JButton("ENTRAR");
        VoteFlixTheme.stylePrimaryButton(btnEntrar);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; 
        gbc.insets = new Insets(40, 10, 10, 10);
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        btnEntrar.setPreferredSize(new Dimension(200, 50));
        
        btnEntrar.addActionListener(e -> {
            String ip = ((JTextField)form.getComponent(1)).getText();
            String porta = ((JTextField)form.getComponent(3)).getText();
            conectar(ip, porta);
            CardLayout cl = (CardLayout) form.getParent().getLayout();
            cl.show(form.getParent(), "LOGIN_FORM");
        });
        
        form.add(btnEntrar, gbc);
        return form;
    }
    
    private JPanel createFormLogin() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        JLabel lblTitle = new JLabel("LOGIN", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 50));
        lblTitle.setForeground(Color.BLACK);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1;
        gbc.insets = new Insets(0, 10, 30, 10);
        form.add(lblTitle, gbc);
        
        gbc.insets = new Insets(5, 10, 5, 10);
        JTextField txtUser = addLabelAndInput(form, "ID USUÁRIO", gbc, 1, 0);
        JPasswordField txtPass = addPassAndInput(form, "SENHA", gbc, 3, 0);
                
        JButton btnEntrar = new JButton("ENTRAR");
        VoteFlixTheme.stylePrimaryButton(btnEntrar);
        gbc.gridy = 6; gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.NONE; 
        btnEntrar.setPreferredSize(new Dimension(300, 50));
        
        btnEntrar.addActionListener(e -> fazerLogin(txtUser.getText(), new String(txtPass.getPassword())));
        form.add(btnEntrar, gbc);
        
        JButton btnCadastrar = new JButton("CADASTRAR");
        VoteFlixTheme.stylePrimaryButton(btnCadastrar);
        btnCadastrar.setBackground(Color.GRAY);
        btnCadastrar.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1, true));
        btnCadastrar.setPreferredSize(new Dimension(300, 50));
        gbc.gridy = 7;
        
        btnCadastrar.addActionListener(e -> fazerCadastro(txtUser.getText(), new String(txtPass.getPassword())));
        form.add(btnCadastrar, gbc);
        
        return form;
    }

    private JTextField addLabelAndInput(JPanel p, String text, GridBagConstraints gbc, int y, int x) {
        gbc.gridx = x; gbc.gridy = y;
        JLabel lbl = new JLabel(text);
        VoteFlixTheme.styleLabel(lbl); 
        lbl.setFont(new Font("Arial", Font.BOLD, 16));
        p.add(lbl, gbc);
        
        JTextField txt = new JTextField(20);
        VoteFlixTheme.styleTextField(txt);
        txt.setPreferredSize(new Dimension(250, 45));
        gbc.gridy = y + 1;
        p.add(txt, gbc);
        return txt;
    }
    
    private JPasswordField addPassAndInput(JPanel p, String text, GridBagConstraints gbc, int y, int x) {
        gbc.gridx = x; gbc.gridy = y;
        JLabel lbl = new JLabel(text);
        VoteFlixTheme.styleLabel(lbl);
        lbl.setFont(new Font("Arial", Font.BOLD, 16));
        p.add(lbl, gbc);
        
        JPasswordField txt = new JPasswordField(20);
        VoteFlixTheme.styleTextField(txt);
        txt.setPreferredSize(new Dimension(250, 45));
        gbc.gridy = y + 1;
        p.add(txt, gbc);
        return txt;
    }


    private JPanel createDashboardScreen() {
        JPanel dashboard = new JPanel(new BorderLayout());

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(Color.BLACK);
        topBar.setPreferredSize(new Dimension(1000, 80));
        topBar.setBorder(new EmptyBorder(0, 20, 0, 20));

        JButton btnBack = new JButton("←"); 
        btnBack.setFont(new Font("Arial", Font.BOLD, 40));
        btnBack.setForeground(Color.WHITE);
        btnBack.setContentAreaFilled(false);
        btnBack.setBorderPainted(false);
        btnBack.setFocusPainted(false);
        btnBack.addActionListener(e -> logout());
        
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 50, 25));
        navPanel.setOpaque(false);

        JButton btnNavFilme = createNavLink("FILME");
        btnNavFilme.addActionListener(e -> trocarAba(Tab.FILME));
        JButton btnNavReview = createNavLink("REVIEW");
        btnNavReview.addActionListener(e -> trocarAba(Tab.REVIEW));
        JButton btnNavPerfil = createNavLink("PERFIL");
        btnNavPerfil.addActionListener(e -> trocarAba(Tab.PERFIL));
        
        navPanel.add(btnNavFilme); navPanel.add(btnNavReview); navPanel.add(btnNavPerfil);

        JLabel lblLogo = new JLabel("<html><font color='#E65100'>VOTE</font><font color='white'>FLIX</font></html>");
        lblLogo.setFont(new Font("Arial", Font.BOLD, 32));

        topBar.add(btnBack, BorderLayout.WEST);
        topBar.add(navPanel, BorderLayout.CENTER);
        topBar.add(lblLogo, BorderLayout.EAST);

        sidebarPanel = new JPanel(new BorderLayout()); 
        sidebarPanel.setBackground(VoteFlixTheme.SIDEBAR_BG); 
        sidebarPanel.setPreferredSize(new Dimension(250, 0));
        
        sidebarContent = new JPanel();
        sidebarContent.setLayout(new BoxLayout(sidebarContent, BoxLayout.Y_AXIS));
        sidebarContent.setBackground(VoteFlixTheme.SIDEBAR_BG);
        
        JScrollPane scrollSidebar = new JScrollPane(sidebarContent);
        scrollSidebar.setBorder(null);
        scrollSidebar.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sidebarPanel.add(scrollSidebar, BorderLayout.CENTER);

        contentPanel = new JPanel(new CardLayout()); 
        contentPanel.setBackground(Color.WHITE);

        dashboard.add(topBar, BorderLayout.NORTH);
        dashboard.add(sidebarPanel, BorderLayout.WEST);
        dashboard.add(contentPanel, BorderLayout.CENTER);

        return dashboard;
    }

    private JButton createNavLink(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial", Font.BOLD, 20));
        btn.setForeground(Color.WHITE);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void trocarAba(Tab tab) {
        this.currentTab = tab;
        sidebarContent.removeAll();
        
        JLabel lblSection = new JLabel(tab.toString(), SwingConstants.CENTER);
        lblSection.setFont(new Font("Arial", Font.BOLD, 28));
        lblSection.setForeground(Color.WHITE);
        lblSection.setBorder(new EmptyBorder(20, 0, 20, 0)); 
        lblSection.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebarContent.add(lblSection);
        
        addSidebarSeparator();

        if (tab == Tab.FILME) {
            addSidebarBtn("Listar Filmes", () -> carregarListaFilmes());
            addSidebarBtn("Buscar Filme", () -> mostrarFormBuscarFilme());
            addSidebarBtn("Criar Filme (Admin)", () -> mostrarFormCriarFilme());
            addSidebarBtn("Editar Filme (Admin)", () -> mostrarFormEditarFilme());
            addSidebarBtn("Excluir Filme (Admin)", () -> mostrarFormExcluirFilme());

        } else if (tab == Tab.REVIEW) {
            addSidebarBtn("Minhas Reviews", () -> carregarMinhasReviews());
            addSidebarBtn("Criar Review", () -> mostrarFormCriarReview());
            addSidebarBtn("Editar Review", () -> mostrarFormEditarReview());
            addSidebarBtn("Apagar Review", () -> mostrarFormExcluirReview());
            addSidebarBtn("Apagar Review (Admin)", () -> mostrarFormExcluirReviewAdm());

        } else if (tab == Tab.PERFIL) {
            addSidebarBtn("Meus Dados", () -> carregarMeusDados());
            addSidebarBtn("Atualizar Cadastro", () -> mostrarFormAtualizarSenha());
            addSidebarBtn("Excluir Conta", () -> mostrarFormExcluirConta());
            addSidebarBtn("Logout", () -> logout());
            addSidebarBtn("Listar Usuários (Admin)", () -> carregarListaUsuarios());
            addSidebarBtn("Editar Usuário (Admin)", () -> mostrarFormEditarUserAdm());
            addSidebarBtn("Excluir Usuário (Admin)", () -> mostrarFormExcluirUserAdm());
        } 

        sidebarContent.revalidate();
        sidebarContent.repaint();
        
        contentPanel.removeAll();
        mostrarBemVindo(tab.toString()); 
        contentPanel.revalidate();
        contentPanel.repaint();
    }
    
    private void addSidebarSeparator() {
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(255,255,255,50));
        sep.setMaximumSize(new Dimension(200, 5));
        sidebarContent.add(sep);
        sidebarContent.add(Box.createVerticalStrut(10));
    }
    
    private void mostrarBemVindo(String area) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        JLabel l = new JLabel("Área: " + area);
        l.setFont(new Font("Arial", Font.BOLD, 40));
        l.setForeground(Color.GRAY);
        p.add(l);
        contentPanel.add(p);
    }

    private void addSidebarBtn(String text, Runnable action) {
        JButton btn = new JButton(text);
     
        btn.setBackground(VoteFlixTheme.SIDEBAR_BG); 
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Arial", Font.BOLD, 14));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false); 
        btn.setHorizontalAlignment(SwingConstants.LEFT); 
        btn.setMaximumSize(new Dimension(250, 50));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(10, 20, 10, 10)); 
        btn.setContentAreaFilled(true);
        btn.setOpaque(true);
        
        // Hover Effect simples
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) { 
                btn.setBackground(VoteFlixTheme.SIDEBAR_BG.brighter()); 
                btn.setBorder(BorderFactory.createMatteBorder(0, 5, 0, 0, Color.WHITE)); 
            }
            public void mouseExited(java.awt.event.MouseEvent evt) { 
                btn.setBackground(VoteFlixTheme.SIDEBAR_BG); 
                btn.setBorder(new EmptyBorder(10, 20, 10, 10));
            }
        });

        btn.addActionListener(e -> action.run());
        sidebarContent.add(btn);
        sidebarContent.add(Box.createVerticalStrut(5));
    }

   
    private void mostrarForm(String titulo, JComponent... campos) {
        contentPanel.removeAll();
        JPanel formPanel = new JPanel(null); 
        formPanel.setBackground(Color.WHITE);
        
        JLabel lblT = new JLabel(titulo);
        lblT.setFont(new Font("Arial", Font.BOLD, 24));
        lblT.setBounds(50, 20, 400, 30);
        formPanel.add(lblT);
        
        int y = 70;
        JButton btnAcao = null;

        for (int i = 0; i < campos.length; i++) {
            JComponent comp = campos[i];
            
            if (comp instanceof JLabel) {
                JLabel lbl = (JLabel) comp;
                lbl.setFont(new Font("Arial", Font.PLAIN, 18)); 
                lbl.setForeground(Color.BLACK);
                lbl.setBounds(50, y, 400, 25);
                formPanel.add(lbl);
                y += 25;
            } else if (comp instanceof JTextField) {
                JTextField txt = (JTextField) comp;
                txt.setBackground(new Color(230, 230, 230)); 
                txt.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
                txt.setFont(new Font("Arial", Font.PLAIN, 16));
                txt.setBounds(50, y, 400, 40); 
                formPanel.add(txt);
                y += 60; 
            } else if (comp instanceof JScrollPane) { 
                comp.setBounds(50, y, 400, 100);
                formPanel.add(comp);
                y += 120;
            } else if (comp instanceof JButton) {
                btnAcao = (JButton) comp;
                btnAcao.setBounds(500, 70, 200, 50); 
                formPanel.add(btnAcao);
            }
        }
        
        contentPanel.add(formPanel);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void mostrarFormCriarFilme() { 
        JTextField tTitulo = new JTextField(); JTextField tDiretor = new JTextField(); JTextField tAno = new JTextField(); JTextField tGenero = new JTextField(); JTextField tSinopse = new JTextField();
        JButton btn = new JButton("CRIAR"); VoteFlixTheme.stylePrimaryButton(btn);
        btn.addActionListener(e -> {
            JsonObject f = new JsonObject(); f.addProperty("titulo", tTitulo.getText()); f.addProperty("diretor", tDiretor.getText()); f.addProperty("ano", tAno.getText()); f.addProperty("sinopse", tSinopse.getText());
            JsonArray g = new JsonArray(); for(String gen : tGenero.getText().split(",")) g.add(gen.trim()); f.add("genero", g);
            JsonObject req = new JsonObject(); req.addProperty("operacao", "CRIAR_FILME"); req.addProperty("token", token); req.add("filme", f); enviarEResponder(req);
        });
        mostrarForm("CRIAR FILME", new JLabel("Titulo"), tTitulo, new JLabel("Diretor"), tDiretor, new JLabel("Ano"), tAno, new JLabel("Gênero (virgula)"), tGenero, new JLabel("Sinopse"), tSinopse, btn);
    }
    
    private void mostrarFormEditarFilme() { 
        
        JTextField tId = new JTextField();
        JTextField tTitulo = new JTextField();
        JTextField tDiretor = new JTextField();
        JTextField tAno = new JTextField();
        JTextField tGenero = new JTextField(); 
        JTextField tSinopse = new JTextField();

        JButton btn = new JButton("EDITAR"); 
        VoteFlixTheme.stylePrimaryButton(btn);
        
        btn.addActionListener(e -> {

            JsonObject f = new JsonObject(); 
            f.addProperty("id", tId.getText());
            f.addProperty("titulo", tTitulo.getText());
            f.addProperty("diretor", tDiretor.getText());
            f.addProperty("ano", tAno.getText());
            f.addProperty("sinopse", tSinopse.getText());
            
            JsonArray g = new JsonArray(); 
            if (!tGenero.getText().isEmpty()) {
                for(String gen : tGenero.getText().split(",")) {
                    g.add(gen.trim());
                }
            }
            f.add("genero", g);
            
            JsonObject req = new JsonObject(); 
            req.addProperty("operacao", "EDITAR_FILME"); 
            req.addProperty("token", token); 
            req.add("filme", f); 
            
            enviarEResponder(req);
        });

        mostrarForm("EDITAR FILME", 
            new JLabel("ID Filme (Obrigatório)"), tId, 
            new JLabel("Novo Título"), tTitulo, 
            new JLabel("Novo Diretor"), tDiretor,
            new JLabel("Novo Ano"), tAno,
            new JLabel("Novo Gênero (vírgula)"), tGenero,
            new JLabel("Nova Sinopse"), tSinopse,
            btn
        );
    }    
    private void mostrarFormExcluirFilme() { 
        JTextField id = new JTextField(); JButton btn = new JButton("EXCLUIR"); VoteFlixTheme.stylePrimaryButton(btn);
        btn.addActionListener(e -> {
            JsonObject req = new JsonObject(); req.addProperty("operacao", "EXCLUIR_FILME"); req.addProperty("token", token); req.addProperty("id", id.getText()); enviarEResponder(req);
        });
        mostrarForm("EXCLUIR FILME", new JLabel("ID do Filme"), id, btn);
    }
    
    private void mostrarFormEditarUserAdm() { 
        JTextField id = new JTextField(); JTextField senha = new JTextField(); JTextField func = new JTextField();
        JButton btn = new JButton("EDITAR"); VoteFlixTheme.stylePrimaryButton(btn);
        btn.addActionListener(e -> {
             JsonObject u = new JsonObject(); u.addProperty("senha", senha.getText());
             JsonObject req = new JsonObject(); req.addProperty("operacao", "ADMIN_EDITAR_USUARIO"); req.addProperty("token", token); req.addProperty("id", id.getText()); req.add("usuario", u); enviarEResponder(req);
        });
        mostrarForm("ATUALIZAR CADASTRO", new JLabel("ID Usuário"), id, new JLabel("Nova Senha"), senha, btn);
    }
    
    private void mostrarFormExcluirUserAdm() { 
        JTextField id = new JTextField(); JButton btn = new JButton("EXCLUIR"); VoteFlixTheme.stylePrimaryButton(btn);
        btn.addActionListener(e -> {
            JsonObject req = new JsonObject(); req.addProperty("operacao", "ADMIN_EXCLUIR_USUARIO"); req.addProperty("token", token); req.addProperty("id", id.getText()); enviarEResponder(req);
        });
        mostrarForm("EXCLUIR CADASTRO", new JLabel("ID do Usuário"), id, btn);
    }
    
    private void mostrarFormExcluirReviewAdm() { 
        JTextField id = new JTextField(); JButton btn = new JButton("EXCLUIR"); VoteFlixTheme.stylePrimaryButton(btn);
        btn.addActionListener(e -> {
            JsonObject req = new JsonObject(); req.addProperty("operacao", "EXCLUIR_REVIEW"); req.addProperty("token", token); req.addProperty("id", id.getText()); enviarEResponder(req);
        });
        mostrarForm("APAGAR RESENHAS", new JLabel("ID da Review"), id, btn);
    }

    private void mostrarFormCriarReview() { 
        JTextField idF = new JTextField(); JTextField nota = new JTextField(); JTextField titulo = new JTextField(); JTextArea desc = new JTextArea();
        JButton btn = new JButton("CRIAR"); VoteFlixTheme.stylePrimaryButton(btn);
        btn.addActionListener(e -> {
            String n = nota.getText().trim(); String d = desc.getText().trim();
            if(n.isEmpty() || d.isEmpty()) { JOptionPane.showMessageDialog(this, "Nota e Descrição obrigatórias"); return; }
            JsonObject r = new JsonObject(); r.addProperty("id_filme", idF.getText()); r.addProperty("nota", n); r.addProperty("titulo", titulo.getText()); r.addProperty("descricao", d);
            JsonObject req = new JsonObject(); req.addProperty("operacao", "CRIAR_REVIEW"); req.addProperty("token", token); req.add("review", r); enviarEResponder(req);
        });
        mostrarForm("CRIAR RESENHA", new JLabel("ID Filme"), idF, new JLabel("Nota (1-5)"), nota, new JLabel("Título"), titulo, new JLabel("Descrição (Max 250)"), new JScrollPane(desc), btn);
    }
    
    private void mostrarFormEditarReview() { 
        JTextField id = new JTextField(); 
        JTextField nota = new JTextField(); 
        JTextField titulo = new JTextField(); 
        JTextField desc = new JTextField();
        
        JButton btn = new JButton("EDITAR"); 
        VoteFlixTheme.stylePrimaryButton(btn);
        
        btn.addActionListener(e -> {
             JsonObject r = new JsonObject(); 
             r.addProperty("id", id.getText()); 
             r.addProperty("nota", nota.getText()); 
             r.addProperty("titulo", titulo.getText()); 
             r.addProperty("descricao", desc.getText());
             
             JsonObject req = new JsonObject(); 
             req.addProperty("operacao", "EDITAR_REVIEW"); 
             req.addProperty("token", token); 
             req.add("review", r); 
             
             enviarEResponder(req);
        });
        
        mostrarForm("EDITAR RESENHA", 
            new JLabel("ID Review"), id, 
            new JLabel("Novo Título"), titulo, 
            new JLabel("Nova Nota"), nota, 
            new JLabel("Nova Descrição"), desc, 
            btn
        );
    }
    
    private void mostrarFormExcluirReview() { 
        JTextField id = new JTextField(); JButton btn = new JButton("APAGAR"); VoteFlixTheme.stylePrimaryButton(btn);
        btn.addActionListener(e -> {
            JsonObject req = new JsonObject(); req.addProperty("operacao", "EXCLUIR_REVIEW"); req.addProperty("token", token); req.addProperty("id", id.getText()); enviarEResponder(req);
        });
        mostrarForm("APAGAR RESENHA", new JLabel("ID da Review"), id, btn);
    }
    
    private void mostrarFormBuscarFilme() { 
        JTextField tId = new JTextField(); JButton btn = new JButton("BUSCAR"); VoteFlixTheme.stylePrimaryButton(btn);
        btn.addActionListener(e -> {
            JsonObject req = new JsonObject(); req.addProperty("operacao", "BUSCAR_FILME_ID"); req.addProperty("token", token); req.addProperty("id_filme", tId.getText());
            JsonObject resp = enviarReceber(req);
            if(resp!=null && resp.get("status").getAsString().equals("200")) mostrarDetalhesFilme(resp); else mostrarErro(resp);
        });
        mostrarForm("BUSCAR RESENHAS DE UM FILME", new JLabel("ID do Filme"), tId, btn);
    }

    private void mostrarFormAtualizarSenha() { 
        JTextField senha = new JTextField(); JButton btn = new JButton("ATUALIZAR"); VoteFlixTheme.stylePrimaryButton(btn);
        btn.addActionListener(e -> {
            JsonObject u = new JsonObject(); u.addProperty("senha", senha.getText());
            JsonObject req = new JsonObject(); req.addProperty("operacao", "EDITAR_PROPRIO_USUARIO"); req.addProperty("token", token); req.add("usuario", u); enviarEResponder(req);
        });
        mostrarForm("ATUALIZAR CADASTRO", new JLabel("Nova Senha"), senha, btn);
    }
    
    private void mostrarFormExcluirConta() { 
        JButton btn = new JButton("EXCLUIR"); VoteFlixTheme.stylePrimaryButton(btn); btn.setBackground(new Color(200, 0, 0));
        btn.addActionListener(e -> {
            JsonObject req = new JsonObject(); req.addProperty("operacao", "EXCLUIR_PROPRIO_USUARIO"); req.addProperty("token", token);
            JsonObject resp = enviarReceber(req);
            if(resp!=null && resp.get("status").getAsString().equals("200")) logout(); else mostrarErro(resp);
        });
        mostrarForm("EXCLUIR MEU USUÁRIO", new JLabel("Tem certeza?"), new JLabel(""), btn);
    }

    private void mostrarDetalhesFilme(JsonObject dados) {
        contentPanel.removeAll();
        JPanel panel = new JPanel(); panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(220, 220, 220)); panel.setBorder(new EmptyBorder(30, 30, 30, 30));
        JsonObject f = dados.getAsJsonObject("filme");
        addTextRow(panel, "ID Filme: " + getStr(f, "id"), new Font("Arial", Font.PLAIN, 18));
        addTextRow(panel, "Titulo: " + getStr(f, "titulo"), new Font("Arial", Font.PLAIN, 18));
        addTextRow(panel, "Diretor: " + getStr(f, "diretor"), new Font("Arial", Font.PLAIN, 18));
        addTextRow(panel, "Ano: " + getStr(f, "ano"), new Font("Arial", Font.PLAIN, 18));
        String gen = "";
        if(f.has("genero")) {
             JsonArray arr = f.getAsJsonArray("genero");
             for(JsonElement e : arr) gen += e.getAsString() + ", ";
             if(gen.length()>2) gen = gen.substring(0, gen.length()-2);
        }
        addTextRow(panel, "Genero: " + gen, new Font("Arial", Font.PLAIN, 18));
        addTextRow(panel, "Nota: " + getStr(f, "nota"), new Font("Arial", Font.PLAIN, 18));
        addTextRow(panel, "Avaliações: " + getStr(f, "qtd_avaliacoes"), new Font("Arial", Font.PLAIN, 18));
        addTextRow(panel, "Sinopse: " + getStr(f, "sinopse"), new Font("Arial", Font.PLAIN, 18));
        panel.add(Box.createVerticalStrut(30));
        JLabel lblRev = new JLabel("Reviews"); lblRev.setFont(new Font("Arial", Font.BOLD, 30)); lblRev.setForeground(VoteFlixTheme.ORANGE_VOTEFLIX); panel.add(lblRev);
        if(dados.has("reviews")) {
            for(JsonElement r : dados.getAsJsonArray("reviews")) {
                JsonObject rev = r.getAsJsonObject(); panel.add(Box.createVerticalStrut(10));
                addTextRow(panel, "ID: " + getStr(rev, "id"), new Font("Arial", Font.PLAIN, 18));
                addTextRow(panel, "Nome: " + getStr(rev, "nome_usuario"), new Font("Arial", Font.PLAIN, 18));
                addTextRow(panel, "Nota: " + getStr(rev, "nota"), new Font("Arial", Font.PLAIN, 18));
                addTextRow(panel, "Titulo: " + getStr(rev, "titulo"), new Font("Arial", Font.PLAIN, 18));
                addTextRow(panel, "Descrição: " + getStr(rev, "descricao"), new Font("Arial", Font.PLAIN, 18));
                addTextRow(panel, "Data: " + getStr(rev, "data"), new Font("Arial", Font.PLAIN, 18));
                addTextRow(panel, "Editado: " + getStr(rev, "editado"), new Font("Arial", Font.PLAIN, 18));
                panel.add(Box.createVerticalStrut(10));
            }
        }
        contentPanel.add(new JScrollPane(panel)); contentPanel.revalidate(); contentPanel.repaint();
    }
    private void addTextRow(JPanel p, String text, Font f) {
        JLabel l = new JLabel("<html><body style='width: 500px'>" + text + "</body></html>");
        l.setFont(f); l.setForeground(Color.BLACK); l.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(l); p.add(Box.createVerticalStrut(5));
    }

    private void mostrarListaCartoes(JsonArray array, String tipo) {
        contentPanel.removeAll();
        JPanel listPanel = new JPanel(new GridLayout(0, 2, 20, 20)); listPanel.setBackground(Color.WHITE); listPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        if (array != null) for (JsonElement el : array) listPanel.add(criarCartao(el.getAsJsonObject(), tipo));
        contentPanel.add(new JScrollPane(listPanel)); contentPanel.revalidate(); contentPanel.repaint();
    }
    private JPanel criarCartao(JsonObject obj, String tipo) {
        JPanel card = new JPanel(); card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(VoteFlixTheme.CARD_BG); card.setBorder(new EmptyBorder(15, 15, 15, 15));
        Font f = new Font("Arial", Font.PLAIN, 14);
        if (tipo.equals("filme")) {
            addCardLine(card, "ID filme: " + getStr(obj, "id"), f);
            addCardLine(card, "Titulo: " + getStr(obj, "titulo"), f);
            addCardLine(card, "Diretor: " + getStr(obj, "diretor"), f);
            addCardLine(card, "Ano: " + getStr(obj, "ano"), f);
            String gen = ""; if(obj.has("genero")){JsonArray a=obj.getAsJsonArray("genero");for(JsonElement e:a)gen+=e.getAsString()+", ";if(gen.length()>2)gen=gen.substring(0,gen.length()-2);}
            addCardLine(card, "Genero: " + gen, f);
            addCardLine(card, "Nota: " + getStr(obj, "nota"), f);
            addCardLine(card, "Qtd Avaliações: " + getStr(obj, "qtd_avaliacoes"), f);
            addCardLine(card, "Sinopse: " + getStr(obj, "sinopse"), f);
        } else if (tipo.equals("review")) {
            addCardLine(card, "ID: " + getStr(obj, "id"), f);
            addCardLine(card, "Filme ID: " + getStr(obj, "id_filme"), f);
            addCardLine(card, "Nota: " + getStr(obj, "nota"), f);
            addCardLine(card, "Titulo: " + getStr(obj, "titulo"), f);
            addCardLine(card, "Descrição: " + getStr(obj, "descricao"), f);
            addCardLine(card, "Data: " + getStr(obj, "data"), f);
            addCardLine(card, "Editado: " + getStr(obj, "editado"), f);
        } else {
            addCardLine(card, "ID: " + getStr(obj, "id"), f);
            addCardLine(card, "Usuário: " + getStr(obj, "nome"), f);
        }
        return card;
    }
    private void addCardLine(JPanel p, String text, Font f) {
        JLabel l = new JLabel("<html><body style='width: 200px'>" + text + "</body></html>");
        l.setFont(f); l.setForeground(Color.BLACK); p.add(l); p.add(Box.createVerticalStrut(3));
    }

    private void carregarListaFilmes() { enviarSimples("LISTAR_FILMES", "filmes", "filme"); }
    private void carregarMinhasReviews() { enviarSimples("LISTAR_REVIEWS_USUARIO", "reviews", "review"); }
    private void carregarListaUsuarios() { enviarSimples("LISTAR_USUARIOS", "usuarios", "usuario"); }
    private void carregarMeusDados() { 
        JsonObject req = new JsonObject(); req.addProperty("operacao", "LISTAR_PROPRIO_USUARIO"); req.addProperty("token", token);
        JsonObject resp = enviarReceber(req);
        if(resp!=null && resp.get("status").getAsString().equals("200")) {
             contentPanel.removeAll(); 
             JPanel p = new JPanel(); p.setBackground(Color.WHITE);
             JLabel l = new JLabel("Usuário: " + resp.get("usuario").getAsString()); l.setFont(new Font("Arial", Font.BOLD, 30));
             p.add(l); contentPanel.add(p); contentPanel.revalidate(); contentPanel.repaint();
        }
    }
    private void enviarSimples(String op, String arrayName, String type) {
        JsonObject req = new JsonObject(); req.addProperty("operacao", op); req.addProperty("token", token);
        JsonObject resp = enviarReceber(req);
        if (resp != null && resp.get("status").getAsString().equals("200")) mostrarListaCartoes(resp.getAsJsonArray(arrayName), type);
        else mostrarErro(resp);
    }
    private void conectar(String ip, String portaStr) {
        try {
            socket = new Socket(ip, Integer.parseInt(portaStr));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            JOptionPane.showMessageDialog(this, "Conectado!");
        } catch (Exception e) { JOptionPane.showMessageDialog(this, "Erro: " + e.getMessage()); }
    }
    private void fazerLogin(String user, String pass) {
        if(socket == null || socket.isClosed()) { JOptionPane.showMessageDialog(this, "Conecte primeiro!"); return; }
        JsonObject req = new JsonObject(); req.addProperty("operacao", "LOGIN"); req.addProperty("usuario", user); req.addProperty("senha", pass);
        JsonObject resp = enviarReceber(req);
        if (resp != null && resp.get("status").getAsString().equals("200")) {
            token = resp.get("token").getAsString(); 
            trocarAba(Tab.FILME); cardLayout.show(mainPanel, "DASHBOARD");
        } else mostrarErro(resp);
    }
    private void fazerCadastro(String user, String pass) {
        if(socket == null || socket.isClosed()) { JOptionPane.showMessageDialog(this, "Conecte primeiro!"); return; }
        JsonObject u = new JsonObject(); u.addProperty("nome", user); u.addProperty("senha", pass);
        JsonObject req = new JsonObject(); req.addProperty("operacao", "CRIAR_USUARIO"); req.add("usuario", u); enviarEResponder(req);
    }
    private void logout() {
            JsonObject req = new JsonObject(); 
            req.addProperty("operacao", "LOGOUT"); 
            req.addProperty("token", token); 
            enviarReceber(req);
        
        
        cardLayout.show(mainPanel, "LOGIN");
    }
    private void enviarEResponder(JsonObject req) {
        JsonObject resp = enviarReceber(req);
        if (resp != null) {
             if (resp.get("status").getAsString().startsWith("2")) JOptionPane.showMessageDialog(this, resp.get("mensagem").getAsString());
             else mostrarErro(resp);
        }
    }
    private JsonObject enviarReceber(JsonObject req) {
        try {
            out.println(gsonNetwork.toJson(req)); 
            String linha = in.readLine();
            if (linha == null) return null;
            return JsonParser.parseString(linha).getAsJsonObject();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Erro rede: " + e.getMessage());
            return null;
        }
    }
    private void mostrarErro(JsonObject resp) {
        String msg = resp != null ? resp.get("mensagem").getAsString() : "Erro desconhecido";
        JOptionPane.showMessageDialog(this, msg, "Erro", JOptionPane.ERROR_MESSAGE);
    }
    private String getStr(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) return obj.get(key).getAsString();
        return "-";
    }
}