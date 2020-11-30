package pack;

import javacard.framework.*;

/**
 *
 * @author OUSSAMA OULKAID
 */
public class MyProject extends Applet {
    
    public static final byte ma_classe = (byte) 0x80;
    public static final byte ins_SELECT_FILE = (byte) 0xA4;
    public static final byte ins_READ_RECORD = (byte) 0xB2;
    public static final byte ins_WRITE_RECORD = (byte) 0xD2;
    public static final byte ins_SUBMIT_CODE = (byte) 0x20;
    public static final byte ins_CLEAR_CARD = (byte) 0x30;
    
    public static byte [] FF02 = {(byte)0x00}; //NBRE_OF_FILE
    // IC=0xAABBCCDD AND PIN=123456789(decimal) ou: BC614E(hexadecimal) (codes initiaux)
    public static byte [] FF03 = {(byte)0xAA, (byte)0xBB, (byte)0xCC, (byte)0xDD, (byte)0x00, (byte)0xBC, (byte)0x61, (byte)0x4E}; //IC and PIN
   
    // public static final byte [][] FF04 = new byte[30][6]; //unsuported
    public static byte [] FF04 = new byte[30*6];
    
    public static byte [] AA00 = new byte[1024]; // fichier transparent: ensemble des finchiers utilisateur, gérées par l'offset, la taille et nbre d'enregistrements
    public static byte [] Offset = new byte[30]; // on stock les offset de chacun des fichers user (30 au maximum)
    //P1=numéro de l'enregistrement, //P2=0x04(ie. 100): l'enregistrement identifié par P1
    
    public static byte USER_FILE_MSB = (byte) 0x00;
    public static byte USER_FILE_LSB = (byte) 0x00;
    
    public static byte [] USER_IC = new byte[30];
    public static byte [] USER_PIN = new byte[30];
    
    private byte FF02_selected;
    private byte FF03_selected;
    private byte FF04_selected;
    private byte Data_length;
    private byte PIN_verified;
    private byte IC_verified;
    private byte NBR_ACTUAL_USER_FILES;
    private byte index;
    private byte USER_FILE_selected;
    private int NBR_USER_IC_verified;
    private int NBR_USER_PIN_verified;
    private int Actual_File_IC;
    private int Actual_File_PIN;
    private int found;
    private int NBRE_TRIES_OF_CODE;

