package eu.fbk.st.pullprinting.Utilities;


import java.util.HashMap;

public class Commons {
    public static String ipaddress = "pullprinting.fbk.eu";
    public static String spoolerID = "b24aab27-7611-e9e0-e054-04c6ea62c20a";
    public static String spoolerName = "FBK_Virtual_Spooler";
    public static String SecureSpoolerID = "ce08a886-c2e5-8ec6-7e1a-f4150f1bd688";
    public static String SecureSpoolerName = "FBK_Secure_Virtual_Spooler";
    public static String messaggioLavoroCompleto= "I lavori sono stati mandati correttamente nella stampante selezionata.";
    public static String NORD_PP_ColorICT_model = "TOSHIBA e-STUDIO4555C";
    public static String NORD_AMM_ColorCopier_model = "ECOSYS P6230cdn";
    public static String acutalPrinter="";
    public static String acutalModel="";

    public static HashMap<String,Double> copy_speed_map = new HashMap<>();
    public static HashMap<String,Double> first_copy_map = new HashMap<String, Double>();

    public static int limit = 50;
    public static int acutalPageNumberBeforePrint=0;


}
