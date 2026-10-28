import java.awt.*;
import java.awt.event.*;

// draws the aeroplane seat map (front of the plane on the left, A B C | aisle | D E F)
// seat number i is in row i / 6 and its letter is i % 6 (0 = A, 1 = B ... 5 = F)
public class SeatMap extends Canvas implements MouseListener
{
    SeatSelectionFrame frame;
    String[] seatNos = new String[0];  // 1A, 1B, ... (filled by SeatSelectionFrame)
    String[] status = new String[0];   // AVAILABLE or BOOKED
    boolean[] picked = new boolean[0]; // true if the seat is selected
    int count = 0;                     // number of selected seats
    int left = 0;                      // x where the plane starts, so the plane comes in the middle

    public SeatMap(SeatSelectionFrame f)
    {
        frame = f;
        setBackground(new Color(232, 240, 250));

        addMouseListener(this); // clicking a seat selects it
    }

    // x position of seat i (every row is 32 pixels after the previous row)
    int seatX(int i)
    {
        return left + 130 + (i / 6) * 32;
    }

    // y position of seat i (every seat is 34 pixels below the previous one, D E F go 30 pixels lower for the aisle)
    int seatY(int i)
    {
        int y = 45 + (i % 6) * 34;
        if (i % 6 >= 3)
            y = y + 30;
        return y;
    }

    // clicking a seat selects it, clicking it again removes it
    public void mousePressed(MouseEvent e)
    {
        for (int i = 0; i < seatNos.length; i++)
        {
            // checking if the mouse is inside seat i (one seat is 28 x 28)
            if (e.getX() >= seatX(i) && e.getX() <= seatX(i) + 28 && e.getY() >= seatY(i) && e.getY() <= seatY(i) + 28)
            {
                if (!status[i].equals("AVAILABLE"))
                    return; // booked seat cannot be selected

                if (picked[i])
                {
                    picked[i] = false;
                    count--;
                }
                else if (count == 6)
                    new MessageBox(frame, "You can book maximum 6 seats at a time");
                else
                {
                    picked[i] = true;
                    count++;
                }
                repaint();
                frame.updateSelected();
            }
        }
    }

    // other mouse events are not used (MouseListener needs all 5 methods)
    public void mouseClicked(MouseEvent e) {}
    public void mouseReleased(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}

    // repaint() normally clears the screen first and it flickers, so here it directly calls paint()
    public void update(Graphics g)
    {
        paint(g);
    }

    public void paint(Graphics g)
    {
        int rows = (seatNos.length + 5) / 6;  // 6 seats in one row
        int width = 130 + rows * 32 + 50;     // length of the plane
        if (width < 600)
            width = 600;                      // small flights also look like a plane
        left = (getWidth() - width) / 2;

        // plane body: one long rectangle with round corners
        g.setColor(new Color(250, 251, 253));
        g.fillRoundRect(left, 15, width, 300, 200, 300);
        g.setColor(new Color(160, 172, 190));
        g.drawRoundRect(left, 15, width, 300, 200, 300);

        // two cockpit windows and the word FRONT
        g.setColor(new Color(22, 54, 110));
        g.fillRoundRect(left + 25, 128, 14, 32, 10, 10);
        g.fillRoundRect(left + 25, 170, 14, 32, 10, 10);
        g.setColor(Color.gray);
        g.setFont(new Font("Arial", Font.BOLD, 13));
        g.drawString("FRONT", left + 50, 170);

        // seat letters A to F on the left side and row numbers in the aisle
        for (int c = 0; c < 6; c++)
            g.drawString("" + "ABCDEF".charAt(c), left + 110, seatY(c) + 19);
        for (int r = 0; r < rows; r++)
            g.drawString("" + (r + 1), seatX(r * 6) + 8, 164);

        // seats: white = available, green = selected, grey with X = booked
        for (int i = 0; i < seatNos.length; i++)
        {
            String text = "" + "ABCDEF".charAt(i % 6);
            Color fill = Color.white;
            Color textColor = new Color(22, 54, 110);
            if (picked[i])
            {
                fill = new Color(40, 160, 90);
                textColor = Color.white;
            }
            else if (!status[i].equals("AVAILABLE"))
            {
                fill = new Color(200, 204, 211);
                textColor = Color.white;
                text = "X";
            }

            g.setColor(fill);
            g.fillRoundRect(seatX(i), seatY(i), 28, 28, 10, 10);
            g.setColor(new Color(150, 160, 178));
            g.drawRoundRect(seatX(i), seatY(i), 28, 28, 10, 10);
            g.setColor(textColor);
            g.drawString(text, seatX(i) + 9, seatY(i) + 19);
        }
    }
}
