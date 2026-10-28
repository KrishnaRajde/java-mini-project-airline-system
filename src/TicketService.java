import java.io.*;
import java.sql.*;
import java.util.*;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;

// 4 static methods used by other windows:
// getDetails() -> booking details as text
// makePdf()    -> makes the PDF ticket
// sendEmail()  -> sends the PDF ticket on email
// randomCode() -> random code for PNR and payment reference
public class TicketService
{
    // returns the booking details of a PNR as text (shown in TextArea, PDF and email)
    public static String getDetails(String pnr) throws Exception
    {
        Connection con = DBConnection.getConnection();

        // query 1: flight and customer details of the PNR (same for all passengers, so only 1 row is needed)
        PreparedStatement ps1 = con.prepareStatement("SELECT * FROM bookings b JOIN flights f ON b.flight_id = f.flight_id "
                + "JOIN customers c ON b.customer_id = c.customer_id WHERE b.pnr = ? LIMIT 1");
        ps1.setString(1, pnr);
        ResultSet rs1 = ps1.executeQuery();
        if (!rs1.next())
        {
            con.close();
            throw new Exception("Booking not found");
        }

        String text = "PNR          : " + pnr + "\n";
        text = text + "Booked By    : " + rs1.getString("name") + "\n";
        text = text + "Flight       : " + rs1.getString("flight_no") + " (" + rs1.getString("airline") + ")\n";
        text = text + "Route        : " + rs1.getString("source") + " -> " + rs1.getString("destination") + "\n";
        text = text + "Departure    : " + rs1.getString("departure_time").substring(0, 16) + "\n";
        text = text + "Arrival      : " + rs1.getString("arrival_time").substring(0, 16) + "\n";
        text = text + "\nPASSENGERS\n";

        // query 2: every passenger of the PNR (one row = one passenger)
        PreparedStatement ps2 = con.prepareStatement("SELECT * FROM bookings b JOIN seats s ON b.seat_id = s.seat_id WHERE b.pnr = ? ORDER BY s.seat_id");
        ps2.setString(1, pnr);
        ResultSet rs2 = ps2.executeQuery();

        int n = 0;
        double total = 0;
        while (rs2.next())
        {
            n++;
            text = text + n + ". " + rs2.getString("passenger_name") + " (" + rs2.getInt("passenger_age") + ", " + rs2.getString("passenger_gender") + ")"
                    + "   Seat " + rs2.getString("seat_no") + "   " + rs2.getString("status") + "   Rs. " + rs2.getDouble("ticket_price") + "\n";

            // cancelled tickets are not added in the total
            if (rs2.getString("status").equals("CONFIRMED"))
                total = total + rs2.getDouble("ticket_price");
        }
        con.close();

        // fare details like a real ticket (GST is 5% of the base fare)
        double gst = total * 5 / 100;
        text = text + "\n------------------------------------------\n";
        text = text + "FARE DETAILS\n";
        text = text + "Base Fare    : Rs. " + String.format("%.2f", total) + "\n";
        text = text + "GST (5%)     : Rs. " + String.format("%.2f", gst) + "\n";
        text = text + "Total Fare   : Rs. " + String.format("%.2f", total + gst) + "\n";
        text = text + "------------------------------------------";
        return text;
    }

    // makes the PDF ticket in the tickets folder and returns its path, eg. tickets/ticket_3F2A9C.pdf
    public static String makePdf(String pnr) throws Exception
    {
        String[] lines = getDetails(pnr).split("\n"); // details text, line by line
        new File("tickets").mkdirs();                 // makes the tickets folder if it is not there
        String path = "tickets/ticket_" + pnr + ".pdf";

        // new PDF with one page
        PDDocument doc = new PDDocument();
        PDPage page = new PDPage();
        doc.addPage(page);

        // cs is like a pen to write on the page
        PDPageContentStream cs = new PDPageContentStream(doc, page);
        cs.beginText();
        cs.setLeading(18);               // gap between two lines
        cs.newLineAtOffset(50, 730);     // starting point (0,0 is the bottom left corner of the page)

        // heading in big bold font
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 20);
        cs.showText("SkyWay Airlines - E-Ticket");
        cs.newLine();
        cs.newLine();

        // details in normal font, one line at a time
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
        for (int i = 0; i < lines.length; i++)
        {
            cs.showText(lines[i]);
            cs.newLine();
        }
        cs.newLine();
        cs.showText("Please reach the airport 2 hours before departure with a valid photo ID.");
        cs.endText();
        cs.close();

        // saving the file
        doc.save(path);
        doc.close();
        return path;
    }

    // sends the PDF ticket to the customer's email using Gmail
    public static void sendEmail(String pnr, String pdfPath) throws Exception
    {
        // 1. reading our gmail id and app password from config.properties
        Properties config = new Properties();
        FileInputStream fis = new FileInputStream("config.properties");
        config.load(fis);
        fis.close();
        String from = config.getProperty("mail.username");
        String pass = config.getProperty("mail.password");

        // 2. finding the customer's name and email from the database
        Connection con = DBConnection.getConnection();
        PreparedStatement ps = con.prepareStatement("SELECT * FROM bookings b JOIN customers c ON b.customer_id = c.customer_id WHERE b.pnr = ? LIMIT 1");
        ps.setString(1, pnr);
        ResultSet rs = ps.executeQuery();
        rs.next();
        String name = rs.getString("name");
        String to = rs.getString("email");
        con.close();

        // 3. gmail server settings
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true"); // secure connection
        Session session = Session.getInstance(props);

        // 4. making the email: from, to and subject
        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(from));
        msg.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
        msg.setSubject("Your E-Ticket - PNR " + pnr);

        // 5. email has 2 parts: the message text and the PDF file
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText("Dear " + name + ",\n\nThank you for booking with SkyWay Airlines.\nYour ticket is attached with this email.\n\nPNR: " + pnr + "\n\nHappy journey!");
        MimeBodyPart filePart = new MimeBodyPart();
        filePart.attachFile(new File(pdfPath));

        Multipart mp = new MimeMultipart();
        mp.addBodyPart(textPart);
        mp.addBodyPart(filePart);
        msg.setContent(mp);

        // 6. sending the email (login with gmail id and app password)
        Transport.send(msg, from, pass);
    }

    // random code like 3F2A9C (used for PNR and payment reference)
    public static String randomCode(int length)
    {
        String id = UUID.randomUUID().toString(); // eg. 3f2a9c1e-7b4d-4e8a-9f12-5c6d7e8f9a0b
        id = id.replace("-", "");                 // removing the dashes
        id = id.toUpperCase();                    // small letters to capital letters
        return id.substring(0, length);           // taking only the first 6 characters
    }
}
