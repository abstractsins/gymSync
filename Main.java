
public class Main {   
    public static void main(String[] args) {

        System.out.println("##### PROGRAM START #####");

        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                App.createAndShowGUI();
            }
        });   
            
    }
}