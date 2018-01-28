/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package emailreceive;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.JFXPanel;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebEvent;
import javafx.scene.web.WebView;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;

import static javafx.concurrent.Worker.State.FAILED;
import javax.mail.internet.MimeBodyPart;

public class Browser extends JFrame {

    private final JFXPanel jfxPanel = new JFXPanel();
    private WebEngine engine;

    private final JPanel panel = new JPanel(new BorderLayout());
    private final JLabel lblStatus = new JLabel();

    private final JButton btnGo = new JButton("<html><center>Tải thư trên <br /> trình duyệt</center></html>");
    private final JButton btnDownload=new JButton("<html><b>Download Attachment</b></html>");
    private final JButton btnTxt=new JButton("Đọc dạng text");
    
    private final JProgressBar progressBar = new JProgressBar();
    
    private final JTextArea txtDetail = new JTextArea();
    
    private ArrayList<MimeBodyPart> attachments;
    
    private String from,to,subject,DateSend,url;
    

    public Browser(String from,String to,String subject,String DateSent,ArrayList<MimeBodyPart> attachments) {
        super();        
        ImageIcon icon=new ImageIcon("email.png");
        this.setIconImage(icon.getImage());
        
        //this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.from=from;
        this.to=to;
        this.subject=subject;
        this.DateSend=DateSend;
        this.url=url;
        
        initComponents();
        txtDetail.append("Người gửi: \t"+from+"\n");
        txtDetail.append("Người nhận: \t"+to+"\n");
        txtDetail.append("Tiêu đề: \t"+subject+"\n");
        txtDetail.append("Ngày gửi: \t"+DateSent);
        
        if(attachments.size()==0)
            btnDownload.setVisible(false);
        else
        {
            this.attachments = new ArrayList<MimeBodyPart>();
            this.attachments=attachments;
        }
        String currDir=System.getProperty("user.dir");
                currDir=currDir.replace("\\", "/");
                currDir="file:///"+currDir;
        loadURL(currDir+"/mail.html");
    }
    
    public Browser()
    {
        super();
        ImageIcon icon =new ImageIcon("email.png");
        this.setIconImage(icon.getImage());
        
        initComponents();
    }

