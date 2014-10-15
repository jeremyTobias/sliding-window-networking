import java.net.DatagramPacket;
import java.net.InetAddress;

public class ErrPacket extends TftpPacket {
	public static final int TYPE = 5; // TFTP packet type

	/*
	* Error Codes
	*/
	public static final int UNDEFINED = 0;
	public static final int FILE_NOT_FOUND = 1;
	public static final int ACCESS_VIOLATION = 2;
	public static final int DISK_FULL = 3;
	public static final int ILLEGAL_OPERATION = 4;
	public static final int UNKNOWN_TID = 5;
	public static final int FILE_ALREADY_EXITS = 6;
	public static final int NO_SUCH_USER = 7;

	private int errCode;

	private String errMsg;

	public ErrPacket(InetAddress addr, int port, int type, int errCode, String errMsg) {
		super(addr, port, type);

		this.errCode = errCode;
		this.errMsg = errMsg;
	}

	public ErrPacket(DatagramPacket dPack, int type) {
		super(dPack, type);

		byte[] pkt_data = dPack.getData();

		this.errCode = (((pkt_data[2] & 0xff) << 8) | (pkt_data[3] & 0xff));

		StringBuffer strBuffer = new StringBuffer();	
		
		for (int i = 4; i < dPack.getLength(); i++) {
			if (pkt_data[i] == 0) break;
			strBuffer.append((char)pkt_data[i]);
		}

		this.errMsg = strBuffer.toString();
	}

	// generate Error packet
	@Override
	public DatagramPacket getDatagramPacket() {
		byte[] pkt_data = new byte[errMsg.length() + 5];

		// set opcode
		pkt_data[0] = 0;
		pkt_data[1] = (byte)this.getType();

		// set error code
		pkt_data[2] = (byte)((this.errCode & 0xffff) >> 8);
		pkt_data[3] = (byte)(this.errCode & 0xff);

		// set error message
		System.arraycopy(errMsg.getBytes(), 0, pkt_data, 4, errMsg.getBytes().length);

		return new DatagramPacket(pkt_data, pkt_data.length, this.getAddress(), this.getPort());
	}
}