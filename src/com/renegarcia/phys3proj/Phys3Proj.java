package com.renegarcia.phys3proj;
import com.renegarcia.phys3proj.helper.MyAbstractTableModel;
import com.renegarcia.phys3proj.Fourier2.FourierBin;
import com.renegarcia.phys3proj.Fourier2.FourierResults;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.pc.IOIOPcApplicationHelper;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.Timer;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 *
 * @author Rene
 */
public class Phys3Proj extends javax.swing.JFrame implements ioio.lib.util.IOIOLooperProvider
{
    private File file;   
    private List<BinStateInfo> binStateListAll;
    private List<BinStateInfo> binStateListPulseOnly;

    private DataOutputStream outputStream;
    private Player player;
    private final PinAssigmentTableModel tableModel;
    private final int TOTAL_VISUAL_BINS = 256;
    private final int PIN_COUNT = 20;
    private final PinScheme pinScheme;
    private static final double DEFAULT_THRESHOLD = 15;
    
    private final IOIOPcApplicationHelper ioioHelper = new IOIOPcApplicationHelper(this);

   
    private final static String KEY_WINDOW_SIZE = "window.size";
    private final static String KEY_AMP = "amplification";
    private final static char DISP_MODE_PULSE = 'P', DISP_MODE_FULL = 'F';
    private char displayMode = DISP_MODE_PULSE;
    

  
    private final int barWidth = 15;
    private final int barSpacing = 6;
    
    private final int circleDiameter = 25;
    private final int circleSpacing = 35;
  

    public Phys3Proj()
    {
        initComponents();
        
        
        this.windowSizeCB.setSelectedIndex(1);
        
        File f = new File("previous.prop");
        if (f.exists())
        {
            try (FileInputStream fis = new FileInputStream(new File("previous.prop")))
            {
                Properties props = new Properties();
                props.load(fis);
                String property = props.getProperty("audio.file");
                if(property != null)
                {
                    File audioFile = new File(property);
                    if(audioFile.exists())
                        loadSourceFile(audioFile);
                }
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
            }
        }
       

   
        createBins();
        this.drawPanel.setPreferredSize( new Dimension(this.binStateListPulseOnly.size()*(circleSpacing) , drawPanel.getHeight()));
        
        tableModel = new PinAssigmentTableModel();
       
        
        pinScheme = new PinScheme();
        for(int i = 0; i < PIN_COUNT; i++)
            pinScheme.add(i+1, binStateListAll.get(i));    
      
        tableModel.setData(pinScheme);
        pinAssignTable.setModel(tableModel);
      
        
        setLocationRelativeTo(null);
        
        //connectToServer();
    }
    
    
    private void connectToServer()
    {
        try
        {
            Socket clientSocket = new Socket("localhost", 8888);
            outputStream = new DataOutputStream(clientSocket.getOutputStream());
            statusLbl.setText("Connected");
        }
        catch (Exception ex)
        {
            System.out.println(ex.getMessage());
            statusLbl.setText("Disconnected");
        }     
    }
    
    private void createBins()
    {          
        binStateListPulseOnly = new ArrayList<>();
        binStateListAll = new ArrayList<>();
        BinStateInfo vb;
        for (int i = 0; i < TOTAL_VISUAL_BINS; i++)
        {
            vb = new BinStateInfo(i);
            vb.setThreshold(DEFAULT_THRESHOLD);
            binStateListAll.add(vb);
            binStateListPulseOnly.add(vb);
        }
        
       
        
        
        
     
    }
   
    private void setupBinNames(int sampleRate, int N)
    {
        binStateListAll.get(0).setName("" + sampleRate / N / 4);
        for(int i = 1; i < TOTAL_VISUAL_BINS; i++)
            binStateListAll.get(i).setName( "" + i * sampleRate/ N);     
    }
    
    
    
    
    
    private void stopAllVisualBins()
    {
        if (binStateListAll != null)
        {
            for (BinStateInfo b : binStateListAll)
            {
                b.setOff();
                b.setLastMagnitude(0);
            }
            drawPanel.repaint();
        }
    }
    
    
    private int getMaxMagnitude(AudioFormat audioFormat, int N)
    {

        int bytesPerChannel = audioFormat.getFrameSize() / audioFormat.getChannels();
        int maxMagnitude;
        if (bytesPerChannel == 1)
            maxMagnitude = Byte.MAX_VALUE;
        else if (bytesPerChannel == 2)
            maxMagnitude = Short.MAX_VALUE;
        else
            maxMagnitude = Integer.MAX_VALUE;

        maxMagnitude = N / 2 * maxMagnitude;

        return maxMagnitude;
    }

    
    
