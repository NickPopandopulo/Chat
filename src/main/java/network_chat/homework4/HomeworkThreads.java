package network_chat.homework4;

public class HomeworkThreads {

    private static final Object lock = new Object();
    private static final char[] chars = {'A', 'B', 'C', 'D', 'E'};
    private static final int amountRepeat = 5;
    private static int currentIndex = 0;

    public static void main(String[] args) {
        for (int i = 0; i < chars.length; i++) {
            int indexOfChars = i;
            new Thread(() -> print(chars[indexOfChars])).start();
        }
    }

    private static void print(char printedLetter) {
        synchronized (lock) {
            for (int i = 0; i < amountRepeat; i++) {
                while (printedLetter != chars[currentIndex]) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                System.out.print(printedLetter);
                if (++currentIndex == chars.length) { // сначала увеличивается индекс, потом проверяется вышли ли за пределы массива chars
                    currentIndex = 0;                 // если вышли за пределы, то возвращаемся к началу массива
                    System.out.print('|');            // для красоты или наглядности
                }
                lock.notifyAll();
            }
        }
    }

}