    private void initComponents() {
        createScene();
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        ActionListener al = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //loadURL(txtURL.getText());
                if(Desktop.isDesktopSupported())
                {
                    try {
                        Desktop.getDesktop().browse(new URI("mail.html"));
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(null, "Không thể mở được mail","Có lỗi xảy ra",JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        };
        
         ActionListener DownAtt = new  ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread(new Runnable(){
                    @Override 
                    public void run()
                    {
                        JFileChooser chooser=new JFileChooser("Lưu tập tin đính kèm");
                        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                        int result=chooser.showOpenDialog(null);

                        if(result==JFileChooser.APPROVE_OPTION)
                        {
                            String path=chooser.getSelectedFile().getAbsolutePath();
                            for(MimeBodyPart part:attachments)
                            {
                                try
                                {
                                    String FolderName=part.getFileName().toString();
                                    try
                                    {
                                        part.saveFile(new File(path+"\\"+part.getFileName()));
                                        JOptionPane.showMessageDialog(null, "Đã lưu thành công file \"" + FolderName + "\"","Đã lưu thành công",JOptionPane.INFORMATION_MESSAGE);
                                    }
                                    catch(Exception ex)
                                    {
                                        JOptionPane.showMessageDialog(null, "Không thể lưu file \"" + FolderName + "\"", "Đã có lỗi xảy ra",JOptionPane.ERROR_MESSAGE);
                                    }    
                                }
                                catch(Exception ex)
                                {
                                    
                                }
                            }
                        }
                    }
            }).start();
            }
        };
         
        
        btnGo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/emailreceive/res/browser.png")));//Browser-icon.png")));
        btnGo.addActionListener(al);
        
        btnDownload.setIcon(new javax.swing.ImageIcon(getClass().getResource("/emailreceive/res/attachment.png")));
        btnDownload.addActionListener(DownAtt);
        
        
        txtDetail.setEditable(false);

        progressBar.setPreferredSize(new Dimension(150, 18));
        progressBar.setStringPainted(true);
        
        //btnTxt.addActionListener(ReadText);

        JPanel topBar = new JPanel(new BorderLayout(5, 0));
        topBar.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
        topBar.add(txtDetail, BorderLayout.CENTER);
        topBar.add(btnGo, BorderLayout.EAST);

        JPanel statusBar = new JPanel(new BorderLayout(5, 0));
        statusBar.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
        statusBar.add(lblStatus, BorderLayout.CENTER);
        statusBar.add(progressBar, BorderLayout.EAST);
        statusBar.add(btnDownload,BorderLayout.WEST);

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(jfxPanel, BorderLayout.CENTER);
        panel.add(statusBar, BorderLayout.SOUTH);
        getContentPane().add(panel);
        setPreferredSize(new Dimension(600, 600));
        pack();
    }

    private void createScene() {

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                WebView view = new WebView();
                engine = view.getEngine();
                engine.titleProperty().addListener(new ChangeListener<String>() {
                    @Override
                    public void changed(ObservableValue<? extends String> observable, String oldValue, final String newValue) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                Browser.this.setTitle(newValue+" | Sent & Receive eMail Application");
                            }
                        });
                    }
                });

                engine.setOnStatusChanged(new EventHandler<WebEvent<String>>() {
                    @Override
                    public void handle(final WebEvent<String> event) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                lblStatus.setText(event.getData());
                            }
                        });
                    }
                });

                engine.locationProperty().addListener(new ChangeListener<String>() {
                    @Override
                    public void changed(ObservableValue<? extends String> ov, String oldValue, final String newValue) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                //txtURL.setText(newValue);
                            }
                        });
                    }
                });

                engine.getLoadWorker().workDoneProperty().addListener(new ChangeListener<Number>() {
                    @Override
                    public void changed(ObservableValue<? extends Number> observableValue, Number oldValue, final Number newValue) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setValue(newValue.intValue());
                            }
                        });
                    }
                });

                engine.getLoadWorker()
                        .exceptionProperty()
                        .addListener(new ChangeListener<Throwable>() {

                            public void changed(ObservableValue<? extends Throwable> o, Throwable old, final Throwable value) {
                                if (engine.getLoadWorker().getState() == FAILED) {
                                    SwingUtilities.invokeLater(new Runnable() {
                                        @Override
                                        public void run() {
                                            JOptionPane.showMessageDialog(
                                            panel,
                                            (value != null)
                                            ? engine.getLocation() + "\n" + value.getMessage()
                                            : engine.getLocation() + "\nLỗi không xác định.",
                                            "Đã có lỗi xảy ra...",
                                            JOptionPane.ERROR_MESSAGE);
                                        }
                                    });
                                }
                            }
                        });
                
                engine.locationProperty().addListener(new ChangeListener<String>()
                {
                    @Override
                    //Override public void changed(ObservableValue<? extends String> ov, final String oldLoc, final String loc)
                    public void changed(ObservableValue<? extends String> observable, final String oldValue, final String newValue)
                    {
                        Desktop d = Desktop.getDesktop();
                        try
                        {
                            URI address = new URI(newValue);

                            if ( !newValue.contains(url) )
                            {
                                Platform.runLater(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        loadURL(oldValue);
                                    }

                                });
                                d.browse(address);
                            }
                        }
                        catch ( Exception e )
                        {
                            
                        }
                    }

                });
                
                jfxPanel.setScene(new Scene(view));
                Platform.setImplicitExit(false);
                //Platform.exit();
            }
        });
    }

    public void loadURL(final String url) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                String tmp = toURL(url);

                if (tmp == null) {
                    tmp = toURL("http://" + url);
                }

                engine.load(tmp);
            }
        });
    }

    private static String toURL(String str) {
        try {
            return new URL(str).toExternalForm();
        } catch (MalformedURLException exception) {
            return null;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                new Browser().setVisible(true);
            }
        });
    }
    
    
}
