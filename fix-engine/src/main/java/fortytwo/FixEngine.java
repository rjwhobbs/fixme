package fortytwo;

public class FixEngine {

  public static int sum(byte[] arr) {
    int sum = 0;

    for (int i = 0; i < arr.length; i++) {
      sum += arr[i];
    }

    return sum;
  }

  public static byte[] insertSeparator(byte[] arr) {

    for (int i = 0; i < arr.length; i++) {
      if (arr[i] == '|') {
        arr[i] = 1;
      }
    }
    return arr;
  }

  public String insertCheckSum(String message) {
//     "24242=1|35=V|";
    String checkSumString;
    int checkSum;
    int originalTotal;

    originalTotal = this.sum(this.insertSeparator(message.getBytes()));
    checkSum = originalTotal % 256;
    checkSumString = Integer.toString(checkSum);

    switch (checkSumString.length()) {
      case 1:
        checkSumString = "00" + checkSumString;
        break;
      case 2:
        checkSumString = "0" + checkSumString;
        break;
      default:
        break;
    }

    return checkSumString;
  }

  public void printRawBytes() {
  }

  public static void testOne() {
    System.out.println("Hello form engine test one");
  }

  public void testTwo() {
    System.out.println("Hello form engine test two");
  }
}

class TestEngine {
  public static void main(String[] args) {
    FixEngine fe = new FixEngine();
    System.out.println(fe.insertCheckSum("24242=1|35=V|"));
  }
}