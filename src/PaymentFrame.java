import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import com.google.zxing.*;
import com.google.zxing.common.*;
import com.google.zxing.qrcode.*;

// shows the total fare and a QR code, customer scans it with the phone and pays on the demo website
// seats are booked only after "Payment Done"
public class PaymentFrame extends Frame implements ActionListener, WindowListener
{
    Label l1, l3, l4, l5, l6, l7, statusLabel;
    Button retryBtn, cancelBtn;
    Panel p1, p2;
    Canvas qrCanvas;
    BitMatrix qr = null; // black and white squares of the QR code (made by ZXing)
    PassengerDetailsFrame detailsFrame;
    int customerId, flightId, count;
    String seats, ref = "", pnr = "";
    double fare, gst, amount;
    boolean done = false; // true after the booking is confirmed

    public PaymentFrame(PassengerDetailsFrame pd, int cid, int fid, String s, int c, double a)
    {
        detailsFrame = pd;
        customerId = cid;
        flightId = fid;
        seats = s;
        count = c;
        fare = a;                 // base fare of all passengers
        gst = fare * 5 / 100;     // 5% GST
        amount = fare + gst;      // amount the customer pays

        // setting the layout
        setLayout(new GridLayout(1, 2, 10, 0)); // left side details, right side QR code

        // creating objects
        l1 = new Label("QR PAYMENT", Label.CENTER);
        l3 = new Label("");
        l4 = new Label("Seats: " + seats + "   (" + count + " passengers)");
        l5 = new Label("Base Fare: Rs. " + String.format("%.2f", fare) + "   +   GST (5%): Rs. " + String.format("%.2f", gst));
        l6 = new Label("Total Amount: Rs. " + String.format("%.2f", amount));
        l7 = new Label("Scan the QR code with your phone");
        statusLabel = new Label("");
        retryBtn = new Button("Retry Payment");
        cancelBtn = new Button("Cancel");
        p1 = new Panel(new GridLayout(8, 1, 0, 5));
        p2 = new Panel(new GridLayout(1, 2, 10, 0));

        // canvas that draws the QR code square by square
        qrCanvas = new Canvas()
        {
            public void paint(Graphics g)
            {
                if (qr == null)
                    return;
                int box = (Math.min(getWidth(), getHeight()) - 20) / qr.getWidth(); // size of one small square
                int x0 = (getWidth() - box * qr.getWidth()) / 2;
                int y0 = (getHeight() - box * qr.getHeight()) / 2;
                g.setColor(Color.white);
                g.fillRect(x0, y0, box * qr.getWidth(), box * qr.getHeight());
                g.setColor(Color.black);
                for (int x = 0; x < qr.getWidth(); x++)
                    for (int y = 0; y < qr.getHeight(); y++)
                        if (qr.get(x, y))
                            g.fillRect(x0 + x * box, y0 + y * box, box, box);
            }
        };

        // design
        setBackground(new Color(232, 240, 250));
        setFont(new Font("Arial", Font.PLAIN, 15));
        l1.setBackground(new Color(22, 54, 110));
        l1.setForeground(Color.white);
        l1.setFont(new Font("Arial", Font.BOLD, 22));
        l6.setFont(new Font("Arial", Font.BOLD, 20));
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));

        // event handling
        retryBtn.addActionListener(this);
        cancelBtn.addActionListener(this);

        // adding components
        p2.add(retryBtn);
        p2.add(cancelBtn);
        p1.add(l1);
        p1.add(l3);
        p1.add(l4);
        p1.add(l5);
        p1.add(l6);
        p1.add(l7);
        p1.add(statusLabel);
        p1.add(p2);
        add(p1);
        add(qrCanvas);

        // closing the window is same as Cancel
        addWindowListener(this);

        // creating the frame
        setTitle("Payment");
        setSize(900, 450);
        setLocationRelativeTo(null);
        setVisible(true);

        newPayment();
    }

    // saves a new PENDING payment in MySQL and shows its QR code
    void newPayment()
    {
        try
        {
            ref = "PAY" + TicketService.randomCode(6); // payment reference like PAY3F2A9C
            Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("INSERT INTO payments (payment_ref, customer_id, flight_id, seats, passengers, amount, status) VALUES (?, ?, ?, ?, ?, ?, 'PENDING')");
            ps.setString(1, ref);
            ps.setInt(2, customerId);
            ps.setInt(3, flightId);
            ps.setString(4, seats);
            ps.setInt(5, count);
            ps.setDouble(6, amount);
            ps.executeUpdate();
            con.close();

            // the link of the payment website is put inside the QR code
            String url = "http://" + PaymentServer.getLaptopIp() + ":5173/?ref=" + ref;
            qr = new QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, 1, 1);
            qrCanvas.repaint();
            PaymentServer.current = this; // so the server can tell this window when payment is done
            l3.setText("Payment Ref: " + ref);
            statusLabel.setForeground(new Color(200, 120, 0));
            statusLabel.setText("Waiting for payment on phone...");
            retryBtn.setEnabled(false);
        }
        catch (Exception e)
        {
            System.out.println(e);
            new MessageBox(this, "Error: " + e.getMessage());
        }
    }

    // called by PaymentServer when "Payment Done" is clicked: books the seats and returns the PNR
    public String paymentSuccess() throws Exception
    {
        try
        {
            pnr = detailsFrame.confirmBooking();
        }
        catch (Exception e)
        {
            // payment was done but seats are gone, so payment is marked FAILED (demo, no money taken)
            PaymentServer.update("UPDATE payments SET status = 'FAILED' WHERE payment_ref = ?", ref);
            statusLabel.setForeground(Color.red);
            statusLabel.setText("Booking failed: " + e.getMessage());
            detailsFrame.seatFrame.loadSeats();
            throw e;
        }

        // saving the PNR with the payment
        Connection con = DBConnection.getConnection();
        PreparedStatement ps = con.prepareStatement("UPDATE payments SET pnr = ? WHERE payment_ref = ?");
        ps.setString(1, pnr);
        ps.setString(2, ref);
        ps.executeUpdate();
        con.close();

        done = true;
        statusLabel.setForeground(new Color(30, 130, 70));
        statusLabel.setText("PAYMENT SUCCESSFUL!  PNR: " + pnr);
        return pnr;
    }

    // called by PaymentServer after replying to the phone, makes the PDF ticket and emails it
    public void makeTicket()
    {
        String msg = "Confirmed! PNR: " + pnr;
        try
        {
            String path = TicketService.makePdf(pnr);
            msg = msg + " | Ticket saved";
            TicketService.sendEmail(pnr, path);
            msg = msg + " and emailed";
        }
        catch (Exception e)
        {
            msg = msg + " | Email failed";
        }
        statusLabel.setText(msg);
        cancelBtn.setLabel("  Close  ");
        PaymentServer.current = null;
    }

    // called by PaymentServer when "Payment Failed" is clicked on the phone
    public void paymentFailed()
    {
        statusLabel.setForeground(Color.red);
        statusLabel.setText("PAYMENT FAILED. Click Retry Payment.");
        retryBtn.setEnabled(true);
    }

    // event handling method
    public void actionPerformed(ActionEvent e)
    {
        Button btn = (Button) e.getSource();

        if (btn == retryBtn)
            newPayment(); // old payment stays FAILED, a new payment with a new QR code is made
        else
            closeWindow();
    }

    void closeWindow()
    {
        if (done)
            detailsFrame.bookingDone(); // closes passenger and seat windows also
        else
        {
            // payment not done, so it is marked FAILED and no seat is booked
            PaymentServer.update("UPDATE payments SET status = 'FAILED' WHERE payment_ref = ? AND status = 'PENDING'", ref);
            PaymentServer.current = null;
        }
        dispose();
    }

    // window events (only closing is used, WindowListener needs all 7 methods)
    public void windowClosing(WindowEvent we)
    {
        closeWindow();
    }
    public void windowOpened(WindowEvent we) {}
    public void windowClosed(WindowEvent we) {}
    public void windowIconified(WindowEvent we) {}
    public void windowDeiconified(WindowEvent we) {}
    public void windowActivated(WindowEvent we) {}
    public void windowDeactivated(WindowEvent we) {}
}
