package emailreceive;

import com.sun.glass.events.KeyEvent;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.io.*;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.FlagTerm;
import javax.mail.search.SubjectTerm;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author thehaohcm
 */
public class EmailReceiveForm extends javax.swing.JFrame {
    private int numMail;
    private Message[] msg;
    private String username,password,smtpserver,smtpport,imapserver,imapport;
    private boolean ssl;
    private Store store;
    private Folder folder;
    private String FolderName;
    private int stt=-1;
    
    private boolean flagSearch=false;
    private void AddRowTable(int stt,String from,String subject,String date)
    {
        DefaultTableModel model=(DefaultTableModel) jTable1.getModel();
        model.addRow(new Object[]{stt,from,subject,date});
    }
    
    private void ConnectIMAP(){
        new Thread(new Runnable(){
            @Override
            public void run(){
                synchronized(this){
                    try{
                        Properties props = new Properties();
                        props.put("mail.store.protocol", "imaps");
                        Session session = Session.getDefaultInstance(props, null);
                        store = session.getStore("imaps");
                        store.connect(imapserver,Integer.parseInt(imapport) ,username, password);
                    }
                    catch(Exception ex)
                    {
                        JOptionPane.showMessageDialog(null, "Bạn vui lòng kiểm tra lại kết nối Internet và tài khoản đăng nhập","Đã có lỗi xảy ra",JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }).start();
    }
    
    private void ReadMail(int stt)
    {
        
        java.awt.EventQueue.invokeLater(new Runnable()
        {
            @Override
            public void run(){
                String Content = "";
                String Content0 = "";
                String MailFrom = null,Subject = null,DateSend = null;
                boolean flagHTML=false;
                Message msgCon;
                ArrayList<MimeBodyPart> attachments = new ArrayList();
                try
                {
                    msgCon=msg[msg.length-stt];
                    Object msgContent = msgCon.getContent();

                    MailFrom=msgCon.getFrom()[0].toString();
                    Subject=msgCon.getSubject().toString();
                    DateSend=msgCon.getSentDate().toString();
                    if(msgContent instanceof Multipart)
                    {
                        Multipart mp = (Multipart) msgCon.getContent();
                        BodyPart bp = mp.getBodyPart(0);
                        for(int i=0;i<mp.getCount();i++)
                        {
                            MimeBodyPart part=(MimeBodyPart)mp.getBodyPart(i);
                            if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {                                
                                attachments.add(part);
                            }
                            else
                            {
                                String str=part.getContent().toString();
                                if(i==0)
                                    Content0=str;
                                else
                                {
                                    if(str.contains("<") && str.contains(">") && str.contains("</"))
                                        flagHTML=true;
                                    Content+=str;
                                }
                            }                            
                        }
                        
                        if(flagHTML==true && Content0.contains("<") && Content0.contains(">") && Content0.contains("</"))
                            Content=Content0+Content;
                        if(flagHTML==false)
                                Content+=Content0;
                    }
                    else
                       Content=msgContent.toString();
                    
                    msgCon.setFlag(Flags.Flag.SEEN, true);
                }
                catch(Exception ex)
                {
                    Content="Chương trình không thể tải thư";
                }

                try {
                    File file=new File("mail.html");
                    if(!file.exists())
                        file.createNewFile();
                    FileOutputStream fstream=new FileOutputStream(file);
                    BufferedWriter bw=new BufferedWriter(new OutputStreamWriter(fstream,"Unicode"));
                    bw.write(Content);
                    bw.close();
                } catch (Exception ex) {
                    
                }
                
                Browser browser = new Browser(MailFrom,username,Subject,DateSend,attachments);
                browser.setVisible(true);
            }
        });
                //.start();
    }
    
    private void ReceiveMail(boolean flg)
    {
        jButton1.setEnabled(false);
        jButton4.setEnabled(false);
        jButton3.setEnabled(false);
        SearchTxt.setEnabled(false);
        SearchBtn.setEnabled(false);
        
        new Thread(new Runnable(){
            @Override
            public void run(){
                synchronized(this)
                {                
                jButton3.setEnabled(false);
                
                DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
                model.setRowCount(0);
                
                try 
                {
                    folder =store.getFolder(FolderName);//"Inbox");
                    folder.open(Folder.READ_WRITE);
                    if(flg==false)
                        if(UnReadRadio.isSelected()==true)
                        {
                            FlagTerm ft = new FlagTerm(new Flags(Flags.Flag.SEEN), false);
                            msg = folder.search(ft);
                        }
                        else
                            msg = folder.getMessages();
                    else
                    {
                        SubjectTerm st=new SubjectTerm(SearchTxt.getText());
                        msg=folder.search(st);
                    }
                    
                    numMail=msg.length-1;
                    int num=(int) NumMailSpinner.getValue();
                    if(msg.length<num)
                        num=msg.length;
                    jTabbedPane1.setTitleAt(1, FolderName+" ("+msg.length+")");
                    int stt=1;
                    for(int i=numMail;i>numMail-num;i--)
                    {
                        String date,from,subject;
                        //System.out.print(msg[i].getFrom()[0].toString());
                        try{
                            date=msg[i].getSentDate().toString();
                        }catch(Exception ex){date="Error";}
                        try{
                            from=msg[i].getFrom()[0].toString();
                        }catch(Exception ex){from="Error";}
                        try{
                            subject=msg[i].getSubject();
                            if(subject==null)
                                subject="[Không có tiêu đề]";
                        }catch(Exception e){subject="Error";}
                        AddRowTable(stt,from,subject,date);
                        stt++;
                    }
                }
                
                catch (MessagingException e) 
                {
                    JOptionPane.showMessageDialog(null, "Bạn vui lòng kiểm tra lại kết nối internet hoặc thông tin đăng nhập", "Đã có lỗi xảy ra", JOptionPane.ERROR_MESSAGE);
                }
                jButton3.setEnabled(true);
                SearchTxt.setEnabled(true);
                if(!SearchTxt.getText().contains("Nhập vào nội dung cần tìm..."))
                    SearchBtn.setEnabled(true);
                flagSearch=flg;
                }
            }
        }).start();
    }
    
    private void ReceiveMail()
    {
        ReceiveMail(false);
    }
    
    private void SearchMail()
    {
        ReceiveMail(true);
    }
    
    //SendMail
    private void SendMail()
    {
        this.setTitle("Đang gửi thư đến "+mailto.getText()+"...Vui lòng chờ trong giây lát...");
        new Thread(new Runnable(){
            @Override
            public void run(){
                jProgressBar1.setVisible(true);
                jButton2.setEnabled(false);
                mailto.setEnabled(false);
                mailBCC.setEnabled(false);
                mailCC.setEnabled(false);
                tieude.setEnabled(false);
                noidung.setEnabled(false);
                tepdinhkem.setEnabled(false);
                jButton5.setEnabled(false);
                jCheckBox1.setEnabled(false);
                jCheckBox2.setEnabled(false);
                jLabel3.setText("Đang gửi thư...");
                jLabel3.setVisible(true);
                jButton6.setEnabled(false);
                
                Properties props = new Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", smtpserver);
                props.put("mail.smtp.port", smtpport);
                Session session = Session.getInstance(props,
                    new javax.mail.Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(username,password);
                    }
                });
                
                try 
                {
                    Message message = new MimeMessage(session);
                    
                    message.setFrom(new InternetAddress(username));
                    message.setRecipients(Message.RecipientType.TO,
                                        InternetAddress.parse(mailto.getText()));
                    
                    if(mailBCC.isEnabled()==true && mailBCC.getText()!="")
                        message.setRecipients(Message.RecipientType.BCC,InternetAddress.parse(mailBCC.getText()));
                    
                    if(mailCC.isEnabled()==true && mailCC.getText()!="")
                        message.setRecipients(Message.RecipientType.CC,InternetAddress.parse(mailCC.getText()));
                    
                    message.setSubject(tieude.getText());
                    
                    if(!tepdinhkem.getText().equals(""))
                    {
                        Multipart multipart=new MimeMultipart();
//                        DataSource source = new FileDataSource(tepdinhkem.getText());
//                        message.setDataHandler(new DataHandler(source));
                        MimeBodyPart messageBodyPart = new MimeBodyPart();
                        messageBodyPart.setContent(noidung.getText(), "text/html; charset=UTF-8");
                        MimeBodyPart attachPart = new MimeBodyPart();
                        attachPart.attachFile(tepdinhkem.getText());
                        multipart.addBodyPart(attachPart);
                        multipart.addBodyPart(messageBodyPart);
                        message.setContent(multipart);
                    }
                    else
                        message.setContent(noidung.getText(),"text/html; charset=UTF-8");
                    Transport.send(message);
                    JOptionPane.showMessageDialog(null, "Đã gửi thư thành công", "Gửi thành công", JOptionPane.INFORMATION_MESSAGE);
                }
                catch (MessagingException e) {
                    JOptionPane.showMessageDialog(null, "Đã có lỗi xảy ra, chương trình không thể gửi được thư. Bạn vui lòng kiểm tra lại thông tin tài khoản và kết nối Internet","Không thể gửi Mail",JOptionPane.ERROR_MESSAGE);
                } catch (IOException ex) {
                    Logger.getLogger(EmailReceiveForm.class.getName()).log(Level.SEVERE, null, ex);
                } 
                mailto.setEnabled(true);
                mailBCC.setEnabled(true);
                mailCC.setEnabled(true);
                tieude.setEnabled(true);
                noidung.setEnabled(true);
                jProgressBar1.setVisible(false);
                jButton2.setEnabled(true);
                tepdinhkem.setEnabled(true);
                jButton5.setEnabled(true);
                jCheckBox1.setEnabled(true);
                jCheckBox2.setEnabled(true);
                jLabel3.setVisible(false);
                jButton6.setEnabled(true);
            }
        }).start();
       this.setTitle(username+" | Send & Receive eMail Application");
    }
    
    private void DeleteMail()
    {
        Message msgCon;
        if(this.stt!=-1)
        {
            try
            {
                msgCon=msg[msg.length-this.stt];
                msgCon.setFlag(Flags.Flag.DELETED,true);
                JOptionPane.showMessageDialog(null, "Đã xóa thư thành công");
            }
            catch(Exception ex)
            {
                JOptionPane.showMessageDialog(null, "Chương trình không thể xóa thư", "Đã có lỗi xảy ra", JOptionPane.ERROR_MESSAGE);
            }
            this.stt=-1;
        }
        else
            JOptionPane.showMessageDialog(null, "Chương trình không thể xóa thư", "Đã có lỗi xảy ra", JOptionPane.ERROR_MESSAGE);
    }
    
    private void ShowListFolder()
    {
        new Thread(new Runnable(){
            @Override
            public void run(){
                synchronized(this){
                
                try
                {
                    DefaultListModel dls=new DefaultListModel();
                    Folder f[]=store.getDefaultFolder().list();
                    for(Folder fd:f)
                        dls.addElement(fd.getName());
                    jList1.setModel(dls);
                }
                catch(Exception ex){

                }
                }
            }
        }).start();
    } 
    
    public EmailReceiveForm(String username,String password,String smtpserver,String smtpport,String imapserver,String imapport,boolean ssl){
        ImageIcon img=new ImageIcon("email.png");
        this.setIconImage(img.getImage());
        
        this.username=username;
        this.password=password;
        this.smtpserver=smtpserver;
        this.smtpport=smtpport;
        this.imapserver=imapserver;
        this.imapport=imapport;
        this.ssl=ssl;
        
        initComponents();
        ConnectIMAP();
        ShowListFolder();
        this.FolderName="Inbox";
        ReceiveMail();

        this.mailBCC.setEnabled(false);
        this.mailCC.setEnabled(false);
        this.jButton4.setEnabled(false);
        this.jButton1.setEnabled(false);
        this.jButton3.setEnabled(false);
        this.tepdinhkem.setText("");
        
    }
    
    public EmailReceiveForm() {
        ImageIcon img=new ImageIcon("email.png");
        this.setIconImage(img.getImage());
        //this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        initComponents();
        
    }  
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        buttonGroup2 = new javax.swing.ButtonGroup();
        jScrollPane4 = new javax.swing.JScrollPane();
        jEditorPane1 = new javax.swing.JEditorPane();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        mailto = new javax.swing.JTextField();
        tieude = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        noidung = new javax.swing.JTextArea();
        mailBCC = new javax.swing.JTextField();
        mailCC = new javax.swing.JTextField();
        jCheckBox1 = new javax.swing.JCheckBox();
        jCheckBox2 = new javax.swing.JCheckBox();
        jLabel1 = new javax.swing.JLabel();
        tepdinhkem = new javax.swing.JTextField();
        jButton5 = new javax.swing.JButton();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        jButton6 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jProgressBar1 = new javax.swing.JProgressBar();
        jLabel3 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        AllReadRadio = new javax.swing.JRadioButton();
        UnReadRadio = new javax.swing.JRadioButton();
        NumMailSpinner = new javax.swing.JSpinner();
        jLabel7 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jList1 = new javax.swing.JList();
        jLabel4 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        SearchTxt = new javax.swing.JTextField();
        SearchBtn = new javax.swing.JButton();
        menuBar = new javax.swing.JMenuBar();
        editMenu = new javax.swing.JMenu();
        pasteMenuItem = new javax.swing.JMenuItem();
        deleteMenuItem = new javax.swing.JMenuItem();
        jMenuItem1 = new javax.swing.JMenuItem();

        jScrollPane4.setViewportView(jEditorPane1);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Send & Receive Application");
        setBackground(java.awt.SystemColor.activeCaption);
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        setFocusable(false);
        setMinimumSize(new java.awt.Dimension(640, 660));
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowActivated(java.awt.event.WindowEvent evt) {
                formWindowActivated(evt);
            }
        });

