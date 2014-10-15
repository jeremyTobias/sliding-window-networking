import java.net.DatagramPacket;
import java.net.InetAddress;

public class DataPacket extends TftpPacket {
	public static final int TYPE = 3; // TFTP packet type
	public static final int MAX_DATA = 512; // max data in bytes
	public static final int MIN_DATA = 0; // min data in bytes
	public static final int MAX_BLOCKNUMBER = 65535; // largest assignable block number

	private int blockNumber;
	private byte[] payload;
	private int payloadLength;

	public DataPacket(InetAddress addr, int port, int type, int blkNum, byte[] payload) {
		super(addr, port, type);

		this.blockNumber = blkNum;
		this.payload = payload;
		this.payloadLength = payload.length;
	}

	public DataPacket(DatagramPacket dPack, int type) {
		super(dPack, type);

		byte[] pkt_data = dPack.getData();

		this.blockNumber = (((pkt_data[2] & 0xff) << 8) | (pkt_data[3] & 0xff));
		this.payloadLength = dPack.getLength() - 4;
		this.payload = new byte[this.payloadLength];

		System.arraycopy(pkt_data, 4, this.payload, 0, this.payloadLength);
	}

	// generate a Data packet
	@Override
	public DatagramPacket getDatagramPacket() {
		byte[] pkt_data = new byte[this.payloadLength + 4];

		// set opcode
		pkt_data[0] = 0;
		pkt_data[1] = (byte)this.getType();

		// set block number
		pkt_data[2] = (byte)((this.blockNumber & 0xffff) >> 8);
		pkt_data[3] = (byte)(this.blockNumber & 0xff);

		// set data
		System.arraycopy(payload, 0, pkt_data, 4, this.payloadLength);

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

	// get payload
	public byte[] getPayload() {
		return payload;
	}

	// set payload
	public void setPaylod(byte[] payload) {
		this.payload = payload;
	}

	// get payload length
	public int getPayloadLength() {
		return payloadLength;
	}

	// set payload length
	public void setPayloadLength(int payloadLength) {
		this.payloadLength = payloadLength;
	}
}