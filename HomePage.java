/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package project1;

import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.File;
import javax.swing.ImageIcon;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;

//Webcam Library Imports

import java.applet.Applet;
import java.applet.AudioClip;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Label;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractorMOG;


/**
 *
 * @author ibrahimdavid
 */
public class HomePage extends javax.swing.JFrame implements Runnable, Thread.UncaughtExceptionHandler{
    
    static{
    
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        
    }

  private Boolean begin = false;
  
  private Boolean firstFrame = true;
  private VideoCapture video = null;
  private CaptureThread thread = null;
  private final MatOfByte matOfByte = new MatOfByte();
  private BufferedImage bufImage = null;
  private InputStream in;
  private Mat frameaux = new Mat();
  private Mat frame = new Mat(480, 640, CvType.CV_8UC3);
  private Mat lastFrame = new Mat(480, 640, CvType.CV_8UC3);
  private Mat currentFrame = new Mat(480, 640, CvType.CV_8UC3);
  private Mat processedFrame = new Mat(480, 640, CvType.CV_8UC3);
  private DisplayPanel image;
 private BackgroundSubtractorMOG bsMOG = new BackgroundSubtractorMOG();
  private int savedelay = 0;
  String currentDir = "";
  String detectionsDir = "detections";

    @Override
    public void run() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    

    

