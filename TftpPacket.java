import java.net.DatagramPacket;
import java.net.InetAddress;

public abstract class TftpPacket {
	/*
	* Max TFTP packet size - 516 bytes
	* 2 bytes - Opcode
	* 2 bytes - misc. (Block Numbers for Ack and Data; Error Code for Error; Separators for RRQ & WRQ)
	* 512 bytes - "data"
	*/
	public static final int PACKET_SIZE = 516;

	/*
	* Opcodes
	*/
	public static final int READ_REQUEST_PACKET = 1;
	//public static final int WRITE_REQUEST_PACKET = 2; // not used in this assignment	
	public static final int DATA_PACKET = 3;
	public static final int ACK_PACKET = 4;
	public static final int ERROR_PACKET = 5;

	private int pkt_type; // type of TFTP packet
	private int port; // TFTP packet port number

	private InetAddress addr; // TFTP packet address

	public TftpPacket(InetAddress addr, int port, int type) {
		pkt_type = type;
		this.port = port;
		this.addr = addr;
	}

	public TftpPacket(DatagramPacket dPack, int type) {
		pkt_type = type;
		this.port = dPack.getPort();
		this.addr = dPack.getAddress();
	}

	// generates a TFTP packet of a specific type
	public final static TftpPacket generatePacket(DatagramPacket dPack) {
		TftpPacket genPack = null;

		byte[] pkt_data = dPack.getData();
		byte opCode = pkt_data[1];

		if (opCode == READ_REQUEST_PACKET) {
			genPack = new RrqPacket(dPack, opCode);
		} else if (opCode == DATA_PACKET) {
			genPack = new DataPacket(dPack, opCode);
		} else if (opCode == ACK_PACKET) {
			genPack = new AckPacket(dPack, opCode);
		} else if (opCode == ERROR_PACKET) {
			genPack = new ErrPacket(dPack, opCode);
		}

		return genPack;
	}

	// will generate a Datagram Packet for a given type of TFTP packet
	public abstract DatagramPacket getDatagramPacket();

	// get packet type
	public int getType() {
		return pkt_type;
	}

	// get packet port number
	public int getPort() {
		return port;
	}

	// set packet port number
	public void setPort(int port) {
		this.port = port;
	}

	// get packet address
	public InetAddress getAddress() {
		return addr;
	}

	// set packet address
	public void setAddress(InetAddress addr) {
		this.addr = addr;
	}
}