    public void paintPulse(Graphics2D g2d)
    { 
        int x;
        int y;
        
    
 
        int panelWidth = drawPanel.getWidth();
        int panelHeight = drawPanel.getHeight();
        y = (panelHeight / 2);
        int xOffSet = (panelWidth - binStateListPulseOnly.size() * circleSpacing) / 2;
        
        String name;
        int x2;
        for(int i = 0; i < binStateListPulseOnly.size(); i++)
        {
            x = xOffSet + circleSpacing * i;
            if (binStateListPulseOnly.get(i).isOn())
                g2d.fillOval(x, y, circleDiameter, circleDiameter);
            
            g2d.setColor(Color.red);
            name = binStateListPulseOnly.get(i).getName();
            if(name.length() == 2)
                x2 = x + 6;
            else
                x2 = x + 3;
            
            g2d.drawString(name, x2, y+16);
            
            g2d.rotate(-Math.PI / 2);

            g2d.drawString("Bin " + i, -y - 60, x + 15);
            g2d.rotate(Math.PI / 2);
            
            g2d.setColor(Color.GREEN);
        }

    }
    
    
    
    
    //<editor-fold defaultstate="collapsed" desc=" sendData() ">
    
    public void sendData() throws IOException
    {
        List<Byte> list = new ArrayList<>();

        byte b;
        BinStateInfo bin;

        for (int i = 0; i < PIN_COUNT; i++)
        {

            bin = pinScheme.get(i + 1);

            //bin is null when it is not associated to a pin
            if (bin == null)
                continue;

            if (bin.isOn())
                b = 48;
            else
                b = 32;

            //todo
            // int pinInt = Phys3Proj.PINNUMBER_PINALIAS_MAP.getBackward( elementList.get(i).pin );
            // byte pin = (byte) pinInt;         
            // b = (byte) (b | pin);
            // list.add(b);        
        }

        byte[] bytes = new byte[list.size()];
        for (int i = 0; i < list.size(); i++)
        {
            bytes[i] = list.get(i);
        }

        outputStream.write(bytes);
    }



    //</editor-fold>


    public void paintFull(Graphics2D g2d)
    {        
        int maxMag = player.maxMagnitude;
        BinStateInfo b;
        double val;

        int yOffset = drawPanel.getHeight() - 40;

        double percent;
        for (int i = 1; i < binStateListAll.size(); i++)
        {
            b = binStateListAll.get(i);
            
            percent = b.getLastMagnitude() / (1.000 * maxMag);
            val = 10000 * percent + 1;
         
            //val is 4 at max
            double log = Math.log(val);
            
            log = log - .20;
          
            if (log < 0)
                log = 0;
            
            int mag = (int) (log * 50);
        

            
            int width = 8;
            int spacing = 8;
            
            int x = i * (width+spacing) + 10;
            int y = yOffset - mag;
            
            int height = mag;

            if ((i+1) % 2 == 0)
            {
                g2d.setColor(Color.red);
                Font f = new Font("Arial", Font.PLAIN, 10);
                g2d.setFont(f);
                g2d.rotate(-Math.PI / 2);

                g2d.drawString(b.getName(), -yOffset - 20, i * (width+spacing) + 16);
                g2d.rotate(Math.PI / 2);

            }

            g2d.setColor(Color.GREEN);
            g2d.fillRect(x, y, width, height);
            
        }
    }
    
    
    
    
      public void paintFull2(Graphics2D g2d)
    {        
        int maxMag = player.maxMagnitude;
        BinStateInfo b;
        double val;

        int yOffset = drawPanel.getHeight() - 40;
        

        double percent;
        for (int i = 1; i < binStateListAll.size(); i++)
        {
            b = binStateListAll.get(i);
            
            percent = b.getLastMagnitude() / (1.000 * maxMag);
            val = 10000 * percent + 1;
         
            //val is 4 at max
            double log = Math.log(val);
            
            log = log - .20;
          
            if (log < 0)
                log = 0;
            
            int mag = (int) (log * 50);
        

            
    
            
            int x = i * (barWidth+barSpacing) + 10;
            int y = yOffset - mag;
            
            int height = mag;

            if ((i+1) % 2 == 0)
            {
                g2d.setColor(Color.red);
                Font f = new Font("Arial", Font.PLAIN, 10);
                g2d.setFont(f);
                g2d.rotate(-Math.PI / 2);

                g2d.drawString(b.getName(), -yOffset - 30, i * (barWidth+barSpacing) + 18);
                g2d.rotate(Math.PI / 2);

            }

            g2d.setColor(Color.GREEN);
            g2d.fillRect(x, y, barWidth, height);
            
        }
    }
    
    
    
