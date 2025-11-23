package demo.src.client;
import demo.src.CriptoUtils;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;

public class TelaMenu extends JFrame {

    // Dados da sessão
    private Socket socket;
    private String usuarioLogado;
    private PrintStream saida;
    private Scanner entrada;

    // Componentes de configuração do jogo
    private JComboBox<String> cbCorCobra;
    private JComboBox<String> cbDificuldade;
    private JLabel lblBemVindo;

    public TelaMenu(Socket socket, String usuario, PrintStream saida, Scanner entrada) {
        super("Snake Game - Menu Principal");
        this.socket = socket;
        this.usuarioLogado = usuario;
        this.saida = saida;
        this.entrada = entrada;

        configurarJanela();
        inicializarComponentes();
    }

    private void configurarJanela() {
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
    }

    private void inicializarComponentes() {
        // --- TOPO: Boas vindas ---
        JPanel panelTopo = new JPanel();
        panelTopo.setBackground(Color.DARK_GRAY);
        panelTopo.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        
        lblBemVindo = new JLabel("Olá, " + usuarioLogado + "!");
        lblBemVindo.setFont(new Font("Arial", Font.BOLD, 22));
        lblBemVindo.setForeground(Color.WHITE);
        panelTopo.add(lblBemVindo);
        add(panelTopo, BorderLayout.NORTH);

        // --- CENTRO: Configurações (GridBagLayout para Req 3.8) ---
        JPanel panelCentro = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Configuração: Cor da Cobra (Req 3.9 - JComboBox)
        gbc.gridx = 0; gbc.gridy = 0;
        panelCentro.add(new JLabel("Cor da Cobra:"), gbc);

        String[] cores = {"Verde", "Azul", "Laranja", "Roxo"};
        cbCorCobra = new JComboBox<>(cores);
        gbc.gridx = 1; gbc.gridy = 0;
        panelCentro.add(cbCorCobra, gbc);

        // Configuração: Dificuldade (Req 3.9 - JComboBox)
        gbc.gridx = 0; gbc.gridy = 1;
        panelCentro.add(new JLabel("Velocidade:"), gbc);

        String[] niveis = {"Lento", "Normal", "Rápido", "Insano"};
        cbDificuldade = new JComboBox<>(niveis);
        cbDificuldade.setSelectedIndex(1); // Padrão Normal
        gbc.gridx = 1; gbc.gridy = 1;
        panelCentro.add(cbDificuldade, gbc);

        add(panelCentro, BorderLayout.CENTER);

        // --- SUL: Botões de Ação ---
        JPanel panelBotoes = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));

        JButton btnJogar = new JButton("INICIAR JOGO");
        btnJogar.setBackground(new Color(46, 204, 113));
        btnJogar.setForeground(Color.WHITE);
        btnJogar.setFont(new Font("Arial", Font.BOLD, 14));
        btnJogar.setPreferredSize(new Dimension(150, 40));

        JButton btnRanking = new JButton("VER RANKING");
        btnRanking.setPreferredSize(new Dimension(150, 40));

        panelBotoes.add(btnRanking);
        panelBotoes.add(btnJogar);
        add(panelBotoes, BorderLayout.SOUTH);

        // --- AÇÕES ---
        
        btnJogar.addActionListener(e -> iniciarJogo());
        
        btnRanking.addActionListener(e -> carregarRanking());
    }

    private void iniciarJogo() {
    // 1. Define a cor
    Color corSelecionada = Color.GREEN;
    String selCor = (String) cbCorCobra.getSelectedItem();
    
    switch (selCor) {
        case "Azul": corSelecionada = Color.BLUE; break;
        case "Laranja": corSelecionada = Color.ORANGE; break;
        case "Roxo": corSelecionada = new Color(128, 0, 128); break;
        default: corSelecionada = Color.GREEN;
    }

    // 2. Define a velocidade (Delay em milissegundos - menor é mais rápido)
    int delay = 100;
    String selDif = (String) cbDificuldade.getSelectedItem();
    
    switch (selDif) {
        case "Lento": delay = 150; break;
        case "Rápido": delay = 60; break;
        case "Insano": delay = 30; break;
        default: delay = 100; // Normal
    }

    // 3. Abre a tela do Jogo
    // 'this.saida' é o PrintStream conectado ao servidor que recebemos no construtor
    TelaJogo jogo = new TelaJogo(socket, usuarioLogado, saida, corSelecionada, delay);
    jogo.setVisible(true);
}

    private void carregarRanking() {
        try {
            saida.println("GET_RANKING");
            
            if (entrada.hasNextLine()) {
                String resposta = entrada.nextLine(); // "RANKING_DATA;Ana:100#Beto:50"
                
                if (resposta.startsWith("RANKING_DATA;")) {
                    String dadosBrutos = resposta.substring(13); // Remove o cabeçalho
                    String[] registros = dadosBrutos.split("#");
                    
                    // Req 3.9: Criação e exibição de JTable
                    String[] colunas = {"Posição", "Jogador", "Pontuação"};
                    DefaultTableModel model = new DefaultTableModel(colunas, 0);
                    
                    int pos = 1;
                    for (String reg : registros) {
                        if (!reg.isEmpty()) {
                            String[] partes = reg.split(":");
                            model.addRow(new Object[]{pos + "º", partes[0], partes[1]});
                            pos++;
                        }
                    }
                    
                    JTable tabela = new JTable(model);
                    JScrollPane scroll = new JScrollPane(tabela);
                    scroll.setPreferredSize(new Dimension(400, 200));
                    
                    JOptionPane.showMessageDialog(this, scroll, "Ranking Global", JOptionPane.PLAIN_MESSAGE);
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao buscar ranking.");
        }
    }
}