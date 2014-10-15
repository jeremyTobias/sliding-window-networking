import java.net.DatagramPacket;
import java.net.InetAddress;

public class AckPacket extends TftpPacket {
	public static final int TYPE = 4; // TFTP packet type

	private int blockNumber;

	public AckPacket(InetAddress addr, int port, int type, int blkNum) {
		super(addr, port, type);

		this.blockNumber = blkNum;
	}

	public AckPacket(DatagramPacket dPack, int type) {
		super(dPack, type);

		byte[] pkt_data = dPack.getData();

		this.blockNumber = (((pkt_data[2] & 0xff) << 8) | (pkt_data[3] & 0xff));
	}

	// generate Ack packet
	@Override
	public DatagramPacket getDatagramPacket() {
		byte[] pkt_data = new byte[4];

		// set opcode
		pkt_data[0] = 0;
		pkt_data[1] = (byte)this.getType();
		
		// set block number
		pkt_data[2] = (byte)((this.blockNumber & 0xffff) >> 8);
		pkt_data[3] = (byte)(this.blockNumber & 0xff);

		return new DatagramPacket(pkt_data, pkt_data.length, this.getAddress(), this.getPort());
	}

	// get block number
	public int getBlockNumber() {
		return blockNumber;
	}

	// set block number
	public void setBlockNumber(int blockNumber) {
		this.blockNumber = blockNumber;
	}
}