    public void paintPartial(Graphics g2d)
    {       
        List<Integer> magList = new ArrayList<>();
      
      
        int size = magList.size();
        
        
        for (int i = 0; i < size ; i++)
        {
            int linearMagnitude = magList.get(i) ;
     
            double log =  Math.log(linearMagnitude /100 + 1);
             
            int xSpacing = 32;
            int ySpacing = 10;
            int xOffset = 20;       
            int yOffset = 40;     
            
            
            double k = 0;
            for(int j = 0; k < log  - 5.5 ; j++, k +=.20)
            {
                int x = i * xSpacing + xOffset;
                int y = 600 - j*ySpacing + yOffset;
                int width = 30;
                int height = 8;
                System.out.printf("mag = %d, log %.2f, x = %d, y = %d, width %d, height %d\n",linearMagnitude, log, x ,y ,width, height);
                g2d.fillRect(x, y, width, height);
            }
            System.out.printf("\n");
            
         
           
        }
        System.out.printf("\n");
     
    }
   
    public void draw(Graphics2D g2d)
    {
        g2d.setColor(Color.GREEN);

        if (displayMode == DISP_MODE_PULSE)
        {
            if (binStateListAll != null)
                paintPulse(g2d);
        }
        else
        {
            //paintFull(g2d);
            paintFull2(g2d);
        }
        
    }

    
    
    private boolean loadSourceFile(File f)
    {
        boolean success = false;
        if(file == null)
        {
            sourceLbl.setText("???");
            sampleRateLbl.setText("???");
            startBtn.setEnabled(false);
        }
        
        try
        {
            AudioFileFormat format = AudioSystem.getAudioFileFormat(f);
            float sampleRate = format.getFormat().getSampleRate();
            sampleRateLbl.setText(String.format("%.0f hz", sampleRate));
            sourceLbl.setText(f.getName());
            file = f;
            startBtn.setEnabled(true);
            success = true;

        }
        catch (UnsupportedAudioFileException | IOException ex)
        {
          
                ex.printStackTrace();
            String message = ex.getMessage();
            if (ex instanceof UnsupportedAudioFileException)
                message = message + "\n" + "Make sure this is a valid '.wav' file. ";

            JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);


        }
        
