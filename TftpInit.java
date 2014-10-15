import java.io.IOException;
import java.net.*;
import java.util.Random;

public class TftpInit {
	private DatagramSocket dSock;

	public TftpInit() throws SocketException {
		dSock = new DatagramSocket();
	}

	public TftpInit(int port, String host) throws SocketException, UnknownHostException {
		dSock = new DatagramSocket(port, InetAddress.getByName(host));
	}

	// receive a datagram packet and turn it into a TFTP packet
	public TftpPacket receive(int timeout) throws SocketTimeoutException, IOException {
		// create new datagram packet
		DatagramPacket dPack = new DatagramPacket(new byte[TftpPacket.PACKET_SIZE], TftpPacket.PACKET_SIZE);
		
		dSock.setSoTimeout(timeout); // initialize timeout
		dSock.receive(dPack); // try to receive packet
		dSock.setSoTimeout(0); // reset timeout to infinity to avoid timeout exception

		// create a new TFTP packet and return it
		return TftpPacket.generatePacket(dPack);
	}


	/*
	* Send a TFTP packet
	* If dropSim is TRUE, generate a random number to simulate dropping packets 1% of the time
	* Else send as normal
	*/
	public void send(TftpPacket tPack, boolean dropSim) throws IOException {
		int num = 50;
		int drop = -1;

		if (dropSim) { // dropSim used, generate a random number
			Random randNum = new Random();
			drop = randNum.nextInt(99);
		}

		if (num != drop) {
			dSock.send(tPack.getDatagramPacket()); // generate a datagram packet and send
		} else {
			System.out.println("Dropping packet.");
		}
	}

	// close connection
	public void close() {
		dSock.close();
	}
}