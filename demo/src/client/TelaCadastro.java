package demo.src.client;
import demo.src.CriptoUtils;
import javax.swing.*;
import java.awt.*;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;

// JDialog é ideal para janelas "filhas" como cadastros
public class TelaCadastro extends JDialog {

    private JTextField txtUsuario;
    private JPasswordField txtSenha;
    private JPasswordField txtConfirmaSenha;
    private JButton btnSalvar;
    private JButton btnCancelar;

    // Precisa dos dados de conexão para enviar ao servidor
    private Socket socket;
    private PrintStream saida;
    private Scanner entrada;

    public TelaCadastro(Frame owner, Socket socket, PrintStream saida, Scanner entrada) {
        super(owner, "Novo Usuário", true); // 'true' torna a janela Modal
        this.socket = socket;
        this.saida = saida;
        this.entrada = entrada;

        configurarJanela();
        inicializarComponentes();
    }

    private void configurarJanela() {
        setSize(350, 250);
        setLayout(new BorderLayout());
        setLocationRelativeTo(getOwner()); // Abre centralizado em relação à tela de Login
        setResizable(false);
    }

    private void inicializarComponentes() {
        // Painel Central com Formulário
        JPanel panelForm = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Linha 1: Usuário
        gbc.gridx = 0; gbc.gridy = 0;
        panelForm.add(new JLabel("Novo Usuário:"), gbc);
        
        gbc.gridx = 1; 
        txtUsuario = new JTextField(15);
        panelForm.add(txtUsuario, gbc);

        // Linha 2: Senha
        gbc.gridx = 0; gbc.gridy = 1;
        panelForm.add(new JLabel("Senha:"), gbc);
        
        gbc.gridx = 1; 
        txtSenha = new JPasswordField(15);
        panelForm.add(txtSenha, gbc);

        // Linha 3: Confirmar Senha (Req 3.10)
        gbc.gridx = 0; gbc.gridy = 2;
        panelForm.add(new JLabel("Confirmar:"), gbc);
        
        gbc.gridx = 1; 
        txtConfirmaSenha = new JPasswordField(15);
        panelForm.add(txtConfirmaSenha, gbc);

        add(panelForm, BorderLayout.CENTER);

        // Painel Sul com Botões
        JPanel panelBotoes = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnSalvar = new JButton("Salvar");
        btnSalvar.setBackground(new Color(46, 204, 113));
        btnSalvar.setForeground(Color.WHITE);
        
        btnCancelar = new JButton("Cancelar");

        panelBotoes.add(btnCancelar);
        panelBotoes.add(btnSalvar);
        
        add(panelBotoes, BorderLayout.SOUTH);

        // Eventos
        btnCancelar.addActionListener(e -> dispose()); // Fecha a janela
        btnSalvar.addActionListener(e -> realizarCadastro());
    }

    private void realizarCadastro() {
        String user = txtUsuario.getText();
        String pass1 = new String(txtSenha.getPassword());
        String pass2 = new String(txtConfirmaSenha.getPassword());

        // Validações Básicas
        if (user.isEmpty() || pass1.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Preencha todos os campos.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Req 3.10: Comparação de senhas
        if (!pass1.equals(pass2)) {
            JOptionPane.showMessageDialog(this, "As senhas não conferem!", "Erro", JOptionPane.ERROR_MESSAGE);
            txtSenha.setText("");
            txtConfirmaSenha.setText("");
            return;
        }

        // Req 3.13: Criptografia
        String hash = CriptoUtils.hashSenha(pass1);

        try {
            // Envia ao servidor
            saida.println("CADASTRAR;" + user + ";" + hash);

            // Aguarda resposta
            if (entrada.hasNextLine()) {
                String resp = entrada.nextLine();
                if ("CADASTRO_OK".equals(resp)) {
                    JOptionPane.showMessageDialog(this, "Usuário cadastrado com sucesso!");
                    dispose(); // Fecha a janela de cadastro
                } else {
                    JOptionPane.showMessageDialog(this, "Erro: Usuário já existe.", "Erro", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro de conexão ao cadastrar.");
        }
    }
}