        return success;
    }
    
    

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        buttonGroup1 = new javax.swing.ButtonGroup();
        displayModeButtonGroup = new javax.swing.ButtonGroup();
        bottomPanel = new javax.swing.JPanel();
        startBtn = new javax.swing.JButton();
        stopBtn = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        statusLbl = new javax.swing.JLabel();
        rightPanel = new javax.swing.JPanel();
        topPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        windowSizeCB = new javax.swing.JComboBox<>();
        jLabel3 = new javax.swing.JLabel();
        ampCB = new javax.swing.JComboBox<>();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        pinAssignTable = new javax.swing.JTable();
        jPanel1 = new javax.swing.JPanel();
        fileRB = new javax.swing.JRadioButton();
        micRB = new javax.swing.JRadioButton();
        browseSourceBtn = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        sourceLbl = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        sampleRateLbl = new javax.swing.JLabel();
        pulseRB = new javax.swing.JRadioButton();
        jLabel6 = new javax.swing.JLabel();
        fullRB = new javax.swing.JRadioButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        drawPanel = new javax.swing.JPanel()
        {
            public void paintComponent(Graphics g)
            {
                super.paintComponent(g);
                draw((Graphics2D)g);
            }
        };
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        importMenuItem = new javax.swing.JMenuItem();
        exportMenuItem = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        exitMenuItem = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Physics 3 Project");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                formWindowClosing(evt);
            }
            public void windowOpened(java.awt.event.WindowEvent evt)
            {
                formWindowOpened(evt);
            }
        });

        bottomPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        startBtn.setText("Start");
        startBtn.setEnabled(false);
        startBtn.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                startBtnActionPerformed(evt);
            }
        });

        stopBtn.setText("Stop");
        stopBtn.setEnabled(false);
        stopBtn.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                stopBtnActionPerformed(evt);
            }
        });

        jLabel4.setText("Connection Status");

        statusLbl.setText("???????????");

        javax.swing.GroupLayout bottomPanelLayout = new javax.swing.GroupLayout(bottomPanel);
        bottomPanel.setLayout(bottomPanelLayout);
        bottomPanelLayout.setHorizontalGroup(
            bottomPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, bottomPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(statusLbl, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(startBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 122, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(stopBtn)
                .addContainerGap())
        );

        bottomPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {startBtn, stopBtn});

        bottomPanelLayout.setVerticalGroup(
            bottomPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(bottomPanelLayout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(bottomPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(startBtn)
                    .addComponent(stopBtn))
                .addContainerGap(23, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, bottomPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(bottomPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(statusLbl))
                .addContainerGap())
        );

        topPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel1.setText("Window Size:");
        jLabel1.setToolTipText("<html>Base frequency will be determined by (frequency of sample rate) / (window size).<br>Smaller windows have higher time resolution but lower frequency resolution.<br>Larger windows have higher frequency resolution but lower time resolution</html>");

        windowSizeCB.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "512", "1024", "2048", "4096" }));

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel3.setText("Source Amplification:");
        jLabel3.setToolTipText("Amplifies the signal.  Just in case the source line is too low.");

        ampCB.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "0", "10", "20", "30", "40", "50", "60", "70", "80", "90", "100" }));

        javax.swing.GroupLayout topPanelLayout = new javax.swing.GroupLayout(topPanel);
        topPanel.setLayout(topPanelLayout);
        topPanelLayout.setHorizontalGroup(
            topPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(topPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(topPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 152, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 123, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(10, 10, 10)
                .addGroup(topPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(ampCB, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(windowSizeCB, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        topPanelLayout.setVerticalGroup(
            topPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(topPanelLayout.createSequentialGroup()
                .addGap(19, 19, 19)
                .addGroup(topPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(windowSizeCB, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(topPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(ampCB, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(20, Short.MAX_VALUE))
        );

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Pin Assignment"));

        pinAssignTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][]
            {
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null}
            },
            new String []
            {
                "Pin", "Bin", "Threshold"
            }
        )
        {
            Class[] types = new Class []
            {
                java.lang.String.class, java.lang.Integer.class, java.lang.Double.class
            };
            boolean[] canEdit = new boolean []
            {
                false, true, true
            };

            public Class getColumnClass(int columnIndex)
            {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex)
            {
                return canEdit [columnIndex];
            }
        });
        jScrollPane1.setViewportView(pinAssignTable);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 208, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Mode"));

        buttonGroup1.add(fileRB);
        fileRB.setSelected(true);
        fileRB.setText("File");
        fileRB.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                rbActionPerformed(evt);
            }
        });

        buttonGroup1.add(micRB);
        micRB.setText("Mic");
        micRB.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                rbActionPerformed(evt);
            }
        });

        browseSourceBtn.setText("Browse Source");
        browseSourceBtn.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                browseSourceBtnActionPerformed(evt);
            }
        });

        jLabel2.setText("Source:");

        sourceLbl.setText("?");

        jLabel5.setText("Sample Rate:");

        sampleRateLbl.setText("?");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(browseSourceBtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(32, 32, 32)
                        .addComponent(fileRB, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(micRB)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel2)
                            .addComponent(jLabel5))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(sourceLbl, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(sampleRateLbl, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(fileRB)
                    .addComponent(micRB))
                .addGap(18, 18, 18)
                .addComponent(browseSourceBtn)
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(sourceLbl))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(sampleRateLbl))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout rightPanelLayout = new javax.swing.GroupLayout(rightPanel);
        rightPanel.setLayout(rightPanelLayout);
        rightPanelLayout.setHorizontalGroup(
            rightPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(rightPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(rightPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(topPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        rightPanelLayout.setVerticalGroup(
            rightPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(rightPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(topPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        displayModeButtonGroup.add(pulseRB);
        pulseRB.setSelected(true);
        pulseRB.setText("Pulse");
        pulseRB.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                displayModeRBActionPerformed(evt);
            }
        });

        jLabel6.setText("Display Mode:");

        displayModeButtonGroup.add(fullRB);
        fullRB.setText("Full");
        fullRB.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                displayModeRBActionPerformed(evt);
            }
        });

        drawPanel.setBackground(new java.awt.Color(51, 51, 51));
        drawPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        drawPanel.setPreferredSize(new java.awt.Dimension(4500, 2));

        javax.swing.GroupLayout drawPanelLayout = new javax.swing.GroupLayout(drawPanel);
        drawPanel.setLayout(drawPanelLayout);
        drawPanelLayout.setHorizontalGroup(
            drawPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 4498, Short.MAX_VALUE)
        );
        drawPanelLayout.setVerticalGroup(
            drawPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        jScrollPane2.setViewportView(drawPanel);

        jMenu1.setText("File");

        importMenuItem.setText("Import Settings");
        importMenuItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                importMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(importMenuItem);

        exportMenuItem.setText("Export Settings");
        exportMenuItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                exportMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(exportMenuItem);
        jMenu1.add(jSeparator1);

        exitMenuItem.setText("Exit");
        exitMenuItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                exitMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(exitMenuItem);

        jMenuBar1.add(jMenu1);

        jMenu2.setText("About");
        jMenuBar1.add(jMenu2);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(bottomPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel6)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(pulseRB, javax.swing.GroupLayout.PREFERRED_SIZE, 63, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(fullRB)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 767, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(rightPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(4, 4, 4)
                        .addComponent(rightPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(11, 11, 11)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(pulseRB)
                            .addComponent(jLabel6)
                            .addComponent(fullRB))
                        .addGap(7, 7, 7)
                        .addComponent(jScrollPane2)))
                .addGap(18, 18, 18)
                .addComponent(bottomPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(10, 10, 10))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void startBtnActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_startBtnActionPerformed
    {//GEN-HEADEREND:event_startBtnActionPerformed
              
        try
        {
            if(player != null)
                player.end();

            fileRB.setEnabled(false);
            micRB.setEnabled(false);
            startBtn.setEnabled(false);
            stopBtn.setEnabled(true);
            browseSourceBtn.setEnabled(false);

            player = new Player();
            player.start(); 
        }
        catch(Exception ex)
        {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
                             
       
     
    }//GEN-LAST:event_startBtnActionPerformed

    private void stopBtnActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_stopBtnActionPerformed
    {//GEN-HEADEREND:event_stopBtnActionPerformed
    
        if(player != null)
            player.end();
     
        fileRB.setEnabled(true);
        micRB.setEnabled(true);
        startBtn.setEnabled(true);
        if(fileRB.isSelected())
            browseSourceBtn.setEnabled(true);
    }//GEN-LAST:event_stopBtnActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt)//GEN-FIRST:event_formWindowClosing
    {//GEN-HEADEREND:event_formWindowClosing
     
        stopAllVisualBins();
        new Thread(()-> 
        {
            
           ioioHelper.stop();
            System.out.printf("stop endded\n");
        }
        ).start();

    }//GEN-LAST:event_formWindowClosing

    private void browseSourceBtnActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_browseSourceBtnActionPerformed
    {//GEN-HEADEREND:event_browseSourceBtnActionPerformed
   
        JFileChooser jfc = new JFileChooser(new File("."));
        jfc.setDialogTitle("Choose a file");
        jfc.setDialogType(JFileChooser.FILES_ONLY);
        jfc.setFileFilter(new FileFilter()
        {
            @Override
            public boolean accept(File f)
            {
                return f.getAbsolutePath().endsWith(".wav");
            }

            @Override
            public String getDescription()
            {
                return "WAV files only";
            }
        });

        int ret = jfc.showOpenDialog(this);

        if (ret != JFileChooser.APPROVE_OPTION)
        {
            System.out.printf("returning\n");
            return;
            
        }

        File f = jfc.getSelectedFile();
        boolean successful = loadSourceFile(f);

        if (successful)
        {
            Properties props = new Properties();
            props.setProperty("audio.file", f.getAbsolutePath());
            try (FileOutputStream fos = new FileOutputStream(new File("previous.prop")))
            {
                props.store(fos, "");
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
            }
           
        }

               
           
    
       
       
      
    }//GEN-LAST:event_browseSourceBtnActionPerformed

    private void exportMenuItemActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_exportMenuItemActionPerformed
    {//GEN-HEADEREND:event_exportMenuItemActionPerformed
        JFileChooser jfc = new JFileChooser();
        jfc.setDialogType(JFileChooser.SAVE_DIALOG);
        jfc.setCurrentDirectory(new File("."));
        //set a default filename (this is where you default extension first comes in)
        jfc.setSelectedFile(new File("preset.txt"));
        //Set an extension filter, so the user sees other XML files
        jfc.setFileFilter(new FileNameExtensionFilter("preset file", "txt"));
        int rv = jfc.showSaveDialog(this);
        if(rv == JFileChooser.CANCEL_OPTION)
            return;
        
        Properties props = new Properties();
        props.put(KEY_WINDOW_SIZE, "" + this.windowSizeCB.getSelectedIndex());
        props.put(KEY_AMP, "" + this.ampCB.getSelectedIndex());
       
    
        String str;
  
        for(int i = 0; i < PIN_COUNT; i++)
        {
            int pinNo = i + 1;
            BinStateInfo bsi  = pinScheme.get(pinNo);
            if(bsi != null)
               str = bsi.getBinNumber() + ":" + bsi.getThreshold();
            else
               str = -1 + ":" + -1;
            
           
            props.put("pin"+pinNo, str);
           
        }
        try(FileOutputStream fos = new FileOutputStream(jfc.getSelectedFile()))
        {
            props.store(fos, "");
        }
        catch(IOException ex)
        {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_exportMenuItemActionPerformed

    private void importMenuItemActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_importMenuItemActionPerformed
    {//GEN-HEADEREND:event_importMenuItemActionPerformed
        JFileChooser jfc = new JFileChooser();
        jfc.setDialogType(JFileChooser.OPEN_DIALOG);
        jfc.setCurrentDirectory(new File("."));
         jfc.setFileFilter(new FileNameExtensionFilter("preset file", "txt"));
        int rv = jfc.showOpenDialog(this);
        if(rv == JFileChooser.CANCEL_OPTION)
            return;
        
        Properties props = new Properties();
        try(FileInputStream fis = new FileInputStream(jfc.getSelectedFile()))
        {
            
            props.load(fis);
            
            String val = props.getProperty(KEY_WINDOW_SIZE);
            windowSizeCB.setSelectedIndex((Integer.parseInt(val)));
            
            val = props.getProperty(KEY_AMP);
            ampCB.setSelectedIndex(Integer.parseInt(val));

          
    
            int binNo;
            double thresh;
            BinStateInfo bin;
            for (int i = 0; i < PIN_COUNT; i++)
            {
                int pinNo = i + 1;
                val = props.getProperty("pin" + pinNo  );
                if(val == null)
                    continue;
   
                binNo = Integer.parseInt( val.split(":")[0] );
                
                if(binNo == -1)
                    continue;
                
                thresh = Double.parseDouble( val.split(":")[1] );
                
                bin = binStateListAll.get(binNo);
                bin.setThreshold(thresh);
                pinScheme.add(pinNo, bin);
           
            }
            tableModel.fireTableDataChanged();
            
            
        }
        catch(IOException ex)
        {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
       
      
    }//GEN-LAST:event_importMenuItemActionPerformed

    private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_exitMenuItemActionPerformed
    {//GEN-HEADEREND:event_exitMenuItemActionPerformed
      this.dispose();
    }//GEN-LAST:event_exitMenuItemActionPerformed

    private void rbActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_rbActionPerformed
    {//GEN-HEADEREND:event_rbActionPerformed
       if(fileRB.isSelected())
       {
          browseSourceBtn.setEnabled(true);
          if(file != null && file.exists())
            loadSourceFile(file);
       }
       else
       {
           browseSourceBtn.setEnabled(false);
           sourceLbl.setText("Mic");
           sampleRateLbl.setText("44100 hz");
           startBtn.setEnabled(true);
       }
           
    }//GEN-LAST:event_rbActionPerformed

    private void displayModeRBActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_displayModeRBActionPerformed
    {//GEN-HEADEREND:event_displayModeRBActionPerformed
       if(pulseRB.isSelected())
       {
           displayMode = DISP_MODE_PULSE;
           this.drawPanel.setPreferredSize( new Dimension(this.binStateListPulseOnly.size()*(circleSpacing) , drawPanel.getHeight()));
         
           
       }
       else
       {
           displayMode = DISP_MODE_FULL;
            this.drawPanel.setPreferredSize( new Dimension(this.binStateListAll.size()*(barWidth+barSpacing) , drawPanel.getHeight()));
           
       }
    }//GEN-LAST:event_displayModeRBActionPerformed

    private void formWindowOpened(java.awt.event.WindowEvent evt)//GEN-FIRST:event_formWindowOpened
    {//GEN-HEADEREND:event_formWindowOpened
     
        new Thread(()-> 
        {
            ioioHelper.start();
        }).start();
 
        
    }//GEN-LAST:event_formWindowOpened

   
    @Override
    public IOIOLooper createIOIOLooper(String connectionType, Object extra)
    {
        return new MyBaseIOLooper();
    }

    
   
 
    
    
    private class Player extends Thread
    {
        private final AudioFormat audioFormat;
        private AudioInputStream audioStream;
        private SourceDataLine sourceLine;
        private TargetDataLine targetLine;
        private int maxMagnitude;
        private BinStateManipulatorThread binStateThread;
       
        private boolean play = true;
        
        
        public Player() throws Exception
        {
            if(fileRB.isSelected())
            {
                audioStream = AudioSystem.getAudioInputStream(file);
                audioFormat = audioStream.getFormat();
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
                sourceLine = (SourceDataLine) AudioSystem.getLine(info);
                sourceLine.open(audioFormat);
                sourceLine.start();
              
            }
            else
            {
                float sampleRate = 44100;
                int sampleSizeInBits = 16;
                int channels = 2;
                boolean signed = true;
                boolean bigEndian = false;
                audioFormat = new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
                targetLine = (TargetDataLine) AudioSystem.getLine(info);
                targetLine.open(audioFormat);
                targetLine.start();          
            }
            
        }
        
        
        public void end()
        {
            stopAllVisualBins();
            play = false;
            binStateThread.end();
        }
 
        @Override
        public void run()
        {       
           try
           {
               
               int N = Integer.parseInt((String) windowSizeCB.getSelectedItem() ); 
               maxMagnitude = getMaxMagnitude(audioFormat, N);
      
               setupBinNames( (int) audioFormat.getSampleRate(), N);
               
              
               Fourier2 fourier = new Fourier2(new com.renegarcia.phys3proj.AudioFormat( (int) audioFormat.getSampleRate(), audioFormat.getChannels(), audioFormat.getSampleSizeInBits() / 8));
               int frameSize = audioFormat.getFrameSize();
              
               int bytesRead;
               
               int bufferSize = N * frameSize;

               byte buffer[] = new byte[bufferSize];
        
               ArrayBlockingQueue<FourierResults> q = new ArrayBlockingQueue<>(100);
               
               int delayQueueSize ;
               
               //binState manipulator thread
               binStateThread = new BinStateManipulatorThread(player, q);
               binStateThread.start();
               
               
               //ui updater thread
               Timer uiTimerThread = new Timer(1, (ActionEvent e) ->
               {
                   drawPanel.repaint();
      
               });                     
               uiTimerThread.setRepeats(true);
               uiTimerThread.start();
               
               
                //device updater thread
               java.util.Timer deviceUpdaterTimerThread = null;
               /*
               if(outputStream != null)
               {                
                   deviceUpdaterTimerThread = new java.util.Timer();
                   
                   final java.util.Timer tFinal = deviceUpdaterTimerThread; //hack
                   deviceUpdaterTimerThread.scheduleAtFixedRate(new TimerTask()
                   {
                       @Override
                       public void run()
                       {
                           try
                           {
                               sendData();
                           }
                           catch (IOException ex)
                           {
                               tFinal.cancel();
                               EventQueue.invokeLater(()->
                               {
                                   statusLbl.setText("Disconnected");
                                   JOptionPane.showMessageDialog(Phys3Proj.this, "Error connecting to python server\n" + ex.getMessage(), "Error", JOptionPane.WARNING_MESSAGE);
                               });
                           }
                       }
                       
                       @Override
                       public boolean cancel()
                       {
                           try
                           {
                               stopAllVisualBins();
                               sendData();
                           }
                           catch(IOException ex)
                           {
                               ex.printStackTrace();
                           }
                           
                           return super.cancel();
                       }
                       
                   }, 0, 20);
               }
              
              */
               
               ConcurrentLinkedQueue<FourierResults> delayQueue = new ConcurrentLinkedQueue<>();
               boolean smooth = true;
               
               FourierResults fr;
               if (fileRB.isSelected())
               {
                   delayQueueSize = 9;
                   while ((bytesRead = audioStream.read(buffer, 0, bufferSize)) != -1 && play)
                   {
                       if (bytesRead != bufferSize)
                           System.out.printf("missed a read\n");

                       fr  = fourier.getFourier(buffer, false);

                       sourceLine.write(buffer, 0, bufferSize);

                       //hack
                       delayQueue.offer(fr);
                       if (delayQueue.size() > delayQueueSize)
                           q.offer(delayQueue.poll());
                   }
                   sourceLine.close();
               }
               
               
               else //mic
               {
                   delayQueueSize = 0;
                   while ((bytesRead = targetLine.read(buffer, 0, bufferSize)) != -1 && play)
                   {
                       if (bytesRead != bufferSize)
                           play = false;

                        fr = fourier.getFourier(buffer, true);
                       
                       //hack
                       delayQueue.offer(fr);
                       if (delayQueue.size() > delayQueueSize)
                           q.offer(delayQueue.poll());
                   }
                   targetLine.close();
               }
             
               
               stopAllVisualBins();
               
               //stop threads
               binStateThread.end();
               
               uiTimerThread.stop();
               
               if(deviceUpdaterTimerThread != null)
                   deviceUpdaterTimerThread.cancel();
           }
           catch ( NumberFormatException | IOException ex)
           {
               EventQueue.invokeLater(()->
               {
                   JOptionPane.showMessageDialog(Phys3Proj.this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
               });
           }
         
            EventQueue.invokeLater(()-> 
            {
                stopBtn.setEnabled(false);
                fileRB.setEnabled(true);
                micRB.setEnabled(true);
                startBtn.setEnabled(true);
                if (fileRB.isSelected())
                    browseSourceBtn.setEnabled(true);
            });

        }




    }
    
    
    private class BinStateManipulatorThread extends Thread
    {
        private final ArrayBlockingQueue<FourierResults> q;
        private final Player player;
        private boolean ended = false;
        
        public BinStateManipulatorThread(Player player, ArrayBlockingQueue q)
        {
            this.q = q;
            this.player = player;
        }
             
        @Override
        public void run()
        {
            FourierResults data;      
           
                    
            try
            {
                while (player.play)
                {
                    data = q.take();
                    
                    for (int i = 0; i < binStateListAll.size(); i++)
                    {
                        BinStateInfo b = binStateListAll.get(i);
                        FourierBin fr = data.fourierBinList1.get(i);

                        //System.out.printf("%.2f\n", maxMagnitude * b.getThreshold()/100);
                        if (fr.magnitude > player.maxMagnitude * b.getThreshold()/100 && !b.isOn())
                            b.setOn();
                        else if ((b.isOn()) && (System.currentTimeMillis() - b.getLastCallTime() > 200))
                            b.setOff();
                        
                        b.setLastMagnitude(fr.magnitude);
                    }
                }
            }
            catch (InterruptedException ex)
            {
                if(!ended)
                {
                    EventQueue.invokeLater(()-> 
                    {
                        JOptionPane.showMessageDialog(Phys3Proj.this, "InterruptedException in player thread\n" + ex.getMessage(), " Fatal Error", JOptionPane.ERROR_MESSAGE);
                    });
                }       
            }

        }
        
        
        public void end()
        {
            ended = true;
            this.interrupt();
        }
    }
  
    
    
    
    private class PinAssigmentTableModel extends MyAbstractTableModel
    {
        public PinAssigmentTableModel()
        {
            super(new String[]{"Pin", "Bin", "Threshold"}, new Class[]{String.class, Integer.class, Double.class}, new boolean[]{false,true,true});
        }
        @Override
        public void setValueAt(Object obj, int rowIndex, int columnIndex)
        {
                    
             int pin = rowIndex + 1;
             if(columnIndex == 1) //bin
             {
                 int binNumber = ((int) obj);
                 
                 if (binNumber < 0)
                 {
                     pinScheme.add(pin,  null);
                    
                     
                     return;
                 }
                                     
                 
                 pinScheme.add(pin, binStateListAll.get(binNumber));
                
             }
             
             
             else if(columnIndex == 2) //threshold
             {
                 double thresh = (double) obj;
                 
                 if(thresh < 1.0 || thresh > 90.0)
                 {
                     JOptionPane.showMessageDialog(Phys3Proj.this, "Threshold must be between 1.0 and 90.0,", "Warning", JOptionPane.WARNING_MESSAGE);
                     return;
                 }
                 pinScheme.get(pin).setThreshold(thresh);         
             }
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex)
        {
           
            Object r = null;
            int pinNo = rowIndex+1;
            if(columnIndex == 0) //pin no
            {
                r = pinNo;
            }
            else if (columnIndex == 1) //binNo
            {
                BinStateInfo bsi = pinScheme.get(pinNo);
                r = bsi != null ? bsi.getBinNumber() : -1;
            }
            else
            {
                BinStateInfo bsi = pinScheme.get(pinNo);
                r = bsi != null ? bsi.getThreshold() : -1;
            }
            
          
            return r;
        }
        
    }
    
    
    
    public class MyBaseIOLooper extends BaseIOIOLooper
    {
      
        private List<DigitalOutput> digitalOutputList;
        
         @Override
        protected void setup() throws ConnectionLostException, InterruptedException
        {
            //todo iodisconnected
            EventQueue.invokeLater(()-> 
            {
                statusLbl.setText("Connected");
            });
            digitalOutputList = new ArrayList<>();

            for (int i = 1; i <= PIN_COUNT; i++)
                digitalOutputList.add(ioio_.openDigitalOutput(i, true));
            
            

        }




        @Override
        public void loop() throws ConnectionLostException, InterruptedException
        {
            BinStateInfo bsi;
            DigitalOutput output;
            for (int i = 0; i < PIN_COUNT; i++)
            {
                bsi = pinScheme.get(i + 1);
                
                if (bsi != null)
                {
                    ioio_.beginBatch();
                    output = digitalOutputList.get(i);
                    output.write(bsi.isOn());
                    ioio_.endBatch();
                   
                }
            }
            Thread.sleep(20);
        }




        @Override
        public void disconnected()
        {
            //todo iodisconnected
            EventQueue.invokeLater(() -> 
            {
                statusLbl.setText("Disconnected");
            });
        }




        @Override
        public void incompatible()
        {
            //todo iodisconnected
            EventQueue.invokeLater(()-> 
            {
                statusLbl.setText("Disconnected");
            });
        }
    }

    
    

    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[])
    {
         
         /* Set look and feel */
         //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
            /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
             * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
             */
            try
            {
                javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException e)
            {
              //  log.log(Level.SEVERE, e.getMessage(), e);
            }
            //</editor-fold>

        

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() ->
            {
                new Phys3Proj().setVisible(true);
            });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<String> ampCB;
    private javax.swing.JPanel bottomPanel;
    private javax.swing.JButton browseSourceBtn;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup displayModeButtonGroup;
    private javax.swing.JPanel drawPanel;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JMenuItem exportMenuItem;
    private javax.swing.JRadioButton fileRB;
    private javax.swing.JRadioButton fullRB;
    private javax.swing.JMenuItem importMenuItem;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JRadioButton micRB;
    private javax.swing.JTable pinAssignTable;
    private javax.swing.JRadioButton pulseRB;
    private javax.swing.JPanel rightPanel;
    private javax.swing.JLabel sampleRateLbl;
    private javax.swing.JLabel sourceLbl;
    private javax.swing.JButton startBtn;
    private javax.swing.JLabel statusLbl;
    private javax.swing.JButton stopBtn;
    private javax.swing.JPanel topPanel;
    private javax.swing.JComboBox<String> windowSizeCB;
    // End of variables declaration//GEN-END:variables
}

