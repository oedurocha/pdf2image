import java.awt.FlowLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatLightLaf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

public class PDFToImageConverterApp extends JFrame {

    /**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private List<File> selectedFiles = new ArrayList<>();
    private JTextArea selectedFilesTextArea;
    private JTextField destinationTextField;


    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    // Define o Look and Feel Nimbus
                    UIManager.setLookAndFeel(new FlatLightLaf());
                } catch (Exception e) {
                    e.printStackTrace();
                }

                new PDFToImageConverterApp().setVisible(true);
            }
        });
    }

    
    public PDFToImageConverterApp() {
        setTitle("Conversor de PDF para Imagem");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(420, 250);
        setLocationRelativeTo(null);
        setIconImage(new ImageIcon(PDFToImageConverterApp.class.getClassLoader().getResource("icon.ico")).getImage());
        initDragAndDrop();

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Seleção de arquivos
        JPanel attachmentPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        
        JLabel attachLabel = new JLabel("Selecionar arquivos:");
        attachLabel.setHorizontalAlignment(SwingConstants.LEFT);
        attachmentPanel.add(attachLabel);
        
        JButton selectFilesButton = new JButton("Procurar");
        selectFilesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectFiles();
            }
        });
        attachmentPanel.add(selectFilesButton);
        
        JButton clearButton = new JButton("Limpar lista");
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearSelectedFiles();
            }
        });
        attachmentPanel.add(clearButton);

        selectedFilesTextArea = new JTextArea(7, 32);
        selectedFilesTextArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(selectedFilesTextArea);
        attachmentPanel.add(scrollPane);
        
        mainPanel.add(attachmentPanel);

        // Diretório de destino
        JPanel destinationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        
        JLabel destinationLabel = new JLabel("Destino:");
        destinationLabel.setHorizontalAlignment(SwingConstants.LEFT);
        destinationPanel.add(destinationLabel);
        
        destinationTextField = new JTextField(20);
        destinationPanel.add(destinationTextField);
        
        JButton selectDestinationButton = new JButton("Procurar");
        selectDestinationButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectDestination();
            }
        });
        destinationPanel.add(selectDestinationButton);
        
        mainPanel.add(destinationPanel);

        // Formato da imagem
        JPanel convertPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel formatLabel = new JLabel("Formato da Imagem:");
        formatLabel.setHorizontalAlignment(SwingConstants.LEFT);
        JComboBox<String> formatComboBox = new JComboBox<>(new String[]{"JPEG", "PNG"});
        
        convertPanel.add(formatLabel);
        convertPanel.add(formatComboBox);
        

        // Botão de conversão
        JButton convertButton = new JButton("Converter");
        convertButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String destinationPath = destinationTextField.getText();
                String format = (String) formatComboBox.getSelectedItem();
                convertToImages(destinationPath, format);
            }
        });
        convertPanel.add(convertButton);
        mainPanel.add(convertPanel);

        
        
        setContentPane(mainPanel);
    }

    private void selectFiles() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".pdf");
            }

            @Override
            public String getDescription() {
                return "Arquivos PDF (*.pdf)";
            }
        });

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File[] files = fileChooser.getSelectedFiles();
            selectedFiles.clear();
            StringBuilder filesText = new StringBuilder();
            for (File file : files) {
                selectedFiles.add(file);
                filesText.append(file.getName()).append("\n");
            }
            selectedFilesTextArea.setText(filesText.toString());
        }
    }

    private void selectDestination() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedDirectory = fileChooser.getSelectedFile();
            destinationTextField.setText(selectedDirectory.getAbsolutePath());
        }
    }

    private void convertToImages(String destinationPath, String format) {
        if (selectedFiles.isEmpty()) {
            showErrorDialog("Nenhum arquivo selecionado.", "Por favor, selecione pelo menos um arquivo PDF.");
            return;
        }

        if (destinationPath.isEmpty()) {
            showErrorDialog("Diretório de destino não especificado.", "Por favor, selecione o diretório de destino.");
            return;
        }

        File destinationDir = new File(destinationPath);
        if (!destinationDir.exists() || !destinationDir.isDirectory()) {
            showErrorDialog("Diretório de destino inválido.", "O diretório de destino especificado não existe.");
            return;
        }

        String customFileName = JOptionPane.showInputDialog(this, "Digite o nome do arquivo convertido:", "Nome do arquivo", JOptionPane.PLAIN_MESSAGE);
        if (customFileName == null || customFileName.trim().isEmpty()) {
            showErrorDialog("Nome de arquivo inválido.", "O nome do arquivo não pode estar em branco.");
            return;
        }

        for (File file : selectedFiles) {
            try (PDDocument document = PDDocument.load(file)) {
                PDFRenderer renderer = new PDFRenderer(document);
                for (int page = 0; page < document.getNumberOfPages(); ++page) {
                    BufferedImage image = renderer.renderImageWithDPI(page, 300);
                    String outputFileName = customFileName + "_page" + (page + 1) + "." + format.toLowerCase();
                    File outputFile = new File(destinationDir, outputFileName);
                    ImageIO.write(image, format, outputFile);
                }
            } catch (Exception e) {
                e.printStackTrace();
                showErrorDialog("Erro na conversão de arquivos PDF para imagens.", "Ocorreu um erro ao converter os arquivos PDF.");
                return;
            }
        }

        showInfoDialog("Conversão concluída.", "Os arquivos PDF foram convertidos com sucesso.");
    }
    
    private void clearSelectedFiles() {
        selectedFiles.clear();
        selectedFilesTextArea.setText("");
    }
    
    private void initDragAndDrop() {
        new DropTarget(this, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent event) {
                event.acceptDrop(DnDConstants.ACTION_COPY);
                Transferable transferable = event.getTransferable();
                if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    try {
                        @SuppressWarnings("unchecked")
                        List<File> droppedFiles = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                        for (File file : droppedFiles) {
                            if (file.getName().toLowerCase().endsWith(".pdf")) {
                                selectedFiles.add(file);
                                selectedFilesTextArea.append(file.getName() + "\n");
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void showErrorDialog(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }

    private void showInfoDialog(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
    }
}
