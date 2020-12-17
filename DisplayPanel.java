package project1;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

class DisplayPanel extends JPanel
{
  private Image img;

  public DisplayPanel(String img)
  {
    this(new ImageIcon(img).getImage());
  }

  public DisplayPanel(Image img)
  {
    this.img = img;
    Dimension size = new Dimension(img.getWidth(null), img.getHeight(null));
    setPreferredSize(size);
    setMinimumSize(size);
    setMaximumSize(size);
    setSize(size);
    setLayout(null);
    setBorder(BorderFactory.createLineBorder(Color.black));
  }

  public void updateImage(Image img)
  {
    this.img = img;
    validate();
    repaint();
  }

  @Override
  public void paintComponent(Graphics g)
  {
    g.drawImage(img, 0, 0, null);
  }
}
