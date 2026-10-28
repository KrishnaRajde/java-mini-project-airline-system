import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class RegisterFrame extends Frame implements ActionListener, WindowListener
{
    Label l1, l2, l3, l4, l5, l6, l7;
    TextField nameTf, emailTf, phoneTf, passTf, confirmTf;
    Button registerBtn, backBtn;

    public RegisterFrame()
    {
        // setting the layout (2 columns, like label and text field)
        setLayout(new GridLayout(7, 2, 0, 10));

        // creating objects
        l1 = new Label("   CREATE ACCOUNT");
        l7 = new Label("");
        l2 = new Label("    Full Name");
        l3 = new Label("    Email");
        l4 = new Label("    Phone (10 digits)");
        l5 = new Label("    Password");
        l6 = new Label("    Confirm Password");
        nameTf = new TextField(20);
        emailTf = new TextField(20);
        phoneTf = new TextField(20);
        passTf = new TextField(20);
        confirmTf = new TextField(20);
        registerBtn = new Button("Register");
        backBtn = new Button("Back");

       
        setBackground(new Color(232, 240, 250));
        setFont(new Font("Arial", Font.PLAIN, 15));
        l1.setBackground(new Color(22, 54, 110));
        l1.setForeground(Color.white);
        l1.setFont(new Font("Arial", Font.BOLD, 22));
        l7.setBackground(new Color(22, 54, 110));
        passTf.setEchoChar('*');
        confirmTf.setEchoChar('*');

       
        registerBtn.addActionListener(this);
        backBtn.addActionListener(this);

        
        add(l1);
        add(l7);
        add(l2);
        add(nameTf);
        add(l3);
        add(emailTf);
        add(l4);
        add(phoneTf);
        add(l5);
        add(passTf);
        add(l6);
        add(confirmTf);
        add(registerBtn);
        add(backBtn);

       
        addWindowListener(this);

       
        setTitle("Customer Registration");
        setSize(480, 450);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // event handling method
    public void actionPerformed(ActionEvent e)
    {
        Button btn = (Button) e.getSource();
        String name = nameTf.getText().trim();
        String email = emailTf.getText().trim();
        String phone = phoneTf.getText().trim();
        String pass = passTf.getText();

       
        if (btn == backBtn)
            dispose();
        else if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || pass.isEmpty())
            new MessageBox(this, "All fields are required");
        else if (!email.matches("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"))
            new MessageBox(this, "Please enter a valid email");
        else if (!phone.matches("[0-9]{10}"))
            new MessageBox(this, "Phone number must be 10 digits");
        else if (pass.length() < 4)
            new MessageBox(this, "Password must be at least 4 characters");
        else if (!pass.equals(confirmTf.getText()))
            new MessageBox(this, "Passwords do not match");
        else
        {
            try
            {
                Connection con = DBConnection.getConnection();
                PreparedStatement ps = con.prepareStatement("INSERT INTO customers (name, email, phone, password) VALUES (?, ?, ?, ?)");
                ps.setString(1, name);
                ps.setString(2, email);
                ps.setString(3, phone);
                ps.setString(4, pass);
                ps.executeUpdate();
                con.close();
                new MessageBox(this, "Registration successful! You can login now.");
                dispose();
            }
            catch (Exception ex)
            {
                System.out.println(ex);
                new MessageBox(this, "Email already registered or error: " + ex.getMessage());
            }
        }
    }

   
    public void windowClosing(WindowEvent we)
    {
        dispose();
    }
    public void windowOpened(WindowEvent we) {}
    public void windowClosed(WindowEvent we) {}
    public void windowIconified(WindowEvent we) {}
    public void windowDeiconified(WindowEvent we) {}
    public void windowActivated(WindowEvent we) {}
    public void windowDeactivated(WindowEvent we) {}
}
