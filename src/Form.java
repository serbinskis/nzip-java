import compression.CompressionType;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.Consumer;

public class Form {
    public static int WIDTH = 350;
    public static int HEIGHT = 200;
    public static int X_OFFSET = 10;
    public static int Y_OFFSET = 35;

    public static void createWindow() {
        JFrame jFrame = new JFrame("nzip Compressor");
        jFrame.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame.setLayout(null);
        jFrame.setResizable(false);

        JButton button0 = new JButton("i");
        button0.setMargin(new Insets(0, 0, 0, 0));
        button0.setBounds(5, 5, 15, 15);
        button0.setFocusPainted(false);
        jFrame.getContentPane().add(button0);

        JTextField textField1 = new JTextField("");
        textField1.setMargin(new Insets(0, 0, 0, 0));
        textField1.setBounds(X_OFFSET+0, Y_OFFSET+0, 210, 25);
        textField1.setEnabled(false);
        textField1.setDisabledTextColor(Color.GRAY);
        jFrame.getContentPane().add(textField1);

        JButton button1 = new JButton("Select File");
        button1.setBounds(X_OFFSET+215, Y_OFFSET+0, 100, 25);
        button1.setMargin(new Insets(0, 0, 0, 0));
        button1.setFocusPainted(false);
        jFrame.getContentPane().add(button1);

        JComboBox<CompressionType> comboBox1 = new JComboBox<>(CompressionType.COMPRESSION_TYPES);
        comboBox1.setBounds(X_OFFSET+0, Y_OFFSET+30, 210, 25);
        jFrame.getContentPane().add(comboBox1);

        JButton button2 = new JButton("Compress");
        button2.setMargin(new Insets(0, 0, 0, 0));
        button2.setBounds(X_OFFSET+215, Y_OFFSET+30, 100, 25);
        button2.setFocusPainted(false);
        jFrame.getContentPane().add(button2);

        JProgressBar progressBar1 = new JProgressBar(0, 100);
        progressBar1.setBounds(X_OFFSET+0, Y_OFFSET+60, 210, 25);
        progressBar1.setStringPainted(true);
        jFrame.getContentPane().add(progressBar1);

        JButton button3 = new JButton("Verify");
        button3.setMargin(new Insets(0, 0, 0, 0));
        button3.setBounds(X_OFFSET+215, Y_OFFSET+60, 100, 25);
        button3.setFocusPainted(false);
        jFrame.getContentPane().add(button3);
        
        JLabel label1 = new JLabel("");
        label1.setBounds(X_OFFSET+0, Y_OFFSET+90, 315, 25);
        label1.setText("Compression => ...");
        jFrame.getContentPane().add(label1);

        jFrame.pack();
        jFrame.setVisible(true);
        jFrame.setLocationRelativeTo(null);

        Consumer<Float> progressCallback = progress -> {
            progressBar1.setValue(Math.round(progress));
            progressBar1.setString(String.format(Locale.US, "%.2f%%", progress));
        };

        button0.addActionListener(e -> {
            JOptionPane.showMessageDialog(jFrame, Main.ABOUT, "About", JOptionPane.PLAIN_MESSAGE);
        });

        button1.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.home") + "/Desktop"));
            if (fileChooser.showOpenDialog(jFrame) != JFileChooser.APPROVE_OPTION) { return; }

            progressBar1.setValue(0);
            label1.setText("Compression => ...");
            textField1.setText(fileChooser.getSelectedFile().getAbsolutePath());
            CompressionType compressionType = CompressionType.getCompressed(textField1.getText());

            button2.setText((compressionType == null) ? "Compress" : "Decompress");
            comboBox1.setEnabled(compressionType == null);
            if (compressionType != null) { comboBox1.setSelectedItem(compressionType); }
            jFrame.pack();
        });

        button2.addActionListener(e -> new Thread(() -> {
            if (textField1.getText().isEmpty()) { return; }
            boolean compressing = comboBox1.isEnabled();
            CompressionType compressionType = comboBox1.getItemAt(comboBox1.getSelectedIndex());
            String comp_filename = textField1.getText() + compressionType.getExtension();
            String decomp_filename = textField1.getText().substring(0, Math.max(textField1.getText().lastIndexOf('.'), 0));
            String result_filename = compressing ? comp_filename : decomp_filename;

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File(result_filename));
            fileChooser.setSelectedFile(new File(result_filename));
            if (fileChooser.showSaveDialog(jFrame) != JFileChooser.APPROVE_OPTION) { return; }
            result_filename = fileChooser.getSelectedFile().getAbsolutePath();

            if (compressing && !result_filename.endsWith(compressionType.getExtension())) {
                result_filename += compressionType.getExtension();
            }

            try {
                button2.setEnabled(false);
                button3.setEnabled(false);
                byte[] data = Files.readAllBytes(Paths.get(textField1.getText()));
                int filesize = data.length;
                if (!compressing) { data = compressionType.decompress(data, progressCallback); }
                if (compressing) { data = compressionType.compress(data, progressCallback); }
                if (compressing) { label1.setText(String.format(Locale.US, "Compression => Type: %s, Ratio: %.2f", compressionType.getName(), ((float) filesize/data.length))); }
                Files.write(Paths.get(result_filename), data);
                JOptionPane.showMessageDialog(jFrame, "File was compressed/decompressed!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(jFrame, "Error reading/compressing file");
                label1.setText("Error: " + ex.getMessage());
            }

            progressBar1.setValue(100);
            button2.setEnabled(true);
            button3.setEnabled(true);
        }).start());
        
        button3.addActionListener(e -> new Thread(() -> {
        	if (textField1.getText().isEmpty()) { button1.doClick(); }
            if (textField1.getText().isEmpty()) { return; }
            CompressionType compressionType = comboBox1.getItemAt(comboBox1.getSelectedIndex());

            try {
            	button2.setEnabled(false);
            	button3.setEnabled(false);
                byte[] data = Files.readAllBytes(Paths.get(textField1.getText()));
                byte[] comp_data = compressionType.compress(data, progress -> progressCallback.accept(progress/2f));
                byte[] decomp_data = compressionType.decompress(comp_data, progress -> progressCallback.accept(50f + progress/2f));
                label1.setText(String.format(Locale.US, "Verification => Type: %s, Ratio: %.2f, Verify: %b", compressionType.getName(), ((float) data.length/comp_data.length), Arrays.equals(data, decomp_data)));
                JOptionPane.showMessageDialog(jFrame, "File was verified!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(jFrame, "Error reading/verifying file");
                label1.setText("Error: " + ex.getMessage());
            }

            progressBar1.setValue(100);
            button2.setEnabled(true);
            button3.setEnabled(true);
        }).start());
    }

    public static void main(String[] args) { createWindow(); }
}