    /**
     * Installs this applet.
     * 
     * @param bArray
     *            the array containing installation parameters
     * @param bOffset
     *            the starting offset in bArray
     * @param bLength
     *            the length in bytes of the parameter data in bArray
     */
    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new MyProject();
    }

    /**
     * Only this class's install method should create the applet object.
     */
    protected MyProject() {
        register();
        this.FF02_selected = 0;
        this.FF03_selected = 0;
        this.FF04_selected = 0;
        this.Data_length = 0;
        this.PIN_verified = 0;
        this.IC_verified = 0;
        this.NBR_ACTUAL_USER_FILES = 0;
        this.USER_FILE_selected = 0;
        this.NBR_USER_IC_verified = 0;
        this.NBR_USER_PIN_verified = 0;
        this.NBRE_TRIES_OF_CODE = 3;
    }

    /**
     * Processes an incoming APDU.
     * 
     * @see APDU
     * @param apdu
     *            the incoming APDU
     */
    public void process(APDU apdu) {
        if(selectingApplet()) return;
        byte[] buf = apdu.getBuffer();
        if(buf[ISO7816.OFFSET_CLA]!=this.ma_classe)
            ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
        
        switch(buf[ISO7816.OFFSET_INS]){
            case ins_SELECT_FILE : 
                if(buf[5]==(byte)0xFF && buf[6]==(byte)0x02){
                    FF02_selected = 1;
                    FF03_selected = 0;
                    FF04_selected = 0;
                    USER_FILE_selected = 0;
                    ISOException.throwIt(ISO7816.SW_NO_ERROR); //User data file was selected seccessfully
                }
                else if(buf[5]==(byte)0xFF && buf[6]==(byte)0x03){
                    FF02_selected = 0;
                    FF03_selected = 1;
                    FF04_selected = 0;
                    USER_FILE_selected = 0;
                    ISOException.throwIt(ISO7816.SW_NO_ERROR); //User data file was selected seccessfully
                }
                else if(buf[5]==(byte)0xFF && buf[6]==(byte)0x04){
                    FF02_selected = 0;
                    FF03_selected = 0;
                    FF04_selected = 1;
                    USER_FILE_selected = 0;
                    ISOException.throwIt(ISO7816.SW_NO_ERROR); //User data file was selected seccessfully
                }
                else{
                    for(int i=0; i<NBR_ACTUAL_USER_FILES; i++){
                        if(buf[5] == FF04[(byte)(i*6 + 4)] && buf[6] == FF04[(byte)(i*6 + 5)]){
                            USER_FILE_MSB = buf[5];
                            USER_FILE_LSB = buf[6];
                            FF02_selected = 0;
                            FF03_selected = 0;
                            FF04_selected = 0;
                            USER_FILE_selected = 1;
                            // le code suivant: pour la cenversion de la valeur de i en hexadecimal format forcé (apparaitre comme un decimal)
                            byte [] i_hex = {0x00, 0x00};
                            int R, k;
                            k = 1; //première valeur sera affectée au LSB de []i_hex
                            while(i != 0){
                                if(k == 1){
                                    R = i%10;
                                    i = i/10;
                                    i_hex[(byte)k] = (byte)R; //LSB
                                    k = 0; //passer à l'indice de MSB de []i_hex
                                }
                                else{
                                    R = i%10;
                                    i = i/10;
                                    if(R == 1)  i_hex[(byte)k] = (byte)(0x0010); //MSB=1
                                    else if(R == 2)  i_hex[(byte)k] = (byte)(0x0020); //MSB=1
                                }
                            }
                            ISOException.throwIt((short) ((short)(0x9100) + (short)i_hex[1] + (short)i_hex[0]));
                            i = NBR_ACTUAL_USER_FILES; //forcer l'arrêt de la boucle for
                            // fin de code de conversion
                        }
                    }
                    if(USER_FILE_selected != 1){
                        FF02_selected = 0;
                        FF03_selected = 0;
                        FF04_selected = 0;
                        USER_FILE_selected = 0;
                        ISOException.throwIt(ISO7816.SW_FILE_NOT_FOUND); //File not found
                    }
                }
            break;
            
            case ins_SUBMIT_CODE :
                Data_length = buf[ISO7816.OFFSET_LC];
                    if(Data_length == 0x08){
                        if(NBRE_TRIES_OF_CODE != 0){ //nous avons un nbre de tentatives non null
                            //initialisation
                            IC_verified = 0;
                            PIN_verified = 0;
                            for(int i=0; i<NBR_ACTUAL_USER_FILES; i++){
                                USER_IC[(byte)i] = 0;
                                USER_PIN[(byte)i] = 0;
                            }
                            //test for admin codes
                            if(buf[5] == FF03[0] && buf[6] == FF03[1] && buf[7] == FF03[2] && buf[8] == FF03[3]){ //IC
                                IC_verified = 1;
                            }
                            if(buf[9] == FF03[4] && buf[10] == FF03[5] && buf[11] == FF03[6] && buf[12] == FF03[7]){ //PIN
                                PIN_verified = 1;
                            }
                            buf[0]=IC_verified;
                            buf[1]=PIN_verified;
                            apdu.setOutgoingAndSend((short)0,(short)2); // IC(admin) | PIN(admin): [if verified set to 1, else set to 0]
                            if(IC_verified == 0 && PIN_verified == 0){ //cette partie traite le code entier
                                NBRE_TRIES_OF_CODE = NBRE_TRIES_OF_CODE - 1;
                                if(NBRE_TRIES_OF_CODE == 0){
                                    ISOException.throwIt((short)0x6983); // code bloqué. données non accessibles! carte n'est plus fonctionnelle
                                }
                                else{
                                ISOException.throwIt((short)((short)0x63C0 + (short)NBRE_TRIES_OF_CODE)); // nobre de tentatives restantes
                                }
                            }
                            else{
                                NBRE_TRIES_OF_CODE = 3;
                            }
                        }
                        else{
                            ISOException.throwIt((short)0x6983); // code bloqué. données non accessibles! carte n'est plus fonctionnelle
                        }
                    }
                    else if(Data_length == 0x02){
                        //initialisation
                        IC_verified = 0;
                        PIN_verified = 0;
                        for(int i=0; i<NBR_ACTUAL_USER_FILES; i++){
                            USER_IC[(byte)i] = 0;
                            USER_PIN[(byte)i] = 0;
                        }
                        //test for user codes
                        Actual_File_IC = 0;
                        Actual_File_PIN = 0;
                        for(int i=0; i<NBR_ACTUAL_USER_FILES; i++){
                            if(buf[5] == FF04[(byte)(i*6 + 2)]){ //IC
                                USER_IC[(byte)i] = 1;
                                NBR_USER_IC_verified = NBR_USER_IC_verified + 1;
                                if(USER_FILE_MSB == FF04[(byte)(i*6 + 4)] && USER_FILE_LSB == FF04[(byte)(i*6 + 5)]){
                                    Actual_File_IC = 1;
                                }
                            }
                            else USER_IC[(byte)i] = 0;
                            if(buf[6] == FF04[(byte)(i*6 + 3)]){ //PIN
                                USER_PIN[(byte)i] = 1;
                                NBR_USER_PIN_verified = NBR_USER_PIN_verified + 1;
                                if(USER_FILE_MSB == FF04[(byte)(i*6 + 4)] && USER_FILE_LSB == FF04[(byte)(i*6 + 5)]){
                                    Actual_File_PIN = 1;
                                }
                            }
                            else USER_PIN[(byte)i] = 0;
                        }
                        buf[0] = (byte)NBR_USER_IC_verified;
                        buf[1] = (byte)NBR_USER_PIN_verified;
                        buf[2] = (byte)Actual_File_IC;
                        buf[3] = (byte)Actual_File_PIN;
                        apdu.setOutgoingAndSend((short)0,(short)4);
                        // structure de la réponde apdu en Le:
                        // NBR_IC_verified(user) | NBR_PIN_verified(user) | selected_file_verification(IC) | selected_file_verification(PIN)
                    }
                    else{
                        ISOException.throwIt((short)0x6A85); //longueur Lc non acceptée
                    }
            break;
            
            case ins_WRITE_RECORD :
                if(IC_verified == 1){
                    if(FF02_selected == 1){
                        Data_length = buf[ISO7816.OFFSET_LC];
                        if(Data_length == 0x01){
                            if(buf[5] <= 0x30){
                                FF02[0] = buf[5];
                                ISOException.throwIt(ISO7816.SW_NO_ERROR);
                            }
                            else{
                                ISOException.throwIt((short)0x6A84); //Pas assez de mémoire: le maximum autorisé est 30 fichier
                            }
                        }
                        else{
                            ISOException.throwIt((short)0x6A85); //longueur Lc non acceptée
                        }
                    }
                    else if(FF03_selected == 1){
                        Data_length = buf[ISO7816.OFFSET_LC];
                        if(Data_length == 0x08){
                            for(int i=0; i<8; i++){
                                FF03[(byte)i] = buf[(byte)(5+i)];
                            }
                            IC_verified = 0; //reinitialize
                            PIN_verified = 0; //reinitialize
                            ISOException.throwIt(ISO7816.SW_NO_ERROR);
                        }
                        else{
                            ISOException.throwIt((short)0x6A85); //longueur Lc non acceptée;
                        }
                    }
                    else if(FF04_selected == 1){
                        if(FF02[0] > NBR_ACTUAL_USER_FILES){
                            Data_length = buf[ISO7816.OFFSET_LC];
                            if(Data_length == 0x06){
                                    for(int i=0; i<6; i++){
                                        FF04[(byte)(NBR_ACTUAL_USER_FILES*6 + i)] = buf[(byte)(5+i)];
                                    }
                                    if(NBR_ACTUAL_USER_FILES == 0){
                                        Offset[(byte)NBR_ACTUAL_USER_FILES] = 0;
                                    }
                                    else{
                                        Offset[(byte)NBR_ACTUAL_USER_FILES] = (byte)(Offset[(byte)(NBR_ACTUAL_USER_FILES - 1)] + (byte)(FF04[(byte)(NBR_ACTUAL_USER_FILES*6 + 0)] * FF04[(byte)(NBR_ACTUAL_USER_FILES*6 + 1)]));
                                    }
                                    NBR_ACTUAL_USER_FILES = (byte)(NBR_ACTUAL_USER_FILES + 1);
                                    ISOException.throwIt(ISO7816.SW_NO_ERROR);
                                // création du fichier: supposée faite.
                            }
                            else{
                                ISOException.throwIt((short)0x6A85); //longueur Lc non acceptée;
                            }
                        }
                        else{
                            ISOException.throwIt((short)0x6581); //impossible de faire une écriture (on a dépasser NBRE_OF_FILE à créer)
                        }
                    }
                    else{
                        ISOException.throwIt((short)0x6786); //commande n'est pas permis (pas de fichier sélectioné)
                    }
                }
                else if(USER_FILE_selected == 1){
                    found = 0;
                    for(int i=0; i<NBR_ACTUAL_USER_FILES; i++){
                        if(FF04[(byte)(i*6 + 4)] == USER_FILE_MSB && FF04[(byte)(i*6 + 5)] == USER_FILE_LSB){
                            if(USER_IC[(byte)i] == 1){
                                Data_length = buf[ISO7816.OFFSET_LC];
                                if(Data_length <= FF04[(byte)(i*6 + 0)]){
                                    if(buf[ISO7816.OFFSET_P2] == 0x04 && buf[ISO7816.OFFSET_P1] > 0x00){ //l'enregistrement idenfié par P1 sera pris en compte
                                        for(int j=0; j<Data_length; j++){
                                            //P1= 0x01(1er enregistrement) ou 0x02(2ème), etc.
                                            AA00[(byte)(Offset[(byte)i] + FF04[(byte)(i*6 + 0)]*(buf[ISO7816.OFFSET_P1]-1) + j)] = buf[(byte)(5 + j)];
                                        }
                                        ISOException.throwIt(ISO7816.SW_NO_ERROR);
                                    }
                                    else{
                                        ISOException.throwIt((short)0x6981); //commande non suportée (P1, P2)
                                    }
                                }
                                else{
                                    ISOException.throwIt((short)0x6A84); //pas assez de mémoire dans le file
                                }
                            }
                            else{
                                ISOException.throwIt((short)0x6982); //La sécurité n'est pas satisfaite
                            }
                            i = NBR_ACTUAL_USER_FILES; //forcer l'arrêt de la boucle
                            found = 1;
                        }
                    }
                    if(found == 0){
                        ISOException.throwIt((short)0x6A82); //Le file n'existe pas
                    }
                }
                else{
                    ISOException.throwIt((short)0x6982); //La sécurité n'est pas satisfaite //IC not verified
                }
            break;
            
            case ins_READ_RECORD :
                if(FF02_selected == 1){
                    if(IC_verified == 1){
                        Data_length = buf[6]; //Le //Lc=1. but no data
                        if(Data_length <= 0x01){
                            buf[0] = FF02[0];
                            apdu.setOutgoingAndSend((short)0, (short)Data_length);
                        }
                        else{
                            ISOException.throwIt((short)0x6700); //Le: erroné
                        }
                    }
                    else{
                        ISOException.throwIt((short)0x6982); //La sécurité n'est pas satisfaite
                    }
                }
                
                else if(PIN_verified == 1){
                    if(FF03_selected == 1){
                        Data_length = buf[6]; //Le //Lc=1. but no data
                        if(Data_length <= 0x08){
                            for(int i=0; i<Data_length; i++){
                                buf[(byte)i] = FF03[(byte)i];
                            }
                            apdu.setOutgoingAndSend((short)0, (short)Data_length);
                        }
                        else{
                            ISOException.throwIt((short)0x6700); //Le: erroné
                        }
                    }
                    else if(FF04_selected == 1){ //Lc doit être égale à 1, et Data = index
                        Data_length = buf[6]; //Le
                        if(Data_length <= 0x06){
                            index = buf[5]; //index goes from 0 to 29
                            if(index <= 0x29 && index >= 0x00 && (index+1) <= NBR_ACTUAL_USER_FILES){ //indice d'enregistrement
                                for(int i=0; i<Data_length; i++){
                                    buf[(byte)i] = FF04[(byte)((index)*6 + i)];
                                }
                                apdu.setOutgoingAndSend((short)0, (short)Data_length);
                            }
                            else{
                                ISOException.throwIt((short)0x6A83); //L'enregistrement n'existe pas
                            }
                        }
                        else{
                            ISOException.throwIt((short)0x6700); //Le: erroné
                        }
                    }
                }
                    
                else if(USER_FILE_selected == 1){
                    found = 0;
                    for(int i=0; i<NBR_ACTUAL_USER_FILES; i++){
                        if(FF04[(byte)(i*6 + 4)] == USER_FILE_MSB && FF04[(byte)(i*6 + 5)] == USER_FILE_LSB){
                            if(USER_PIN[(byte)i] == 1){
                                Data_length = buf[6]; // Le //Lc=1. but no data
                                if(Data_length <= FF04[(byte)(i*6 + 0)]){
                                    if(buf[ISO7816.OFFSET_P2] == 0x04 && buf[ISO7816.OFFSET_P1] > 0x00){ //l'enregistrement idenfié par P1 sera pris en compte
                                        for(int j=0; j<Data_length; j++){
                                            buf[(byte)j] = AA00[(byte)(Offset[(byte)i] + FF04[(byte)(i*6 + 0)]*(buf[ISO7816.OFFSET_P1]-1) + j)];
                                        }
                                        apdu.setOutgoingAndSend((short)0,(short)Data_length);
                                    }
                                    else{
                                        ISOException.throwIt((short)0x6981); //commande non suportée (P1, P2)
                                    }
                                }
                                else{
                                    ISOException.throwIt((short)0x6700); //Le: erroné
                                }
                            }
                            else{
                                ISOException.throwIt((short)0x6982); //La sécurité n'est pas satisfaite
                            }
                            i = NBR_ACTUAL_USER_FILES; //forcer l'arrêt de la boucle
                            found = 1;
                        }
                    }
                    if(found == 0){
                        ISOException.throwIt((short)0x6A82); //Le file n'existe pas
                    }
                }
                else{
                    ISOException.throwIt((short)0x6982); //La sécurité n'est pas satisfaite //PIN not verified
                }
            break;
            
            case ins_CLEAR_CARD :
                if(IC_verified == 1){
                    FF02[0] = (byte)0x00;
                    FF03[0]=(byte)0xAA; FF03[1]=(byte)0xBB; FF03[2]=(byte)0xCC; FF03[3]=(byte)0xDD; //valeur initiale
                    FF03[4]=(byte)0x00; FF03[5]=(byte)0xBC; FF03[6]=(byte)0x61; FF03[7]=(byte)0x4E; //valeur initiale
                    for(int i=0; i<NBR_ACTUAL_USER_FILES*6 ; i++){
                        FF04[(byte)i] = (byte)0x00;
                        Offset[(byte)i] = (byte)0x00;
                    }
                    for(int i=0; i<NBR_ACTUAL_USER_FILES*10 ; i++){
                        AA00[(byte)i] = 0;
                    }
                    IC_verified = 0;
                    PIN_verified = 0;
                    for(int i=0; i<NBR_ACTUAL_USER_FILES ; i++){
                        USER_IC[(byte)i] = 0;
                        USER_PIN[(byte)i] = 0;
                    }
                    NBR_ACTUAL_USER_FILES = 0;
                }
                else{
                    ISOException.throwIt((short)0x6982); //La sécurité n'est pas satisfaite //PIN not verified
                }
            break;
            
            default : ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }
}