  class CaptureThread extends Thread
  {
    @Override
    public void run()
    {
      if(video.isOpened())
      {
        while(begin == true)
        {
          //video.read(frameaux);
          video.retrieve(frameaux);
          Imgproc.resize(frameaux, frame, frame.size());
          frame.copyTo(currentFrame);
          
          if(firstFrame)
          {
            frame.copyTo(lastFrame);
            firstFrame = false;
            continue;
          }

          if(CheckBoxMotionDetection.isSelected())
          {
            Imgproc.GaussianBlur(currentFrame, currentFrame, new Size(3, 3), 0);
            Imgproc.GaussianBlur(lastFrame, lastFrame, new Size(3, 3), 0);
            
            //bsMOG.apply(frame, processedFrame, 0.005);
            Core.subtract(currentFrame, lastFrame, processedFrame);
            //Core.absdiff(frame,lastFrame,processedFrame);
            
            Imgproc.cvtColor(processedFrame, processedFrame, Imgproc.COLOR_RGB2GRAY);
            //
            
            int threshold = mdSensitivity.getValue();
            //Imgproc.adaptiveThreshold(processedFrame, processedFrame, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY_INV, 5, 2);
            Imgproc.threshold(processedFrame, processedFrame, threshold, 255, Imgproc.THRESH_BINARY);

            ArrayList<Rect> array = detection_contours(currentFrame, processedFrame);
            
          //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
          
            if(array.size() > 0)
            {
              Iterator<Rect> it2 = array.iterator();
              while(it2.hasNext())
              {
                Rect obj = it2.next();
                Core.rectangle(currentFrame, obj.br(), obj.tl(),
                  new Scalar(0, 255, 0), 1);
              }
            }
            //*/
            
            if(CheckBoxAlarm.isSelected())
            {
              double sensibility = SliderSensitivity.getValue();
              //System.out.println(sensibility);
              double nonZeroPixels = Core.countNonZero(processedFrame);
              //System.out.println("nonZeroPixels: " + nonZeroPixels);

              double nrows = processedFrame.rows();
              double ncols = processedFrame.cols();
              double total = nrows * ncols / 10;

              double detections = (nonZeroPixels / total) * 100;
              //System.out.println(detections);
              if(detections >= sensibility)
              {
                  
                //Activating the In-Built on-Screen & Audio Alarm:
                Core.putText(currentFrame, "MOVEMENT DETECTED!", 
                  new Point(5,currentFrame.cols()/2), //currentFrame.rows()/2 currentFrame.cols()/2
                  Core.FONT_HERSHEY_TRIPLEX , new Double(1), new Scalar(0,0,255));
                    statusLbl.setText("Movement detected!");
                PlayBeep();

                if(CheckBoxSave.isSelected())
                {
                  if(savedelay == 2)
                  {
                    String filename = SaveLocationTxt.getText() + File.separator + "Motion Capture_" + getCurrentTimeStamp() + ".jpg";
                    
                    statusLbl.setText("Saving results in: " + filename);
                    
                   //System.out.println("Saving results in: " + filename);
                    Highgui.imwrite(filename, processedFrame);
                    savedelay = 0;
                  }
                  else
                    savedelay = savedelay + 1;
                }
                
                
              }
              else
              {
                savedelay = 0;
                
              }
            }
            
          }
          
          
          currentFrame.copyTo(processedFrame);

          Highgui.imencode(".jpg", processedFrame, matOfByte);
          byte[] byteArray = matOfByte.toArray();

          try
          {
            in = new ByteArrayInputStream(byteArray);
            bufImage = ImageIO.read(in);
          }
          catch(Exception ex)
          {
            ex.printStackTrace();
          }

          image.updateImage(bufImage);

          frame.copyTo(lastFrame);

          try
          {
            Thread.sleep(1);
          }
          catch(Exception ex)
          {
          }
        }
      }
    }
  }
  

   
    /**
     * Creates new form HomePage
     */
    public HomePage() {
        initComponents();
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        
        image = new DisplayPanel(new ImageIcon("temp/640x480.png").getImage());
        zone1screen.add(image, BorderLayout.CENTER);

        currentDir = Paths.get(".").toAbsolutePath().normalize().toString();
        detectionsDir = currentDir + File.separator + detectionsDir;

        SaveLocationTxt.setText(detectionsDir);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jMenuItem1 = new javax.swing.JMenuItem();
        jPanel1 = new javax.swing.JPanel();
        statusPanel = new javax.swing.JPanel();
        statusLabel = new javax.swing.JLabel();
        statusLbl = new javax.swing.JLabel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        storedMedia = new javax.swing.JPanel();
        jTabbedPane2 = new javax.swing.JTabbedPane();
        videoPanel = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        videoJTree = new javax.swing.JTree();
        videoTreeLbl = new javax.swing.JLabel();
        open_vid_btn = new javax.swing.JButton();
        photoPanel = new javax.swing.JPanel();
        imagedisplay = new javax.swing.JLabel();
        clearBtn = new javax.swing.JButton();
        browseBtn = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        photoJTree = new javax.swing.JTree();
        open_file_btn = new javax.swing.JButton();
        jSeparator3 = new javax.swing.JSeparator();
        liveMediaPanel = new javax.swing.JPanel();
        zone1screen = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        mdPanel = new javax.swing.JPanel();
        mdClose = new javax.swing.JButton();
        CheckBoxMotionDetection = new javax.swing.JCheckBox();
        mdSensitivity = new javax.swing.JSlider();
        jLabel6 = new javax.swing.JLabel();
        CheckBoxSave = new javax.swing.JCheckBox();
        SaveLocationTxt = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        camSwitchBtn = new javax.swing.JToggleButton();
        analysePanel = new javax.swing.JPanel();
        canny = new javax.swing.JCheckBox();
        threshold = new javax.swing.JSlider();
        jLabel5 = new javax.swing.JLabel();
        analyseClose = new javax.swing.JButton();
        FDpanel = new javax.swing.JPanel();
        FDon = new javax.swing.JButton();
        FDoff = new javax.swing.JButton();
        FDclose = new javax.swing.JButton();
        jSeparator2 = new javax.swing.JSeparator();
        alarmPanel = new javax.swing.JPanel();
        CheckBoxAlarm = new javax.swing.JCheckBox();
        SliderSensitivity = new javax.swing.JSlider();
        jLabel7 = new javax.swing.JLabel();
        alarmPanelClose = new javax.swing.JButton();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        zoneSelect = new javax.swing.JComboBox<>();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        refreshBtn = new javax.swing.JMenuItem();
        ThemeMenu = new javax.swing.JMenu();
        defaultThemeBtn = new javax.swing.JMenuItem();
        lightThemeBtn = new javax.swing.JMenuItem();
        darkThemeBtn = new javax.swing.JMenuItem();
        exitBtn = new javax.swing.JMenuItem();
        aiMenu = new javax.swing.JMenu();
        trainMenu = new javax.swing.JMenuItem();
        trackingMenu = new javax.swing.JMenuItem();
        detectionMenu = new javax.swing.JMenuItem();
        analyseMenu = new javax.swing.JMenuItem();
        jMenu3 = new javax.swing.JMenu();
        alarmMenu = new javax.swing.JMenuItem();
        jMenuItem11 = new javax.swing.JMenuItem();

        jMenuItem1.setText("jMenuItem1");

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("logo1.png")));
        setResizable(false);

        jPanel1.setMinimumSize(new java.awt.Dimension(1370, 730));

        statusPanel.setBackground(new java.awt.Color(0, 51, 102));
        statusPanel.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(255, 0, 0), 2, true));

        statusLabel.setFont(new java.awt.Font("Comic Sans MS", 1, 14)); // NOI18N
        statusLabel.setForeground(new java.awt.Color(255, 0, 0));
        statusLabel.setText("Status:");

        statusLbl.setBackground(new java.awt.Color(51, 102, 255));
        statusLbl.setFont(new java.awt.Font("Comic Sans MS", 1, 13)); // NOI18N
        statusLbl.setForeground(new java.awt.Color(255, 255, 255));
        statusLbl.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(255, 0, 0), 1, true));

        javax.swing.GroupLayout statusPanelLayout = new javax.swing.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(statusLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(statusLbl)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(statusLbl, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(statusPanelLayout.createSequentialGroup()
                        .addComponent(statusLabel)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );

        jTabbedPane1.setBackground(new java.awt.Color(51, 102, 255));
        jTabbedPane1.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 51, 102), 2, true));
        jTabbedPane1.setForeground(new java.awt.Color(0, 0, 0));
        jTabbedPane1.setTabLayoutPolicy(javax.swing.JTabbedPane.SCROLL_TAB_LAYOUT);

        storedMedia.setBackground(new java.awt.Color(51, 102, 255));

        jTabbedPane2.setBackground(new java.awt.Color(0, 0, 0));
        jTabbedPane2.setTabLayoutPolicy(javax.swing.JTabbedPane.SCROLL_TAB_LAYOUT);
        jTabbedPane2.setTabPlacement(javax.swing.JTabbedPane.LEFT);

        videoPanel.setBackground(new java.awt.Color(102, 153, 255));
        videoPanel.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 51, 102), 2, true));

        photoJTree.setModel(new FileSystemModel(new File("D:\\detections")));
        videoJTree.setBackground(new java.awt.Color(102, 153, 255));
        videoJTree.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 51, 102), 2, true));
        videoJTree.setFont(new java.awt.Font("Comic Sans MS", 1, 14)); // NOI18N
        videoJTree.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                videoJTreeMouseClicked(evt);
            }
        });
        jScrollPane2.setViewportView(videoJTree);

        videoTreeLbl.setFont(new java.awt.Font("Comic Sans MS", 1, 13)); // NOI18N
        videoTreeLbl.setForeground(new java.awt.Color(0, 51, 102));
        videoTreeLbl.setText("Saved Videos:");

        open_vid_btn.setBackground(new java.awt.Color(0, 51, 102));
        open_vid_btn.setFont(new java.awt.Font("Comic Sans MS", 1, 13)); // NOI18N
        open_vid_btn.setForeground(new java.awt.Color(0, 204, 51));
        open_vid_btn.setText("Open File");
        open_vid_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                open_vid_btnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout videoPanelLayout = new javax.swing.GroupLayout(videoPanel);
        videoPanel.setLayout(videoPanelLayout);
        videoPanelLayout.setHorizontalGroup(
            videoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(videoPanelLayout.createSequentialGroup()
                .addGap(59, 59, 59)
                .addGroup(videoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(open_vid_btn)
                    .addComponent(videoTreeLbl)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 426, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(622, Short.MAX_VALUE))
        );
        videoPanelLayout.setVerticalGroup(
            videoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(videoPanelLayout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addComponent(videoTreeLbl, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 554, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(open_vid_btn)
                .addContainerGap(504, Short.MAX_VALUE))
        );

        jTabbedPane2.addTab("Video", videoPanel);

        photoPanel.setBackground(new java.awt.Color(102, 153, 255));
        photoPanel.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 51, 102), 2, true));
        photoPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        imagedisplay.setBackground(new java.awt.Color(0, 0, 0));
        imagedisplay.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        imagedisplay.setOpaque(true);
        photoPanel.add(imagedisplay, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 30, 460, 410));

        clearBtn.setBackground(new java.awt.Color(0, 0, 0));
        clearBtn.setFont(new java.awt.Font("Comic Sans MS", 1, 13)); // NOI18N
        clearBtn.setForeground(new java.awt.Color(204, 0, 0));
        clearBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/project1/img/edit-clear-icon.png"))); // NOI18N
        clearBtn.setText("Clear");
        clearBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearBtnActionPerformed(evt);
            }
        });
        photoPanel.add(clearBtn, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 450, 90, 30));

        browseBtn.setBackground(new java.awt.Color(51, 102, 255));
        browseBtn.setFont(new java.awt.Font("Comic Sans MS", 1, 13)); // NOI18N
        browseBtn.setForeground(new java.awt.Color(0, 0, 0));
        browseBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/project1/img/open.png"))); // NOI18N
        browseBtn.setText("...");
        browseBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseBtnActionPerformed(evt);
            }
        });
        photoPanel.add(browseBtn, new org.netbeans.lib.awtextra.AbsoluteConstraints(510, 30, 50, 30));

        jLabel1.setFont(new java.awt.Font("Comic Sans MS", 1, 13)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(0, 0, 0));
        jLabel1.setText("Saved Images:");
        photoPanel.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(690, 10, -1, 34));

        photoJTree.setModel(new FileSystemModel(new File("D:\\detections")));
        photoJTree.setBackground(new java.awt.Color(102, 153, 255));
        photoJTree.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 51, 102), 2, true));
        photoJTree.setFont(new java.awt.Font("Comic Sans MS", 1, 14)); // NOI18N
        photoJTree.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                photoJTreeMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(photoJTree);

        photoPanel.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(670, 40, 426, 554));

        open_file_btn.setBackground(new java.awt.Color(0, 51, 102));
        open_file_btn.setFont(new java.awt.Font("Comic Sans MS", 1, 13)); // NOI18N
        open_file_btn.setForeground(new java.awt.Color(0, 204, 51));
        open_file_btn.setText("Open File");
        open_file_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                open_file_btnActionPerformed(evt);
            }
        });
        photoPanel.add(open_file_btn, new org.netbeans.lib.awtextra.AbsoluteConstraints(670, 610, 90, -1));

        jSeparator3.setBackground(new java.awt.Color(0, 0, 0));
        jSeparator3.setOrientation(javax.swing.SwingConstants.VERTICAL);
        photoPanel.add(jSeparator3, new org.netbeans.lib.awtextra.AbsoluteConstraints(610, 20, 20, 890));

        jTabbedPane2.addTab("Photo", photoPanel);

        javax.swing.GroupLayout storedMediaLayout = new javax.swing.GroupLayout(storedMedia);
        storedMedia.setLayout(storedMediaLayout);
        storedMediaLayout.setHorizontalGroup(
            storedMediaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(storedMediaLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 1161, Short.MAX_VALUE)
                .addGap(215, 215, 215))
        );
        storedMediaLayout.setVerticalGroup(
            storedMediaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(storedMediaLayout.createSequentialGroup()
                .addComponent(jTabbedPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 1163, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 54, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Stored Media", storedMedia);

        liveMediaPanel.setBackground(new java.awt.Color(102, 153, 255));
        liveMediaPanel.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 51, 102), 2, true));

        zone1screen.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(255, 0, 0), 3, true));
        zone1screen.setPreferredSize(new java.awt.Dimension(640, 480));
        zone1screen.setLayout(new java.awt.BorderLayout());

        jLabel2.setFont(new java.awt.Font("Comic Sans MS", 1, 13)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(0, 51, 102));
        jLabel2.setText("Select Zone:");

        mdPanel.setBackground(new java.awt.Color(153, 153, 255));
        mdPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 51, 102), 2, true), "Motion Detection:", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Comic Sans MS", 1, 13), new java.awt.Color(0, 51, 102))); // NOI18N

        mdClose.setBackground(new java.awt.Color(255, 0, 0));
        mdClose.setForeground(new java.awt.Color(255, 0, 0));
        mdClose.setAlignmentY(0.0F);
        mdClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mdCloseActionPerformed(evt);
            }
        });

        CheckBoxMotionDetection.setBackground(new java.awt.Color(153, 153, 255));
        CheckBoxMotionDetection.setFont(new java.awt.Font("Comic Sans MS", 1, 13)); // NOI18N
        CheckBoxMotionDetection.setForeground(new java.awt.Color(0, 51, 102));
        CheckBoxMotionDetection.setText("Motion Detection");

        mdSensitivity.setBackground(new java.awt.Color(153, 153, 255));
        mdSensitivity.setForeground(new java.awt.Color(0, 51, 102));
        mdSensitivity.setMaximum(255);
        mdSensitivity.setToolTipText("Set Motion Detection Sensitivity");

        jLabel6.setFont(new java.awt.Font("Comic Sans MS", 1, 13)); // NOI18N
        jLabel6.setForeground(new java.awt.Color(0, 51, 102));
        jLabel6.setText("Sensitivity:");

        CheckBoxSave.setBackground(new java.awt.Color(153, 153, 255));
        CheckBoxSave.setFont(new java.awt.Font("Comic Sans MS", 1, 13)); // NOI18N
        CheckBoxSave.setForeground(new java.awt.Color(0, 51, 102));
        CheckBoxSave.setText("Save:");
        CheckBoxSave.setToolTipText("Save Frames of Captured Motion");

        SaveLocationTxt.setBackground(new java.awt.Color(153, 153, 255));
        SaveLocationTxt.setFont(new java.awt.Font("Comic Sans MS", 1, 13)); // NOI18N
        SaveLocationTxt.setForeground(new java.awt.Color(0, 51, 102));
        SaveLocationTxt.setToolTipText("Enter Save Location for Frames of Captured Motion");
        SaveLocationTxt.setBorder(javax.swing.BorderFactory.createTitledBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 51, 102), 1, true), "Location:", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Comic Sans MS", 1, 13), new java.awt.Color(0, 51, 102))); // NOI18N

        jLabel8.setFont(new java.awt.Font("Comic Sans MS", 1, 12)); // NOI18N
        jLabel8.setForeground(new java.awt.Color(255, 0, 0));
        jLabel8.setText("High");

        jLabel9.setForeground(new java.awt.Color(255, 0, 0));
        jLabel9.setText("Low");

        javax.swing.GroupLayout mdPanelLayout = new javax.swing.GroupLayout(mdPanel);
        mdPanel.setLayout(mdPanelLayout);
        mdPanelLayout.setHorizontalGroup(
            mdPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mdPanelLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(mdClose, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mdPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(mdPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mdPanelLayout.createSequentialGroup()
                        .addComponent(CheckBoxMotionDetection)
                        .addGap(151, 151, 151))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mdPanelLayout.createSequentialGroup()
                        .addComponent(jLabel9)
                        .addGap(15, 15, 15))))
            .addGroup(mdPanelLayout.createSequentialGroup()
                .addGroup(mdPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(mdPanelLayout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addGroup(mdPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(mdSensitivity, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(mdPanelLayout.createSequentialGroup()
                                .addComponent(CheckBoxSave)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(SaveLocationTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 362, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE))))
                    .addGroup(mdPanelLayout.createSequentialGroup()
                        .addGroup(mdPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(mdPanelLayout.createSequentialGroup()
                                .addGap(14, 14, 14)
                                .addComponent(jLabel8))
                            .addGroup(mdPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel6)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        mdPanelLayout.setVerticalGroup(
            mdPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mdPanelLayout.createSequentialGroup()
                .addComponent(mdClose, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(28, 28, 28)
                .addComponent(CheckBoxMotionDetection)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 15, Short.MAX_VALUE)
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel8)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(mdSensitivity, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel9)
                .addGap(18, 18, 18)
                .addGroup(mdPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(SaveLocationTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mdPanelLayout.createSequentialGroup()
                        .addComponent(CheckBoxSave)
                        .addGap(11, 11, 11)))
                .addGap(29, 29, 29))
        );

        jSeparator1.setBackground(new java.awt.Color(0, 0, 0));
        jSeparator1.setOrientation(javax.swing.SwingConstants.VERTICAL);

        jLabel3.setBackground(new java.awt.Color(204, 204, 204));
        jLabel3.setFont(new java.awt.Font("Comic Sans MS", 1, 13)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(0, 51, 102));
        jLabel3.setText("Toggle Camera View:");

        jLabel4.setBackground(new java.awt.Color(204, 204, 204));
        jLabel4.setFont(new java.awt.Font("Comic Sans MS", 1, 13)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(0, 51, 102));
        jLabel4.setText("Options:");
        jLabel4.setToolTipText("Use the Menu Bar Above to Display Options.");

        camSwitchBtn.setBackground(new java.awt.Color(0, 51, 102));
        camSwitchBtn.setForeground(new java.awt.Color(255, 0, 0));
        camSwitchBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/project1/img/ON-icon.png"))); // NOI18N
        camSwitchBtn.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                camSwitchBtnItemStateChanged(evt);
            }
        });

        analysePanel.setBackground(new java.awt.Color(0, 51, 102));
        analysePanel.setBorder(javax.swing.BorderFactory.createTitledBorder(new javax.swing.border.LineBorder(new java.awt.Color(255, 0, 0), 2, true), "Analyse Options:", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Comic Sans MS", 1, 13), new java.awt.Color(255, 0, 0))); // NOI18N

        canny.setFont(new java.awt.Font("Comic Sans MS", 1, 13)); // NOI18N
        canny.setForeground(new java.awt.Color(255, 0, 0));
        canny.setText("Edge Detection");
        canny.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cannyActionPerformed(evt);
            }
        });

        jLabel5.setFont(new java.awt.Font("Comic Sans MS", 1, 13)); // NOI18N
        jLabel5.setForeground(new java.awt.Color(255, 0, 0));
        jLabel5.setText("Canny Threshold:");

        analyseClose.setBackground(new java.awt.Color(255, 0, 0));
        analyseClose.setForeground(new java.awt.Color(255, 0, 0));
        analyseClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                analyseCloseActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout analysePanelLayout = new javax.swing.GroupLayout(analysePanel);
        analysePanel.setLayout(analysePanelLayout);
        analysePanelLayout.setHorizontalGroup(
            analysePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(analysePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(canny)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(analysePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(threshold, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5))
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, analysePanelLayout.createSequentialGroup()
                .addGap(0, 413, Short.MAX_VALUE)
                .addComponent(analyseClose, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        analysePanelLayout.setVerticalGroup(
            analysePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(analysePanelLayout.createSequentialGroup()
                .addComponent(analyseClose, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(21, 21, 21)
                .addGroup(analysePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(analysePanelLayout.createSequentialGroup()
                        .addGap(13, 13, 13)
                        .addComponent(canny))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, analysePanelLayout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(threshold, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(69, Short.MAX_VALUE))
        );

        FDpanel.setBackground(new java.awt.Color(153, 153, 255));
        FDpanel.setBorder(javax.swing.BorderFactory.createTitledBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 51, 102), 2, true), "Face Detection", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Comic Sans MS", 1, 13), new java.awt.Color(0, 51, 102))); // NOI18N

        FDon.setBackground(new java.awt.Color(0, 51, 102));
        FDon.setFont(new java.awt.Font("Comic Sans MS", 1, 12)); // NOI18N
        FDon.setForeground(new java.awt.Color(0, 204, 51));
        FDon.setText("ON");
        FDon.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                FDonActionPerformed(evt);
            }
        });

        FDoff.setBackground(new java.awt.Color(0, 51, 102));
        FDoff.setFont(new java.awt.Font("Comic Sans MS", 1, 12)); // NOI18N
        FDoff.setForeground(new java.awt.Color(255, 0, 0));
        FDoff.setText("OFF");
        FDoff.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                FDoffActionPerformed(evt);
            }
        });

        FDclose.setBackground(new java.awt.Color(255, 0, 0));
        FDclose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                FDcloseActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout FDpanelLayout = new javax.swing.GroupLayout(FDpanel);
        FDpanel.setLayout(FDpanelLayout);
        FDpanelLayout.setHorizontalGroup(
            FDpanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, FDpanelLayout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addComponent(FDon)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 40, Short.MAX_VALUE)
                .addComponent(FDoff)
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, FDpanelLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(FDclose, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        FDpanelLayout.setVerticalGroup(
            FDpanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, FDpanelLayout.createSequentialGroup()
                .addComponent(FDclose)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 14, Short.MAX_VALUE)
                .addGroup(FDpanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(FDon)
                    .addComponent(FDoff))
                .addGap(25, 25, 25))
        );

        jSeparator2.setBackground(new java.awt.Color(0, 0, 0));
        jSeparator2.setForeground(new java.awt.Color(0, 0, 0));

        alarmPanel.setBackground(new java.awt.Color(153, 153, 255));
        alarmPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 51, 102), 2, true), "Alarm", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Comic Sans MS", 1, 13), new java.awt.Color(0, 51, 102))); // NOI18N

        CheckBoxAlarm.setBackground(new java.awt.Color(153, 153, 255));
        CheckBoxAlarm.setFont(new java.awt.Font("Comic Sans MS", 1, 13)); // NOI18N
        CheckBoxAlarm.setForeground(new java.awt.Color(0, 51, 102));
        CheckBoxAlarm.setText("Alarm");

        SliderSensitivity.setBackground(new java.awt.Color(153, 153, 255));

        jLabel7.setFont(new java.awt.Font("Comic Sans MS", 1, 13)); // NOI18N
        jLabel7.setForeground(new java.awt.Color(0, 51, 102));
        jLabel7.setText("Sensitivity:");

        alarmPanelClose.setBackground(new java.awt.Color(255, 0, 0));
        alarmPanelClose.setForeground(new java.awt.Color(255, 0, 0));
        alarmPanelClose.setAlignmentY(0.0F);
        alarmPanelClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                alarmPanelCloseActionPerformed(evt);
            }
        });

        jLabel11.setFont(new java.awt.Font("Comic Sans MS", 1, 12)); // NOI18N
        jLabel11.setForeground(new java.awt.Color(255, 0, 0));
        jLabel11.setText("High");

        jLabel12.setForeground(new java.awt.Color(255, 0, 0));
        jLabel12.setText("Low");

        javax.swing.GroupLayout alarmPanelLayout = new javax.swing.GroupLayout(alarmPanel);
        alarmPanel.setLayout(alarmPanelLayout);
        alarmPanelLayout.setHorizontalGroup(
            alarmPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(alarmPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(alarmPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(alarmPanelLayout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(jLabel11)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(alarmPanelLayout.createSequentialGroup()
                        .addGroup(alarmPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(SliderSensitivity, javax.swing.GroupLayout.DEFAULT_SIZE, 323, Short.MAX_VALUE)
                            .addGroup(alarmPanelLayout.createSequentialGroup()
                                .addComponent(jLabel7)
                                .addGap(0, 0, Short.MAX_VALUE)))
                        .addContainerGap())
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, alarmPanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(alarmPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(alarmPanelClose, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, alarmPanelLayout.createSequentialGroup()
                                .addComponent(jLabel12)
                                .addGap(15, 15, 15))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, alarmPanelLayout.createSequentialGroup()
                                .addComponent(CheckBoxAlarm)
                                .addGap(127, 127, 127))))))
        );
        alarmPanelLayout.setVerticalGroup(
            alarmPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(alarmPanelLayout.createSequentialGroup()
                .addComponent(alarmPanelClose, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addComponent(CheckBoxAlarm)
                .addGap(10, 10, 10)
                .addComponent(jLabel7)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel11)
                .addGap(4, 4, 4)
                .addComponent(SliderSensitivity, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel12)
                .addContainerGap(27, Short.MAX_VALUE))
        );

        zoneSelect.setBackground(new java.awt.Color(102, 153, 255));
        zoneSelect.setFont(new java.awt.Font("Comic Sans MS", 1, 13)); // NOI18N
        zoneSelect.setForeground(new java.awt.Color(255, 0, 0));
        zoneSelect.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "0", "1", "2", "3" }));
        zoneSelect.setBorder(null);

        javax.swing.GroupLayout liveMediaPanelLayout = new javax.swing.GroupLayout(liveMediaPanel);
        liveMediaPanel.setLayout(liveMediaPanelLayout);
        liveMediaPanelLayout.setHorizontalGroup(
            liveMediaPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(liveMediaPanelLayout.createSequentialGroup()
                .addGap(42, 42, 42)
                .addComponent(zone1screen, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(liveMediaPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(liveMediaPanelLayout.createSequentialGroup()
                        .addGap(34, 34, 34)
                        .addGroup(liveMediaPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel2)
                            .addComponent(camSwitchBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(zoneSelect, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, 153, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(liveMediaPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(liveMediaPanelLayout.createSequentialGroup()
                        .addGap(153, 153, 153)
                        .addComponent(FDpanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(liveMediaPanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(liveMediaPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(liveMediaPanelLayout.createSequentialGroup()
                                .addGroup(liveMediaPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 446, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(liveMediaPanelLayout.createSequentialGroup()
                                        .addGap(59, 59, 59)
                                        .addComponent(alarmPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap(53, Short.MAX_VALUE))
                            .addGroup(liveMediaPanelLayout.createSequentialGroup()
                                .addGroup(liveMediaPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(analysePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(liveMediaPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(mdPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel4)))
                                .addGap(0, 0, Short.MAX_VALUE))))))
        );
        liveMediaPanelLayout.setVerticalGroup(
            liveMediaPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(liveMediaPanelLayout.createSequentialGroup()
                .addGap(46, 46, 46)
                .addGroup(liveMediaPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(liveMediaPanelLayout.createSequentialGroup()
                        .addGap(20, 20, 20)
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(camSwitchBtn)
                        .addGap(80, 80, 80)
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(zoneSelect, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(liveMediaPanelLayout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addGap(18, 18, 18)
                        .addComponent(mdPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(48, 48, 48)
                        .addComponent(FDpanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(48, 48, 48)
                        .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(26, 26, 26)
                        .addComponent(alarmPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(68, 68, 68)
                        .addComponent(analysePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(zone1screen, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 926, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(158, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Live Media", liveMediaPanel);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(statusPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jTabbedPane1)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addComponent(statusPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 1247, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jMenuBar1.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 51, 102), 1, true));
        jMenuBar1.setFont(new java.awt.Font("Comic Sans MS", 0, 12)); // NOI18N

        jMenu1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/project1/img/home-icon.png"))); // NOI18N
        jMenu1.setToolTipText("System Options");
        jMenu1.setLabel("Home");
        jMenu1.setMaximumSize(new java.awt.Dimension(80, 80));

        refreshBtn.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.ALT_MASK));
        refreshBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/project1/img/reload-icon.png"))); // NOI18N
        refreshBtn.setText("Refresh");
        refreshBtn.setToolTipText("System Refresh");
        refreshBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                refreshBtnActionPerformed(evt);
            }
        });
        jMenu1.add(refreshBtn);

        ThemeMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/project1/img/theme-icon.png"))); // NOI18N
        ThemeMenu.setText("Theme");
        ThemeMenu.setToolTipText("Change System Theme");

        defaultThemeBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/project1/img/default-icon.png"))); // NOI18N
        defaultThemeBtn.setText("Default");
        defaultThemeBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                defaultThemeBtnActionPerformed(evt);
            }
        });
        ThemeMenu.add(defaultThemeBtn);

        lightThemeBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/project1/img/Lamp-icon.png"))); // NOI18N
        lightThemeBtn.setText("Light");
        lightThemeBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lightThemeBtnActionPerformed(evt);
            }
        });
        ThemeMenu.add(lightThemeBtn);

        darkThemeBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/project1/img/darkSquare.png"))); // NOI18N
        darkThemeBtn.setText("Dark");
        darkThemeBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                darkThemeBtnActionPerformed(evt);
            }
        });
        ThemeMenu.add(darkThemeBtn);

        jMenu1.add(ThemeMenu);

        exitBtn.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F4, java.awt.event.InputEvent.ALT_MASK));
        exitBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/project1/img/exit-icon.png"))); // NOI18N
        exitBtn.setText("Exit");
        exitBtn.setToolTipText("Exit System");
        exitBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitBtnActionPerformed(evt);
            }
        });
        jMenu1.add(exitBtn);

        jMenuBar1.add(jMenu1);

        aiMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/project1/img/robot1-icon.png"))); // NOI18N
        aiMenu.setText("Ai");
        aiMenu.setToolTipText("Select A.I. Options");
        aiMenu.setMaximumSize(new java.awt.Dimension(80, 80));

        trainMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_T, java.awt.event.InputEvent.CTRL_MASK));
        trainMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/project1/img/teacher-icon.png"))); // NOI18N
        trainMenu.setText("Train");
        trainMenu.setToolTipText("Train the A.I. to recognise select Faces");
        trainMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                trainMenuActionPerformed(evt);
            }
        });
        aiMenu.add(trainMenu);

        trackingMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_T, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        trackingMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/project1/img/tracking-icon.png"))); // NOI18N
        trackingMenu.setText("Tracking");
        trackingMenu.setToolTipText("A.I. Tracking Options");
        trackingMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                trackingMenuActionPerformed(evt);
            }
        });
        aiMenu.add(trackingMenu);

        detectionMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_D, java.awt.event.InputEvent.CTRL_MASK));
        detectionMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/project1/img/detect-icon.png"))); // NOI18N
        detectionMenu.setText("Detection");
        detectionMenu.setToolTipText("A.I. Detection Options");
        detectionMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                detectionMenuActionPerformed(evt);
            }
        });
        aiMenu.add(detectionMenu);

        analyseMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, java.awt.event.InputEvent.ALT_MASK));
        analyseMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/project1/img/statistics-icon.png"))); // NOI18N
        analyseMenu.setText("Analyse");
        analyseMenu.setToolTipText("A.I. Analysis Options");
        analyseMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                analyseMenuActionPerformed(evt);
            }
        });
        aiMenu.add(analyseMenu);

        jMenuBar1.add(aiMenu);

        jMenu3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/project1/img/find-icon.png"))); // NOI18N
        jMenu3.setText("Sentry");
        jMenu3.setToolTipText("Sentry Mode Triggers Preset Autonomous Functionality");
        jMenu3.setMaximumSize(new java.awt.Dimension(80, 80));
        jMenu3.setPreferredSize(new java.awt.Dimension(80, 21));

        alarmMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, java.awt.event.InputEvent.ALT_MASK));
        alarmMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/project1/img/alarm-icon.png"))); // NOI18N
        alarmMenu.setText("Alarm");
        alarmMenu.setToolTipText("System Alarm Preferences");
        alarmMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                alarmMenuActionPerformed(evt);
            }
        });
        jMenu3.add(alarmMenu);

        jMenuItem11.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, java.awt.event.InputEvent.ALT_MASK));
        jMenuItem11.setIcon(new javax.swing.ImageIcon(getClass().getResource("/project1/img/findface-icon.png"))); // NOI18N
        jMenuItem11.setText("Face I.D.");
        jMenuItem11.setToolTipText("Identify Known Faces");
        jMenu3.add(jMenuItem11);

        jMenuBar1.add(jMenu3);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void browseBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseBtnActionPerformed
        // TODO add your handling code here:
          JFileChooser fc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
          fc.setDialogTitle("Multiple Image selection:");
          fc.setMultiSelectionEnabled(true);
          FileNameExtensionFilter filter = new FileNameExtensionFilter("PNG and JPG images", "png", "jpg");
          fc.addChoosableFileFilter(filter);
            
           int result = fc.showOpenDialog(null);
           
           if (result == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            String sname = file.getAbsolutePath(); 
            imagedisplay.setIcon(ResizeImage(sname));
        }
    
    }//GEN-LAST:event_browseBtnActionPerformed

    private void clearBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearBtnActionPerformed
     // TODO add your handling code here:
        imagedisplay.setIcon(null);
    }//GEN-LAST:event_clearBtnActionPerformed

    private void refreshBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_refreshBtnActionPerformed
        // TODO add your handling code here:
       
    }//GEN-LAST:event_refreshBtnActionPerformed

    private void exitBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitBtnActionPerformed
        // Exit the Application
        System.exit(0);
    }//GEN-LAST:event_exitBtnActionPerformed

    private void camSwitchBtnItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_camSwitchBtnItemStateChanged
        // Toggle Camera View ON/OFF:
         if(camSwitchBtn.isSelected()){
            camSwitchBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/project1/img/turn-off-icon.png"))); 
            camSwitchBtn.setText("ON");
            
            //ACTIVATE: 
             start();
            
         }else{
             //camSwitchBtn.setBackground(new java.awt.Color(0, 51, 102));
             camSwitchBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/project1/img/ON-icon.png"))); 
             camSwitchBtn.setText("OFF");
             
            //DEACTIVATE:  
                stop();

         } 
        
    }//GEN-LAST:event_camSwitchBtnItemStateChanged

    private void detectionMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_detectionMenuActionPerformed
        // Open Detection Panels:
        mdPanel.setVisible(true);
        FDpanel.setVisible(true);
        
    }//GEN-LAST:event_detectionMenuActionPerformed

    private void analyseMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_analyseMenuActionPerformed
        // Open Analyse Menu:
        analysePanel.setVisible(true);
    }//GEN-LAST:event_analyseMenuActionPerformed

    private void trackingMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_trackingMenuActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_trackingMenuActionPerformed

    private void trainMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_trainMenuActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_trainMenuActionPerformed

    private void mdCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mdCloseActionPerformed
        // Close Motion Detection Panel:
        mdPanel.setVisible(false);
    }//GEN-LAST:event_mdCloseActionPerformed

    private void FDonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FDonActionPerformed
        // Activate Face Detection:

        
    }//GEN-LAST:event_FDonActionPerformed

    private void FDcloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FDcloseActionPerformed
        // Close Face Detection Panel:
        FDpanel.setVisible(false);
    }//GEN-LAST:event_FDcloseActionPerformed

    private void FDoffActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FDoffActionPerformed
        // Dectivate Face Detection:
    
    }//GEN-LAST:event_FDoffActionPerformed

    private void alarmPanelCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_alarmPanelCloseActionPerformed
        // TODO add your handling code here:
        alarmPanel.setVisible(false);
    }//GEN-LAST:event_alarmPanelCloseActionPerformed

    private void alarmMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_alarmMenuActionPerformed
        // Display Alarm Panel:
        alarmPanel.setVisible(true);
    }//GEN-LAST:event_alarmMenuActionPerformed

    private void analyseCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_analyseCloseActionPerformed
        // Close Analyse Menu:
        analysePanel.setVisible(false);
    }//GEN-LAST:event_analyseCloseActionPerformed

    private void cannyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cannyActionPerformed
        // TODO add your handling code here:

    }//GEN-LAST:event_cannyActionPerformed

    private void photoJTreeMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_photoJTreeMouseClicked
        // TODO add your handling code here:
        JTreeVar = photoJTree.getSelectionPath().toString().replaceAll("[\\[\\]]", "").replace(", ", "\\");
        
        
    }//GEN-LAST:event_photoJTreeMouseClicked

    private void open_file_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_open_file_btnActionPerformed
        // TODO add your handling code here:
        try{
            File Selection = new File(JTreeVar);
            
            if(Selection.exists()){
                
                if(Desktop.isDesktopSupported()){
                    
                    Desktop.getDesktop().open(Selection);
                }else{
                  JOptionPane.showMessageDialog(this, "Awt Desktop is not Supported!", "Error", 
                        JOptionPane.INFORMATION_MESSAGE);   
                }
                
            }else{
             
                JOptionPane.showMessageDialog(this, "File does not Exist!", "Error", 
                        JOptionPane.INFORMATION_MESSAGE);
            }
        
        }catch(Exception ex){
        
            ex.printStackTrace();
        }
            
            
            
            
    }//GEN-LAST:event_open_file_btnActionPerformed

    private void videoJTreeMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_videoJTreeMouseClicked
        // TODO add your handling code here:
    }//GEN-LAST:event_videoJTreeMouseClicked

    private void open_vid_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_open_vid_btnActionPerformed
        // TODO add your handling code here:
        
    }//GEN-LAST:event_open_vid_btnActionPerformed

    private void lightThemeBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lightThemeBtnActionPerformed
        //Change to "Light Theme":
        statusPanel.setBackground(new java.awt.Color(152, 204, 244));
        liveMediaPanel.setBackground(new java.awt.Color(152, 204, 244));
        photoPanel.setBackground(new java.awt.Color(152, 204, 244));
        videoPanel.setBackground(new java.awt.Color(152, 204, 244));
    }//GEN-LAST:event_lightThemeBtnActionPerformed

    private void darkThemeBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_darkThemeBtnActionPerformed
        //Change to "Dark Theme":
        statusPanel.setBackground(new java.awt.Color(0, 51, 102));
        liveMediaPanel.setBackground(new java.awt.Color(0, 51, 102));
        photoPanel.setBackground(new java.awt.Color(0, 51, 102));
        videoPanel.setBackground(new java.awt.Color(0, 51, 102));
        videoTreeLbl.setForeground(new java.awt.Color(255, 0, 0));
    }//GEN-LAST:event_darkThemeBtnActionPerformed

    private void defaultThemeBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_defaultThemeBtnActionPerformed
        // Restore to the "default Theme":
        statusPanel.setBackground(new java.awt.Color(0, 51, 102));
        videoTreeLbl.setForeground(new java.awt.Color(0, 51, 102));
        liveMediaPanel.setBackground(new java.awt.Color(102, 153, 255)); 
        photoPanel.setBackground(new java.awt.Color(102, 153, 255)); 
        videoPanel.setBackground(new java.awt.Color(102, 153, 255)); 
        
        
    }//GEN-LAST:event_defaultThemeBtnActionPerformed

    /**
     * @param args the command line arguments
     */
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
            java.util.logging.Logger.getLogger(HomePage.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(HomePage.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(HomePage.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(HomePage.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        
        
        
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                HomePage hp = new HomePage();
                hp.setVisible(true);
                hp.setLocationRelativeTo(null);
               
            }
        });
        
        //System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox CheckBoxAlarm;
    private javax.swing.JCheckBox CheckBoxMotionDetection;
    private javax.swing.JCheckBox CheckBoxSave;
    private javax.swing.JButton FDclose;
    private javax.swing.JButton FDoff;
    private javax.swing.JButton FDon;
    public javax.swing.JPanel FDpanel;
    private javax.swing.JTextField SaveLocationTxt;
    private javax.swing.JSlider SliderSensitivity;
    private javax.swing.JMenu ThemeMenu;
    private javax.swing.JMenu aiMenu;
    private javax.swing.JMenuItem alarmMenu;
    public javax.swing.JPanel alarmPanel;
    private javax.swing.JButton alarmPanelClose;
    private javax.swing.JButton analyseClose;
    private javax.swing.JMenuItem analyseMenu;
    public javax.swing.JPanel analysePanel;
    private javax.swing.JButton browseBtn;
    private javax.swing.JToggleButton camSwitchBtn;
    private javax.swing.JCheckBox canny;
    private javax.swing.JButton clearBtn;
    private javax.swing.JMenuItem darkThemeBtn;
    private javax.swing.JMenuItem defaultThemeBtn;
    private javax.swing.JMenuItem detectionMenu;
    private javax.swing.JMenuItem exitBtn;
    private javax.swing.JLabel imagedisplay;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem11;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTabbedPane jTabbedPane2;
    private javax.swing.JMenuItem lightThemeBtn;
    private javax.swing.JPanel liveMediaPanel;
    private javax.swing.JButton mdClose;
    public javax.swing.JPanel mdPanel;
    private javax.swing.JSlider mdSensitivity;
    private javax.swing.JButton open_file_btn;
    private javax.swing.JButton open_vid_btn;
    private javax.swing.JTree photoJTree;
    private javax.swing.JPanel photoPanel;
    private javax.swing.JMenuItem refreshBtn;
    public javax.swing.JLabel statusLabel;
    public javax.swing.JLabel statusLbl;
    private javax.swing.JPanel statusPanel;
    private javax.swing.JPanel storedMedia;
    private javax.swing.JSlider threshold;
    private javax.swing.JMenuItem trackingMenu;
    private javax.swing.JMenuItem trainMenu;
    private javax.swing.JTree videoJTree;
    private javax.swing.JPanel videoPanel;
    private javax.swing.JLabel videoTreeLbl;
    private javax.swing.JPanel zone1screen;
    private javax.swing.JComboBox<String> zoneSelect;
    // End of variables declaration//GEN-END:variables

     // Method to resize imageIcon with the same size of a Jlabel
    public ImageIcon ResizeImage(String ImagePath)
    {
        ImageIcon MyImage = new ImageIcon(ImagePath);
        Image img = MyImage.getImage();
        Image newImg = img.getScaledInstance(imagedisplay.getWidth(), imagedisplay.getHeight(), Image.SCALE_SMOOTH);
        ImageIcon image = new ImageIcon(newImg);
        return image;
    }




    
    //Method to Play Alarm tone:
     public void PlayBeep(){
        AudioClip clip = Applet.newAudioClip(getClass().getResource("/project1/sounds/Error-sound.wav"));
        clip.play();
         
    }

    private void start() {
        //Start Button Clicked:
        
    if(!begin){
     statusLbl.setText("Live:Cam 0"); 

     int source = Integer.parseInt(zoneSelect.getSelectedItem().toString());
     
            if(zoneSelect.equals("")){
            statusLbl.setText("No cameras found!");	
            }
     
    

      video = new VideoCapture(source);

      if(video.isOpened())
      {
        thread = new CaptureThread();
        thread.start();
        begin = true;
        firstFrame = true;
      }
    }
    }
    
      private void stop()
  {
    //Stop Button Clicked:

    if(begin){
        
      try
      {
        Thread.sleep(500);
      }
      catch(Exception ex)
      {
      }
      video.release();
      begin = false;
    }
    statusLbl.setText("Camera View Off");
    

  }


 public ArrayList<Rect> detection_contours(Mat frame, Mat outmat)
  {
    Mat v = new Mat();
    Mat vv = outmat.clone();
    List<MatOfPoint> contours = new ArrayList();
    Imgproc.findContours(vv, contours, v, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

    double maxArea = 100;
    int maxAreaIdx;
    Rect r;
    ArrayList<Rect> rect_array = new ArrayList();

    for(int idx = 0; idx < contours.size(); idx++)
    {
      Mat contour = contours.get(idx);
      double contourarea = Imgproc.contourArea(contour);
      if(contourarea > maxArea)
      {
        // maxArea = contourarea;
        maxAreaIdx = idx;
        r = Imgproc.boundingRect(contours.get(maxAreaIdx));
        rect_array.add(r);
        Imgproc.drawContours(frame, contours, maxAreaIdx, new Scalar(0, 0, 255));
      }
    }

    v.release();
    return rect_array;
  }
      
  public static String getCurrentTimeStamp()
  {
    SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");//dd/MM/yyyy
    Date now = new Date();
    String strDate = sdfDate.format(now);
    return strDate;
  }     
      
  
  public void addDate(){
      Date currentDate = GregorianCalendar.getInstance().getTime();
      DateFormat df = DateFormat.getDateInstance();
      String dateString = df.format(currentDate);

                Label date = new Label();
                date.setForeground(Color.BLACK);
                date.setBackground(null);
                date.setText(dateString);
                
                zone1screen.add(date);
  }
      
String JTreeVar;



}
