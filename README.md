# JavaCard Applet Inspired By ACOS3 Card Of Company ACS

<div style="text-align:center"><img src="https://i.ibb.co/PYtDMKN/image1.png" /></div>

> This is an academic project, which aims to customise a JacaCard applet to perform functionalities for a specific use case
---

### Table of Contents

- [Description](#description)
- [Specifications](#specifications)
- [Files Management](#files-management)
- [Instructions Management](#instructions-management)
- [Project Files](#project-files)
- [Author Info](#author-info)

---

## Description

The objective of this project is to create an applet inspired by the ACOS3 card from the company ACS.

The applet must support a set of instructions described in the [specifications](#specifications) and manage the application files, while taking into account the different exceptions and special cases of use.

In general, a JavaCard technology architecture is presented as follows :

<div style="text-align:center"><img src="https://i.ibb.co/c2d2GZd/image2.png" /></div>

---

## Specifications

1. The applet contains a single application/class. CLA = 0x80

2. Give access to the three highest priority files :

    - **FF02**, which contains a byte (*NBRE_OF_FILE*), with the attribute "IC code" (for reading and writing)
    - **FF03**, which contains 8 bytes (IC code and PIN code)
    - **FF04**, which contains 6 * 30 bytes (30 is the maximum number of files authorized to create), the six bytes have the same structure as that of the ACOS3 card. (see the following image)

<div style="text-align:center"><img src="https://i.ibb.co/3RkKVMw/image3.png" /></div>

3. The applet must accept the following statements: **SELECT FILE**, **READ_RECORD**, **WRITE_RECORD**, **SUBMIT_CODE**, **CLEAR_CARD**

---

## Files Management

| File | Statement | Associated Variables |
| --- | --- | --- |
| `FF02` | public static byte [] FF02 = {(byte)0x00}; | FF02_selected |
| `FF03` | public static byte [] FF03 = {(byte)0xAA, (byte)0xBB, (byte)0xCC, (byte)0xDD, (byte)0x00, (byte)0xBC, (byte)0x61, (byte)0x4E}; *// default IC code and PIN code* | FF03_selected |
| `FF04` | public static byte [] FF04 = new byte[30*6]; | FF04_selected |
| `AA00` | public static byte [] AA00 = new byte[1024]; *// a transparent file that contains all user files to be created* | USER_FILE_selected |

---

## Instructions Management

<table>
    <thead>
        <tr>
            <th>Instruction</th>
            <th>Statement (Type: public static final byte)</th>
            <th colspan=7>Corresponding APDU Command</th>
        </tr>
        <tr>
            <th></th>
            <th></th>
            <th>CLA</th>
            <th>INS</th>
            <th>P1</th>
            <th>P2</th>
            <th>Lc</th>
            <th>Data</th>
            <th>Le</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>SELECT_FILE</td>
            <td>ins_SELECT_FILE = (byte) 0xA4;</td>
            <td>0x80</td>
            <td>0xA4</td>
            <td>0x00</td>
            <td>0x00</td>
            <td>0x02</td>
            <td>MSB and LSB parts <i>(file name)</i></td>
            <td>0x00</td>
        </tr>
        <tr>
            <td>READ_RECORD</td>
            <td>ins_READ_RECORD = (byte) 0xB2;</td>
            <td>0x80</td>
            <td>0xB2</td>
            <td>0x00</td>
            <td>0x00</td>
            <td>0x01</td>
            <td>0x00</td>
            <td>Number of bytes to read</td>
        </tr>
        <tr>
            <td>WRITE_RECORD</td>
            <td>ins_WRITE_RECORD = (byte) 0xD2;</td>
            <td>0x80</td>
            <td>0xD2</td>
            <td>0x00</td>
            <td>0x00</td>
            <td>Number of bytes to write</td>
            <td>Bytes to be written in the selected file</td>
            <td>0x00</td>
        </tr>
        <tr>
            <td>SUBMIT_CODE</td>
            <td>ins_SUBMIT_CODE = (byte) 0x20;</td>
            <td>0x80</td>
            <td>0x20</td>
            <td>0x00</td>
            <td>0x00</td>
            <td>0x08 <b>or</b> 0x00</td>
            <td>IC code and OIN code</td>
            <td>0x00</td>
        </tr>
        <tr>
            <td>CLEAR_CARD</td>
            <td>ins_CLEAR_CARD = (byte) 0x30;</td>
            <td>0x80</td>
            <td>0x30</td>
            <td>0x00</td>
            <td>0x00</td>
            <td>0x00</td>
            <td>void</td>
            <td>0x00</td>
        </tr>
    </tbody>
</table>

**Notes :* 

1. **MSB** and **LMB** stand respectively for the **M**ost **S**ignificant **B**it and the **L**east **S**ignificant **B**it

2. For the instruction **SUBMIT_CODE**, **Lc** take :

    - 0x08 : if you want to submit the administrator code (which gives access to the files **FF02**, **FF03** and **FF04**)
    - 0x02 : if you want to submit the code for the use of newly created user files

---

## Project Files

The project contains two files :

1. JavaCard_Applet.java : contains the code correspending to the applet
2. Testing_Script.scr : contains a script that describes a specific scenario for the use of the developed applet. Mainly compesed of APDU commands. The results will be visible in the output window. This is alowing us to test our applet and check the consistency of the APDU responses for each APDU command

---

## Author Info

- Email - oussama.oulkaid@gmail.com
- LinkedIn - [Oussama Oulkaid](https://www.linkedin.com/in/oulkaid)