        jTabbedPane1.setBackground(java.awt.SystemColor.activeCaption);
        jTabbedPane1.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N

        jPanel1.setBackground(java.awt.SystemColor.activeCaption);

        jPanel4.setBackground(java.awt.SystemColor.activeCaption);
        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Điền vào thông tin", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 3, 11))); // NOI18N
        jPanel4.setPreferredSize(new java.awt.Dimension(550, 530));

        jLabel2.setFont(new java.awt.Font("Tahoma", 3, 12)); // NOI18N
        jLabel2.setText("Gửi đến email:");

        jLabel5.setFont(new java.awt.Font("Tahoma", 3, 12)); // NOI18N
        jLabel5.setText("Tiêu đề:");

        mailto.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mailtoActionPerformed(evt);
            }
        });
        mailto.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                mailtoKeyPressed(evt);
            }
            public void keyReleased(java.awt.event.KeyEvent evt) {
                mailtoKeyReleased(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                mailtoKeyTyped(evt);
            }
        });

        tieude.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                tieudeKeyPressed(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                tieudeKeyTyped(evt);
            }
        });

        jLabel6.setFont(new java.awt.Font("Tahoma", 3, 12)); // NOI18N
        jLabel6.setText("Nội dung:");

        noidung.setColumns(20);
        noidung.setRows(5);
        noidung.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                noidungFocusGained(evt);
            }
        });
        noidung.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                noidungKeyPressed(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                noidungKeyTyped(evt);
            }
        });
        jScrollPane3.setViewportView(noidung);

        mailBCC.setEnabled(false);
        mailBCC.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mailBCCActionPerformed(evt);
            }
        });
        mailBCC.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                mailBCCKeyPressed(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                mailBCCKeyTyped(evt);
            }
        });

        mailCC.setEnabled(false);
        mailCC.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mailCCActionPerformed(evt);
            }
        });
        mailCC.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                mailCCKeyPressed(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                mailCCKeyTyped(evt);
            }
        });

        jCheckBox1.setPreferredSize(new java.awt.Dimension(20, 20));
        jCheckBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox1ActionPerformed(evt);
            }
        });

        jCheckBox2.setPreferredSize(new java.awt.Dimension(20, 20));
        jCheckBox2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox2ActionPerformed(evt);
            }
        });

        jLabel1.setFont(new java.awt.Font("Tahoma", 3, 11)); // NOI18N
        jLabel1.setText("Tệp đính kèm:");

        tepdinhkem.setEditable(false);
        tepdinhkem.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tepdinhkemMouseClicked(evt);
            }
        });
        tepdinhkem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tepdinhkemActionPerformed(evt);
            }
        });
        tepdinhkem.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                tepdinhkemKeyPressed(evt);
            }
        });

        jButton5.setIcon(new javax.swing.ImageIcon(getClass().getResource("/emailreceive/res/open_folder.png"))); // NOI18N
        jButton5.setText("Browser...");
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });

        jLabel10.setFont(new java.awt.Font("Tahoma", 3, 11)); // NOI18N
        jLabel10.setText("BCC:");

        jLabel16.setFont(new java.awt.Font("Tahoma", 3, 11)); // NOI18N
        jLabel16.setText("CC:");

        jButton6.setIcon(new javax.swing.ImageIcon(getClass().getResource("/emailreceive/res/clipboard.png"))); // NOI18N
        jButton6.setText("Lấy nội dung từ Clipboard");
        jButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton6ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(jPanel4Layout.createSequentialGroup()
                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                            .addGroup(jPanel4Layout.createSequentialGroup()
                                                .addComponent(jLabel16)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(jCheckBox2, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE))
                                            .addGroup(jPanel4Layout.createSequentialGroup()
                                                .addComponent(jLabel10)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(jCheckBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                        .addGap(6, 6, 6))
                                    .addGroup(jPanel4Layout.createSequentialGroup()
                                        .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(mailBCC, javax.swing.GroupLayout.DEFAULT_SIZE, 448, Short.MAX_VALUE)
                                    .addComponent(mailto)))
                            .addGroup(jPanel4Layout.createSequentialGroup()
                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                    .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 28, Short.MAX_VALUE)
                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(mailCC, javax.swing.GroupLayout.DEFAULT_SIZE, 448, Short.MAX_VALUE)
                                    .addComponent(tieude)
                                    .addComponent(jButton6))))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel15, javax.swing.GroupLayout.DEFAULT_SIZE, 25, Short.MAX_VALUE)
                            .addComponent(jLabel14, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel13, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel12, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel11, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jScrollPane3)
                        .addGap(9, 9, 9))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 36, Short.MAX_VALUE)
                        .addComponent(tepdinhkem, javax.swing.GroupLayout.PREFERRED_SIZE, 325, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jButton5, javax.swing.GroupLayout.PREFERRED_SIZE, 103, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel18, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGap(4, 4, 4)
                        .addComponent(jLabel2))
                    .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(mailto, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel11, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGap(7, 7, 7)
                        .addComponent(jLabel12, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jCheckBox1, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(mailBCC, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel10)))
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(jLabel13, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 8, Short.MAX_VALUE)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(mailCC, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jCheckBox2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel16, javax.swing.GroupLayout.Alignment.TRAILING))
                        .addGap(7, 7, 7)))
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tieude, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5)
                    .addComponent(jLabel14, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel6)
                        .addComponent(jButton6))
                    .addComponent(jLabel15, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 293, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel18, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel1)
                        .addComponent(tepdinhkem, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jButton5)))
                .addContainerGap())
        );

        jButton2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/emailreceive/res/Mail-Send-icon.png"))); // NOI18N
        jButton2.setText("Gửi thư");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jProgressBar1.setIndeterminate(true);

        jLabel3.setText("Đang quét hộp thư...");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, 603, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jProgressBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 196, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 141, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 318, Short.MAX_VALUE)
                        .addComponent(jButton2)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, 507, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jProgressBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jButton2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        jPanel4.getAccessibleContext().setAccessibleName("Điền vào thông tin sau:");

        jTabbedPane1.addTab("Gửi thư", new javax.swing.ImageIcon(getClass().getResource("/emailreceive/res/email-send-icon.png")), jPanel1); // NOI18N

        jPanel2.setBackground(java.awt.SystemColor.activeCaption);
        jPanel2.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, "", null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "STT", "Người gửi", "Tiêu đề", "Ngày gửi"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jTable1.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jTable1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTable1MouseClicked(evt);
            }
        });
        jScrollPane2.setViewportView(jTable1);
        if (jTable1.getColumnModel().getColumnCount() > 0) {
            jTable1.getColumnModel().getColumn(0).setMinWidth(35);
            jTable1.getColumnModel().getColumn(0).setPreferredWidth(35);
            jTable1.getColumnModel().getColumn(0).setMaxWidth(35);
            jTable1.getColumnModel().getColumn(1).setPreferredWidth(150);
            jTable1.getColumnModel().getColumn(2).setPreferredWidth(200);
            jTable1.getColumnModel().getColumn(3).setPreferredWidth(175);
        }

        jPanel2.add(jScrollPane2, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 80, 460, 440));

        AllReadRadio.setBackground(java.awt.SystemColor.activeCaption);
        buttonGroup2.add(AllReadRadio);
        AllReadRadio.setFont(new java.awt.Font("Tahoma", 3, 11)); // NOI18N
        AllReadRadio.setSelected(true);
        AllReadRadio.setText("Tải tất cả các thư");
        AllReadRadio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AllReadRadioActionPerformed(evt);
            }
        });
        jPanel2.add(AllReadRadio, new org.netbeans.lib.awtextra.AbsoluteConstraints(140, 50, 130, -1));

        UnReadRadio.setBackground(java.awt.SystemColor.activeCaption);
        buttonGroup2.add(UnReadRadio);
        UnReadRadio.setFont(new java.awt.Font("Tahoma", 3, 11)); // NOI18N
        UnReadRadio.setText("Chỉ tải những thư chưa đọc");
        UnReadRadio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                UnReadRadioActionPerformed(evt);
            }
        });
        jPanel2.add(UnReadRadio, new org.netbeans.lib.awtextra.AbsoluteConstraints(420, 50, -1, -1));

        NumMailSpinner.setModel(new javax.swing.SpinnerNumberModel(10, 1, 50, 1));
        jPanel2.add(NumMailSpinner, new org.netbeans.lib.awtextra.AbsoluteConstraints(560, 10, 50, 30));

        jLabel7.setText("Số thư cần tải:");
        jPanel2.add(jLabel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(470, 20, -1, 20));

        jScrollPane1.setFont(new java.awt.Font("Tahoma", 3, 15)); // NOI18N
        jScrollPane1.setPreferredSize(new java.awt.Dimension(50, 128));

        jList1.setFont(new java.awt.Font("Tahoma", 3, 13)); // NOI18N
        jList1.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jList1.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jList1.setVisibleRowCount(-1);
        jList1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jList1MouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(jList1);

        jPanel2.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 80, 130, 440));

        jLabel4.setFont(new java.awt.Font("Tahoma", 3, 12)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(153, 0, 0));
        jLabel4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/emailreceive/res/inbox-icon.png"))); // NOI18N
        jLabel4.setText("Hộp thư:");
        jPanel2.add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 50, 110, 30));

        jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/emailreceive/res/1428157700_delete-16.png"))); // NOI18N
        jButton1.setText("Xóa Thư");
        jButton1.setPreferredSize(new java.awt.Dimension(105, 23));
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jPanel2.add(jButton1, new org.netbeans.lib.awtextra.AbsoluteConstraints(480, 530, 130, 30));

        jButton3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/emailreceive/res/Reload-icon.png"))); // NOI18N
        jButton3.setText("Tải lại hộp thư");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });
        jPanel2.add(jButton3, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 530, 130, 30));

        jButton4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/emailreceive/res/Status-mail-read-icon.png"))); // NOI18N
        jButton4.setText("Đọc Thư");
        jButton4.setPreferredSize(new java.awt.Dimension(105, 23));
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });
        jPanel2.add(jButton4, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 530, 130, 30));

        SearchTxt.setText("Nhập vào nội dung cần tìm...");
        SearchTxt.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                SearchTxtFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                SearchTxtFocusLost(evt);
            }
        });
        SearchTxt.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                SearchTxtMouseClicked(evt);
            }
        });
        SearchTxt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SearchTxtActionPerformed(evt);
            }
        });
        SearchTxt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                SearchTxtKeyPressed(evt);
            }
            public void keyReleased(java.awt.event.KeyEvent evt) {
                SearchTxtKeyReleased(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                SearchTxtKeyTyped(evt);
            }
        });
        jPanel2.add(SearchTxt, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, 310, 30));

        SearchBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/emailreceive/res/search-icon.png"))); // NOI18N
        SearchBtn.setText("Tìm kiếm thư");
        SearchBtn.setEnabled(false);
        SearchBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SearchBtnActionPerformed(evt);
            }
        });
        jPanel2.add(SearchBtn, new org.netbeans.lib.awtextra.AbsoluteConstraints(330, 10, 120, 30));

        jTabbedPane1.addTab("Hộp thư", new javax.swing.ImageIcon(getClass().getResource("/emailreceive/res/inbox-icon16.png")), jPanel2); // NOI18N

        editMenu.setMnemonic('e');
        editMenu.setText("File");

        pasteMenuItem.setMnemonic('p');
        pasteMenuItem.setText("Thay đổi tài khoản");
        pasteMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pasteMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(pasteMenuItem);

        deleteMenuItem.setMnemonic('d');
        deleteMenuItem.setText("Thoát chương trình");
        deleteMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(deleteMenuItem);

        jMenuItem1.setText("Giới thiệu");
        jMenuItem1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jMenuItem1MouseClicked(evt);
            }
        });
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        editMenu.add(jMenuItem1);

        menuBar.add(editMenu);

        setJMenuBar(menuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 602, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        setSize(new java.awt.Dimension(644, 662));
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void UnReadRadioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_UnReadRadioActionPerformed

    }//GEN-LAST:event_UnReadRadioActionPerformed

    private void AllReadRadioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AllReadRadioActionPerformed

    }//GEN-LAST:event_AllReadRadioActionPerformed

    private void jTable1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTable1MouseClicked
        // TODO add your handling code here:
        this.stt=Integer.parseInt(jTable1.getValueAt(jTable1.getSelectedRow(), 0).toString());
        
        if(evt.getClickCount()==2&&this.stt!=-1)
            ReadMail(this.stt);
        
        this.jButton4.setEnabled(true);
        //if(flagSearch==false)
            this.jButton1.setEnabled(true);
    }//GEN-LAST:event_jTable1MouseClicked

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        if("".equals(mailto.getText()))
            JOptionPane.showMessageDialog(null,"Vui lòng nhập vào địa chỉ email người nhận","Thiếu thông tin",JOptionPane.INFORMATION_MESSAGE);
        else if(tieude.getText()=="")
        {
            int reply = JOptionPane.showConfirmDialog(null, "Chưa nhập tiêu đề. Bạn có muốn tiếp tục gửi thư?","Tiêu đề email còn trống",JOptionPane.YES_NO_OPTION);
            if(reply==JOptionPane.YES_OPTION)
                SendMail();
        }
        else if(noidung.getText()=="")
        {
            int reply = JOptionPane.showConfirmDialog(null, "Chưa nhập nội dung. Bạn có muốn tiếp tục gửi thư?","Nội dung email còn trống",JOptionPane.YES_NO_OPTION);
            if(reply==JOptionPane.YES_OPTION)
                SendMail();
        }
        else
                SendMail();
    }//GEN-LAST:event_jButton2ActionPerformed

    private void pasteMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pasteMenuItemActionPerformed
        
        IntializeForm frame = new IntializeForm(username,password,smtpserver,smtpport,imapserver,imapport,ssl);
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        frame.pack();
        frame.setVisible(true);
        
        this.dispose();
    }//GEN-LAST:event_pasteMenuItemActionPerformed

    private void deleteMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteMenuItemActionPerformed
        // TODO add your handling code here:
        this.dispose();
    }//GEN-LAST:event_deleteMenuItemActionPerformed

    private void formWindowActivated(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowActivated
        // TODO add your handling code here:
        jProgressBar1.setVisible(false);
        jLabel3.setVisible(false);
        
        ShowListFolder();
    }//GEN-LAST:event_formWindowActivated

    private void jList1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jList1MouseClicked
        // TODO add your handling code here:
        String c=(String)jList1.getSelectedValue();
        this.FolderName=c;
        if(evt.getClickCount()==2)
            ReceiveMail();
    }//GEN-LAST:event_jList1MouseClicked

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // TODO add your handling code here:
        if(this.stt!=-1)
        {
            int reply= JOptionPane.showConfirmDialog(null,"Bạn có thật sự muốn xóa thư?","Xác nhận",JOptionPane.YES_NO_OPTION);
            if(reply==JOptionPane.YES_OPTION)
                try
                {
                    msg[msg.length-this.stt].setFlag(Flags.Flag.DELETED, true);
                    //folder.close(true);
                    JOptionPane.showMessageDialog(null,"Đã xóa thư thành công");
                    if(flagSearch==true)
                        SearchMail();
                    else
                        ReceiveMail();
                    this.stt=-1;
                }
                catch(Exception ex)
                {
                    JOptionPane.showMessageDialog(null,"Chương trình không thể xóa được thư", "Đã có lỗi xảy ra", JOptionPane.ERROR_MESSAGE);
                }
        }
        //DeleteMail();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        // TODO add your handling code here:
        ReceiveMail();
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        // TODO add your handling code here:
        if(this.stt!=-1)
            ReadMail(this.stt);
    }//GEN-LAST:event_jButton4ActionPerformed

    private void jMenuItem1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jMenuItem1MouseClicked
        // TODO add your handling code here:
        
    }//GEN-LAST:event_jMenuItem1MouseClicked

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        // TODO add your handling code here:
        JOptionPane.showMessageDialog(null,new About(),"About Send & Receive eMail Application",JOptionPane.OK_OPTION,new ImageIcon());
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        // TODO add your handling code here:
        new Thread(new Runnable()
            {
                @Override
                public void run(){
                    JFileChooser chooser = new JFileChooser();
                    chooser.setDialogTitle("Chọn file đính kèm");
                    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                    int result=chooser.showOpenDialog(null);
                    if(result==JFileChooser.APPROVE_OPTION)
                    {
                        String path=chooser.getSelectedFile().getAbsolutePath();
                        File file=new File(path);
                        if(file.length()>1048576)  //1 MB
                        {
                            JOptionPane.showMessageDialog(null, "Chương trình không thể gửi file có kích thước trên 1MB. Bạn vui lòng chọn lại file","Không thể chọn file",JOptionPane.WARNING_MESSAGE);
                            tepdinhkem.setText(null);
                            jLabel18.setIcon(null);
                        }
                        else
                        {
                            tepdinhkem.setText(path);
                            jLabel18.setIcon(new ImageIcon(getClass().getResource("/emailreceive/res/check.png")));
                        }
                    }
                    else if(result==JFileChooser.CANCEL_OPTION && tepdinhkem.getText().length()!=0)
                    {
                        int r = JOptionPane.showConfirmDialog(null,"Bạn muốn loại bỏ tệp đính kèm?", "Xác nhận",JOptionPane.YES_NO_OPTION);
                        if(r==JOptionPane.YES_OPTION)
                        {
                            tepdinhkem.setText(null);
                            jLabel18.setIcon(null);
                        }
                    }
                }
            }).start();
    }//GEN-LAST:event_jButton5ActionPerformed

    private void jCheckBox2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox2ActionPerformed
        // TODO add your handling code here:
        if(jCheckBox2.isSelected()==true)
        {
            jLabel13.setVisible(false);
            mailCC.setEnabled(true);
        }
        else
        {
            jLabel13.setVisible(false);
            mailCC.setEnabled(false);
        }
    }//GEN-LAST:event_jCheckBox2ActionPerformed

    private void jCheckBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox1ActionPerformed
        // TODO add your handling code here:
        if(jCheckBox1.isSelected()==true)
        {
            jLabel12.setVisible(true);
            mailBCC.setEnabled(true);
        }
        else
        {
            jLabel12.setVisible(false);
            mailBCC.setEnabled(false);
        }
    }//GEN-LAST:event_jCheckBox1ActionPerformed

    private void mailCCKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_mailCCKeyTyped
        // TODO add your handling code here:
        if(!mailCC.getText().contains("@") || !mailCC.getText().contains("."))
            jLabel13.setIcon(new javax.swing.ImageIcon(getClass().getResource("/emailreceive/res/error.png")));
        else
            jLabel13.setIcon(new javax.swing.ImageIcon(getClass().getResource("/emailreceive/res/check.png")));
        if(mailCC.getText().contains(";"))
            mailCC.setText(mailCC.getText().replace(";", ","));
    }//GEN-LAST:event_mailCCKeyTyped

    private void mailCCActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mailCCActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_mailCCActionPerformed

    private void mailBCCKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_mailBCCKeyTyped
        // TODO add your handling code here:
        if(!mailBCC.getText().contains("@") || !mailBCC.getText().contains("."))
            jLabel12.setIcon(new javax.swing.ImageIcon(getClass().getResource("/emailreceive/res/error.png")));
        else
            jLabel12.setIcon(new javax.swing.ImageIcon(getClass().getResource("/emailreceive/res/check.png")));
        if(mailBCC.getText().contains(";"))
            mailBCC.setText(mailBCC.getText().replace(";", ","));
    }//GEN-LAST:event_mailBCCKeyTyped

    private void mailBCCActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mailBCCActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_mailBCCActionPerformed

    private void noidungKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_noidungKeyTyped
        // TODO add your handling code here:
        if(noidung.getText().length()==0)
            jLabel15.setIcon(new javax.swing.ImageIcon(getClass().getResource("/emailreceive/res/error.png")));
        else
            jLabel15.setIcon(new javax.swing.ImageIcon(getClass().getResource("/emailreceive/res/check.png")));
    }//GEN-LAST:event_noidungKeyTyped

    private void tieudeKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tieudeKeyTyped
        // TODO add your handling code here:
        if(tieude.getText().length()==0)
            jLabel14.setIcon(new javax.swing.ImageIcon(getClass().getResource("/emailreceive/res/error.png")));
        else
            jLabel14.setIcon(new javax.swing.ImageIcon(getClass().getResource("/emailreceive/res/check.png")));
    }//GEN-LAST:event_tieudeKeyTyped

    private void mailtoKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_mailtoKeyTyped
        // TODO add your handling code here:
        if(!mailto.getText().contains("@") || !mailto.getText().contains("."))
            jLabel11.setIcon(new javax.swing.ImageIcon(getClass().getResource("/emailreceive/res/error.png")));
        else
            jLabel11.setIcon(new javax.swing.ImageIcon(getClass().getResource("/emailreceive/res/check.png")));
        
        if(mailto.getText().contains(";"))
            mailto.setText(mailto.getText().replace(";", ","));
        
    }//GEN-LAST:event_mailtoKeyTyped

    private void mailtoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mailtoActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_mailtoActionPerformed

    private void tepdinhkemKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tepdinhkemKeyPressed
        // TODO add your handling code here:
        
    }//GEN-LAST:event_tepdinhkemKeyPressed

    private void tepdinhkemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tepdinhkemActionPerformed
        // TODO add your handling code here:
        
    }//GEN-LAST:event_tepdinhkemActionPerformed

    private void tepdinhkemMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tepdinhkemMouseClicked
        // TODO add your handling code here:
        if(evt.getClickCount()==2)
            jButton5.doClick();
    }//GEN-LAST:event_tepdinhkemMouseClicked

    private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton6ActionPerformed
        // TODO add your handling code here:
        String data; 
        try { 
            data = (String) Toolkit.getDefaultToolkit()
                    .getSystemClipboard().getData(DataFlavor.stringFlavor);
            noidung.setText(data);
            
        } catch(Exception ex)
        {
            
        }
        finally
        {
            noidung.requestFocus();
        }
    }//GEN-LAST:event_jButton6ActionPerformed

    private void noidungFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_noidungFocusGained
        // TODO add your handling code here:
        if(noidung.getText().length()==0)
            jLabel15.setIcon(new javax.swing.ImageIcon(getClass().getResource("/emailreceive/res/error.png")));
        else
            jLabel15.setIcon(new javax.swing.ImageIcon(getClass().getResource("/emailreceive/res/check.png")));
    }//GEN-LAST:event_noidungFocusGained

    private void SearchBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SearchBtnActionPerformed
        // TODO add your handling code here:
        if(SearchTxt.getText().length()==0)
            JOptionPane.showMessageDialog(null, "Bạn chưa nhập nội dung tìm kiếm","Không thể tìm thư",JOptionPane.ERROR);
        else
        {
            this.flagSearch=true;
            ReceiveMail(flagSearch);
            //SearchMail();
        }
    }//GEN-LAST:event_SearchBtnActionPerformed

    private void SearchTxtMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_SearchTxtMouseClicked
        // TODO add your handling code here:
        if(SearchTxt.getText().contains("Nhập vào nội dung cần tìm...") && SearchTxt.isEnabled()==true)
            SearchTxt.setText("");
    }//GEN-LAST:event_SearchTxtMouseClicked

    private void SearchTxtKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_SearchTxtKeyPressed
        // TODO add your handling code here:
        if(evt.getKeyCode()==KeyEvent.VK_ENTER && SearchBtn.isEnabled()==true)
            SearchBtn.doClick();
    }//GEN-LAST:event_SearchTxtKeyPressed

    private void SearchTxtFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_SearchTxtFocusGained
        // TODO add your handling code here:
        
    }//GEN-LAST:event_SearchTxtFocusGained

    private void SearchTxtFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_SearchTxtFocusLost
        // TODO add your handling code here:
        if(SearchTxt.getText().length()==0)
        {
            SearchTxt.setText("Nhập vào nội dung cần tìm...");
            SearchBtn.setEnabled(false);
        }
    }//GEN-LAST:event_SearchTxtFocusLost

    private void SearchTxtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SearchTxtActionPerformed
        // TODO add your handling code here:
        
    }//GEN-LAST:event_SearchTxtActionPerformed

    private void SearchTxtKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_SearchTxtKeyTyped
        // TODO add your handling code here:
        
    }//GEN-LAST:event_SearchTxtKeyTyped

    private void SearchTxtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_SearchTxtKeyReleased
        // TODO add your handling code here:
        if(SearchTxt.getText().length()>0)
            SearchBtn.setEnabled(true);
        else
            SearchBtn.setEnabled(false);
    }//GEN-LAST:event_SearchTxtKeyReleased

    private void mailtoKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_mailtoKeyPressed
        // TODO add your handling code here:
        
    }//GEN-LAST:event_mailtoKeyPressed

    private void mailBCCKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_mailBCCKeyPressed
        // TODO add your handling code here:
    }//GEN-LAST:event_mailBCCKeyPressed

    private void mailCCKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_mailCCKeyPressed
        // TODO add your handling code here:
    }//GEN-LAST:event_mailCCKeyPressed

    private void tieudeKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tieudeKeyPressed
        // TODO add your handling code here:
    }//GEN-LAST:event_tieudeKeyPressed

    private void noidungKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_noidungKeyPressed
        // TODO add your handling code here:
    }//GEN-LAST:event_noidungKeyPressed

    private void mailtoKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_mailtoKeyReleased
        // TODO add your handling code here:
        
    }//GEN-LAST:event_mailtoKeyReleased
    
       public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(EmailReceiveForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(EmailReceiveForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(EmailReceiveForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(EmailReceiveForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new EmailReceiveForm().setVisible(true);
            }
        });

    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JRadioButton AllReadRadio;
    private javax.swing.JSpinner NumMailSpinner;
    private javax.swing.JButton SearchBtn;
    private javax.swing.JTextField SearchTxt;
    private javax.swing.JRadioButton UnReadRadio;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.JMenuItem deleteMenuItem;
    private javax.swing.JMenu editMenu;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JCheckBox jCheckBox2;
    private javax.swing.JEditorPane jEditorPane1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JList jList1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JProgressBar jProgressBar1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JTextField mailBCC;
    private javax.swing.JTextField mailCC;
    private javax.swing.JTextField mailto;
    private javax.swing.JMenuBar menuBar;
    public static javax.swing.JTextArea noidung;
    private javax.swing.JMenuItem pasteMenuItem;
    private javax.swing.JTextField tepdinhkem;
    private javax.swing.JTextField tieude;
    // End of variables declaration//GEN-END:variables
}