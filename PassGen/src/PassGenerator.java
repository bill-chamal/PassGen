import java.util.LinkedList;
import java.util.Random;

public class PassGenerator {
    private Random rand = new Random();
    private int length, bound = 0;
    private boolean upperCase, numbers, symbols, lowercase;
    private LinkedList<RandSymbol> randsymbList = new LinkedList<RandSymbol>();

    public PassGenerator(int length, boolean lowercase, boolean uppcase, boolean numbers, boolean symbols) {
        this.length = length;
        this.lowercase = lowercase;
        this.upperCase = uppcase;
        this.numbers = numbers;
        this.symbols = symbols;
        createList();
    }

    private void createList() {
        if (lowercase) {
            randsymbList.add(new RandSymbol(97, 122));
            bound++;
        }
        if (upperCase) {
            randsymbList.add(new RandSymbol(65, 90));
            bound++;
        }
        if (numbers) {
            randsymbList.add(new RandSymbol(48, 57));
            bound++;
        }
        if (symbols) {
            randsymbList.add(new RandSymbol(33, 47));
            randsymbList.getLast().addBound(58,64);
            randsymbList.getLast().addBound(91,95);
            bound++;
        }
    }

    public String getRandCharacter() {
        if (!randsymbList.isEmpty())
            return randsymbList.get(rand.nextInt(bound)).getRandSymbol(); // Equal possibilities
        else
            return "";
    }

    public String getPassword() {
        StringBuilder passwd = new StringBuilder();
        for (int i = 0; i < length; i++)
            passwd.append(getRandCharacter());
        return passwd.toString();
    }

    private class RandSymbol {
        // In the LinkedList we have an array integer to save the low and high limit
        private LinkedList<Integer[]> range = new LinkedList<>();

        private RandSymbol(int low, int high) {
            range.add(new Integer[]{low, high});
        }

        public void addBound(int low, int high){
            range.add(new Integer[]{low, high});
        }

        public String getRandSymbol() {
            int index = rand.nextInt(range.size());
            // get a random number between 2 numbers
            char ch = (char)(rand.nextInt(range.get(index)[1] - range.get(index)[0]) + range.get(index)[0]);
            return Character.toString(ch);
        }
    }
}
