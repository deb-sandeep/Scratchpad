package com.sandy.scratchpad.jn.math;

import java.text.DecimalFormat;

public class NumToWordsIndian {

  private static final String[] tensNames = {
    "",
    " ten",
    " twenty",
    " thirty",
    " forty",
    " fifty",
    " sixty",
    " seventy",
    " eighty",
    " ninety"
  };

  private static final String[] numNames = {
    "",
    " one",
    " two",
    " three",
    " four",
    " five",
    " six",
    " seven",
    " eight",
    " nine",
    " ten",
    " eleven",
    " twelve",
    " thirteen",
    " fourteen",
    " fifteen",
    " sixteen",
    " seventeen",
    " eighteen",
    " nineteen"
  };

  private NumToWordsIndian() {}

  private static String convertLessThanOneThousand(int number) {
    String soFar;

    if (number % 100 < 20){
      soFar = numNames[number % 100];
      number /= 100;
    }
    else {
      soFar = numNames[number % 10];
      number /= 10;

      soFar = tensNames[number % 10] + soFar;
      number /= 10;
    }
    if (number == 0) return soFar;
    return numNames[number] + " hundred" + soFar;
  }


  public static String convert(long number) {
    // 0 to 999 99 99 99
    if (number == 0) { return "zero"; }

    String snumber = Long.toString(number);

    // pad with "0"
    String mask = "000000000";
    DecimalFormat df = new DecimalFormat(mask);
    snumber = df.format(number);

    // XX nn nn nnn
    int crores = Integer.parseInt(snumber.substring(0,2));
    // nn XX nn nnn
    int lakhs  = Integer.parseInt(snumber.substring(2,4));
    // nn nn XX nnn
    int tenThousand = Integer.parseInt(snumber.substring(4,6));
    // nn nn nn XXX
    int thousands = Integer.parseInt(snumber.substring(6,9));

    String tradCrores;
    switch (crores) {
    case 0:
      tradCrores = "";
      break;
    case 1 :
      tradCrores = convertLessThanOneThousand(crores)
      + " crore ";
      break;
    default :
      tradCrores = convertLessThanOneThousand(crores)
      + " crore ";
    }
    String result =  tradCrores;

    String tradLakhs;
    switch (lakhs) {
    case 0:
      tradLakhs = "";
      break;
    case 1 :
      tradLakhs = convertLessThanOneThousand(lakhs)
         + " lakh ";
      break;
    default :
      tradLakhs = convertLessThanOneThousand(lakhs)
         + " lakh ";
    }
    result =  result + tradLakhs;

    String tradTenThousand;
    switch (tenThousand) {
    case 0:
      tradTenThousand = "";
      break;
    case 1 :
      tradTenThousand = "one thousand ";
      break;
    default :
      tradTenThousand = convertLessThanOneThousand(tenThousand)
         + " thousand ";
    }
    result =  result + tradTenThousand;

    String tradThousand;
    tradThousand = convertLessThanOneThousand(thousands);
    result =  result + tradThousand;

    // remove extra spaces!
    return result.replaceAll("^\\s+", "").replaceAll("\\b\\s{2,}\\b", " ");
  }

  /**
   * testing
   * @param args
   */
  public static void main(String[] args) {
    System.out.println("*** " + NumToWordsIndian.convert(0));
    System.out.println("*** " + NumToWordsIndian.convert(1));
    System.out.println("*** " + NumToWordsIndian.convert(16));
    System.out.println("*** " + NumToWordsIndian.convert(100));
    System.out.println("*** " + NumToWordsIndian.convert(118));
    System.out.println("*** " + NumToWordsIndian.convert(200));
    System.out.println("*** " + NumToWordsIndian.convert(219));
    System.out.println("*** " + NumToWordsIndian.convert(800));
    System.out.println("*** " + NumToWordsIndian.convert(801));
    System.out.println("*** " + NumToWordsIndian.convert(1316));
    System.out.println("*** " + NumToWordsIndian.convert(1000000));
    System.out.println("*** " + NumToWordsIndian.convert(2000000));
    System.out.println("*** " + NumToWordsIndian.convert(3000200));
    System.out.println("*** " + NumToWordsIndian.convert(700000));
    System.out.println("*** " + NumToWordsIndian.convert(9000000));
    System.out.println("*** " + NumToWordsIndian.convert(9001000));
    System.out.println("*** " + NumToWordsIndian.convert(23456789));
    System.out.println("*** " + NumToWordsIndian.convert(47483647));

    /*
     *** zero
     *** one
     *** sixteen
     *** one hundred
     *** one hundred eighteen
     *** two hundred
     *** two hundred nineteen
     *** eight hundred
     *** eight hundred one
     *** one thousand three hundred sixteen
     *** one million
     *** two millions
     *** three millions two hundred
     *** seven hundred thousand
     *** nine millions
     *** nine millions one thousand
     *** one hundred twenty three millions four hundred
     **      fifty six thousand seven hundred eighty nine
     *** two billion one hundred forty seven millions
     **      four hundred eighty three thousand six hundred forty seven
     *** three billion ten
     **/
  }
}