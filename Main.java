import Peer.Peer;
public class Main {

    public static void main(String[] args) {
        Peer A = new Peer();
        Peer B = new Peer();

        System.out.println("Starting peer A");
        A.start_peer();
        System.out.println("Starting peer B");
        B.start_peer();
    }
}
