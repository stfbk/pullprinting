package eu.enterprise.st.pullprinting.Utilities;


import java.util.HashMap;

public class Commons {
    public static String ipaddress = "pullprinting.enterprise.eu";
    public static String spoolerID = "0274460a-ebc5-3c57-174e-a386afa40f2f";
    public static String spoolerName = "FBK_Virtual_Spooler";
    public static String SecureSpoolerID = "0cb19594-f389-37e6-a417-a436592ab448";
    public static String SecureSpoolerName = "FBK_Secure_Virtual_Spooler";
    public static String messaggioLavoroCompleto= "The jobs were successfully sent to the selected printer.";
    public static String NORD_PP_ColorICT_model = "TOSHIBA e-STUDIO4555C";
    public static String NORD_AMM_ColorCopier_model = "ECOSYS P6230cdn";
    public static String acutalPrinter="";
    public static String acutalModel="";

    public static HashMap<String,Double> copy_speed_map = new HashMap<>();
    public static HashMap<String,Double> first_copy_map = new HashMap<String, Double>();

    public static int limit = 50;
    public static int acutalPageNumberBeforePrint=0;


}
