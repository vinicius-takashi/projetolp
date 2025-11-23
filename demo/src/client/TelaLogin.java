package demo.src.client;
import demo.src.CriptoUtils;
import java.util.Locale;
import java.util.ResourceBundle;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;

public class TelaLogin extends JFrame {

    // Componentes de Interface
    private ResourceBundle bundle;
    private JLabel lblUsuario; // Precisam ser globais para mudarmos o texto depois
    private JLabel lblSenha;
    private JLabel lblTitulo;
    private JComboBox<String> cbIdioma;
    // Conexão com o Servidor
    private Socket socket;
    private PrintStream saida;
    private Scanner entrada;

    public TelaLogin() {
        super("Snake Game - Autenticação");
        configurarJanela();
        inicializarComponentes();
        conectarAoServidor();
    }

    private void configurarJanela() {
        setSize(400, 350);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Centraliza na tela
        setLayout(new BorderLayout());
        setResizable(false);
    }

    private void inicializarComponentes() {
        // Carrega padrão Português
        bundle = ResourceBundle.getBundle("messages", new Locale("pt", "BR"));

        // --- Cabeçalho ---
        JPanel panelTitulo = new JPanel();
        panelTitulo.setBackground(new Color(46, 204, 113));
        lblTitulo = new JLabel(bundle.getString("titulo")); // Usa o bundle
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 24));
        lblTitulo.setForeground(Color.WHITE);
        panelTitulo.add(lblTitulo);
        add(panelTitulo, BorderLayout.NORTH);

        // --- Formulário ---
        JPanel panelForm = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Seletor de Idioma (NOVO)
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        String[] idiomas = { "Português", "English", "Español", "Français", "Italiano" };
        cbIdioma = new JComboBox<>(idiomas);
        panelForm.add(cbIdioma, gbc);

        // Ação ao trocar idioma
        cbIdioma.addActionListener(e -> atualizarTextos());

        gbc.gridwidth = 1; // Reseta largura

        // Usuario
        gbc.gridx = 0;
        gbc.gridy = 1;
        lblUsuario = new JLabel(bundle.getString("usuario"));
        panelForm.add(lblUsuario, gbc);

        gbc.gridx = 1;
        txtUsuario = new JTextField(15);
        panelForm.add(txtUsuario, gbc);

        // Senha
        gbc.gridx = 0;
        gbc.gridy = 2;
        lblSenha = new JLabel(bundle.getString("senha"));
        panelForm.add(lblSenha, gbc);

        gbc.gridx = 1;
        txtSenha = new JPasswordField(15);
        panelForm.add(txtSenha, gbc);

        add(panelForm, BorderLayout.CENTER);

        // --- Rodapé ---
        JPanel panelBotoes = new JPanel(new FlowLayout());
        btnEntrar = new JButton(bundle.getString("entrar"));
        btnCadastrar = new JButton(bundle.getString("cadastrar"));

        // ... (resto do código de botões igual ao anterior) ...

        panelBotoes.add(btnEntrar);
        panelBotoes.add(btnCadastrar);

        // Status label
        lblStatus = new JLabel("...");
        JPanel panelSul = new JPanel(new BorderLayout());
        panelSul.add(lblStatus, BorderLayout.NORTH);
        panelSul.add(panelBotoes, BorderLayout.SOUTH);
        add(panelSul, BorderLayout.SOUTH);
        // --- Ações dos Botões ---
        btnEntrar.addActionListener(this::realizarLogin);

        // Dica: O botão Cadastrar pode abrir um JDialog
        btnCadastrar.addActionListener(e -> {
            if (socket == null || socket.isClosed()) {
                JOptionPane.showMessageDialog(this, "Não é possível cadastrar: Sem conexão com o servidor.");
                return;
            }
            // Abre a tela de cadastro passando a conexão atual
            TelaCadastro telaCad = new TelaCadastro(this, socket, saida, entrada);
            telaCad.setVisible(true);
        });

        // Permitir login ao apertar ENTER no campo de senha
        getRootPane().setDefaultButton(btnEntrar);
    }

    private void conectarAoServidor() {
        try {
            // Tenta conectar ao servidor criado no passo anterior
            socket = new Socket("localhost", 12345);
            saida = new PrintStream(socket.getOutputStream());
            entrada = new Scanner(socket.getInputStream());
            lblStatus.setText("Conectado ao Servidor");
            lblStatus.setForeground(new Color(0, 100, 0));
        } catch (Exception e) {
            lblStatus.setText("Erro: Servidor Offline");
            lblStatus.setForeground(Color.RED);
            btnEntrar.setEnabled(false);
            btnCadastrar.setEnabled(false);
        }
    }

    private void realizarLogin(ActionEvent e) {
        String usuario = txtUsuario.getText();
        String senhaPura = new String(txtSenha.getPassword());

        if (usuario.isEmpty() || senhaPura.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Preencha todos os campos!");
            return;
        }

        // Req 3.13: Criptografar antes de enviar
        String senhaHash = CriptoUtils.hashSenha(senhaPura);

        try {
            // Envia protocolo: LOGIN;usuario;hash
            saida.println("LOGIN;" + usuario + ";" + senhaHash);

            if (entrada.hasNextLine()) {
                String resposta = entrada.nextLine();

                if ("LOGIN_OK".equals(resposta)) {
                    // Login Sucesso: Fecha essa tela e abre o Jogo
                    this.dispose();
                    new ClienteSnake().setVisible(true); // Abre a classe principal
                } else {
                    JOptionPane.showMessageDialog(this, "Usuário ou Senha incorretos.", "Erro",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro de comunicação com o servidor.");
        }
    }

    private void atualizarTextos() {
        String selecionado = (String) cbIdioma.getSelectedItem();
        Locale locale;

        switch (selecionado) {
            case "English":
                locale = new Locale("en", "US");
                break;
            case "Español":
                locale = new Locale("es", "ES");
                break;
            case "Français":
                locale = new Locale("fr", "FR");
                break;
            case "Italiano":
                locale = new Locale("it", "IT");
                break;
            default:
                locale = new Locale("pt", "BR");
                break;
        }

        // Recarrega o arquivo properties correto
        bundle = ResourceBundle.getBundle("messages", locale);

        // Atualiza os componentes da tela
        lblTitulo.setText(bundle.getString("titulo"));
        lblUsuario.setText(bundle.getString("usuario"));
        lblSenha.setText(bundle.getString("senha"));
        btnEntrar.setText(bundle.getString("entrar"));
        btnCadastrar.setText(bundle.getString("cadastrar"));

        // Atualiza o layout para garantir que textos maiores caibam
        this.revalidate();
        this.repaint();
    }

    // Método para testar visualmente apenas a tela de login
    public static void main(String[] args) {
        try {
            // Deixa a interface com cara do sistema operacional (Windows)
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        }

        SwingUtilities.invokeLater(() -> new TelaLogin().setVisible(true));
    }
}